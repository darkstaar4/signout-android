/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoServiceConstants
import com.amazonaws.regions.Regions
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ListItemStyle
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.MatrixUserHeader
import io.element.android.libraries.matrix.ui.components.MatrixUserWithNullProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Composable
fun UserPreferences(
    user: MatrixUser?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
    MatrixUserHeader(
            matrixUser = user,
            isCurrentUser = true // Hide Matrix ID in settings
        )
        
        // Add Cognito user information
        user?.let { matrixUser ->
            CognitoUserInfo(
                matrixUser = matrixUser,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun CognitoUserInfo(
    matrixUser: MatrixUser,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cognitoInfo by remember { mutableStateOf<CognitoUserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSessionMismatch by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    
    LaunchedEffect(matrixUser.userId) {
        try {
            Timber.d("CognitoUserInfo: Loading user info for ${matrixUser.userId}")
            val result = getCognitoUserInfoWithSessionCheck(context, matrixUser.userId.value)
            cognitoInfo = result.cognitoData
            showSessionMismatch = result.hasSessionMismatch
            Timber.d("CognitoUserInfo: Loaded info: $cognitoInfo, mismatch: $showSessionMismatch")
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserInfo: Error loading user info")
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loading profile...",
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary
                )
            }
        } else if (showSessionMismatch) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ElementTheme.colors.bgSubtleSecondary
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Session Mismatch",
                        tint = ElementTheme.colors.iconCriticalPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Profile Session Mismatch",
                        style = ElementTheme.typography.fontBodyLgMedium,
                        color = ElementTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Matrix account doesn't match your Cognito profile session. Please login to sync your profile information.",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showLoginDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login to Profile")
                    }
                }
            }
        } else if (cognitoInfo != null) {
            // Show the existing profile information
            CognitoInfoDisplay(cognitoInfo!!)
        }
    }

    // Login Dialog
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isLoggingIn) {
                    showLoginDialog = false
                    loginEmail = ""
                    loginPassword = ""
                }
            },
            title = {
                Text("Login to Profile")
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Cognito credentials to sync your profile:",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = loginEmail,
                        onValueChange = { loginEmail = it },
                        label = { Text("Email") },
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (loginEmail.isNotBlank() && loginPassword.isNotBlank()) {
                            isLoggingIn = true
                            // Perform login in background
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    Timber.d("Manual login attempt for: $loginEmail")
                                    val userPool = CognitoUserPool(
                                        context.applicationContext,
                                        "us-east-1_ltpOFMyVw",
                                        "6j4hfhvvdokuj458r42aoj0j4d",
                                        null,
                                        Regions.US_EAST_1
                                    )
                                    
                                    val user = userPool.getUser(loginEmail)
                                    val authDetails = AuthenticationDetails(loginEmail, loginPassword, null)
                                    
                                    val latch = CountDownLatch(1)
                                    var loginSuccess = false
                                    
                                    user.getSessionInBackground(object : AuthenticationHandler {
                                        override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                                            Timber.d("Manual login successful for: $loginEmail")
                                            
                                            // After successful login, check and update custom:matrix_username if needed
                                            user.getDetailsInBackground(object : GetDetailsHandler {
                                                override fun onSuccess(userDetails: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails?) {
                                                    val attributes = userDetails?.attributes?.attributes
                                                    val currentMatrixUsername = attributes?.get("custom:matrix_username")
                                                    val expectedMatrixUsername = matrixUser.userId.value.substringAfter("@").substringBefore(":")
                                                    
                                                    if (currentMatrixUsername == null || currentMatrixUsername != expectedMatrixUsername) {
                                                        Timber.d("Updating custom:matrix_username from '$currentMatrixUsername' to '$expectedMatrixUsername'")
                                                        
                                                        // Update the custom:matrix_username attribute
                                                        val updateAttributes = CognitoUserAttributes().apply {
                                                            addAttribute("custom:matrix_username", expectedMatrixUsername)
                                                        }
                                                        
                                                        user.updateAttributesInBackground(updateAttributes, object : UpdateAttributesHandler {
                                                            override fun onSuccess(attributesVerificationList: MutableList<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails>?) {
                                                                Timber.d("Successfully updated custom:matrix_username attribute")
                                                            }
                                                            
                                                            override fun onFailure(exception: Exception?) {
                                                                Timber.w("Failed to update custom:matrix_username attribute: ${exception?.message}")
                                                            }
                                                        })
                                                    }
                                                }
                                                
                                                override fun onFailure(exception: Exception?) {
                                                    Timber.w("Failed to get user details after login: ${exception?.message}")
                                                }
                                            })
                                            
                                            loginSuccess = true
                                            latch.countDown()
                                        }
                                        
                                        override fun onFailure(exception: Exception?) {
                                            Timber.e(exception, "Manual login failed for: $loginEmail")
                                            latch.countDown()
                                        }
                                        
                                        override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {
                                            authenticationContinuation?.setAuthenticationDetails(authDetails)
                                            authenticationContinuation?.continueTask()
                                        }
                                        
                                        override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation?) {
                                            // MFA not supported in this flow
                                            multiFactorAuthenticationContinuation?.continueTask()
                                        }
                                        
                                        override fun authenticationChallenge(continuation: ChallengeContinuation?) {
                                            // Handle challenges if needed
                                            continuation?.continueTask()
                                        }
                                    })
                                    
                                    latch.await(30, TimeUnit.SECONDS)
                                    
                                    withContext(Dispatchers.Main) {
                                        isLoggingIn = false
                                        if (loginSuccess) {
                                            showLoginDialog = false
                                            showSessionMismatch = false
                                            loginEmail = ""
                                            loginPassword = ""
                                            // Refresh the profile info
                                            isLoading = true
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    val result = getCognitoUserInfoWithSessionCheck(context, matrixUser.userId.value)
                                                    withContext(Dispatchers.Main) {
                                                        cognitoInfo = result.cognitoData
                                                        showSessionMismatch = result.hasSessionMismatch
                                                        isLoading = false
                                                    }
                                                } catch (e: Exception) {
                                                    Timber.e(e, "Error refreshing profile after login")
                                                    withContext(Dispatchers.Main) {
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        } else {
                                            // Show error message
                                            Timber.e("Login failed")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Manual login error")
                                    withContext(Dispatchers.Main) {
                                        isLoggingIn = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoggingIn && loginEmail.isNotBlank() && loginPassword.isNotBlank()
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Login")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (!isLoggingIn) {
                            showLoginDialog = false
                            loginEmail = ""
                            loginPassword = ""
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CognitoInfoDisplay(cognitoInfo: CognitoUserData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Display professional information
        if (cognitoInfo.fullName.isNotEmpty()) {
            Text(
                text = cognitoInfo.fullName,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (cognitoInfo.professionalTitle.isNotEmpty()) {
            Text(
                text = cognitoInfo.professionalTitle,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Display specialty and office city on the same line
        val specialtyAndCity = buildString {
            if (cognitoInfo.specialty.isNotEmpty()) {
                append(cognitoInfo.specialty)
            }
            if (cognitoInfo.officeCity.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(cognitoInfo.officeCity)
            }
        }
        
        if (specialtyAndCity.isNotEmpty()) {
            Text(
                text = specialtyAndCity,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class CognitoUserData(
    val fullName: String = "",
    val professionalTitle: String = "",
    val specialty: String = "",
    val officeCity: String = ""
)

private data class CognitoUserInfoResult(
    val cognitoData: CognitoUserData?,
    val hasSessionMismatch: Boolean = false
)

private suspend fun getCognitoUserInfoWithSessionCheck(
    context: android.content.Context,
    matrixUserId: String
): CognitoUserInfoResult {
    return withContext(Dispatchers.IO) {
        try {
            Timber.d("getCognitoUserInfoWithSessionCheck: Starting session check for Matrix user: $matrixUserId")
            val userPool = CognitoUserPool(
                context.applicationContext,
                "us-east-1_ltpOFMyVw",
                "6j4hfhvvdokuj458r42aoj0j4d",
                null,
                Regions.US_EAST_1
            )
            
            val currentUser = userPool.currentUser
            Timber.d("getCognitoUserInfoWithSessionCheck: Current Cognito user: ${currentUser?.userId}")
            
            if (currentUser != null) {
                val latch = CountDownLatch(1)
                var result: CognitoUserInfoResult? = null
                
                currentUser.getDetailsInBackground(object : GetDetailsHandler {
                    override fun onSuccess(userDetails: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails?) {
                        Timber.d("getCognitoUserInfoWithSessionCheck: Got user details successfully")
                        val attributes = userDetails?.attributes?.attributes
                        Timber.d("getCognitoUserInfoWithSessionCheck: Attributes: $attributes")
                        
                        // Check if this Cognito user matches the Matrix user
                        val cognitoMatrixUsername = attributes?.get("custom:matrix_username")
                        val expectedMatrixUsername = matrixUserId.substringAfter("@").substringBefore(":")
                        
                        val hasSessionMismatch = cognitoMatrixUsername != expectedMatrixUsername
                        
                        if (hasSessionMismatch) {
                            Timber.w("getCognitoUserInfoWithSessionCheck: Session mismatch detected!")
                            Timber.w("getCognitoUserInfoWithSessionCheck: Cognito matrix_username: $cognitoMatrixUsername")
                            Timber.w("getCognitoUserInfoWithSessionCheck: Expected matrix_username: $expectedMatrixUsername")
                            
                            // Try to fix the session mismatch by logging out the wrong user
                            try {
                                Timber.d("getCognitoUserInfoWithSessionCheck: Attempting to fix session mismatch...")
                                currentUser.signOut()
                                Timber.d("getCognitoUserInfoWithSessionCheck: Successfully logged out wrong user")
                            } catch (e: Exception) {
                                Timber.w("getCognitoUserInfoWithSessionCheck: Failed to logout wrong user: ${e.message}")
                            }
                            
                            result = CognitoUserInfoResult(
                                cognitoData = null,
                                hasSessionMismatch = true
                            )
                        } else {
                            // Session is correct, return user data
                            val cognitoData = CognitoUserData(
                                fullName = buildString {
                                    val firstName = attributes?.get("given_name") ?: ""
                                    val lastName = attributes?.get("family_name") ?: ""
                                    if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                        append("$firstName $lastName".trim())
                                    }
                                },
                                professionalTitle = attributes?.get("custom:professional_title") ?: "",
                                specialty = attributes?.get("custom:specialty") ?: "",
                                officeCity = attributes?.get("custom:office_city") ?: ""
                            )
                            
                            result = CognitoUserInfoResult(
                                cognitoData = cognitoData,
                                hasSessionMismatch = false
                            )
                        }
                        
                        Timber.d("getCognitoUserInfoWithSessionCheck: Created result: $result")
                        latch.countDown()
                    }
                    
                    override fun onFailure(exception: Exception?) {
                        Timber.w("getCognitoUserInfoWithSessionCheck: Failed to get Cognito user details: ${exception?.message}")
                        // This is likely an authentication error - treat as session mismatch
                        try {
                            currentUser.signOut()
                            Timber.d("getCognitoUserInfoWithSessionCheck: Logged out user due to authentication failure")
                        } catch (e: Exception) {
                            Timber.w("getCognitoUserInfoWithSessionCheck: Failed to logout user: ${e.message}")
                        }
                        result = CognitoUserInfoResult(cognitoData = null, hasSessionMismatch = true)
                        latch.countDown()
                    }
                })
                
                // Wait for response with timeout
                val completed = latch.await(5, TimeUnit.SECONDS)
                if (!completed) {
                    Timber.w("getCognitoUserInfoWithSessionCheck: Timeout waiting for user details")
                    return@withContext CognitoUserInfoResult(cognitoData = null, hasSessionMismatch = true)
                }
                
                Timber.d("getCognitoUserInfoWithSessionCheck: Returning result: $result")
                result ?: CognitoUserInfoResult(cognitoData = null, hasSessionMismatch = true)
            } else {
                Timber.w("getCognitoUserInfoWithSessionCheck: No current user found")
                CognitoUserInfoResult(cognitoData = null, hasSessionMismatch = false)
            }
        } catch (e: Exception) {
            Timber.w("getCognitoUserInfoWithSessionCheck: Error getting Cognito user info: ${e.message}")
            CognitoUserInfoResult(cognitoData = null, hasSessionMismatch = false)
        }
    }
}

private suspend fun getCognitoUserInfo(context: android.content.Context): CognitoUserData? {
    return withContext(Dispatchers.IO) {
        try {
            Timber.d("getCognitoUserInfo: Starting to get user info")
            val userPool = CognitoUserPool(
                context.applicationContext,
                "us-east-1_ltpOFMyVw",
                "6j4hfhvvdokuj458r42aoj0j4d",
                null,
                Regions.US_EAST_1
            )
            
            val currentUser = userPool.currentUser
            Timber.d("getCognitoUserInfo: Current user: ${currentUser?.userId}")
            
            if (currentUser != null) {
                val latch = CountDownLatch(1)
                var result: CognitoUserData? = null
                
                currentUser.getDetailsInBackground(object : GetDetailsHandler {
                    override fun onSuccess(userDetails: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails?) {
                        Timber.d("getCognitoUserInfo: Got user details successfully")
                        val attributes = userDetails?.attributes?.attributes
                        Timber.d("getCognitoUserInfo: Attributes: $attributes")
                        
                        result = CognitoUserData(
                            fullName = buildString {
                                val firstName = attributes?.get("given_name") ?: ""
                                val lastName = attributes?.get("family_name") ?: ""
                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    append("$firstName $lastName".trim())
                                }
                            },
                            professionalTitle = attributes?.get("custom:professional_title") ?: "",
                            specialty = attributes?.get("custom:specialty") ?: "",
                            officeCity = attributes?.get("custom:office_city") ?: ""
                        )
                        Timber.d("getCognitoUserInfo: Created result: $result")
                        latch.countDown()
                    }
                    
                    override fun onFailure(exception: Exception?) {
                        Timber.w("getCognitoUserInfo: Failed to get Cognito user details: ${exception?.message}")
                        result = null
                        latch.countDown()
                    }
                })
                
                // Wait for response with timeout
                val completed = latch.await(5, TimeUnit.SECONDS)
                if (!completed) {
                    Timber.w("getCognitoUserInfo: Timeout waiting for user details")
                }
                Timber.d("getCognitoUserInfo: Returning result: $result")
                result
            } else {
                Timber.w("getCognitoUserInfo: No current user found")
                null
            }
        } catch (e: Exception) {
            Timber.w("getCognitoUserInfo: Error getting Cognito user info: ${e.message}")
            null
        }
    }
}

@PreviewsDayNight
@Composable
internal fun UserPreferencesPreview(@PreviewParameter(MatrixUserWithNullProvider::class) matrixUser: MatrixUser?) = ElementPreview {
    UserPreferences(matrixUser)
}
