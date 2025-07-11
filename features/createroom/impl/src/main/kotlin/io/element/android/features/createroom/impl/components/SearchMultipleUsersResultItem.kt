/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.createroom.impl.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Checkbox
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserMapping

@Composable
internal fun SearchMultipleUsersResultItem(
    result: UserSearchResult,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val matrixUser = result.matrixUser
    val userMapping = result.userMapping
    
    // Format display name: use given_name family_name from Cognito if available
    val displayName = userMapping?.displayName ?: matrixUser.displayName ?: matrixUser.userId.value
    
    // Format secondary text: @preferred_username | custom:specialty | custom:office_city
    val secondaryText = buildString {
        if (userMapping != null) {
            // Use Matrix username (e.g., "nabilbaig") instead of Cognito UUID
            val preferredUsername = userMapping.matrixUsername ?: extractUsernameFromMatrixId(matrixUser.userId.value)
            append("@$preferredUsername")
            
            // Add specialty if available
            userMapping.specialty?.let { specialty ->
                if (specialty.isNotEmpty()) {
                    append(" | $specialty")
                }
            }
            
            // Add office city if available
            userMapping.officeCity?.let { city ->
                if (city.isNotEmpty()) {
                    append(" | $city")
                }
            }
        } else {
            // Fallback to Matrix ID if no Cognito data
            append(matrixUser.userId.value)
        }
    }

    ListItem(
        modifier = modifier,
        onClick = { onSelectionChanged(!isSelected) },
        headlineContent = {
            Text(
                text = displayName,
                style = ElementTheme.typography.fontBodyLgRegular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = secondaryText,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = ListItemContent.Custom {
            Avatar(
                avatarData = AvatarData(
                    id = matrixUser.userId.value,
                    name = displayName,
                    url = matrixUser.avatarUrl,
                    size = AvatarSize.UserListItem,
                ),
                avatarType = AvatarType.User,
                modifier = Modifier.clip(CircleShape),
            )
        },
        trailingContent = ListItemContent.Checkbox(
            checked = isSelected,
            enabled = true,
        ),
    )
}

private fun extractUsernameFromMatrixId(matrixId: String): String {
    // Extract username from @username:domain format
    return matrixId.removePrefix("@").substringBefore(":")
}

@PreviewsDayNight
@Composable
internal fun SearchMultipleUsersResultItemPreview() = ElementPreview {
    SearchMultipleUsersResultItem(
        result = UserSearchResult(
            matrixUser = MatrixUser(
                userId = UserId("@racexcars:signout.io"),
                displayName = "Race X Cars",
                avatarUrl = null
            ),
            userMapping = UserMapping(
                matrixUserId = "@racexcars:signout.io",
                matrixUsername = "racexcars",
                cognitoUsername = "racexcars",
                displayName = "Race X Cars",
                firstName = "Race",
                lastName = "Cars",
                email = "racexcars@example.com",
                specialty = "Automotive Engineering",
                officeCity = "Detroit",
                avatarUrl = null
            )
        ),
        isSelected = false,
        onSelectionChanged = {},
    )
}
