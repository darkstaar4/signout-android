/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.regions.Regions
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.MatrixUserHeader
import io.element.android.libraries.matrix.ui.components.MatrixUserWithNullProvider
import kotlinx.coroutines.Dispatchers
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
    
    LaunchedEffect(matrixUser.userId) {
        try {
            Timber.d("CognitoUserInfo: Loading user info for ${matrixUser.userId}")
            cognitoInfo = getCognitoUserInfo(context)
            Timber.d("CognitoUserInfo: Loaded info: $cognitoInfo")
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserInfo: Error loading user info")
        } finally {
            isLoading = false
        }
    }
    
    if (isLoading) {
        // Show loading state
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Loading user info...",
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        cognitoInfo?.let { info ->
            Column(modifier = modifier) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Display professional information
                if (info.fullName.isNotEmpty()) {
                    Text(
                        text = info.fullName,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (info.professionalTitle.isNotEmpty()) {
                    Text(
                        text = info.professionalTitle,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Display specialty and office city on the same line
                val specialtyAndCity = buildString {
                    if (info.specialty.isNotEmpty()) {
                        append(info.specialty)
                    }
                    if (info.officeCity.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append(info.officeCity)
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
        } ?: run {
            // Show fallback when no Cognito info is available
            Column(modifier = modifier) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SignOut User",
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class CognitoUserData(
    val fullName: String = "",
    val professionalTitle: String = "",
    val specialty: String = "",
    val officeCity: String = ""
)

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
                
                currentUser.getDetailsInBackground(object : com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler {
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
