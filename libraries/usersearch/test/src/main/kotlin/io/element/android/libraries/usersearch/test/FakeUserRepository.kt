/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.test

import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResponse
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeUserRepository : UserRepository {
    var providedQuery: String? = null
        private set

    private val flow = MutableSharedFlow<UserSearchResultState>()

    override fun search(query: String): Flow<UserSearchResultState> {
        providedQuery = query
        return flow
    }

    override suspend fun searchUsers(query: String, limit: Long): UserSearchResponse {
        providedQuery = query
        return UserSearchResponse(results = emptyList(), limited = false)
    }

    suspend fun emitState(state: UserSearchResultState) {
        flow.emit(state)
    }
}
