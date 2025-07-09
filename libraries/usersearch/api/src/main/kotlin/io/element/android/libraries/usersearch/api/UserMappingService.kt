/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

/**
 * Data class for user mapping that includes Cognito user information
 */
data class UserMapping(
    val matrixUserId: String, // e.g., "@racexcars:signout.io"
    val matrixUsername: String, // e.g., "racexcars"
    val cognitoUsername: String?, // e.g., "racexcars" (preferred_username)
    val displayName: String, // e.g., "Race X Cars" (given_name + family_name)
    val firstName: String?, // e.g., "Race" (given_name)
    val lastName: String?, // e.g., "X Cars" (family_name)
    val email: String?, // e.g., "race@example.com"
    val specialty: String?, // e.g., "Addiction Medicine" (custom:specialty)
    val officeCity: String?, // e.g., "Fresno" (custom:office_city)
    val avatarUrl: String? = null, // Matrix avatar URL
)

/**
 * Service for managing user mappings between Matrix users and Cognito user data.
 * This service stores enhanced user information that includes Cognito attributes
 * like specialty and office city for better search result display.
 */
interface UserMappingService {
    
    /**
     * Initialize the service. This should be called once when the service is created.
     */
    fun initialize()
    
    /**
     * Get a user mapping by their Matrix username (without @ or domain).
     * 
     * @param username The Matrix username (e.g., "racexcars")
     * @return The user mapping if found, null otherwise
     */
    fun getUserMapping(username: String): UserMapping?
    
    /**
     * Search for users by query string. This searches across display names,
     * usernames, specialties, and office cities.
     * 
     * @param query The search query
     * @return List of matching user mappings
     */
    fun searchUsers(query: String): List<UserMapping>
    
    /**
     * Add a user mapping from Cognito data. This creates a UserMapping from
     * the provided Cognito user attributes.
     * 
     * @param matrixUserId The Matrix user ID (e.g., "@username:domain")
     * @param matrixUsername The Matrix username (e.g., "username")
     * @param cognitoUsername The Cognito preferred_username
     * @param firstName The Cognito given_name
     * @param lastName The Cognito family_name
     * @param email The Cognito email
     * @param specialty The Cognito custom:specialty
     * @param officeCity The Cognito custom:office_city
     */
    fun addUserFromCognitoData(
        matrixUserId: String,
        matrixUsername: String,
        cognitoUsername: String?,
        firstName: String?,
        lastName: String?,
        email: String?,
        specialty: String?,
        officeCity: String?
    )
    
    /**
     * Remove a user mapping by their Matrix username.
     * 
     * @param username The Matrix username (e.g., "racexcars")
     */
    fun removeUser(username: String)
    
    /**
     * Clear all user mappings. This is useful for logout scenarios.
     */
    fun clearAll()
} 