/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.enter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyUserStory
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyViewState
import io.element.android.features.securebackup.impl.tools.RecoveryKeyTools
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SecureBackupEnterRecoveryKeyPresenter @Inject constructor(
    private val encryptionService: EncryptionService,
    private val recoveryKeyTools: RecoveryKeyTools,
) : Presenter<SecureBackupEnterRecoveryKeyState> {
    @Composable
    override fun present(): SecureBackupEnterRecoveryKeyState {
        val coroutineScope = rememberCoroutineScope()
        var recoveryKey by rememberSaveable {
            mutableStateOf("")
        }
        val submitAction: MutableState<AsyncAction<Unit>> = remember {
            mutableStateOf(AsyncAction.Uninitialized)
        }

        fun handleEvents(event: SecureBackupEnterRecoveryKeyEvents) {
            when (event) {
                SecureBackupEnterRecoveryKeyEvents.ClearDialog -> {
                    submitAction.value = AsyncAction.Uninitialized
                }
                is SecureBackupEnterRecoveryKeyEvents.OnRecoveryKeyChange -> {
                    val previousRecoveryKey = recoveryKey
                    recoveryKey = if (previousRecoveryKey.isEmpty() && recoveryKeyTools.isRecoveryKeyFormatValid(event.recoveryKey)) {
                        // A Recovery key has been entered, remove the spaces for a better rendering
                        event.recoveryKey.replace("\\s+".toRegex(), "")
                    } else {
                        // Keep the recovery key as entered by the user. May contains spaces.
                        event.recoveryKey
                    }
                }
                SecureBackupEnterRecoveryKeyEvents.Submit -> {
                    // No need to remove the spaces, the SDK will do it.
                    coroutineScope.submitRecoveryKey(recoveryKey, submitAction)
                }
            }
        }

        return SecureBackupEnterRecoveryKeyState(
            recoveryKeyViewState = RecoveryKeyViewState(
                recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
                formattedRecoveryKey = recoveryKey,
                inProgress = submitAction.value.isLoading(),
            ),
            isSubmitEnabled = recoveryKey.isNotEmpty() && submitAction.value.isUninitialized(),
            submitAction = submitAction.value,
            eventSink = ::handleEvents
        )
    }

    private fun CoroutineScope.submitRecoveryKey(
        recoveryKey: String,
        action: MutableState<AsyncAction<Unit>>
    ) = launch {
        android.util.Log.d("SecureBackupEnter", "submitRecoveryKey() started with key: ${recoveryKey.take(10)}...")
        android.util.Log.d("SecureBackupEnter", "Current backup state: ${encryptionService.backupStateStateFlow.value}")
        android.util.Log.d("SecureBackupEnter", "Current recovery state: ${encryptionService.recoveryStateStateFlow.value}")
        
        suspend {
            android.util.Log.d("SecureBackupEnter", "About to call encryptionService.recover()")
            val result = encryptionService.recover(recoveryKey)
            if (result.isSuccess) {
                android.util.Log.d("SecureBackupEnter", "encryptionService.recover() succeeded")
                android.util.Log.d("SecureBackupEnter", "New backup state: ${encryptionService.backupStateStateFlow.value}")
                android.util.Log.d("SecureBackupEnter", "New recovery state: ${encryptionService.recoveryStateStateFlow.value}")
                
                // If backup state is still UNKNOWN after recovery, try to enable backups
                // This should help synchronize the backup state properly
                if (encryptionService.backupStateStateFlow.value == io.element.android.libraries.matrix.api.encryption.BackupState.UNKNOWN) {
                    android.util.Log.d("SecureBackupEnter", "Backup state still UNKNOWN, attempting to enable backups")
                    val enableResult = encryptionService.enableBackups()
                    if (enableResult.isSuccess) {
                        android.util.Log.d("SecureBackupEnter", "enableBackups() succeeded")
                        android.util.Log.d("SecureBackupEnter", "Updated backup state: ${encryptionService.backupStateStateFlow.value}")
                        android.util.Log.d("SecureBackupEnter", "Updated recovery state: ${encryptionService.recoveryStateStateFlow.value}")
                    } else {
                        val error = enableResult.exceptionOrNull()
                        android.util.Log.w("SecureBackupEnter", "enableBackups() failed: ${error?.message}")
                        // BackupExistsOnServer is expected in this case, so we don't treat it as a failure
                        if (error?.message?.contains("BackupExistsOnServer") == true) {
                            android.util.Log.d("SecureBackupEnter", "BackupExistsOnServer error is expected, continuing...")
                        }
                    }
                }
                
                // Final state check
                android.util.Log.d("SecureBackupEnter", "Final backup state: ${encryptionService.backupStateStateFlow.value}")
                android.util.Log.d("SecureBackupEnter", "Final recovery state: ${encryptionService.recoveryStateStateFlow.value}")
                
                Unit // Return Unit for runCatchingUpdatingState
            } else {
                android.util.Log.e("SecureBackupEnter", "encryptionService.recover() failed: ${result.exceptionOrNull()?.message}")
                throw result.exceptionOrNull() ?: Exception("Unknown error")
            }
        }.runCatchingUpdatingState(action)
    }
}
