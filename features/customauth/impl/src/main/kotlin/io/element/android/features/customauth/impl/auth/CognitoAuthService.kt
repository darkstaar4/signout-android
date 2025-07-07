/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.auth

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException
import com.amazonaws.services.cognitoidentityprovider.model.CodeDeliveryFailureException
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.SingleIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.VerificationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.suspendCoroutine
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession

@SingleIn(AppScope::class)
class CognitoAuthService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val USER_POOL_ID = "us-east-1_ltpOFMyVw"
            private const val CLIENT_ID = "6j4hfhvvdokuj458r42aoj0j4d"
            private const val IDENTITY_POOL_ID = "us-east-1:your-identity-pool-id" // Optional for basic auth
            private val REGION = Regions.US_EAST_1
        }

        private val userPool: CognitoUserPool by lazy {
            CognitoUserPool(context, USER_POOL_ID, CLIENT_ID, null, REGION)
        }

        private val credentialsProvider: CognitoCachingCredentialsProvider by lazy {
            CognitoCachingCredentialsProvider(
                context,
                IDENTITY_POOL_ID,
                REGION,
            )
        }

        data class AuthResult(
            val isSuccess: Boolean,
            val user: CognitoUser? = null,
            val error: String? = null,
            val needsVerification: Boolean = false,
            val needsPasswordReset: Boolean = false,
        )

        data class RegisterResult(
            val isSuccess: Boolean,
            val error: String? = null,
            val fieldError: Pair<String, String>? = null,
            val username: String? = null,
            val needsVerification: Boolean = false
        )

        data class UserData(
            val username: String,
            val email: String,
            val firstName: String,
            val lastName: String,
            val phoneNumber: String,
            val npiNumber: String? = null,
            val professionalTitle: String? = null,
            val specialty: String? = null,
            val country: String? = null,
            val officeAddress: String? = null,
            val officeCity: String? = null,
            val officeState: String? = null,
            val officeZip: String? = null,
            // Matrix credentials (auto-generated)
            val matrixUsername: String? = null,
            val matrixPassword: String? = null,
        )

        data class ConfirmResult(
            val isSuccess: Boolean,
            val error: String? = null
        )

        suspend fun login(
            username: String,
            password: String,
            matrixIntegrationService: MatrixIntegrationService? = null,
        ): AuthResult {
            return suspendCancellableCoroutine { continuation ->
                        // Use email as login identifier (Cognito username)
        val loginIdentifier = if (username.contains("@")) {
            // Already an email, use as-is
            username
        } else {
            // For non-email usernames, show a clear error message
            continuation.resume(AuthResult(
                isSuccess = false,
                error = "Please use your email address to log in."
            ))
            return@suspendCancellableCoroutine
        }
                
                val cognitoUser = userPool.getUser(loginIdentifier)

                cognitoUser.getSessionInBackground(
                    object : AuthenticationHandler {
                        override fun onSuccess(
                            userSession: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession,
                            newDevice: CognitoDevice?,
                        ) {
                            // After successful Cognito login, handle Matrix login if service is provided
                            matrixIntegrationService?.let { service ->
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        // Get user details to check for Matrix credentials
                                        cognitoUser.getDetailsInBackground(object : com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler {
                                            override fun onSuccess(userDetails: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails?) {
                                                val attributes = userDetails?.attributes
                                                var matrixUsername = attributes?.attributes?.get("custom:matrix_username")
                                                var matrixPassword = attributes?.attributes?.get("custom:matrix_password")
                                                
                                                // Get the preferred username for Matrix account creation
                                                val preferredUsername = attributes?.attributes?.get("preferred_username") ?: username
                                                
                                                // If Matrix credentials don't exist, generate them for backward compatibility
                                                if (matrixUsername == null || matrixPassword == null) {
                                                    Timber.d("Matrix credentials not found for user $loginIdentifier, generating new ones")
                                                    
                                                    matrixUsername = service.formatMatrixUsername(preferredUsername)
                                                    matrixPassword = service.generateMatrixPassword()
                                                    
                                                    // Update Cognito user with Matrix credentials for future logins
                                                    val updatedAttributes = CognitoUserAttributes().apply {
                                                        addAttribute("custom:matrix_username", matrixUsername)
                                                        addAttribute("custom:matrix_password", matrixPassword)
                                                    }
                                                    
                                                    cognitoUser.updateAttributesInBackground(
                                                        updatedAttributes,
                                                        object : com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler {
                                                            override fun onSuccess(attributesVerificationList: MutableList<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails>?) {
                                                                Timber.d("Matrix credentials updated in Cognito for user: $loginIdentifier")
                                                            }
                                                            
                                                            override fun onFailure(exception: Exception?) {
                                                                Timber.w("Failed to update Matrix credentials in Cognito: ${exception?.message}")
                                                            }
                                                        }
                                                    )
                                                }
                                                
                                                // Attempt Matrix login/registration
                                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                    try {
                                                        val firstName = attributes?.attributes?.get("given_name") ?: ""
                                                        val lastName = attributes?.attributes?.get("family_name") ?: ""
                                                        val displayName = "$firstName $lastName".trim()
                                                        
                                                        val matrixResult = service.createMatrixAccount(
                                                            username = matrixUsername!!,
                                                            password = matrixPassword!!,
                                                            displayName = displayName
                                                        )
                                                        
                                                        if (matrixResult.isSuccess) {
                                                            Timber.d("Matrix login/registration successful for user: $loginIdentifier")
                                                        } else {
                                                            Timber.w("Matrix login/registration failed: ${matrixResult.exceptionOrNull()}")
                                                        }
                                                    } catch (exception: Exception) {
                                                        Timber.e(exception, "Exception during Matrix login/registration")
                                                    }
                                                }
                                            }
                                            
                                            override fun onFailure(exception: Exception?) {
                                                Timber.e(exception, "Failed to get user details for Matrix login")
                                            }
                                        })
                                    } catch (exception: Exception) {
                                        Timber.e(exception, "Exception during Matrix login setup")
                                    }
                                }
                            }
                            
                            continuation.resume(AuthResult(isSuccess = true, user = cognitoUser))
                        }

                        override fun getAuthenticationDetails(
                            authenticationContinuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations
                                .AuthenticationContinuation,
                            userId: String,
                        ) {
                            val authDetails =
                                AuthenticationDetails(
                                    userId,
                                    password,
                                    null,
                                )
                            authenticationContinuation.setAuthenticationDetails(authDetails)
                            authenticationContinuation.continueTask()
                        }

                        override fun getMFACode(
                            continuation:
                                com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation,
                        ) {
                            // Handle MFA if needed
                            continuation.setMfaCode("") // This would need to be implemented
                            continuation.continueTask()
                        }

                        override fun authenticationChallenge(
                            continuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation,
                        ) {
                            // Handle authentication challenges
                            continuation.continueTask()
                        }

                        override fun onFailure(exception: Exception) {
                            val authResult =
                                when (exception) {
                                    is UserNotConfirmedException ->
                                        AuthResult(
                                            isSuccess = false,
                                            error = "Please verify your email address before signing in.",
                                            needsVerification = true,
                                        )
                                    is NotAuthorizedException ->
                                        AuthResult(
                                            isSuccess = false,
                                            error = "Invalid username or password.",
                                        )
                                    is UserNotFoundException ->
                                        AuthResult(
                                            isSuccess = false,
                                            error = "No account found with this username.",
                                        )
                                    else ->
                                        AuthResult(
                                            isSuccess = false,
                                            error = exception.message ?: "Login failed. Please try again.",
                                        )
                                }
                            continuation.resume(authResult)
                        }
                    },
                )
            }
        }

        suspend fun register(
            userData: UserData,
            password: String,
            matrixIntegrationService: MatrixIntegrationService,
        ): RegisterResult {
            return suspendCancellableCoroutine { continuation ->
                // Generate Matrix credentials
                val matrixPassword = matrixIntegrationService.generateMatrixPassword()
                val matrixUsername = matrixIntegrationService.formatMatrixUsername(userData.username)

                val userAttributes =
                    CognitoUserAttributes().apply {
                        addAttribute("email", userData.email)
                        addAttribute("given_name", userData.firstName)
                        addAttribute("family_name", userData.lastName)
                        addAttribute("phone_number", userData.phoneNumber)
                        addAttribute("preferred_username", userData.username)

                        // Custom attributes for healthcare professionals
                        userData.npiNumber?.let { if (it.isNotEmpty()) addAttribute("custom:npi_number", it) }
                        userData.professionalTitle?.let { if (it.isNotEmpty()) addAttribute("custom:professional_title", it) }
                        userData.specialty?.let { if (it.isNotEmpty()) addAttribute("custom:specialty", it) }
                        userData.country?.let { if (it.isNotEmpty()) addAttribute("custom:country", it) }
                        userData.officeAddress?.let { if (it.isNotEmpty()) addAttribute("custom:office_address", it) }
                        userData.officeCity?.let { if (it.isNotEmpty()) addAttribute("custom:office_city", it) }
                        userData.officeState?.let { if (it.isNotEmpty()) addAttribute("custom:office_state", it) }
                        userData.officeZip?.let { if (it.isNotEmpty()) addAttribute("custom:office_zip", it) }

                        // Matrix credentials
                        addAttribute("custom:matrix_username", matrixUsername)
                        addAttribute("custom:matrix_password", matrixPassword)
                    }

                userPool.signUpInBackground(
                    userData.email, // Use email as the Cognito username (required by the User Pool)
                    password,
                    userAttributes,
                    null,
                    object : SignUpHandler {
                        override fun onSuccess(
                            user: CognitoUser,
                            signUpResult: com.amazonaws.services.cognitoidentityprovider.model.SignUpResult,
                        ) {
                            // After successful Cognito registration, create Matrix account
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    val displayName = "${userData.firstName} ${userData.lastName}"
                                    val matrixResult =
                                        matrixIntegrationService.createMatrixAccount(
                                            username = matrixUsername,
                                            password = matrixPassword,
                                            displayName = displayName,
                                        )

                                    if (matrixResult.isSuccess) {
                                        Timber.d("Matrix account created successfully for user: ${userData.username}")
                                        continuation.resume(
                                            RegisterResult(
                                                isSuccess = true,
                                                needsVerification = !signUpResult.userConfirmed,
                                                username = userData.username,
                                            ),
                                        )
                                    } else {
                                        Timber.e("Failed to create Matrix account: ${matrixResult.exceptionOrNull()}")
                                        continuation.resume(
                                            RegisterResult(
                                                isSuccess = false,
                                                error = "Registration successful but Matrix account creation failed. Please contact support.",
                                                username = userData.username,
                                            ),
                                        )
                                    }
                                } catch (exception: Exception) {
                                    Timber.e(exception, "Exception during Matrix account creation")
                                    continuation.resume(
                                        RegisterResult(
                                            isSuccess = false,
                                            error = "Registration successful but Matrix account creation failed. Please contact support.",
                                            username = userData.username,
                                        ),
                                    )
                                }
                            }
                        }

                        override fun onFailure(exception: Exception) {
                            Timber.e(exception, "Cognito registration failed with exception: ${exception::class.simpleName}")
                            Timber.e("Full error message: ${exception.message}")
                            
                            val registerResult = when (exception) {
                                is InvalidParameterException -> {
                                    val message = exception.message ?: ""
                                    Timber.e("InvalidParameterException details: $message")
                                    when {
                                        message.contains("email", ignoreCase = true) -> RegisterResult(
                                            isSuccess = false,
                                            error = "AWS Cognito email validation failed: $message",
                                            fieldError = "email" to "Email format rejected by AWS: $message"
                                        )
                                        message.contains("phone", ignoreCase = true) -> RegisterResult(
                                            isSuccess = false,
                                            error = "Invalid phone number format",
                                            fieldError = "phoneNumber" to "Please enter a valid phone number"
                                        )
                                        message.contains("password", ignoreCase = true) -> RegisterResult(
                                            isSuccess = false,
                                            error = "Password does not meet requirements",
                                            fieldError = "password" to "Password must be at least 8 characters with uppercase, lowercase, and number"
                                        )
                                        else -> RegisterResult(
                                            isSuccess = false,
                                            error = "AWS validation failed: $message"
                                        )
                                    }
                                }
                                is UsernameExistsException -> RegisterResult(
                                    isSuccess = false,
                                    error = "Username already exists",
                                    fieldError = "username" to "This username is already taken. Please choose a different one."
                                )
                                is InvalidPasswordException -> RegisterResult(
                                    isSuccess = false,
                                    error = "Invalid password",
                                    fieldError = "password" to "Password must be at least 8 characters with uppercase, lowercase, and number"
                                )
                                is CodeDeliveryFailureException -> RegisterResult(
                                    isSuccess = false,
                                    error = "Failed to send verification code. Please check your email address."
                                )
                                else -> RegisterResult(
                                    isSuccess = false,
                                    error = "Registration failed: ${exception.message}"
                                )
                            }
                            continuation.resume(registerResult)
                        }
                    },
                )
            }
        }

        suspend fun confirmSignUp(
            username: String,
            confirmationCode: String
        ): ConfirmResult =
            withContext(Dispatchers.IO) {
                suspendCoroutine { continuation ->
                    val cognitoUser = userPool.getUser(username)
                    
                    val confirmationHandler = object : GenericHandler {
                        override fun onSuccess() {
                            Timber.d("Email verification successful for user: $username")
                            continuation.resume(ConfirmResult(isSuccess = true))
                        }

                        override fun onFailure(exception: Exception) {
                            Timber.e(exception, "Email verification failed")
                            val error = when (exception) {
                                is InvalidParameterException -> "Invalid verification code. Please try again."
                                is NotAuthorizedException -> "Verification code has expired. Please request a new one."
                                is UserNotFoundException -> "User is already confirmed."
                                else -> "Verification failed: ${exception.message}"
                            }
                            continuation.resume(ConfirmResult(isSuccess = false, error = error))
                        }
                    }
                    
                    cognitoUser.confirmSignUpInBackground(confirmationCode, true, confirmationHandler)
                }
            }

        suspend fun resendConfirmationCode(username: String): ConfirmResult =
            withContext(Dispatchers.IO) {
                suspendCoroutine { continuation ->
                    val cognitoUser = userPool.getUser(username)
                    
                    val resendHandler = object : VerificationHandler {
                        override fun onSuccess(verificationCodeDeliveryMedium: CognitoUserCodeDeliveryDetails) {
                            Timber.d("Verification code resent to ${verificationCodeDeliveryMedium.destination}")
                            continuation.resume(ConfirmResult(isSuccess = true))
                        }

                        override fun onFailure(exception: Exception) {
                            Timber.e(exception, "Failed to resend verification code")
                            continuation.resume(
                                ConfirmResult(
                                    isSuccess = false,
                                    error = "Failed to resend code: ${exception.message}"
                                )
                            )
                        }
                    }
                    
                    cognitoUser.resendConfirmationCodeInBackground(resendHandler)
                }
            }

        suspend fun updateMatrixCredentials(
            username: String,
            matrixUserId: String,
            matrixUsername: String,
            matrixPassword: String
        ): ConfirmResult =
            withContext(Dispatchers.IO) {
                suspendCoroutine { continuation ->
                    val cognitoUser = userPool.getUser(username)
                    
                    // Create attribute update
                    val userAttributes = CognitoUserAttributes()
                    userAttributes.addAttribute("custom:matrix_user_id", matrixUserId)
                    userAttributes.addAttribute("custom:matrix_username", matrixUsername)
                    userAttributes.addAttribute("custom:matrix_password", matrixPassword)
                    
                    val updateHandler = object : UpdateAttributesHandler {
                        override fun onSuccess(attributesVerificationList: List<CognitoUserCodeDeliveryDetails>) {
                            Timber.d("Matrix credentials updated successfully for user: $username")
                            continuation.resume(ConfirmResult(isSuccess = true))
                        }

                        override fun onFailure(exception: Exception) {
                            Timber.e(exception, "Failed to update Matrix credentials")
                            continuation.resume(
                                ConfirmResult(
                                    isSuccess = false,
                                    error = "Failed to update credentials: ${exception.message}"
                                )
                            )
                        }
                    }
                    
                    // Note: This requires the user to be authenticated
                    // We might need to get a session first
                    cognitoUser.getSessionInBackground(object : AuthenticationHandler {
                        override fun onSuccess(
                            userSession: CognitoUserSession,
                            newDevice: CognitoDevice?
                        ) {
                            // Now update attributes with valid session
                            cognitoUser.updateAttributesInBackground(userAttributes, updateHandler)
                        }

                        override fun getAuthenticationDetails(
                            authenticationContinuation: AuthenticationContinuation,
                            userId: String
                        ) {
                            // This shouldn't happen if user just verified
                            continuation.resume(
                                ConfirmResult(
                                    isSuccess = false,
                                    error = "User session expired. Please login again."
                                )
                            )
                        }

                        override fun getMFACode(continuation: MultiFactorAuthenticationContinuation) {
                            // Not implemented
                        }

                        override fun authenticationChallenge(continuation: ChallengeContinuation) {
                            // Not implemented
                        }

                        override fun onFailure(exception: Exception) {
                            Timber.e(exception, "Failed to get user session")
                            continuation.resume(
                                ConfirmResult(
                                    isSuccess = false,
                                    error = "Failed to authenticate: ${exception.message}"
                                )
                            )
                        }
                    })
                }
            }

        suspend fun forgotPassword(email: String): AuthResult {
            return suspendCancellableCoroutine { continuation ->
                val cognitoUser = userPool.getUser(email)

                cognitoUser.forgotPasswordInBackground(
                    object : com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler {
                        override fun onSuccess() {
                            continuation.resume(AuthResult(isSuccess = true, user = cognitoUser))
                        }

                        override fun getResetCode(
                            continuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation,
                        ) {
                            // This would need to be handled in the UI
                            continuation.continueTask()
                        }

                        override fun onFailure(exception: Exception) {
                            continuation.resume(
                                AuthResult(
                                    isSuccess = false,
                                    error = exception.message ?: "Password reset failed. Please try again.",
                                ),
                            )
                        }
                    },
                )
            }
        }

        fun getCurrentUser(): CognitoUser? {
            return userPool.currentUser
        }

        fun signOut() {
            userPool.currentUser?.signOut()
            credentialsProvider.clear()
        }

        data class UsernameValidationResult(
            val isAvailable: Boolean,
            val error: String? = null,
        )

        suspend fun validateUsername(username: String): UsernameValidationResult {
            // Basic validation first
            if (username.isBlank()) {
                return UsernameValidationResult(
                    isAvailable = false,
                    error = "Username cannot be empty",
                )
            }

            if (username.length < 3) {
                return UsernameValidationResult(
                    isAvailable = false,
                    error = "Username must be at least 3 characters long",
                )
            }

            if (username.length > 32) {
                return UsernameValidationResult(
                    isAvailable = false,
                    error = "Username must be less than 32 characters long",
                )
            }

            // Check for valid characters (alphanumeric, underscore, hyphen)
            val validUsernameRegex = Regex("^[a-zA-Z0-9_-]+$")
            if (!validUsernameRegex.matches(username)) {
                return UsernameValidationResult(
                    isAvailable = false,
                    error = "Username can only contain letters, numbers, underscores, and hyphens",
                )
            }

            // Check availability in Cognito
            return suspendCancellableCoroutine { continuation ->
                try {
                    // Try to get user details - if successful, username exists
                    val cognitoUser = userPool.getUser(username)

                    cognitoUser.getDetailsInBackground(
                        object : GetDetailsHandler {
                            override fun onSuccess(userDetails: CognitoUserDetails?) {
                                // User exists, username is taken
                                continuation.resume(
                                    UsernameValidationResult(
                                        isAvailable = false,
                                        error = "Username is already taken",
                                    ),
                                )
                            }

                            override fun onFailure(exception: Exception?) {
                                // User doesn't exist or other error
                                when (exception) {
                                    is UserNotFoundException -> {
                                        // Username is available
                                        continuation.resume(
                                            UsernameValidationResult(isAvailable = true),
                                        )
                                    }
                                    is NotAuthorizedException -> {
                                        // This happens when trying to check a non-existent user
                                        // which means the username is available
                                        continuation.resume(
                                            UsernameValidationResult(isAvailable = true),
                                        )
                                    }
                                    else -> {
                                        // For other errors, assume username is available to avoid blocking registration
                                        Timber.w("Username validation error: ${exception?.message}")
                                        continuation.resume(
                                            UsernameValidationResult(isAvailable = true),
                                        )
                                    }
                                }
                            }
                        },
                    )
                } catch (exception: Exception) {
                    // If there's an error, assume username is available to avoid blocking registration
                    Timber.w("Username validation exception: ${exception.message}")
                    continuation.resume(
                        UsernameValidationResult(isAvailable = true),
                    )
                }
            }
        }

        // Simplified login - users must use their email address

        suspend fun checkMatrixCredentialsMigration(username: String): Boolean {
            return suspendCancellableCoroutine { continuation ->
                val cognitoUser = userPool.getUser(username)
                
                cognitoUser.getDetailsInBackground(object : GetDetailsHandler {
                    override fun onSuccess(userDetails: CognitoUserDetails?) {
                        val attributes = userDetails?.attributes
                        val hasMatrixUsername = attributes?.attributes?.containsKey("custom:matrix_username") == true
                        val hasMatrixPassword = attributes?.attributes?.containsKey("custom:matrix_password") == true
                        
                        // Return true if migration is needed (missing Matrix credentials)
                        continuation.resume(!(hasMatrixUsername && hasMatrixPassword))
                    }
                    
                    override fun onFailure(exception: Exception?) {
                        // If we can't get details, assume migration is needed
                        continuation.resume(true)
                    }
                })
            }
        }

        suspend fun isUserPreVerified(username: String): Boolean {
            return suspendCancellableCoroutine { continuation ->
                val cognitoUser = userPool.getUser(username)
                
                cognitoUser.getDetailsInBackground(object : GetDetailsHandler {
                    override fun onSuccess(userDetails: CognitoUserDetails?) {
                        val attributes = userDetails?.attributes
                        val isPreVerified = attributes?.attributes?.get("custom:pre_verified") == "true"
                        continuation.resume(isPreVerified)
                    }
                    
                    override fun onFailure(exception: Exception?) {
                        // If we can't get details, assume not pre-verified
                        continuation.resume(false)
                    }
                })
            }
        }

        suspend fun registerWithoutMatrix(
            userData: UserData,
            password: String
        ): RegisterResult =
            withContext(Dispatchers.IO) {
                suspendCoroutine { continuation ->
                    val userAttributes =
                        CognitoUserAttributes().apply {
                            addAttribute("email", userData.email)
                            addAttribute("given_name", userData.firstName)
                            addAttribute("family_name", userData.lastName)
                            addAttribute("phone_number", userData.phoneNumber)
                            addAttribute("preferred_username", userData.username)

                            // Custom attributes for healthcare professionals
                            userData.npiNumber?.let { if (it.isNotEmpty()) addAttribute("custom:npi_number", it) }
                            userData.professionalTitle?.let { if (it.isNotEmpty()) addAttribute("custom:professional_title", it) }
                            userData.specialty?.let { if (it.isNotEmpty()) addAttribute("custom:specialty", it) }
                            userData.country?.let { if (it.isNotEmpty()) addAttribute("custom:country", it) }
                            userData.officeAddress?.let { if (it.isNotEmpty()) addAttribute("custom:office_address", it) }
                            userData.officeCity?.let { if (it.isNotEmpty()) addAttribute("custom:office_city", it) }
                            userData.officeState?.let { if (it.isNotEmpty()) addAttribute("custom:office_state", it) }
                            userData.officeZip?.let { if (it.isNotEmpty()) addAttribute("custom:office_zip", it) }
                            
                            // Add pre-verified flag to indicate user verified email during registration
                            addAttribute("custom:pre_verified", "true")
                            
                            // Don't add Matrix credentials yet - they'll be added after verification
                        }

                    val signUpHandler =
                        object : SignUpHandler {
                            override fun onSuccess(
                                user: CognitoUser,
                                signUpResult: SignUpResult,
                            ) {
                                Timber.d("Cognito registration successful for user: ${user.userId}")
                                
                                if (!signUpResult.userConfirmed) {
                                    // User needs to verify email
                                    continuation.resume(
                                        RegisterResult(
                                            isSuccess = true,
                                            username = userData.username,
                                            needsVerification = true
                                        )
                                    )
                                } else {
                                    // User is already confirmed (shouldn't happen in normal flow)
                                    continuation.resume(
                                        RegisterResult(
                                            isSuccess = true,
                                            username = userData.username,
                                            needsVerification = false
                                        )
                                    )
                                }
                            }

                            override fun onFailure(exception: Exception) {
                                Timber.e(exception, "Cognito registration failed with exception: ${exception::class.simpleName}")
                                Timber.e("Full error message: ${exception.message}")
                                
                                val registerResult = when (exception) {
                                    is InvalidParameterException -> {
                                        val message = exception.message ?: ""
                                        Timber.e("InvalidParameterException details: $message")
                                        when {
                                            message.contains("email", ignoreCase = true) -> RegisterResult(
                                                isSuccess = false,
                                                error = "AWS Cognito email validation failed: $message",
                                                fieldError = "email" to "Email format rejected by AWS: $message"
                                            )
                                            message.contains("phone", ignoreCase = true) -> RegisterResult(
                                                isSuccess = false,
                                                error = "Invalid phone number format",
                                                fieldError = "phoneNumber" to "Please enter a valid phone number"
                                            )
                                            message.contains("password", ignoreCase = true) -> RegisterResult(
                                                isSuccess = false,
                                                error = "Password does not meet requirements",
                                                fieldError = "password" to "Password must be at least 8 characters with uppercase, lowercase, and number"
                                            )
                                            else -> RegisterResult(
                                                isSuccess = false,
                                                error = "AWS validation failed: $message"
                                            )
                                        }
                                    }
                                    is UsernameExistsException -> RegisterResult(
                                        isSuccess = false,
                                        error = "Username already exists",
                                        fieldError = "username" to "This username is already taken. Please choose a different one."
                                    )
                                    is InvalidPasswordException -> RegisterResult(
                                        isSuccess = false,
                                        error = "Invalid password",
                                        fieldError = "password" to "Password must be at least 8 characters with uppercase, lowercase, and number"
                                    )
                                    is CodeDeliveryFailureException -> RegisterResult(
                                        isSuccess = false,
                                        error = "Failed to send verification code. Please check your email address."
                                    )
                                    else -> RegisterResult(
                                        isSuccess = false,
                                        error = "Registration failed: ${exception.message}"
                                    )
                                }
                                continuation.resume(registerResult)
                            }
                        }

                    userPool.signUpInBackground(
                        userData.email, // Use email as the Cognito username (required by the User Pool)
                        password,
                        userAttributes,
                        null,
                        signUpHandler,
                    )
                }
            }
    }
