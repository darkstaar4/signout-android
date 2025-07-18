/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.messages.sender

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.regions.Regions
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileTimelineDetails
import io.element.android.libraries.usersearch.api.UserMappingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * CompositionLocal to provide UserMappingService to child composables in matrixui
 */
val LocalUserMappingService = staticCompositionLocalOf<UserMappingService?> { null }

// https://www.figma.com/file/Ni6Ii8YKtmXCKYNE90cC67/Timeline-(new)?type=design&node-id=917-80169&mode=design&t=A0CJCBbMqR8NOwUQ-0
@Composable
fun SenderName(
    senderId: UserId,
    senderProfile: ProfileTimelineDetails,
    senderNameMode: SenderNameMode,
    modifier: Modifier = Modifier,
    currentUserId: UserId? = null,
) {
    val context = LocalContext.current
    val userMappingService = LocalUserMappingService.current
    var enhancedDisplayName by remember { mutableStateOf<String?>(null) }
    
    // Get enhanced display name for any user using UserMappingService
    LaunchedEffect(senderId, userMappingService) {
        if (userMappingService != null) {
            try {
                val username = senderId.value.substringAfter("@").substringBefore(":")
                val userMapping = userMappingService.getUserMapping(username)
                
                if (userMapping != null) {
                    enhancedDisplayName = userMapping.displayName
                    Timber.d("SenderName: Found enhanced display name for $username: ${userMapping.displayName}")
                } else {
                    Timber.d("SenderName: No enhanced display name found for $username")
                    // For current user, try to get from Cognito as fallback
                    if (senderId == currentUserId) {
                        enhancedDisplayName = getCognitoDisplayName(context)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "SenderName: Error getting enhanced display name for ${senderId.value}")
                // For current user, try to get from Cognito as fallback
                if (senderId == currentUserId) {
                    enhancedDisplayName = getCognitoDisplayName(context)
                }
            }
        } else if (senderId == currentUserId) {
            // Fallback to Cognito for current user if UserMappingService is not available
            enhancedDisplayName = getCognitoDisplayName(context)
        }
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (senderProfile) {
            is ProfileTimelineDetails.Error,
            ProfileTimelineDetails.Pending,
            ProfileTimelineDetails.Unavailable -> {
                MainText(text = enhancedDisplayName ?: senderId.value, mode = senderNameMode)
            }
            is ProfileTimelineDetails.Ready -> {
                val displayName = senderProfile.displayName
                
                // Priority: Enhanced display name > Matrix display name > User ID
                val nameToShow = enhancedDisplayName ?: displayName ?: senderId.value
                
                MainText(text = nameToShow, mode = senderNameMode)
                
                // Only show secondary text (Matrix ID) if display name is ambiguous and we're not using enhanced name
                if (enhancedDisplayName == null && senderProfile.displayNameAmbiguous && !displayName.isNullOrEmpty()) {
                    SecondaryText(text = senderId.value, mode = senderNameMode)
                }
            }
        }
    }
}

private suspend fun getCognitoDisplayName(context: Context): String? = withContext(Dispatchers.IO) {
    try {
        val userPool = CognitoUserPool(context, "us-east-1_ltpOFMyVw", "7qj6hl3mql3kgcgp6lhqj4qd15", null, Regions.US_EAST_1)
        val currentUser: CognitoUser = userPool.currentUser
        
        if (currentUser.userId != null) {
            var result: String? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            
            currentUser.getDetails(object : com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler {
                override fun onSuccess(cognitoUserDetails: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails?) {
                    try {
                        val attributes = cognitoUserDetails?.attributes
                        val firstName = attributes?.attributes?.get("given_name") ?: ""
                        val lastName = attributes?.attributes?.get("family_name") ?: ""
                        
                        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            result = "$firstName $lastName".trim()
                        }
                    } catch (e: Exception) {
                        // Ignore
                    } finally {
                        latch.countDown()
                    }
                }
                
                override fun onFailure(exception: Exception?) {
                    latch.countDown()
                }
            })
            
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            result
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun RowScope.MainText(
    text: String,
    mode: SenderNameMode,
) {
    val style = when (mode) {
        is SenderNameMode.Timeline -> ElementTheme.typography.fontBodyMdMedium
        SenderNameMode.ActionList,
        SenderNameMode.Reply -> ElementTheme.typography.fontBodySmMedium
    }
    val modifier = when (mode) {
        is SenderNameMode.Timeline -> Modifier.alignByBaseline()
        SenderNameMode.ActionList,
        SenderNameMode.Reply -> Modifier
    }
    val color = when (mode) {
        is SenderNameMode.Timeline -> mode.mainColor
        SenderNameMode.ActionList,
        SenderNameMode.Reply -> ElementTheme.colors.textPrimary
    }
    Text(
        modifier = modifier.clipToBounds(),
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowScope.SecondaryText(
    text: String,
    mode: SenderNameMode,
) {
    val style = when (mode) {
        is SenderNameMode.Timeline -> ElementTheme.typography.fontBodySmRegular
        SenderNameMode.ActionList,
        SenderNameMode.Reply -> ElementTheme.typography.fontBodyXsRegular
    }
    val modifier = when (mode) {
        is SenderNameMode.Timeline -> Modifier.alignByBaseline()
        SenderNameMode.ActionList,
        SenderNameMode.Reply -> Modifier
    }
    Text(
        modifier = modifier.clipToBounds(),
        text = text,
        style = style,
        color = ElementTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@PreviewsDayNight
@Composable
internal fun SenderNamePreview(
    @PreviewParameter(SenderNameDataProvider::class) senderNameData: SenderNameData,
) = ElementPreview {
    SenderName(
        senderId = senderNameData.userId,
        senderProfile = senderNameData.profileTimelineDetails,
        senderNameMode = senderNameData.senderNameMode,
    )
}
