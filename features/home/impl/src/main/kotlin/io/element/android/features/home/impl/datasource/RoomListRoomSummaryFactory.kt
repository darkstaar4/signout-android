/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.datasource

import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.libraries.core.extensions.orEmpty
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.eventformatter.api.RoomLastMessageFormatter
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.isDm
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.toInviteSender
import io.element.android.libraries.usersearch.api.CognitoUserIntegrationService
import io.element.android.libraries.usersearch.api.UserMappingService
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class RoomListRoomSummaryFactory @Inject constructor(
    private val dateFormatter: DateFormatter,
    private val roomLastMessageFormatter: RoomLastMessageFormatter,
    private val userMappingService: UserMappingService,
    private val cognitoUserIntegrationService: CognitoUserIntegrationService,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) {
    fun create(roomSummary: RoomSummary): RoomListRoomSummary {
        val roomInfo = roomSummary.info
        val avatarData = roomInfo.getAvatarData(size = AvatarSize.RoomListItem)
        
        // Enhance DM room names with user mapping data
        val displayName = if (roomInfo.isDm && roomInfo.heroes.isNotEmpty()) {
            val heroUser = roomInfo.heroes.firstOrNull()
            if (heroUser != null) {
                val username = heroUser.userId.value.substringAfter("@").substringBefore(":")
                Timber.d("RoomListRoomSummaryFactory: Processing DM room for user $username (${heroUser.userId.value})")
                val userMapping = userMappingService.getUserMapping(username)
                if (userMapping != null) {
                    Timber.d("RoomListRoomSummaryFactory: Enhanced DM name for $username: ${userMapping.displayName}")
                    userMapping.displayName
                } else {
                    Timber.d("RoomListRoomSummaryFactory: No mapping found for $username, triggering discovery")
                    // Trigger discovery in background
                    sessionCoroutineScope.launch {
                        try {
                            Timber.d("RoomListRoomSummaryFactory: Triggering discovery for user $username")
                            cognitoUserIntegrationService.discoverUserMapping(
                                matrixUserId = heroUser.userId.value,
                                matrixDisplayName = heroUser.displayName
                            )
                            Timber.d("RoomListRoomSummaryFactory: Discovery completed for user $username")
                        } catch (e: Exception) {
                            Timber.w(e, "RoomListRoomSummaryFactory: Failed to discover user mapping for $username")
                        }
                    }
                    roomInfo.name
                }
            } else {
                Timber.d("RoomListRoomSummaryFactory: DM room has no hero users")
                roomInfo.name
            }
        } else {
            if (roomInfo.isDm) {
                Timber.d("RoomListRoomSummaryFactory: DM room has no heroes")
            }
            roomInfo.name
        }
        
        return RoomListRoomSummary(
            id = roomSummary.roomId.value,
            roomId = roomSummary.roomId,
            name = displayName,
            numberOfUnreadMessages = roomInfo.numUnreadMessages,
            numberOfUnreadMentions = roomInfo.numUnreadMentions,
            numberOfUnreadNotifications = roomInfo.numUnreadNotifications,
            isMarkedUnread = roomInfo.isMarkedUnread,
            timestamp = dateFormatter.format(
                timestamp = roomSummary.lastMessageTimestamp,
                mode = DateFormatterMode.TimeOrDate,
                useRelative = true,
            ),
            lastMessage = roomSummary.lastMessage?.let { message ->
                roomLastMessageFormatter.format(message.event, roomInfo.isDm)
            }.orEmpty(),
            avatarData = avatarData,
            userDefinedNotificationMode = roomInfo.userDefinedNotificationMode,
            hasRoomCall = roomInfo.hasRoomCall,
            isDirect = roomInfo.isDirect,
            isFavorite = roomInfo.isFavorite,
            inviteSender = roomInfo.inviter?.toInviteSender(),
            isDm = roomInfo.isDm,
            canonicalAlias = roomInfo.canonicalAlias,
            displayType = when (roomInfo.currentUserMembership) {
                CurrentUserMembership.INVITED -> {
                    RoomSummaryDisplayType.INVITE
                }
                CurrentUserMembership.KNOCKED -> {
                    RoomSummaryDisplayType.KNOCKED
                }
                else -> {
                    RoomSummaryDisplayType.ROOM
                }
            },
            heroes = roomInfo.heroes.map { user ->
                user.getAvatarData(size = AvatarSize.RoomListItem)
            }.toImmutableList(),
            isTombstoned = roomInfo.successorRoom != null,
        )
    }
}
