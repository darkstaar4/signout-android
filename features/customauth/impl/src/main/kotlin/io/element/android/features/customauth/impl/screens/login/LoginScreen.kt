/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.customauth.impl.R

@Composable
fun LoginScreen(
    state: LoginState,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showResetPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var forgotPasswordEmail by rememberSaveable { mutableStateOf("") }
    var tempPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    focusManager.clearFocus()
                },
        color = Color.White,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo Container
            Box(
                modifier =
                    Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.signout_square_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Sign in to Your App",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Secure messaging platform for healthcare providers",
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Security Info
            Card(
                modifier = Modifier.widthIn(max = 320.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = Color(0xFF0EA5E9),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted, secure, and HIPAA-compliant",
                        color = Color(0xFF0EA5E9),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                // Username Field
                Text(
                    text = "Username",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.username,
                    onValueChange = { state.eventSink(LoginEvents.SetUsername(it)) },
                    placeholder = { Text("Enter your username") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                Text(
                    text = "Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                var passwordVisible by rememberSaveable { mutableStateOf(false) }

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { state.eventSink(LoginEvents.SetPassword(it)) },
                    placeholder = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (state.canSubmit) {
                                    state.eventSink(LoginEvents.Submit)
                                }
                            },
                        ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            // Sign In Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    state.eventSink(LoginEvents.Submit)
                },
                enabled = state.canSubmit,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp)
                        .height(48.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        disabledContainerColor = Color(0xFF94A3B8),
                    ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forgot Password
            TextButton(
                onClick = { showForgotPasswordDialog = true },
            ) {
                Text(
                    text = "Forgot your password?",
                    color = Color(0xFF0EA5E9),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Register CTA
            TextButton(
                onClick = onNavigateToRegister,
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color(0xFF64748B),
                    fontSize = 15.sp,
                )
                Text(
                    text = "Register now",
                    color = Color(0xFF0EA5E9),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // USA Pride Banner
            USAPrideBanner()
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = forgotPasswordEmail,
            onEmailChange = { forgotPasswordEmail = it },
            onDismiss = { showForgotPasswordDialog = false },
            onSubmit = { email ->
                // Handle forgot password
                state.eventSink(LoginEvents.ForgotPassword(email))
                showForgotPasswordDialog = false
            },
            isLoading = false, // Add loading state when needed
        )
    }

    // Reset Password Dialog
    if (showResetPasswordDialog) {
        ResetPasswordDialog(
            tempPassword = tempPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            onTempPasswordChange = { tempPassword = it },
            onNewPasswordChange = { newPassword = it },
            onConfirmPasswordChange = { confirmPassword = it },
            onDismiss = { showResetPasswordDialog = false },
            onSubmit = { temp, new, confirm ->
                // Handle reset password
                state.eventSink(LoginEvents.ResetPassword(temp, new, confirm))
                showResetPasswordDialog = false
            },
            isLoading = false, // Add loading state when needed
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isLoading: Boolean,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "Forgot Password",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enter your email address and we'll send you a temporary password.",
                    fontSize = 16.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = { Text("Enter your email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF94A3B8),
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = { onSubmit(email) },
                        enabled = email.isNotEmpty() && !isLoading,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9),
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Send Reset Email",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetPasswordDialog(
    tempPassword: String,
    newPassword: String,
    confirmPassword: String,
    onTempPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
    isLoading: Boolean,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "Reset Password",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enter the temporary password from your email and set a new password.",
                    fontSize = 16.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = onTempPasswordChange,
                    placeholder = { Text("Temporary password from email") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    placeholder = { Text("New password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = { Text("Confirm new password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF94A3B8),
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = { onSubmit(tempPassword, newPassword, confirmPassword) },
                        enabled =
                            tempPassword.isNotEmpty() && newPassword.isNotEmpty() &&
                                confirmPassword.isNotEmpty() && !isLoading,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9),
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Reset Password",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun USAPrideBanner() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFF0F9FF),
            ),
        border = BorderStroke(1.dp, Color(0xFFE0F2FE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🇺🇸",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 8.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Designed & Made in the USA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0EA5E9),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Your data is protected by US technology and servers",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    ElementTheme {
        LoginScreen(
            state =
                LoginState(
                    username = "",
                    password = "",
                    isLoading = false,
                    errorMessage = null,
                    canSubmit = false,
                    isLoginSuccessful = false,
                    eventSink = {},
                ),
            onNavigateToRegister = {},
        )
    }
}

@Preview
@Composable
private fun USAPrideBannerPreview() {
    ElementTheme {
        USAPrideBanner()
    }
}
