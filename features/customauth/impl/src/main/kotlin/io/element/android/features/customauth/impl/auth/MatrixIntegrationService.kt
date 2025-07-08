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
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
    val params: Map<String, String> = emptyMap(),
    val completed: List<String> = emptyList(),
)

@Serializable
data class MatrixFlow(
    val stages: List<String>,
)

@Serializable
data class MatrixNonceResponse(
    val nonce: String
)

@Serializable
data class MatrixSharedSecretRegisterRequest(
    val nonce: String,
    val username: String,
    val displayname: String,
    val password: String,
    val admin: Boolean,
    val mac: String
)

@Serializable
data class MatrixSharedSecretRegisterResponse(
    val user_id: String,
    val access_token: String,
    val device_id: String,
    val home_server: String? = null
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
            // Shared secret for Matrix registration
            private const val REGISTRATION_SHARED_SECRET = "y0H*aX1tmBbCY1lg#kzl2xJ^&2OX~S7q3P36r=.AGqKH&8VHne"
            // Admin access token for password reset
            private const val ADMIN_ACCESS_TOKEN = "syt_YWRtaW4_HxOSZvzRUrQsPaiqnAZT_331KTn"
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
                
                // Try to login first (for existing users), then handle password reset or registration
                val sessionId = try {
                    Timber.d("Attempting login for existing Matrix user: $matrixUsername")
                    val loginResult = matrixAuthenticationService.login(matrixUsername, password)
                    if (loginResult.isFailure) {
                        Timber.d("Login failed: ${loginResult.exceptionOrNull()?.message}")
                        throw loginResult.exceptionOrNull() ?: Exception("Login failed")
                    }
                    loginResult.getOrThrow()
                } catch (loginException: Exception) {
                    Timber.d("Login failed, checking if user exists: ${loginException.message}")
                    
                    // Check if the error indicates wrong password vs user doesn't exist
                    val errorMessage = loginException.message?.lowercase() ?: ""
                    
                    if (errorMessage.contains("invalid username or password") || errorMessage.contains("forbidden")) {
                        // User might exist but password is wrong - try to reset password first
                        try {
                            Timber.d("Attempting to reset Matrix password for existing user: $matrixUsername")
                            resetMatrixUserPassword(matrixUsername, password)
                            
                            // Try login again with new password
                            val retryLoginResult = matrixAuthenticationService.login(matrixUsername, password)
                            if (retryLoginResult.isSuccess) {
                                Timber.d("Login successful after password reset")
                                retryLoginResult.getOrThrow()
                            } else {
                                throw retryLoginResult.exceptionOrNull() ?: Exception("Login failed after password reset")
                            }
                        } catch (resetException: Exception) {
                            Timber.d("Password reset failed, attempting registration: ${resetException.message}")
                            
                            // If password reset fails, try registration
                            try {
                                registerMatrixUser(matrixUsername, password, displayName)
                            } catch (registrationException: Exception) {
                                Timber.e(registrationException, "Matrix registration failed")
                                throw Exception("Matrix registration failed: ${registrationException.message}", registrationException)
                            }
                        }
                    } else {
                        // User doesn't exist, try registration
                        try {
                            registerMatrixUser(matrixUsername, password, displayName)
                        } catch (registrationException: Exception) {
                            Timber.e(registrationException, "Matrix registration failed")
                            throw Exception("Matrix registration failed: ${registrationException.message}", registrationException)
                        }
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
                    Timber.d("Starting Matrix user registration using shared secret for username: $username")
                    
                    // Step 1: Get a nonce from the admin API
                    val nonceUrl = "$MATRIX_HOMESERVER_URL/_synapse/admin/v1/register"
                    Timber.d("Getting nonce from: $nonceUrl")

                    val nonceRequest = Request.Builder()
                        .url(nonceUrl)
                        .get()
                        .build()

                    val nonceResponse = httpClient.newCall(nonceRequest).execute()
                    val nonceResponseBody = nonceResponse.body?.string() ?: ""
                    
                    Timber.d("Nonce response code: ${nonceResponse.code}")
                    Timber.d("Nonce response: $nonceResponseBody")
                    
                    if (!nonceResponse.isSuccessful) {
                        Timber.e("Failed to get nonce: ${nonceResponse.code} ${nonceResponse.message}")
                        throw Exception("Failed to get nonce: ${nonceResponse.code} ${nonceResponse.message} - $nonceResponseBody")
                    }

                    // Parse nonce manually to avoid serialization issues
                    val nonce = try {
                        val jsonObject = org.json.JSONObject(nonceResponseBody)
                        jsonObject.getString("nonce")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse nonce response: $nonceResponseBody")
                        throw Exception("Invalid nonce response from server: ${e.message}")
                    }
                    Timber.d("Received nonce: $nonce")

                    // Step 2: Generate HMAC-SHA1 digest
                    val mac = generateHmacSha1(nonce, username, password, admin = false, REGISTRATION_SHARED_SECRET)
                    Timber.d("Generated HMAC digest")

                    // Step 3: Register the user - create JSON manually to avoid serialization issues
                    val registerRequestJson = org.json.JSONObject().apply {
                        put("nonce", nonce)
                        put("username", username)
                        put("displayname", displayName)
                        put("password", password)
                        put("admin", false)
                        put("mac", mac)
                    }.toString()

                    val registerRequestBody = registerRequestJson.toRequestBody("application/json".toMediaType())

                    Timber.d("Matrix shared secret registration request prepared")

                    val registerHttpRequest = Request.Builder()
                        .url(nonceUrl)
                        .post(registerRequestBody)
                        .build()

                    val registerResponse = httpClient.newCall(registerHttpRequest).execute()
                    val registerResponseBody = registerResponse.body?.string() ?: ""
                    
                    Timber.d("Matrix registration response code: ${registerResponse.code}")
                    Timber.d("Matrix registration response: $registerResponseBody")

                    if (!registerResponse.isSuccessful) {
                        Timber.e("Matrix registration failed: ${registerResponse.code} ${registerResponse.message}")
                        throw Exception("Matrix registration failed: ${registerResponse.code} ${registerResponse.message} - $registerResponseBody")
                    }

                    // Parse registration response manually to avoid serialization issues
                    val (userId, accessToken, deviceId) = try {
                        val jsonObject = org.json.JSONObject(registerResponseBody)
                        Triple(
                            jsonObject.getString("user_id"),
                            jsonObject.getString("access_token"),
                            jsonObject.getString("device_id")
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse registration response: $registerResponseBody")
                        throw Exception("Invalid registration response from server: ${e.message}")
                    }

                    Timber.d("Matrix registration successful. User ID: $userId")
                    Timber.d("Access token length: ${accessToken.length}")
                    Timber.d("Device ID: $deviceId")

                    // Step 4: Create external session and import it
                    val externalSession = ExternalSession(
                        userId = userId,
                        homeserverUrl = MATRIX_HOMESERVER_URL,
                        accessToken = accessToken,
                        deviceId = deviceId,
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
        
        private fun generateHmacSha1(
            nonce: String,
            username: String,
            password: String,
            admin: Boolean,
            sharedSecret: String
        ): String {
            val adminString = if (admin) "admin" else "notadmin"
            val content = "$nonce\u0000$username\u0000$password\u0000$adminString"
            
            val keySpec = SecretKeySpec(sharedSecret.toByteArray(), "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(keySpec)
            val result = mac.doFinal(content.toByteArray())
            
            return result.joinToString("") { "%02x".format(it) }
        }

        private suspend fun resetMatrixUserPassword(
            username: String,
            newPassword: String,
        ): Unit = withContext(Dispatchers.IO) {
            try {
                Timber.d("Resetting Matrix password for user: $username")
                
                // Use the admin API to reset user password
                val resetUrl = "$MATRIX_HOMESERVER_URL/_synapse/admin/v2/users/@$username:signout.io"
                
                val resetRequestJson = org.json.JSONObject().apply {
                    put("password", newPassword)
                }.toString()
                
                val resetRequestBody = resetRequestJson.toRequestBody("application/json".toMediaType())
                
                val resetRequest = Request.Builder()
                    .url(resetUrl)
                    .put(resetRequestBody)
                    .addHeader("Authorization", "Bearer $ADMIN_ACCESS_TOKEN")
                    .build()
                
                val resetResponse = httpClient.newCall(resetRequest).execute()
                val resetResponseBody = resetResponse.body?.string() ?: ""
                
                Timber.d("Password reset response code: ${resetResponse.code}")
                Timber.d("Password reset response: $resetResponseBody")
                
                if (!resetResponse.isSuccessful) {
                    Timber.e("Password reset failed: ${resetResponse.code} ${resetResponse.message}")
                    throw Exception("Password reset failed: ${resetResponse.code} ${resetResponse.message} - $resetResponseBody")
                }
                
                Timber.d("Password reset successful for user: $username")
            } catch (e: Exception) {
                Timber.e(e, "Password reset failed with exception: ${e.message}")
                throw e
            }
        }
    } 
