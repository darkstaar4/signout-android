/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.createroom.impl.userlist

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.recent.RecentDirectRoom
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.aMatrixUserList
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.UserMapping
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Fake UserMappingService for testing purposes
private class FakeUserMappingService : UserMappingService {
    override fun getUserMapping(username: String): UserMapping? = null
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

open class UserListStateProvider : PreviewParameterProvider<UserListState> {
    override val values: Sequence<UserListState>
        get() = sequenceOf(
            aUserListState(),
            aUserListState(
                isSearchActive = false,
                selectedUsers = aListOfSelectedUsers(),
                selectionMode = SelectionMode.Multiple,
            ),
            aUserListState(isSearchActive = true),
            aUserListState(isSearchActive = true, searchQuery = "someone"),
            aUserListState(isSearchActive = true, searchQuery = "someone", selectionMode = SelectionMode.Multiple),
            aUserListState(
                isSearchActive = true,
                searchQuery = "@someone:matrix.org",
                selectedUsers = aMatrixUserList().toImmutableList(),
                searchResults = SearchBarResultState.Results(aListOfUserSearchResults()),
            ),
            aUserListState(
                isSearchActive = true,
                searchQuery = "@someone:matrix.org",
                selectionMode = SelectionMode.Multiple,
                selectedUsers = aMatrixUserList().toImmutableList(),
                searchResults = SearchBarResultState.Results(aListOfUserSearchResults()),
            ),
            aUserListState(
                isSearchActive = true,
                searchQuery = "something-with-no-results",
                searchResults = SearchBarResultState.NoResultsFound()
            ),
            aUserListState(
                isSearchActive = true,
                searchQuery = "someone",
                selectionMode = SelectionMode.Single,
            ),
            aUserListState(
                recentDirectRooms = aRecentDirectRoomList(),
            ),
        )
}

fun aUserListState(
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    searchResults: SearchBarResultState<ImmutableList<UserSearchResult>> = SearchBarResultState.Initial(),
    selectedUsers: List<MatrixUser> = emptyList(),
    showSearchLoader: Boolean = false,
    selectionMode: SelectionMode = SelectionMode.Single,
    recentDirectRooms: List<RecentDirectRoom> = emptyList(),
    userMappingService: UserMappingService = FakeUserMappingService(),
    eventSink: (UserListEvents) -> Unit = {},
) = UserListState(
    searchQuery = searchQuery,
    isSearchActive = isSearchActive,
    searchResults = searchResults,
    selectedUsers = selectedUsers.toImmutableList(),
    showSearchLoader = showSearchLoader,
    selectionMode = selectionMode,
    recentDirectRooms = recentDirectRooms.toImmutableList(),
    userMappingService = userMappingService,
    eventSink = eventSink
)

fun aListOfSelectedUsers() = aMatrixUserList().take(6).toImmutableList()
fun aListOfUserSearchResults() = aMatrixUserList().take(6).map { UserSearchResult(it) }.toImmutableList()

fun aRecentDirectRoomList(
    count: Int = 5
): List<RecentDirectRoom> = aMatrixUserList()
    .take(count)
    .map {
        RecentDirectRoom(RoomId("!aRoom:id"), it)
    }
