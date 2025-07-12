/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.messages.impl.actionlist.ActionListState
import io.element.android.features.messages.impl.actionlist.anActionListState
import io.element.android.features.messages.impl.crypto.identity.IdentityChangeState
import io.element.android.features.messages.impl.crypto.identity.anIdentityChangeState
import io.element.android.features.messages.impl.link.LinkState
import io.element.android.features.messages.impl.link.aLinkState
import io.element.android.features.messages.impl.messagecomposer.MessageComposerState
import io.element.android.features.messages.impl.messagecomposer.aMessageComposerState
import io.element.android.features.messages.impl.pinned.banner.PinnedMessagesBannerState
import io.element.android.features.messages.impl.pinned.banner.aLoadedPinnedMessagesBannerState
import io.element.android.features.messages.impl.timeline.TimelineState
import io.element.android.features.messages.impl.timeline.aTimelineItemList
import io.element.android.features.messages.impl.timeline.aTimelineState
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionEvents
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionState
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryEvents
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryState
import io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet.ReadReceiptBottomSheetEvents
import io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet.ReadReceiptBottomSheetState
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemTextContent
import io.element.android.features.messages.impl.timeline.protection.TimelineProtectionState
import io.element.android.features.messages.impl.timeline.protection.aTimelineProtectionState
import io.element.android.features.messages.impl.voicemessages.composer.VoiceMessageComposerState
import io.element.android.features.messages.impl.voicemessages.composer.aVoiceMessageComposerState
import io.element.android.features.messages.impl.voicemessages.composer.aVoiceMessagePreviewState
import io.element.android.features.roommembermoderation.api.RoomMemberModerationEvents
import io.element.android.features.roommembermoderation.api.RoomMemberModerationState
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.encryption.identity.IdentityState
import io.element.android.libraries.matrix.api.room.tombstone.SuccessorRoom
import io.element.android.libraries.textcomposer.model.MessageComposerMode
import io.element.android.libraries.textcomposer.model.aTextEditorStateRich
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.usersearch.api.UserMappingService
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Fake UserMappingService for testing purposes
private class FakeUserMappingService : UserMappingService {
    override fun getUserMapping(matrixUsername: String): UserMapping? = null
    override fun addUserMapping(userMapping: UserMapping) {}
    override val userMappingUpdates: Flow<UserMapping> = emptyFlow()
    override fun addUserFromCognitoData(
        matrixUserId: String,
        matrixUsername: String,
        cognitoUsername: String,
        givenName: String,
        familyName: String,
        email: String,
        specialty: String?,
        officeCity: String?,
        avatarUrl: String?
    ) {}
    override fun searchUsers(query: String): List<UserMapping> = emptyList()
    override fun searchUsers(query: String, limit: Long): List<UserMapping> = emptyList()
    override fun removeUser(matrixUsername: String) {}
    override fun clearAll() {}
    override fun getCachedMappingsCount(): Int = 0
}

open class MessagesStateProvider : PreviewParameterProvider<MessagesState> {
    override val values: Sequence<MessagesState>
        get() = sequenceOf(
            aMessagesState(),
            aMessagesState(hasNetworkConnection = false),
            aMessagesState(composerState = aMessageComposerState(showAttachmentSourcePicker = true)),
            aMessagesState(userEventPermissions = aUserEventPermissions(canSendMessage = false)),
            aMessagesState(showReinvitePrompt = true),
            aMessagesState(roomName = null),
            aMessagesState(composerState = aMessageComposerState(showTextFormatting = true)),
            aMessagesState(
                enableVoiceMessages = true,
                voiceMessageComposerState = aVoiceMessageComposerState(showPermissionRationaleDialog = true),
            ),
            aMessagesState(
                enableVoiceMessages = true,
                voiceMessageComposerState = aVoiceMessageComposerState(
                    voiceMessageState = aVoiceMessagePreviewState(),
                    showSendFailureDialog = true
                ),
            ),
            aMessagesState(
                pinnedMessagesBannerState = aLoadedPinnedMessagesBannerState(
                    knownPinnedMessagesCount = 4,
                    currentPinnedMessageIndex = 0,
                ),
            ),
            aMessagesState(roomName = "A DM with a very looong name", dmUserVerificationState = IdentityState.Verified),
            aMessagesState(roomName = "A DM with a very looong name", dmUserVerificationState = IdentityState.VerificationViolation),
            aMessagesState(successorRoom = SuccessorRoom(RoomId("!id:domain"), null)),
        )
}

fun aMessagesState(
    roomName: String? = "Room name",
    roomAvatar: AvatarData = AvatarData("!id:domain", "Room name", size = AvatarSize.TimelineRoom),
    userEventPermissions: UserEventPermissions = aUserEventPermissions(),
    composerState: MessageComposerState = aMessageComposerState(
        textEditorState = aTextEditorStateRich(initialText = "Hello", initialFocus = true),
        isFullScreen = false,
        mode = MessageComposerMode.Normal,
    ),
    voiceMessageComposerState: VoiceMessageComposerState = aVoiceMessageComposerState(),
    timelineState: TimelineState = aTimelineState(
        timelineItems = aTimelineItemList(aTimelineItemTextContent()),
        // Render a focused event for an event with sender information displayed
        focusedEventIndex = 2,
    ),
    timelineProtectionState: TimelineProtectionState = aTimelineProtectionState(),
    identityChangeState: IdentityChangeState = anIdentityChangeState(),
    linkState: LinkState = aLinkState(),
    readReceiptBottomSheetState: ReadReceiptBottomSheetState = aReadReceiptBottomSheetState(),
    actionListState: ActionListState = anActionListState(),
    customReactionState: CustomReactionState = aCustomReactionState(),
    reactionSummaryState: ReactionSummaryState = aReactionSummaryState(),
    hasNetworkConnection: Boolean = true,
    showReinvitePrompt: Boolean = false,
    enableVoiceMessages: Boolean = true,
    pinnedMessagesBannerState: PinnedMessagesBannerState = aLoadedPinnedMessagesBannerState(),
    dmUserVerificationState: IdentityState? = null,
    dmUserMapping: UserMapping? = null,
    roomMemberModerationState: RoomMemberModerationState = aRoomMemberModerationState(),
    successorRoom: SuccessorRoom? = null,
    userMappingService: UserMappingService = FakeUserMappingService(),
    eventSink: (MessagesEvents) -> Unit = {},
) = MessagesState(
    roomId = RoomId("!id:domain"),
    roomName = roomName,
    roomAvatar = roomAvatar,
    heroes = persistentListOf(),
    userEventPermissions = userEventPermissions,
    composerState = composerState,
    voiceMessageComposerState = voiceMessageComposerState,
    timelineProtectionState = timelineProtectionState,
    identityChangeState = identityChangeState,
    linkState = linkState,
    timelineState = timelineState,
    readReceiptBottomSheetState = readReceiptBottomSheetState,
    actionListState = actionListState,
    customReactionState = customReactionState,
    reactionSummaryState = reactionSummaryState,
    hasNetworkConnection = hasNetworkConnection,
    snackbarMessage = null,
    inviteProgress = AsyncData.Uninitialized,
    showReinvitePrompt = showReinvitePrompt,
    enableTextFormatting = true,
    enableVoiceMessages = enableVoiceMessages,
    appName = "Element",
    pinnedMessagesBannerState = pinnedMessagesBannerState,
    dmUserVerificationState = dmUserVerificationState,
    dmUserMapping = dmUserMapping,
    roomMemberModerationState = roomMemberModerationState,
    successorRoom = successorRoom,
    userMappingService = userMappingService,
    eventSink = eventSink,
)

fun aRoomMemberModerationState(
    canKick: Boolean = false,
    canBan: Boolean = false,
) = object : RoomMemberModerationState {
    override val canKick: Boolean = canKick
    override val canBan: Boolean = canBan
    override val eventSink: (RoomMemberModerationEvents) -> Unit = {}
}

fun aUserEventPermissions(
    canRedactOwn: Boolean = false,
    canRedactOther: Boolean = false,
    canSendMessage: Boolean = true,
    canSendReaction: Boolean = true,
    canPinUnpin: Boolean = false,
) = UserEventPermissions(
    canRedactOwn = canRedactOwn,
    canRedactOther = canRedactOther,
    canSendMessage = canSendMessage,
    canSendReaction = canSendReaction,
    canPinUnpin = canPinUnpin,
)

fun aReactionSummaryState(
    target: ReactionSummaryState.Summary? = null,
    eventSink: (ReactionSummaryEvents) -> Unit = {}
) = ReactionSummaryState(
    target = target,
    eventSink = eventSink,
)

fun aCustomReactionState(
    target: CustomReactionState.Target = CustomReactionState.Target.None,
    eventSink: (CustomReactionEvents) -> Unit = {},
) = CustomReactionState(
    target = target,
    selectedEmoji = persistentSetOf(),
    eventSink = eventSink,
)

fun aReadReceiptBottomSheetState(
    selectedEvent: TimelineItem.Event? = null,
    eventSink: (ReadReceiptBottomSheetEvents) -> Unit = {},
) = ReadReceiptBottomSheetState(
    selectedEvent = selectedEvent,
    eventSink = eventSink,
)
