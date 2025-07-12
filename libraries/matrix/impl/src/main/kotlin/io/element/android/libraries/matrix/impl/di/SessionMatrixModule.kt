/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MediaPreviewService
import io.element.android.libraries.matrix.api.notification.NotificationService
import io.element.android.libraries.matrix.api.notificationsettings.NotificationSettingsService
import io.element.android.libraries.matrix.api.pusher.PushersService
import io.element.android.libraries.matrix.api.room.RoomMembershipObserver
import io.element.android.libraries.matrix.api.roomdirectory.RoomDirectoryService
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.sync.SyncService
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import io.element.android.libraries.matrix.impl.RustMatrixClient
import io.element.android.libraries.usersearch.api.CognitoUserIntegrationService
import io.element.android.libraries.usersearch.api.UserMappingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Module
@ContributesTo(SessionScope::class)
object SessionMatrixModule {

    @Provides
    @SessionCoroutineScope
    fun providesSessionCoroutineScope(matrixClient: MatrixClient): CoroutineScope {
        return matrixClient.sessionCoroutineScope
    }

    @Provides
    fun providesRoomListService(matrixClient: MatrixClient): RoomListService {
        return matrixClient.roomListService
    }

    @Provides
    fun providesRoomDirectoryService(matrixClient: MatrixClient): RoomDirectoryService {
        return matrixClient.roomDirectoryService()
    }

    @Provides
    fun providesMatrixMediaLoader(matrixClient: MatrixClient): MatrixMediaLoader {
        return matrixClient.mediaLoader
    }

    @Provides
    fun providesMediaPreviewService(matrixClient: MatrixClient): MediaPreviewService {
        return matrixClient.mediaPreviewService()
    }

    @Provides
    fun providesSyncService(matrixClient: MatrixClient): SyncService {
        return matrixClient.syncService()
    }

    @Provides
    fun providesEncryptionService(matrixClient: MatrixClient): EncryptionService {
        return matrixClient.encryptionService()
    }

    @Provides
    fun providesNotificationService(matrixClient: MatrixClient): NotificationService {
        return matrixClient.notificationService()
    }

    @Provides
    fun providesNotificationSettingsService(matrixClient: MatrixClient): NotificationSettingsService {
        return matrixClient.notificationSettingsService()
    }

    @Provides
    fun providesSessionVerificationService(matrixClient: MatrixClient): SessionVerificationService {
        return matrixClient.sessionVerificationService()
    }

    @Provides
    fun providesPushersService(matrixClient: MatrixClient): PushersService {
        return matrixClient.pushersService()
    }

    @Provides
    fun providesRoomMembershipObserver(matrixClient: MatrixClient): RoomMembershipObserver {
        return matrixClient.roomMembershipObserver()
    }

    @Provides
    fun providesSessionMatrixSetup(
        matrixClient: MatrixClient,
        userMappingService: UserMappingService,
        cognitoUserIntegrationService: CognitoUserIntegrationService,
        @SessionCoroutineScope coroutineScope: CoroutineScope
    ): SessionMatrixSetup {
        return SessionMatrixSetup(
            matrixClient,
            userMappingService,
            cognitoUserIntegrationService,
            coroutineScope
        )
    }
}

class SessionMatrixSetup @Inject constructor(
    private val matrixClient: MatrixClient,
    private val userMappingService: UserMappingService,
    private val cognitoUserIntegrationService: CognitoUserIntegrationService,
    @SessionCoroutineScope private val coroutineScope: CoroutineScope
) {
    init {
        setupMatrix()
    }

    private fun setupMatrix() {
        // Set up user mapping service for the matrix client
        if (matrixClient is RustMatrixClient) {
            matrixClient.setUserMappingService(userMappingService)
        }

        // Populate AWS backend data in background
        coroutineScope.launch {
            try {
                cognitoUserIntegrationService.populateCurrentUserMapping()
                Timber.d("SessionMatrixSetup: AWS backend user mapping populated")
                
                // Trigger room list refresh after AWS backend population
                matrixClient.roomListService.allRooms.rebuildSummaries()
                Timber.d("SessionMatrixSetup: Room list refreshed after AWS backend population")
            } catch (e: Exception) {
                Timber.e(e, "SessionMatrixSetup: Failed to populate AWS backend user mapping")
            }
        }

        // Set up periodic refresh of user mappings
        coroutineScope.launch {
            try {
                // Wait 30 seconds before starting periodic refresh
                kotlinx.coroutines.delay(30_000)
                
                while (true) {
                    try {
                        // Refresh current user mapping every 5 minutes
                        cognitoUserIntegrationService.populateCurrentUserMapping()
                        Timber.d("SessionMatrixSetup: Periodic refresh of user mappings completed")
                        
                        // Trigger room list refresh after periodic update
                        matrixClient.roomListService.allRooms.rebuildSummaries()
                        
                        // Wait 5 minutes before next refresh
                        kotlinx.coroutines.delay(300_000)
                    } catch (e: Exception) {
                        Timber.w(e, "SessionMatrixSetup: Error during periodic user mapping refresh")
                        // Wait 1 minute before retrying
                        kotlinx.coroutines.delay(60_000)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "SessionMatrixSetup: Failed to set up periodic user mapping refresh")
            }
        }
    }
}

