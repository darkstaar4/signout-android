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
import androidx.compose.runtime.setValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.features.customauth.impl.auth.CognitoAuthService
import io.element.android.features.customauth.impl.auth.MatrixIntegrationService
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.launch
import timber.log.Timber

class VerificationPresenter @AssistedInject constructor(
    @Assisted private val params: Params,
    private val cognitoAuthService: CognitoAuthService,
    private val matrixIntegrationService: MatrixIntegrationService,
) : Presenter<VerificationState> {
    
    @Composable
    override fun present(): VerificationState {
        val coroutineScope = rememberCoroutineScope()
        
        // Use parameters passed from navigation
        val username = params.username
        val email = params.email
        var verificationCode by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var isResending by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isVerificationSuccessful by remember { mutableStateOf(false) }
        var isMatrixAccountCreated by remember { mutableStateOf(false) }
        
        fun handleEvents(event: VerificationEvents) {
            when (event) {
                is VerificationEvents.SetVerificationCode -> {
                    verificationCode = event.code
                    errorMessage = null
                }
                VerificationEvents.Submit -> {
                    if (verificationCode.length == 6) {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            try {
                                Timber.d("Starting email verification for user: $email")
                                
                                // Verify the code with Cognito (use email as Cognito username)
                                val verifyResult = cognitoAuthService.confirmSignUp(email, verificationCode)
                                
                                if (verifyResult.isSuccess) {
                                    Timber.d("Email verification successful for user: $email")
                                    
                                    // Email verification successful - show success state
                                    isVerificationSuccessful = true
                                    
                                    // Try to create Matrix account in background
                                    Timber.d("Starting Matrix account creation for username: $username")
                                    
                                    try {
                                        val matrixPassword = matrixIntegrationService.generateMatrixPassword()
                                        val matrixUsername = matrixIntegrationService.formatMatrixUsername(username)
                                        
                                        Timber.d("Generated Matrix credentials - username: $matrixUsername, password length: ${matrixPassword.length}")
                                        
                                        // Create Matrix account
                                        val matrixResult = matrixIntegrationService.createMatrixAccount(
                                            username = matrixUsername,
                                            password = matrixPassword,
                                            displayName = username
                                        )
                                        
                                        if (matrixResult.isSuccess) {
                                            val matrixAccount = matrixResult.getOrThrow()
                                            Timber.d("Matrix account created successfully: ${matrixAccount.matrixUserId}")
                                            
                                            // Update Cognito with Matrix credentials
                                            val updateResult = cognitoAuthService.updateMatrixCredentials(
                                                username = email,
                                                matrixUserId = matrixAccount.matrixUserId,
                                                matrixUsername = matrixAccount.matrixUsername,
                                                matrixPassword = matrixAccount.matrixPassword
                                            )
                                            
                                            if (updateResult.isSuccess) {
                                                Timber.d("Matrix credentials stored in Cognito successfully")
                                                isMatrixAccountCreated = true
                                            } else {
                                                Timber.w("Failed to store Matrix credentials in Cognito: ${updateResult.error}")
                                            }
                                        } else {
                                            val error = matrixResult.exceptionOrNull()
                                            Timber.w("Matrix account creation failed: ${error?.message}")
                                            Timber.w("Matrix error details: ${error?.cause?.message}")
                                        }
                                    } catch (matrixException: Exception) {
                                        Timber.w(matrixException, "Matrix account creation failed with exception")
                                    }
                                } else {
                                    Timber.w("Email verification failed: ${verifyResult.error}")
                                    errorMessage = verifyResult.error ?: "Verification failed"
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Verification process failed")
                                errorMessage = "Verification failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
                VerificationEvents.ResendCode -> {
                    coroutineScope.launch {
                        isResending = true
                        errorMessage = null
                        
                        try {
                            val result = cognitoAuthService.resendConfirmationCode(email)
                            if (!result.isSuccess) {
                                errorMessage = result.error ?: "Failed to resend code. Please try again."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to resend code: ${e.message}"
                        } finally {
                            isResending = false
                        }
                    }
                }
                VerificationEvents.ClearError -> {
                    errorMessage = null
                }
                VerificationEvents.Continue -> {
                    // This event will be handled by the parent node
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
            canSubmit = verificationCode.length == 6,
            isVerificationSuccessful = isVerificationSuccessful,
            isMatrixAccountCreated = isMatrixAccountCreated,
            eventSink = ::handleEvents
        )
    }
    
    data class Params(
        val username: String,
        val email: String
    )
    
    @AssistedFactory
    interface Factory {
        fun create(params: Params): VerificationPresenter
    }
} 