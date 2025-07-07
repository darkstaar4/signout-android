/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.verification

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.customauth.impl.R
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Surface as ElementSurface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh

@Composable
fun VerificationScreen(
    state: VerificationState,
    onNavigateToLogin: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    
    // Request focus on the code field when the screen loads
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Handle successful verification
    if (state.isVerificationSuccessful) {
        // Show success screen
        VerificationSuccessScreen(
            username = state.username,
            isMatrixAccountCreated = state.isMatrixAccountCreated,
            onContinue = onNavigateToCredentials,
            modifier = modifier
        )
        return
    }

    ElementSurface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = Color(0xFFE0F2FE),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.signout_square_logo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF0EA5E9)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Verify Your Email",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "We've sent a verification code to",
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = state.email,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Verification code field
            OutlinedTextField(
                value = state.verificationCode,
                onValueChange = { code ->
                    if (code.length <= 6 && code.all { it.isDigit() }) {
                        state.eventSink(VerificationEvents.SetVerificationCode(code))
                    }
                },
                label = { Text("Verification Code") },
                placeholder = { Text("Enter 6-digit code") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.canSubmit) {
                            state.eventSink(VerificationEvents.Submit)
                        }
                    }
                ),
                isError = state.errorMessage != null,
                supportingText = {
                    state.errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFEF4444)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedIndicatorColor = Color(0xFF0EA5E9),
                    unfocusedIndicatorColor = Color(0xFFE2E8F0),
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Verify button
            Button(
                onClick = { state.eventSink(VerificationEvents.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit && !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Verify Email",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend code
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive the code?",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = { state.eventSink(VerificationEvents.ResendCode) },
                    enabled = !state.isResending
                ) {
                    Text(
                        text = if (state.isResending) "Sending..." else "Resend",
                        color = Color(0xFF0EA5E9),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Back to login
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Back to Login",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun VerificationScreenPreview() {
    ElementTheme {
        VerificationScreen(
            state = VerificationState(
                username = "johndoe",
                email = "john.doe@example.com",
                verificationCode = "",
                isLoading = false,
                isResending = false,
                errorMessage = null,
                canSubmit = false,
                isVerificationSuccessful = false,
                isMatrixAccountCreated = false,
                eventSink = {}
            ),
            onNavigateToLogin = {},
            onNavigateToCredentials = {}
        )
    }
}

@Composable
private fun VerificationSuccessScreen(
    username: String,
    isMatrixAccountCreated: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElementSurface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF0EA5E9)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Success title
            Text(
                text = "Email Verified Successfully!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Success message
            Text(
                text = "Welcome, $username! Your email has been verified and your account is ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            
            // Matrix account status
            if (isMatrixAccountCreated) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Matrix Account Created",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF059669)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Matrix account created successfully",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF059669)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Continue button
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
} 