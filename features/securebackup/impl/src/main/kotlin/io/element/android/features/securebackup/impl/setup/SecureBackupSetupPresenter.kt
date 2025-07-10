/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.securebackup.impl.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.freeletics.flowredux.compose.StateAndDispatch
import com.freeletics.flowredux.compose.rememberStateAndDispatch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.features.securebackup.impl.loggerTagSetup
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyUserStory
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyViewState
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.encryption.EnableRecoveryProgress
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import timber.log.Timber

class SecureBackupSetupPresenter @AssistedInject constructor(
    @Assisted private val isChangeRecoveryKeyUserStory: Boolean,
    private val stateMachine: SecureBackupSetupStateMachine,
    private val encryptionService: EncryptionService,
) : Presenter<SecureBackupSetupState> {
    @AssistedFactory
    interface Factory {
        fun create(isChangeRecoveryKeyUserStory: Boolean): SecureBackupSetupPresenter
    }

    @Composable
    override fun present(): SecureBackupSetupState {
        android.util.Log.d("SecureBackupSetup", "present() called - SecureBackupSetupPresenter is running")
        val coroutineScope = rememberCoroutineScope()
        val stateAndDispatch = stateMachine.rememberStateAndDispatch()
        val setupState by remember {
            derivedStateOf { stateAndDispatch.state.value.toSetupState() }
        }
        var showSaveConfirmationDialog by remember { mutableStateOf(false) }

        fun handleEvents(event: SecureBackupSetupEvents) {
            android.util.Log.d("SecureBackupSetup", "handleEvents called with: $event")
            when (event) {
                SecureBackupSetupEvents.CreateRecoveryKey -> {
                    android.util.Log.d("SecureBackupSetup", "CreateRecoveryKey event triggered!")
                    coroutineScope.createOrChangeRecoveryKey(stateAndDispatch)
                }
                SecureBackupSetupEvents.RecoveryKeyHasBeenSaved ->
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.UserSavedKey)
                SecureBackupSetupEvents.DismissDialog -> {
                    showSaveConfirmationDialog = false
                }
                SecureBackupSetupEvents.Done -> {
                    showSaveConfirmationDialog = true
                }
            }
        }

        val recoveryKeyViewState = RecoveryKeyViewState(
            recoveryKeyUserStory = if (isChangeRecoveryKeyUserStory) RecoveryKeyUserStory.Change else RecoveryKeyUserStory.Setup,
            formattedRecoveryKey = setupState.recoveryKey(),
            inProgress = setupState is SetupState.Creating,
        )

        return SecureBackupSetupState(
            isChangeRecoveryKeyUserStory = isChangeRecoveryKeyUserStory,
            recoveryKeyViewState = recoveryKeyViewState,
            setupState = setupState,
            showSaveConfirmationDialog = showSaveConfirmationDialog,
            eventSink = ::handleEvents
        )
    }

    private fun SecureBackupSetupStateMachine.State?.toSetupState(): SetupState {
        return when (this) {
            null,
            SecureBackupSetupStateMachine.State.Initial -> SetupState.Init
            SecureBackupSetupStateMachine.State.CreatingKey -> SetupState.Creating
            is SecureBackupSetupStateMachine.State.KeyCreated -> SetupState.Created(formattedRecoveryKey = key)
            is SecureBackupSetupStateMachine.State.KeyCreatedAndSaved -> SetupState.CreatedAndSaved(formattedRecoveryKey = key)
        }
    }

    private fun CoroutineScope.createOrChangeRecoveryKey(
        stateAndDispatch: StateAndDispatch<SecureBackupSetupStateMachine.State, SecureBackupSetupStateMachine.Event>
    ) = launch {
        android.util.Log.d("SecureBackupSetup", "createOrChangeRecoveryKey() started")
        stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.UserCreatesKey)
        if (isChangeRecoveryKeyUserStory) {
            android.util.Log.d("SecureBackupSetup", "Change recovery key user story")
            Timber.tag(loggerTagSetup.value).d("Calling encryptionService.resetRecoveryKey()")
            encryptionService.resetRecoveryKey().fold(
                onSuccess = {
                    Timber.tag(loggerTagSetup.value).d("resetRecoveryKey() succeeded with key: ${it.take(10)}...")
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkHasCreatedKey(it))
                },
                onFailure = {
                    Timber.tag(loggerTagSetup.value).e(it, "resetRecoveryKey() failed")
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkError(it))
                }
            )
        } else {
            // Check prerequisites before attempting recovery
            android.util.Log.d("SecureBackupSetup", "Checking prerequisites for recovery key generation")
            Timber.tag(loggerTagSetup.value).d("Checking prerequisites for recovery key generation")
            Timber.tag(loggerTagSetup.value).d("Current backup state: ${encryptionService.backupStateStateFlow.value}")
            Timber.tag(loggerTagSetup.value).d("Current recovery state: ${encryptionService.recoveryStateStateFlow.value}")
            
            // Check if backup exists on server first
            val backupExists = encryptionService.doesBackupExistOnServer().getOrNull() ?: false
            android.util.Log.d("SecureBackupSetup", "Backup exists on server: $backupExists")
            
            if (backupExists) {
                // If backup exists, use resetRecoveryKey instead of enableRecovery
                android.util.Log.d("SecureBackupSetup", "Backup exists on server, using resetRecoveryKey()")
                Timber.tag(loggerTagSetup.value).d("Backup exists on server, using resetRecoveryKey()")
                encryptionService.resetRecoveryKey().fold(
                    onSuccess = { recoveryKey ->
                        android.util.Log.d("SecureBackupSetup", "resetRecoveryKey() succeeded with key: ${recoveryKey.take(10)}...")
                        Timber.tag(loggerTagSetup.value).d("resetRecoveryKey() succeeded with key: ${recoveryKey.take(10)}...")
                        stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkHasCreatedKey(recoveryKey))
                    },
                    onFailure = { error ->
                        android.util.Log.e("SecureBackupSetup", "resetRecoveryKey() failed: ${error.message}")
                        Timber.tag(loggerTagSetup.value).e("resetRecoveryKey() failed", error)
                        stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkError(error))
                    }
                )
            } else {
                // If no backup exists, use the original enableRecovery approach
                android.util.Log.d("SecureBackupSetup", "No backup exists, using enableRecovery()")
                
                // CRITICAL: Start observing BEFORE calling enableRecovery to avoid race condition
                Timber.tag(loggerTagSetup.value).d("Starting observeEncryptionService coroutine BEFORE enableRecovery call")
                observeEncryptionService(stateAndDispatch)

                // Add a small delay to ensure the observer is set up
                kotlinx.coroutines.delay(100)

                android.util.Log.d("SecureBackupSetup", "About to call encryptionService.enableRecovery()")
                Timber.tag(loggerTagSetup.value).d("Calling encryptionService.enableRecovery()")
                encryptionService.enableRecovery(waitForBackupsToUpload = false).fold(
                    onSuccess = {
                        android.util.Log.d("SecureBackupSetup", "enableRecovery() succeeded, waiting for progress updates")
                        Timber.tag(loggerTagSetup.value).d("enableRecovery() succeeded, waiting for progress updates")
                    },
                    onFailure = { error ->
                        android.util.Log.e("SecureBackupSetup", "enableRecovery() failed: ${error.javaClass.simpleName}")
                        Timber.tag(loggerTagSetup.value).e("enableRecovery() failed", error)
                        stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkError(error))
                    }
                )
            }
        }
    }

    private fun CoroutineScope.observeEncryptionService(
        stateAndDispatch: StateAndDispatch<SecureBackupSetupStateMachine.State, SecureBackupSetupStateMachine.Event>
    ) = launch {
        Timber.tag(loggerTagSetup.value).d("Starting to observe enableRecoveryProgressStateFlow")
        try {
            // Add timeout to prevent hanging indefinitely
            kotlinx.coroutines.withTimeout(120_000) { // 2 minutes timeout
                encryptionService.enableRecoveryProgressStateFlow.collect { enableRecoveryProgress ->
                    Timber.tag(loggerTagSetup.value).d("New enableRecoveryProgress: ${enableRecoveryProgress.javaClass.simpleName}")
                    when (enableRecoveryProgress) {
                        is EnableRecoveryProgress.Starting -> {
                            Timber.tag(loggerTagSetup.value).d("Progress: Starting")
                        }
                        is EnableRecoveryProgress.CreatingBackup -> {
                            Timber.tag(loggerTagSetup.value).d("Progress: CreatingBackup")
                        }
                        is EnableRecoveryProgress.CreatingRecoveryKey -> {
                            Timber.tag(loggerTagSetup.value).d("Progress: CreatingRecoveryKey")
                        }
                        is EnableRecoveryProgress.BackingUp -> {
                            val progress = "${enableRecoveryProgress.backedUpCount}/${enableRecoveryProgress.totalCount}"
                            Timber.tag(loggerTagSetup.value).d("Progress: BackingUp ($progress)")
                        }
                        is EnableRecoveryProgress.RoomKeyUploadError -> {
                            Timber.tag(loggerTagSetup.value).w("Progress: RoomKeyUploadError")
                        }
                        is EnableRecoveryProgress.Done -> {
                            Timber.tag(loggerTagSetup.value).d("Progress: Done with key: ${enableRecoveryProgress.recoveryKey.take(10)}...")
                            stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkHasCreatedKey(enableRecoveryProgress.recoveryKey))
                            return@collect // Exit the collect loop once we're done
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Timber.tag(loggerTagSetup.value).e("Recovery process timed out after 2 minutes")
            stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkError(Exception("Recovery process timed out")))
        } catch (e: Exception) {
            Timber.tag(loggerTagSetup.value).e(e, "Error in observeEncryptionService")
            stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkError(e))
        }
    }
}
