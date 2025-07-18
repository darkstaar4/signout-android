/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

import io.element.android.libraries.matrix.api.user.MatrixUser

data class UserSearchResult(
    val matrixUser: MatrixUser,
    val userMapping: UserMapping? = null,
    val isUnresolved: Boolean = false,
)

data class UserSearchResultState(
    val results: List<UserSearchResult>,
    val isSearching: Boolean,
)

// New UserSearchResponse for the enhanced search API
data class UserSearchResponse(
    val results: List<MatrixUser>,
    val limited: Boolean = false,
)
