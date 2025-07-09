/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings

fun MatrixUser.getAvatarData(size: AvatarSize) = AvatarData(
    id = userId.value,
    name = displayName,
    url = avatarUrl,
    size = size,
)

fun MatrixUser.getBestName(): String {
    return displayName?.takeIf { it.isNotEmpty() } ?: userId.value
}

fun MatrixUser.getBestNameForCurrentUser(isCurrentUser: Boolean): String {
    return if (isCurrentUser && displayName?.isNotEmpty() == true) {
        // For the current user, show only the display name (e.g., "John Smith") without the Matrix ID
        displayName!!
    } else {
        // For other users, use the standard logic
        getBestName()
    }
}

fun MatrixUser.getUserIdForDisplay(isCurrentUser: Boolean): String? {
    return if (isCurrentUser) {
        // For the current user, don't show the Matrix ID to hide signout.io domain
        null
    } else {
        // For other users, show the Matrix ID if they have a display name
        if (displayName?.isNotEmpty() == true) userId.value else null
    }
}

@Composable
fun MatrixUser.getFullName(): String {
    return displayName.let { name ->
        if (name.isNullOrBlank()) {
            userId.value
        } else {
            // Strip the domain from the user ID to show only the username part
            val usernameOnly = userId.value.substringBefore(":")
            stringResource(CommonStrings.common_name_and_id, name, usernameOnly)
        }
    }
}
