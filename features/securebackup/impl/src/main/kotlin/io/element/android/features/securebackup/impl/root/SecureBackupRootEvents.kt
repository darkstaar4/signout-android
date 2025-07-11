/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.root

sealed interface SecureBackupRootEvents {
    data object RetryKeyBackupState : SecureBackupRootEvents
    data object EnableKeyStorage : SecureBackupRootEvents
    data object DisplayKeyStorageDisabledError : SecureBackupRootEvents
    data object DismissDialog : SecureBackupRootEvents
    data object VerifyDevice : SecureBackupRootEvents
}
