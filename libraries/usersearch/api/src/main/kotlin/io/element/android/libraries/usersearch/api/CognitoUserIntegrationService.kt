/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.api

/**
 * Service for integrating Cognito user data with the user search system.
 * This service handles fetching user data from Cognito and populating the UserMappingService
 * with real user information for enhanced search results.
 */
interface CognitoUserIntegrationService {
    
    /**
     * Populate the UserMappingService with the current user's Cognito data.
     * This should be called during login to ensure the current user's information
     * is available for search results.
     */
    suspend fun populateCurrentUserMapping()
    
    /**
     * Attempt to discover and store Cognito data for a Matrix user.
     * This is called when a user appears in search results but we don't have
     * their Cognito mapping yet.
     * 
     * @param matrixUserId The Matrix user ID (e.g., "@username:domain")
     * @param matrixDisplayName The display name from Matrix (may contain Cognito data)
     */
    suspend fun discoverUserMapping(matrixUserId: String, matrixDisplayName: String?)
    
    /**
     * Extract and store user mapping from Matrix display name if it contains Cognito data.
     * During registration, Matrix display names are set from Cognito given_name + family_name,
     * so we can extract some information from existing Matrix users.
     * 
     * @param matrixUserId The Matrix user ID
     * @param matrixDisplayName The Matrix display name (potentially from Cognito)
     * @param matrixUsername The Matrix username (extracted from user ID)
     */
    suspend fun extractMappingFromMatrixData(
        matrixUserId: String, 
        matrixDisplayName: String?, 
        matrixUsername: String
    )
} 