/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun search(query: String): Flow<UserSearchResultState>
    
    suspend fun searchUsers(query: String, limit: Long = 10): UserSearchResponse
}
