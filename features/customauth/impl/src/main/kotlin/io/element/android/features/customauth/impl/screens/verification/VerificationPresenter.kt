/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.verification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.element.android.features.customauth.impl.auth.CognitoAuthService
import io.element.android.features.customauth.impl.auth.MatrixIntegrationService
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class VerificationPresenter
    @Inject
    constructor(
        private val cognitoAuthService: CognitoAuthService,
        private val matrixIntegrationService: MatrixIntegrationService,
    ) : Presenter<VerificationState> {
        
        @Composable
        fun present(
            username: String,
            email: String,
            password: String
        ): VerificationState {
            var verificationCode by rememberSaveable { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var isResending by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var isVerificationSuccessful by remember { mutableStateOf(false) }
            var isMatrixAccountCreated by remember { mutableStateOf(false) }

            val coroutineScope = rememberCoroutineScope()

            val canSubmit = verificationCode.isNotEmpty() && !isLoading

            fun handleEvents(event: VerificationEvents) {
                when (event) {
                    is VerificationEvents.SetVerificationCode -> {
                        verificationCode = event.code
                        errorMessage = null
                    }
                    VerificationEvents.Submit -> {
                        if (canSubmit) {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null

                                try {
                                    val result = cognitoAuthService.confirmSignUp(email, verificationCode.trim())
                                    if (result.isSuccess) {
                                        Timber.d("Email verification successful for user: $email")
                                        
                                        // IMPORTANT: Login the user first to establish Cognito session
                                        // This is required before updating Matrix credentials
                                        try {
                                            Timber.d("Logging in new user to establish Cognito session: $email")
                                            val loginResult = cognitoAuthService.login(email, password, matrixIntegrationService)
                                            if (loginResult.isSuccess) {
                                                Timber.d("Auto-login successful after verification")
                                                
                                                // Now update Matrix credentials with valid session
                                                val updateResult = cognitoAuthService.updateMatrixCredentials(
                                                    username = email,
                                                    matrixUserId = "@${username}:signout.io",
                                                    matrixUsername = username,
                                                    matrixPassword = password
                                                )
                                                
                                                if (updateResult.isSuccess) {
                                                    Timber.d("Matrix credentials stored in Cognito successfully")
                                                    isMatrixAccountCreated = true
                                                } else {
                                                    Timber.w("Failed to store Matrix credentials in Cognito: ${updateResult.error}")
                                                    // Still mark as successful since login worked
                                                    isMatrixAccountCreated = true
                                                }
                                                
                                                // User mapping will be populated after Matrix session is established
                                                Timber.d("Matrix session will be established, user mapping will be populated")
                                            } else {
                                                Timber.w("Auto-login failed after verification: ${loginResult.error}")
                                                errorMessage = "Registration successful but login failed. Please try logging in manually."
                                            }
                                        } catch (loginException: Exception) {
                                            Timber.w(loginException, "Failed to auto-login after verification")
                                            errorMessage = "Registration successful but login failed. Please try logging in manually."
                                        }
                                        
                                        isVerificationSuccessful = true
                                    } else {
                                        errorMessage = result.error ?: "Verification failed. Please try again."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "An unexpected error occurred."
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                    VerificationEvents.ClearError -> {
                        errorMessage = null
                    }
                    VerificationEvents.ResendCode -> {
                        coroutineScope.launch {
                            isResending = true
                            errorMessage = null
                            
                            try {
                                val result = cognitoAuthService.resendConfirmationCode(email)
                                if (result.isSuccess) {
                                    Timber.d("Confirmation code resent successfully")
                                } else {
                                    errorMessage = result.error ?: "Failed to resend code. Please try again."
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to resend code."
                            } finally {
                                isResending = false
                            }
                        }
                    }
                    VerificationEvents.Continue -> {
                        // This event is handled by the parent node
                        Timber.d("Continue button pressed - verification complete")
                    }
                }
            }

            return VerificationState(
                username = username,
                email = email,
                verificationCode = verificationCode,
                isLoading = isLoading,
                isResending = isResending,
                errorMessage = errorMessage,
                canSubmit = canSubmit,
                isVerificationSuccessful = isVerificationSuccessful,
                isMatrixAccountCreated = isMatrixAccountCreated,
                eventSink = ::handleEvents,
            )
        }
        
        @Composable
        override fun present(): VerificationState {
            // This should not be called directly - use the other present method
            throw IllegalStateException("Use present(username, email, password) instead")
        }
    } 