/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

/**
 * Simple data class for user mapping
 */
data class UserMapping(
    val matrixUserId: String, // e.g., "@racexcars:signout.io"
    val matrixUsername: String, // e.g., "racexcars"
    val cognitoUsername: String?, // e.g., "RaceX"
    val displayName: String, // e.g., "Race Cars"
    val firstName: String?, // e.g., "Race"
    val lastName: String?, // e.g., "Cars"
    val email: String?, // e.g., "racexcars@gmail.com"
    val avatarUrl: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Interface for user mapping operations
 */
interface UserMappingService {
    suspend fun getUserMapping(username: String): UserMapping?
    suspend fun searchUsers(query: String): List<UserMapping>
    suspend fun updateUserMapping(mapping: UserMapping)
    suspend fun syncUserMappings()
    suspend fun initializeWithDefaultData()
} 