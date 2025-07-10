/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.room

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.powerlevels.RoomPowerLevels
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.impl.room.history.map
import io.element.android.libraries.matrix.impl.room.join.map
import io.element.android.libraries.matrix.impl.room.member.RoomMemberMapper
import io.element.android.libraries.matrix.impl.room.powerlevels.RoomPowerLevelsValuesMapper
import io.element.android.libraries.matrix.impl.room.tombstone.map
import io.element.android.libraries.usersearch.api.UserMappingService
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import org.matrix.rustcomponents.sdk.Membership as RustMembership
import org.matrix.rustcomponents.sdk.RoomHero
import org.matrix.rustcomponents.sdk.RoomInfo as RustRoomInfo
import org.matrix.rustcomponents.sdk.RoomNotificationMode as RustRoomNotificationMode
import org.matrix.rustcomponents.sdk.RoomPowerLevels as RustRoomPowerLevels
import uniffi.matrix_sdk_base.EncryptionState
import javax.inject.Inject

class RoomInfoMapper @Inject constructor() {
    private var userMappingService: UserMappingService? = null

    fun setUserMappingService(service: UserMappingService) {
        this.userMappingService = service
    }

    fun map(rustRoomInfo: RustRoomInfo): RoomInfo = rustRoomInfo.let {
        val enhancedName = if (it.isDirect && it.activeMembersCount.toLong() == 2L) {
            // For DMs, try to enhance the name with user mapping data
            enhanceDirectRoomName(it.displayName, it.elementHeroes())
        } else {
            it.displayName
        }

        return RoomInfo(
            id = RoomId(it.id),
            creator = it.creator?.let(::UserId),
            name = enhancedName,
            rawName = it.rawName,
            topic = it.topic,
            avatarUrl = it.avatarUrl,
            isPublic = it.isPublic,
            isDirect = it.isDirect,
            isEncrypted = when (it.encryptionState) {
                EncryptionState.ENCRYPTED -> true
                EncryptionState.NOT_ENCRYPTED -> false
                EncryptionState.UNKNOWN -> null
            },
            joinRule = it.joinRule?.map(),
            isSpace = it.isSpace,
            isFavorite = it.isFavourite,
            canonicalAlias = it.canonicalAlias?.let(::RoomAlias),
            alternativeAliases = it.alternativeAliases.map(::RoomAlias).toImmutableList(),
            currentUserMembership = it.membership.map(),
            inviter = it.inviter?.let(RoomMemberMapper::map),
            activeMembersCount = it.activeMembersCount.toLong(),
            invitedMembersCount = it.invitedMembersCount.toLong(),
            joinedMembersCount = it.joinedMembersCount.toLong(),
            roomPowerLevels = it.powerLevels?.let(::mapPowerLevels),
            highlightCount = it.highlightCount.toLong(),
            notificationCount = it.notificationCount.toLong(),
            userDefinedNotificationMode = it.cachedUserDefinedNotificationMode?.map(),
            hasRoomCall = it.hasRoomCall,
            activeRoomCallParticipants = it.activeRoomCallParticipants.map(::UserId).toImmutableList(),
            heroes = it.elementHeroes().toImmutableList(),
            pinnedEventIds = it.pinnedEventIds.map(::EventId).toImmutableList(),
            isMarkedUnread = it.isMarkedUnread,
            numUnreadMessages = it.numUnreadMessages.toLong(),
            numUnreadMentions = it.numUnreadMentions.toLong(),
            numUnreadNotifications = it.numUnreadNotifications.toLong(),
            historyVisibility = it.historyVisibility.map(),
            successorRoom = it.successorRoom?.map(),
        )
    }

    private fun enhanceDirectRoomName(originalName: String?, heroes: List<MatrixUser>): String? {
        // If there's no original name or no heroes, return as-is
        if (originalName == null || heroes.isEmpty()) {
            return originalName
        }

        // If UserMappingService is not available, return original name
        val mappingService = userMappingService ?: return originalName

        // Get the hero user (the other user in the DM)
        val heroUser = heroes.firstOrNull() ?: return originalName

        // Extract username from hero user ID
        val username = heroUser.userId.value.substringAfter("@").substringBefore(":")

        // Try to get enhanced user mapping
        val userMapping = mappingService.getUserMapping(username)

        return if (userMapping != null) {
            // Use the enhanced display name (First Name Last Name)
            userMapping.displayName
        } else {
            // Fall back to original name
            originalName
        }
    }
}

fun RustMembership.map(): CurrentUserMembership = when (this) {
    RustMembership.INVITED -> CurrentUserMembership.INVITED
    RustMembership.JOINED -> CurrentUserMembership.JOINED
    RustMembership.LEFT -> CurrentUserMembership.LEFT
    RustMembership.KNOCKED -> CurrentUserMembership.KNOCKED
    RustMembership.BANNED -> CurrentUserMembership.BANNED
}

fun RustRoomNotificationMode.map(): RoomNotificationMode = when (this) {
    RustRoomNotificationMode.ALL_MESSAGES -> RoomNotificationMode.ALL_MESSAGES
    RustRoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY -> RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY
    RustRoomNotificationMode.MUTE -> RoomNotificationMode.MUTE
}

/**
 * Map a RoomHero to a MatrixUser. There is not need to create a RoomHero type on the application side.
 */
fun RoomHero.map(): MatrixUser = MatrixUser(
    userId = UserId(userId),
    displayName = displayName,
    avatarUrl = avatarUrl
)

fun mapPowerLevels(roomPowerLevels: RustRoomPowerLevels): RoomPowerLevels {
    return RoomPowerLevels(
        values = RoomPowerLevelsValuesMapper.map(roomPowerLevels.values()),
        users = roomPowerLevels.userPowerLevels().mapKeys { (key, _) -> UserId(key) }.toPersistentMap()
    )
}
