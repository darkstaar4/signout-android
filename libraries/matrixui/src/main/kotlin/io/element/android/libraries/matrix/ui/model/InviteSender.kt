/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.ui.R

@Immutable
data class InviteSender(
    val userId: UserId,
    val displayName: String,
    val avatarData: AvatarData,
    val membershipChangeReason: String?,
) {
    @Composable
    fun annotatedString(): AnnotatedString {
        // Extract just the username part (without homeserver) from the full Matrix ID
        val username = userId.value.substringAfter("@").substringBefore(":")
        return stringResource(R.string.screen_invites_invited_you, displayName, username).let { text ->
            val senderNameStart = LocalContext.current.getString(R.string.screen_invites_invited_you).indexOf("%1\$s")
            AnnotatedString(
                text = text,
                spanStyles = listOf(
                    AnnotatedString.Range(
                        SpanStyle(
                            fontWeight = FontWeight.Medium,
                            color = ElementTheme.colors.textPrimary
                        ),
                        start = senderNameStart,
                        end = senderNameStart + displayName.length
                    )
                )
            )
        }
    }
}

fun RoomMember.toInviteSender() = InviteSender(
    userId = userId,
    displayName = displayName ?: "",
    avatarData = getAvatarData(size = AvatarSize.InviteSender),
    membershipChangeReason = membershipChangeReason
)
