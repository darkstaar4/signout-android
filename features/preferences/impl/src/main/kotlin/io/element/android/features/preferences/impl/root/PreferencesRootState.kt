/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.matrix.api.core.DeviceId
import io.element.android.libraries.matrix.api.user.MatrixUser

data class PreferencesRootState(
    val myUser: MatrixUser,
    val version: String,
    val deviceId: DeviceId?,
    val showSecureBackup: Boolean,
    val showSecureBackupBadge: Boolean,
    val accountManagementUrl: String?,
    val devicesManagementUrl: String?,
    val canReportBug: Boolean,
    val showAnalyticsSettings: Boolean,
    val showDeveloperSettings: Boolean,
    val canDeactivateAccount: Boolean,
    val showLockScreenSettings: Boolean,
    val showNotificationSettings: Boolean,
    val showBlockedUsersItem: Boolean,
    val directLogoutState: DirectLogoutState,
    val snackbarMessage: SnackbarMessage?,
    val pendingNavigationEvent: PreferencesRootEvents?,
    val eventSink: (PreferencesRootEvents) -> Unit,
)
