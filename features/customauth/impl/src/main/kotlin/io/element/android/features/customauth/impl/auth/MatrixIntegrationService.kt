package io.element.android.features.customauth.impl.auth

import android.content.Context
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.auth.external.ExternalSession
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

interface MatrixIntegrationService {
    suspend fun createMatrixAccount(
        username: String,
        password: String,
        displayName: String,
    ): Result<MatrixAccountResult>

    fun generateMatrixPassword(): String

    fun formatMatrixUsername(username: String): String
}

data class MatrixAccountResult(
    val matrixUserId: String,
    val matrixUsername: String,
    val matrixPassword: String,
    val sessionId: SessionId,
)

@Serializable
data class MatrixRegisterRequest(
    val username: String,
    val password: String,
    val initial_device_display_name: String? = null,
    val auth: MatrixAuthData? = null,
)

@Serializable
data class MatrixAuthData(
    val type: String,
    val session: String? = null,
    val token: String? = null,
)

@Serializable
data class MatrixRegisterResponse(
    val user_id: String,
    val access_token: String,
    val device_id: String,
    val home_server: String? = null,
)

@Serializable
data class MatrixRegisterFlowResponse(
    val session: String,
    val flows: List<MatrixFlow>,
    val params: Map<String, Any> = emptyMap(),
    val completed: List<String> = emptyList(),
)

@Serializable
data class MatrixFlow(
    val stages: List<String>,
)

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DefaultMatrixIntegrationService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val matrixAuthenticationService: MatrixAuthenticationService,
    ) : MatrixIntegrationService {
        companion object {
            private const val MATRIX_HOMESERVER_URL = "https://signout.io"
            private const val PASSWORD_LENGTH = 16
            private const val PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
            private const val REGISTRATION_TOKEN = "healthcare_signup_2024"
        }

        private val httpClient = OkHttpClient()
        private val json = Json { ignoreUnknownKeys = true }

        override suspend fun createMatrixAccount(
            username: String,
            password: String,
            displayName: String,
        ): Result<MatrixAccountResult> {
            return try {
                Timber.d("Creating/logging into Matrix account for username: $username")
                
                // Set the homeserver first
                val homeserverResult = matrixAuthenticationService.setHomeserver(MATRIX_HOMESERVER_URL)
                if (homeserverResult.isFailure) {
                    Timber.e("Failed to set homeserver: ${homeserverResult.exceptionOrNull()?.message}")
                    return Result.failure(Exception("Failed to set homeserver: ${homeserverResult.exceptionOrNull()?.message}"))
                }
                
                // Format the Matrix username (should be just the local part for registration)
                val matrixUsername = formatMatrixUsername(username)
                val fullMatrixUserId = "@$matrixUsername:signout.io"
                
                Timber.d("Formatted Matrix username: $matrixUsername, Full user ID: $fullMatrixUserId")
                
                // Try to login first (for existing users), then register if needed
                val sessionId = try {
                    Timber.d("Attempting login for existing Matrix user: $matrixUsername")
                    val loginResult = matrixAuthenticationService.login(matrixUsername, password)
                    if (loginResult.isFailure) {
                        Timber.d("Login failed: ${loginResult.exceptionOrNull()?.message}")
                        throw loginResult.exceptionOrNull() ?: Exception("Login failed")
                    }
                    loginResult.getOrThrow()
                } catch (loginException: Exception) {
                    Timber.d("Login failed, attempting registration: ${loginException.message}")
                    
                    // Register the user with the Matrix homeserver
                    try {
                        registerMatrixUser(matrixUsername, password, displayName)
                    } catch (registrationException: Exception) {
                        Timber.e(registrationException, "Matrix registration failed")
                        throw Exception("Matrix registration failed: ${registrationException.message}", registrationException)
                    }
                }
                
                Timber.d("Matrix account creation/login successful for user: $matrixUsername")
                
                Result.success(
                    MatrixAccountResult(
                        matrixUserId = fullMatrixUserId,
                        matrixUsername = matrixUsername,
                        matrixPassword = password,
                        sessionId = sessionId
                    )
                )
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to create/login to Matrix account")
                Result.failure(exception)
            }
        }

        private suspend fun registerMatrixUser(
            username: String,
            password: String,
            displayName: String,
        ): SessionId =
            withContext(Dispatchers.IO) {
                try {
                    Timber.d("Starting Matrix user registration for username: $username")
                    
                    // Step 1: Get registration flow
                    val registerUrl = "$MATRIX_HOMESERVER_URL/_matrix/client/v3/register"
                    Timber.d("Registration URL: $registerUrl")

                    val initialRequest =
                        Request.Builder()
                            .url(registerUrl)
                            .post("{}".toRequestBody("application/json".toMediaType()))
                            .build()

                    val initialResponse = httpClient.newCall(initialRequest).execute()
                    val responseBody = initialResponse.body?.string() ?: ""
                    
                    Timber.d("Matrix registration flow response code: ${initialResponse.code}")
                    Timber.d("Matrix registration flow response: $responseBody")
                    
                    if (!initialResponse.isSuccessful) {
                        Timber.e("Failed to get registration flow: ${initialResponse.code} ${initialResponse.message}")
                        throw Exception("Failed to get registration flow: ${initialResponse.code} ${initialResponse.message} - $responseBody")
                    }

                    val flowResponse = try {
                        json.decodeFromString<MatrixRegisterFlowResponse>(responseBody)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse registration flow response: $responseBody")
                        throw Exception("Invalid registration flow response from server: ${e.message}")
                    }

                    Timber.d("Registration flow parsed successfully. Session: ${flowResponse.session}")
                    Timber.d("Available flows: ${flowResponse.flows.map { it.stages }}")

                    // Step 2: First stage - registration token authentication
                    val tokenRequest =
                        MatrixRegisterRequest(
                            username = username,
                            password = password,
                            initial_device_display_name = "Element X Android - $displayName",
                            auth =
                                MatrixAuthData(
                                    type = "m.login.registration_token",
                                    session = flowResponse.session,
                                    token = REGISTRATION_TOKEN,
                                ),
                        )

                    val tokenRequestBody =
                        json.encodeToString(tokenRequest)
                            .toRequestBody("application/json".toMediaType())

                    Timber.d("Matrix token registration request: ${json.encodeToString(tokenRequest)}")

                    val tokenRequestHttp =
                        Request.Builder()
                            .url(registerUrl)
                            .post(tokenRequestBody)
                            .build()

                    val tokenResponse = httpClient.newCall(tokenRequestHttp).execute()
                    val tokenResponseBody = tokenResponse.body?.string() ?: ""
                    
                    Timber.d("Matrix token registration response code: ${tokenResponse.code}")
                    Timber.d("Matrix token registration response: $tokenResponseBody")

                    if (!tokenResponse.isSuccessful) {
                        Timber.e("Token registration failed: ${tokenResponse.code} ${tokenResponse.message}")
                        throw Exception("Token registration failed: ${tokenResponse.code} ${tokenResponse.message} - $tokenResponseBody")
                    }

                    // Parse the intermediate response (should show token stage completed)
                    val tokenFlowResponse = try {
                        json.decodeFromString<MatrixRegisterFlowResponse>(tokenResponseBody)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse token registration response: $tokenResponseBody")
                        throw Exception("Invalid token registration response from server: ${e.message}")
                    }

                    Timber.d("Token registration successful. Completed stages: ${tokenFlowResponse.completed}")

                    // Step 3: Second stage - dummy authentication to complete registration
                    val dummyRequest =
                        MatrixRegisterRequest(
                            username = username,
                            password = password,
                            initial_device_display_name = "Element X Android - $displayName",
                            auth =
                                MatrixAuthData(
                                    type = "m.login.dummy",
                                    session = tokenFlowResponse.session,
                                ),
                        )

                    val dummyRequestBody =
                        json.encodeToString(dummyRequest)
                            .toRequestBody("application/json".toMediaType())

                    Timber.d("Matrix dummy registration request: ${json.encodeToString(dummyRequest)}")

                    val dummyRequestHttp =
                        Request.Builder()
                            .url(registerUrl)
                            .post(dummyRequestBody)
                            .build()

                    val registerResponse = httpClient.newCall(dummyRequestHttp).execute()
                    val registerResponseBody = registerResponse.body?.string() ?: ""
                    
                    Timber.d("Matrix final registration response code: ${registerResponse.code}")
                    Timber.d("Matrix final registration response: $registerResponseBody")

                    if (!registerResponse.isSuccessful) {
                        Timber.e("Final registration failed: ${registerResponse.code} ${registerResponse.message}")
                        throw Exception("Final registration failed: ${registerResponse.code} ${registerResponse.message} - $registerResponseBody")
                    }

                    val registerResult = try {
                        json.decodeFromString<MatrixRegisterResponse>(registerResponseBody)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse final registration response: $registerResponseBody")
                        throw Exception("Invalid final registration response from server: ${e.message}")
                    }

                    Timber.d("Matrix registration successful. User ID: ${registerResult.user_id}")
                    Timber.d("Access token length: ${registerResult.access_token.length}")
                    Timber.d("Device ID: ${registerResult.device_id}")

                    // Step 3: Create external session and import it
                    val externalSession =
                        ExternalSession(
                            userId = registerResult.user_id,
                            homeserverUrl = MATRIX_HOMESERVER_URL,
                            accessToken = registerResult.access_token,
                            deviceId = registerResult.device_id,
                            refreshToken = null,
                            slidingSyncProxy = null,
                        )

                    Timber.d("Creating external session for import")

                    // Import the session into Element X
                    val importResult = matrixAuthenticationService.importCreatedSession(externalSession)
                    if (importResult.isFailure) {
                        Timber.e("Failed to import session: ${importResult.exceptionOrNull()?.message}")
                        throw Exception("Failed to import session: ${importResult.exceptionOrNull()?.message}")
                    }
                    
                    val sessionId = importResult.getOrThrow()
                    Timber.d("Session imported successfully. Session ID: $sessionId")
                    
                    sessionId
                } catch (e: Exception) {
                    Timber.e(e, "Matrix registration failed with exception: ${e.message}")
                    throw e
                }
            }

        override fun generateMatrixPassword(): String {
            val random = SecureRandom()
            return (1..PASSWORD_LENGTH)
                .map { PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)] }
                .joinToString("")
        }

        override fun formatMatrixUsername(username: String): String {
            // Ensure the username is valid for Matrix (lowercase, no special chars except _)
            return username.lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .take(32) // Matrix usernames have limits
        }
    } 
