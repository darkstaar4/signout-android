/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.ui.R
import io.element.android.libraries.matrix.ui.model.InviteSender
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.UserMapping
import timber.log.Timber

@Composable
fun InviteSenderView(
    inviteSender: InviteSender,
    modifier: Modifier = Modifier,
    hideAvatarImage: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(vertical = 2.dp)) {
            Avatar(
                avatarData = inviteSender.avatarData,
                avatarType = AvatarType.User,
                hideImage = hideAvatarImage,
            )
        }
        Text(
            text = inviteSender.annotatedString(),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun InviteSenderViewPreview() = ElementPreview {
    InviteSenderView(
        inviteSender = InviteSender(
            userId = UserId("@bob:example.com"),
            displayName = "Bob",
            avatarData = AvatarData(
                id = "@bob:example.com",
                name = "Bob",
                url = null,
                size = AvatarSize.InviteSender,
            ),
            membershipChangeReason = null,
        )
    )
}
