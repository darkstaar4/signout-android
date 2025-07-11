/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.matrix.ui.model.getBestNameForCurrentUser
import io.element.android.libraries.matrix.ui.model.getUserIdForDisplay

@Composable
fun MatrixUserHeader(
    matrixUser: MatrixUser?,
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean = false,
    // TODO handle click on this item, to let the user be able to update their profile.
    // onClick: () -> Unit,
) {
    if (matrixUser == null) {
        MatrixUserHeaderPlaceholder(modifier = modifier)
    } else {
        MatrixUserHeaderContent(
            matrixUser = matrixUser,
            isCurrentUser = isCurrentUser,
            modifier = modifier,
            // onClick = onClick
        )
    }
}

@Composable
private fun MatrixUserHeaderContent(
    matrixUser: MatrixUser,
    isCurrentUser: Boolean = false,
    modifier: Modifier = Modifier,
    // onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            // .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            modifier = Modifier
                .padding(vertical = 12.dp),
            avatarData = matrixUser.getAvatarData(size = AvatarSize.UserPreference),
            avatarType = AvatarType.User,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Name
            Text(
                modifier = Modifier.clipToBounds(),
                text = matrixUser.getBestNameForCurrentUser(isCurrentUser),
                maxLines = 1,
                style = ElementTheme.typography.fontHeadingSmMedium,
                overflow = TextOverflow.Ellipsis,
                color = ElementTheme.colors.textPrimary,
            )
            // Id
            matrixUser.getUserIdForDisplay(isCurrentUser)?.let { userIdToDisplay ->
                Text(
                    text = userIdToDisplay,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun MatrixUserHeaderPreview(@PreviewParameter(MatrixUserProvider::class) matrixUser: MatrixUser) = ElementPreview {
    MatrixUserHeader(matrixUser)
}
