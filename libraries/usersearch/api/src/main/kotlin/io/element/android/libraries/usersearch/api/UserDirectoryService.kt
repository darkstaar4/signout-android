/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

/**
 * Service for managing user directory data from Cognito
 * This service stores and retrieves real Cognito user attributes
 */
interface UserDirectoryService {
    
    /**
     * Store user profile data from Cognito after successful registration/login
     */
    suspend fun storeUserProfile(
        matrixUserId: String,
        cognitoData: CognitoUserData
    ): Result<Unit>
    
    /**
     * Add a user to the directory
     */
    suspend fun addUser(userEntry: UserDirectoryEntry): Result<Unit>
    
    /**
     * Search users by name, specialty, or other attributes
     */
    suspend fun searchUsers(
        query: String,
        limit: Int = 50
    ): Result<List<UserDirectoryEntry>>
    
    /**
     * Search users by name, specialty, or other attributes (simplified version)
     */
    suspend fun search(
        query: String,
        limit: Long = 50
    ): List<UserDirectoryEntry>
    
    /**
     * Get a specific user's profile
     */
    suspend fun getUserProfile(matrixUserId: String): Result<UserDirectoryEntry?>
    
    /**
     * Update user profile data
     */
    suspend fun updateUserProfile(
        matrixUserId: String,
        cognitoData: CognitoUserData
    ): Result<Unit>
}

/**
 * Real Cognito user data structure
 */
data class CognitoUserData(
    val matrixUserId: String,
    val matrixUsername: String,
    val cognitoUsername: String,
    val givenName: String,
    val familyName: String,
    val email: String,
    val phoneNumber: String? = null,
    val specialty: String? = null,
    val officeCity: String? = null,
    val officeState: String? = null,
    val professionalTitle: String? = null,
    val npiNumber: String? = null
)

/**
 * User directory entry with real Cognito data
 */
data class UserDirectoryEntry(
    val matrixUserId: String,
    val cognitoUsername: String,
    val displayName: String,
    val givenName: String,
    val familyName: String,
    val email: String,
    val specialty: String? = null,
    val officeCity: String? = null,
    val officeState: String? = null,
    val professionalTitle: String? = null,
    val phoneNumber: String? = null,
    val npiNumber: String? = null,
    val avatarUrl: String? = null
) 