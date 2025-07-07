/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.login

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
import javax.inject.Inject

class LoginPresenter
    @Inject
    constructor(
        private val cognitoAuthService: CognitoAuthService,
        private val matrixIntegrationService: MatrixIntegrationService,
    ) : Presenter<LoginState> {
        @Composable
        override fun present(): LoginState {
            var username by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var isLoginSuccessful by remember { mutableStateOf(false) }

            val coroutineScope = rememberCoroutineScope()

            val canSubmit = username.isNotEmpty() && password.isNotEmpty() && !isLoading

            fun handleEvents(event: LoginEvents) {
                when (event) {
                    is LoginEvents.SetUsername -> {
                        username = event.username
                        errorMessage = null
                    }
                    is LoginEvents.SetPassword -> {
                        password = event.password
                        errorMessage = null
                    }
                    LoginEvents.Submit -> {
                        if (canSubmit) {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null

                                try {
                                    val result = cognitoAuthService.login(username.trim(), password, matrixIntegrationService)
                                    if (result.isSuccess) {
                                        // Login successful - set flag to trigger navigation
                                        isLoginSuccessful = true
                                    } else {
                                        errorMessage = result.error ?: "Login failed. Please try again."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "An unexpected error occurred."
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                    LoginEvents.ClearError -> {
                        errorMessage = null
                    }
                    is LoginEvents.ForgotPassword -> {
                        coroutineScope.launch {
                            try {
                                val result = cognitoAuthService.forgotPassword(event.username)
                                if (result.isSuccess) {
                                    // Show success message or navigate to password reset
                                    errorMessage = "Password reset email sent. Please check your inbox."
                                } else {
                                    errorMessage = result.error ?: "Failed to send password reset email."
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to send password reset email."
                            }
                        }
                    }
                    is LoginEvents.ResetPassword -> {
                        coroutineScope.launch {
                            try {
                                // Validate passwords match
                                if (event.newPassword != event.confirmPassword) {
                                    errorMessage = "New passwords do not match."
                                    return@launch
                                }

                                if (event.newPassword.length < 8) {
                                    errorMessage = "Password must be at least 8 characters long."
                                    return@launch
                                }

                                // Implement password reset with temp password
                                // This would require additional Cognito API calls
                                errorMessage = "Password reset functionality will be implemented."
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to reset password."
                            }
                        }
                    }
                }
            }

            return LoginState(
                username = username,
                password = password,
                isLoading = isLoading,
                errorMessage = errorMessage,
                canSubmit = canSubmit,
                isLoginSuccessful = isLoginSuccessful,
                eventSink = ::handleEvents,
            )
        }
    }
