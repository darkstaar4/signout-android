/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.features.securebackup.impl.loggerTagDisable
import io.element.android.features.securebackup.impl.loggerTagRoot
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SecureBackupRootPresenter @Inject constructor(
    private val encryptionService: EncryptionService,
    private val sessionVerificationService: SessionVerificationService,
    private val buildMeta: BuildMeta,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<SecureBackupRootState> {
    @Composable
    override fun present(): SecureBackupRootState {
        val localCoroutineScope = rememberCoroutineScope()
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()

        val backupState by encryptionService.backupStateStateFlow.collectAsState()
        val recoveryState by encryptionService.recoveryStateStateFlow.collectAsState()
        val sessionVerifiedStatus by sessionVerificationService.sessionVerifiedStatus.collectAsState()
        val enableAction: MutableState<AsyncAction<Unit>> = remember { mutableStateOf(AsyncAction.Uninitialized) }
        var displayKeyStorageDisabledError by remember { mutableStateOf(false) }
        var onDeviceVerificationClick: (() -> Unit)? by remember { mutableStateOf(null) }
        
        // Add debugging for state changes
        android.util.Log.d("SecureBackupRoot", "Current states - backup: $backupState, recovery: $recoveryState")
        
        Timber.tag(loggerTagRoot.value).d("backupState: $backupState")
        Timber.tag(loggerTagRoot.value).d("recoveryState: $recoveryState")
        Timber.tag(loggerTagRoot.value).d("sessionVerifiedStatus: $sessionVerifiedStatus")

        val doesBackupExistOnServerAction: MutableState<AsyncData<Boolean>> = remember { mutableStateOf(AsyncData.Uninitialized) }

        LaunchedEffect(backupState) {
            if (backupState == BackupState.UNKNOWN) {
                getKeyBackupStatus(doesBackupExistOnServerAction)
            }
        }

        fun handleEvents(event: SecureBackupRootEvents) {
            when (event) {
                SecureBackupRootEvents.RetryKeyBackupState -> localCoroutineScope.getKeyBackupStatus(doesBackupExistOnServerAction)
                SecureBackupRootEvents.EnableKeyStorage -> localCoroutineScope.enableBackup(enableAction)
                SecureBackupRootEvents.DismissDialog -> {
                    enableAction.value = AsyncAction.Uninitialized
                    displayKeyStorageDisabledError = false
                }
                SecureBackupRootEvents.DisplayKeyStorageDisabledError -> displayKeyStorageDisabledError = true
                SecureBackupRootEvents.VerifyDevice -> {
                    // Trigger device verification callback
                    onDeviceVerificationClick?.invoke()
                }
            }
        }

        return SecureBackupRootState(
            enableAction = enableAction.value,
            backupState = backupState,
            doesBackupExistOnServer = doesBackupExistOnServerAction.value,
            recoveryState = recoveryState,
            sessionVerifiedStatus = sessionVerifiedStatus,
            appName = buildMeta.applicationName,
            displayKeyStorageDisabledError = displayKeyStorageDisabledError,
            snackbarMessage = snackbarMessage,
            eventSink = ::handleEvents,
        )
    }

    private fun CoroutineScope.getKeyBackupStatus(action: MutableState<AsyncData<Boolean>>) = launch {
        suspend {
            encryptionService.doesBackupExistOnServer().getOrThrow()
        }.runCatchingUpdatingState(action)
    }

    private fun CoroutineScope.enableBackup(action: MutableState<AsyncAction<Unit>>) = launch {
        suspend {
            Timber.tag(loggerTagDisable.value).d("Calling encryptionService.enableBackups()")
            encryptionService.enableBackups().getOrThrow()
        }.runCatchingUpdatingState(action)
    }
}
