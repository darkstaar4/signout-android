/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.deeplink.usecase

import android.app.Activity
import io.element.android.libraries.androidutils.system.startSharePlainTextIntent
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.services.toolbox.api.strings.StringProvider
import javax.inject.Inject
import io.element.android.libraries.androidutils.R as AndroidUtilsR

class InviteFriendsUseCase @Inject constructor(
    private val stringProvider: StringProvider,
    private val buildMeta: BuildMeta,
) {
    fun execute(activity: Activity) {
        val appName = buildMeta.applicationName
        activity.startSharePlainTextIntent(
            activityResultLauncher = null,
            chooserTitle = stringProvider.getString(CommonStrings.action_invite_friends),
            text = stringProvider.getString(CommonStrings.invite_friends_text),
            extraTitle = stringProvider.getString(CommonStrings.invite_friends_rich_title, appName),
            noActivityFoundMessage = stringProvider.getString(AndroidUtilsR.string.error_no_compatible_app_found)
        )
    }
}
