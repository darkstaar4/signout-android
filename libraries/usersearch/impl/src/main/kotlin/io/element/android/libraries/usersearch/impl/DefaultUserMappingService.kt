/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.usersearch.api.UserMappingService
import timber.log.Timber
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(SessionScope::class)
class DefaultUserMappingService @Inject constructor() : UserMappingService {
    
    private val userMappings = ConcurrentHashMap<String, UserMapping>()
    private var isInitialized = false
    
    override fun initialize() {
        if (isInitialized) return
        
        Timber.d("UserMappingService: Initializing")
        // Clear any existing mappings on initialization
        userMappings.clear()
        
        // Add basic Matrix user mappings that we know exist
        // These will be enhanced with real Cognito data when discovered
        addBasicMatrixMapping("racexcars", "@racexcars:signout.io", "RaceX Cars")
        
        isInitialized = true
        Timber.d("UserMappingService: Initialized with ${userMappings.size} real Matrix mappings")
    }
    
    private fun addBasicMatrixMapping(username: String, matrixUserId: String, displayName: String) {
        val mapping = UserMapping(
            matrixUserId = matrixUserId,
            matrixUsername = username,
            cognitoUsername = null, // Will be populated later
            displayName = displayName,
            firstName = null,
            lastName = null,
            email = null,
            specialty = null,
            officeCity = null,
            avatarUrl = null
        )
        
        userMappings[username] = mapping
        Timber.d("UserMappingService: Added mapping for $username: $displayName")
    }
    
    override fun getUserMapping(username: String): UserMapping? {
        return userMappings[username]
    }
    
    override fun searchUsers(query: String): List<UserMapping> {
        if (query.isBlank()) return emptyList()
        
        val searchQuery = query.lowercase().trim()
        
        return userMappings.values.filter { mapping ->
            // Search in display name
            mapping.displayName.lowercase().contains(searchQuery) ||
            // Search in Matrix username
            mapping.matrixUsername.lowercase().contains(searchQuery) ||
            // Search in Cognito username
            mapping.cognitoUsername?.lowercase()?.contains(searchQuery) == true ||
            // Search in first name
            mapping.firstName?.lowercase()?.contains(searchQuery) == true ||
            // Search in last name
            mapping.lastName?.lowercase()?.contains(searchQuery) == true ||
            // Search in email
            mapping.email?.lowercase()?.contains(searchQuery) == true ||
            // Search in specialty
            mapping.specialty?.lowercase()?.contains(searchQuery) == true ||
            // Search in office city
            mapping.officeCity?.lowercase()?.contains(searchQuery) == true
        }.sortedBy { mapping ->
            // Sort by relevance: exact matches first, then display name matches
            when {
                mapping.matrixUsername.lowercase() == searchQuery -> 0
                mapping.displayName.lowercase().startsWith(searchQuery) -> 1
                mapping.firstName?.lowercase() == searchQuery -> 2
                mapping.lastName?.lowercase() == searchQuery -> 3
                else -> 4
            }
        }
    }
    
    override fun addUserFromCognitoData(
        matrixUserId: String,
        matrixUsername: String,
        cognitoUsername: String?,
        firstName: String?,
        lastName: String?,
        email: String?,
        specialty: String?,
        officeCity: String?
    ) {
        // Create display name from first and last name
        val displayName = when {
            firstName != null && lastName != null -> "$firstName $lastName"
            firstName != null -> firstName
            lastName != null -> lastName
            cognitoUsername != null -> cognitoUsername
            else -> matrixUsername
        }
        
        val mapping = UserMapping(
            matrixUserId = matrixUserId,
            matrixUsername = matrixUsername,
            cognitoUsername = cognitoUsername,
            displayName = displayName,
            firstName = firstName,
            lastName = lastName,
            email = email,
            specialty = specialty,
            officeCity = officeCity,
            avatarUrl = null // Will be updated from Matrix data if available
        )
        
        userMappings[matrixUsername] = mapping
        Timber.d("UserMappingService: Added mapping for $matrixUsername: $displayName")
    }
    
    override fun removeUser(username: String) {
        val removed = userMappings.remove(username)
        if (removed != null) {
            Timber.d("UserMappingService: Removed mapping for $username")
        }
    }
    
    override fun clearAll() {
        val count = userMappings.size
        userMappings.clear()
        Timber.d("UserMappingService: Cleared $count user mappings")
    }
} 