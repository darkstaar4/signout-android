/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

import kotlinx.coroutines.flow.Flow

/**
 * Data class for user mapping that includes Cognito user information
 */
data class UserMapping(
    val matrixUserId: String, // e.g., "@racexcars:signout.io"
    val matrixUsername: String, // e.g., "racexcars"
    val cognitoUsername: String, // e.g., "racexcars" (preferred_username)
    val displayName: String, // e.g., "Race X Cars" (given_name + family_name)
    val firstName: String, // e.g., "Race" (given_name)
    val lastName: String, // e.g., "X Cars" (family_name)
    val email: String, // e.g., "racexcars@gmail.com"
    val specialty: String?, // e.g., "Orthopedic Surgery"
    val officeCity: String?, // e.g., "San Diego"
    val avatarUrl: String? = null // Avatar URL if available
)

/**
 * Service for managing user mappings between Matrix usernames and enhanced Cognito user data
 */
interface UserMappingService {
    /**
     * Get user mapping for a given matrix username
     */
    fun getUserMapping(matrixUsername: String): UserMapping?
    
    /**
     * Add user mapping from Cognito data
     */
    fun addUserFromCognitoData(
        matrixUserId: String,
        matrixUsername: String,
        cognitoUsername: String,
        givenName: String,
        familyName: String,
        email: String,
        specialty: String?,
        officeCity: String?,
        avatarUrl: String? = null
    )
    
    /**
     * Add user mapping directly
     */
    fun addUserMapping(userMapping: UserMapping)
    
    /**
     * Search for users by query string
     */
    fun searchUsers(query: String): List<UserMapping>
    
    /**
     * Search for users by query string with limit
     */
    fun searchUsers(query: String, limit: Long): List<UserMapping>
    
    /**
     * Remove user mapping
     */
    fun removeUser(matrixUsername: String)
    
    /**
     * Clear all mappings
     */
    fun clearAll()
    
    /**
     * Get the count of cached mappings (for optimization checks)
     */
    fun getCachedMappingsCount(): Int
    
    /**
     * Flow that emits when user mappings are added or updated
     */
    val userMappingUpdates: Flow<UserMapping>
} 