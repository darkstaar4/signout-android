/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.eventformatter.impl

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.timeline.item.event.MembershipChange
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.CognitoUserIntegrationService
import io.element.android.services.toolbox.api.strings.StringProvider
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoomMembershipContentFormatter @Inject constructor(
    private val matrixClient: MatrixClient,
    private val sp: StringProvider,
    private val userMappingService: UserMappingService,
    private val cognitoUserIntegrationService: CognitoUserIntegrationService,
) {
    fun format(
        membershipContent: RoomMembershipContent,
        senderDisambiguatedDisplayName: String,
        senderIsYou: Boolean,
        senderMatrixId: String? = null,
    ): CharSequence? {
        val userId = membershipContent.userId
        val memberIsYou = matrixClient.isMe(userId)
        
        // Get enhanced display name using UserMappingService
        val enhancedDisplayName = getEnhancedDisplayName(userId.value, membershipContent.userDisplayName)
        val userDisplayNameOrId = enhancedDisplayName ?: membershipContent.userDisplayName ?: userId.value
        
        // Get enhanced sender display name using UserMappingService
        val enhancedSenderDisplayName = if (senderMatrixId != null) {
            getEnhancedDisplayName(senderMatrixId, senderDisambiguatedDisplayName) ?: senderDisambiguatedDisplayName
        } else {
            senderDisambiguatedDisplayName
        }
        
        val reason = membershipContent.reason?.takeIf { it.isNotBlank() }
        return when (membershipContent.change) {
            MembershipChange.JOINED -> if (memberIsYou) {
                sp.getString(R.string.state_event_room_join_by_you)
            } else {
                sp.getString(R.string.state_event_room_join, enhancedSenderDisplayName)
            }
            MembershipChange.LEFT -> if (memberIsYou) {
                sp.getString(R.string.state_event_room_leave_by_you)
            } else {
                sp.getString(R.string.state_event_room_leave, enhancedSenderDisplayName)
            }
            MembershipChange.BANNED, MembershipChange.KICKED_AND_BANNED -> if (senderIsYou) {
                if (reason != null) {
                    sp.getString(R.string.state_event_room_ban_by_you_with_reason, userDisplayNameOrId, reason)
                } else {
                    sp.getString(R.string.state_event_room_ban_by_you, userDisplayNameOrId)
                }
            } else {
                if (reason != null) {
                    sp.getString(R.string.state_event_room_ban_with_reason, enhancedSenderDisplayName, userDisplayNameOrId, reason)
                } else {
                    sp.getString(R.string.state_event_room_ban, enhancedSenderDisplayName, userDisplayNameOrId)
                }
            }
            MembershipChange.UNBANNED -> if (senderIsYou) {
                sp.getString(R.string.state_event_room_unban_by_you, userDisplayNameOrId)
            } else {
                sp.getString(R.string.state_event_room_unban, enhancedSenderDisplayName, userDisplayNameOrId)
            }
            MembershipChange.KICKED -> if (senderIsYou) {
                if (reason != null) {
                    sp.getString(R.string.state_event_room_remove_by_you_with_reason, userDisplayNameOrId, reason)
                } else {
                    sp.getString(R.string.state_event_room_remove_by_you, userDisplayNameOrId)
                }
            } else {
                if (reason != null) {
                    sp.getString(R.string.state_event_room_remove_with_reason, enhancedSenderDisplayName, userDisplayNameOrId, reason)
                } else {
                    sp.getString(R.string.state_event_room_remove, enhancedSenderDisplayName, userDisplayNameOrId)
                }
            }
            MembershipChange.INVITED -> if (senderIsYou) {
                sp.getString(R.string.state_event_room_invite_by_you, userDisplayNameOrId)
            } else if (memberIsYou) {
                sp.getString(R.string.state_event_room_invite_you, enhancedSenderDisplayName)
            } else {
                sp.getString(R.string.state_event_room_invite, enhancedSenderDisplayName, userDisplayNameOrId)
            }
            MembershipChange.INVITATION_ACCEPTED -> if (memberIsYou) {
                sp.getString(R.string.state_event_room_invite_accepted_by_you)
            } else {
                sp.getString(R.string.state_event_room_invite_accepted, userDisplayNameOrId)
            }
            MembershipChange.INVITATION_REJECTED -> if (memberIsYou) {
                sp.getString(R.string.state_event_room_reject_by_you)
            } else {
                sp.getString(R.string.state_event_room_reject, userDisplayNameOrId)
            }
            MembershipChange.INVITATION_REVOKED -> if (senderIsYou) {
                sp.getString(R.string.state_event_room_third_party_revoked_invite_by_you, userDisplayNameOrId)
            } else {
                sp.getString(R.string.state_event_room_third_party_revoked_invite, enhancedSenderDisplayName, userDisplayNameOrId)
            }
            MembershipChange.KNOCKED -> if (memberIsYou) {
                sp.getString(R.string.state_event_room_knock_by_you)
            } else {
                sp.getString(R.string.state_event_room_knock, enhancedSenderDisplayName)
            }
            MembershipChange.KNOCK_ACCEPTED -> if (senderIsYou) {
                sp.getString(R.string.state_event_room_knock_accepted_by_you, userDisplayNameOrId)
            } else {
                sp.getString(R.string.state_event_room_knock_accepted, enhancedSenderDisplayName, userDisplayNameOrId)
            }
            MembershipChange.KNOCK_RETRACTED -> if (memberIsYou) {
                sp.getString(R.string.state_event_room_knock_retracted_by_you)
            } else {
                sp.getString(R.string.state_event_room_knock_retracted, enhancedSenderDisplayName)
            }
            MembershipChange.KNOCK_DENIED -> if (senderIsYou) {
                sp.getString(R.string.state_event_room_knock_denied_by_you, userDisplayNameOrId)
            } else if (memberIsYou) {
                sp.getString(R.string.state_event_room_knock_denied_you, enhancedSenderDisplayName)
            } else {
                sp.getString(R.string.state_event_room_knock_denied, enhancedSenderDisplayName, userDisplayNameOrId)
            }
            MembershipChange.NONE -> if (senderIsYou) {
                sp.getString(R.string.state_event_room_none_by_you)
            } else {
                sp.getString(R.string.state_event_room_none, enhancedSenderDisplayName)
            }
            MembershipChange.ERROR -> {
                Timber.v("Filtering timeline item for room membership: $membershipContent")
                null
            }
            MembershipChange.NOT_IMPLEMENTED -> {
                Timber.v("Filtering timeline item for room membership: $membershipContent")
                null
            }
            null -> {
                Timber.v("Filtering timeline item for room membership: $membershipContent")
                null
            }
        }
    }
    
    /**
     * Get enhanced display name using UserMappingService
     * @param matrixUserId The full Matrix user ID (e.g., "@racexcars:signout.io")
     * @param fallbackDisplayName The fallback display name to use if no mapping is found
     * @return Enhanced display name (First Name Last Name) or null if not found
     */
    private fun getEnhancedDisplayName(matrixUserId: String, fallbackDisplayName: String?): String? {
        return try {
            // Extract username from Matrix user ID
            val username = matrixUserId.substringAfter("@").substringBefore(":")
            
            // Get user mapping from service
            val userMapping = userMappingService.getUserMapping(username)
            
            if (userMapping != null) {
                Timber.d("RoomMembershipContentFormatter: Found enhanced display name for $username: ${userMapping.displayName}")
                userMapping.displayName
            } else {
                Timber.d("RoomMembershipContentFormatter: No enhanced display name found for $username, triggering discovery")
                // Trigger user discovery in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        cognitoUserIntegrationService.discoverUserMapping(
                            matrixUserId = matrixUserId,
                            matrixDisplayName = fallbackDisplayName
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "RoomMembershipContentFormatter: Failed to discover user mapping for $username")
                    }
                }
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "RoomMembershipContentFormatter: Error getting enhanced display name for $matrixUserId")
            null
        }
    }
}
