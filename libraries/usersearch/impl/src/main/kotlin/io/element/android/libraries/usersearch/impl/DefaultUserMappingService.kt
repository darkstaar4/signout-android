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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class DefaultUserMappingService @Inject constructor() : UserMappingService {
    
    private val mutex = Mutex()
    private val userMappings = mutableMapOf<String, UserMapping>()
    
    override suspend fun getUserMapping(username: String): UserMapping? {
        return mutex.withLock {
            userMappings[username.lowercase()]
        }
    }
    
    override suspend fun searchUsers(query: String): List<UserMapping> {
        return mutex.withLock {
            val lowercaseQuery = query.lowercase()
            userMappings.values.filter { mapping ->
                mapping.matrixUsername.lowercase().contains(lowercaseQuery) ||
                mapping.displayName.lowercase().contains(lowercaseQuery) ||
                mapping.cognitoUsername?.lowercase()?.contains(lowercaseQuery) == true ||
                mapping.firstName?.lowercase()?.contains(lowercaseQuery) == true ||
                mapping.lastName?.lowercase()?.contains(lowercaseQuery) == true ||
                mapping.email?.lowercase()?.contains(lowercaseQuery) == true
            }.sortedBy { it.displayName }
        }
    }
    
    override suspend fun updateUserMapping(mapping: UserMapping) {
        mutex.withLock {
            userMappings[mapping.matrixUsername.lowercase()] = mapping
            Timber.d("DefaultUserMappingService: Updated mapping for ${mapping.matrixUsername}")
        }
    }
    
    override suspend fun syncUserMappings() {
        // Basic implementation - in a real app this might sync with a remote service
        Timber.d("DefaultUserMappingService: syncUserMappings called")
    }
    
    override suspend fun initializeWithDefaultData() {
        mutex.withLock {
            // Add some default test data
            val defaultMappings = listOf(
                UserMapping(
                    matrixUserId = "@test:signout.io",
                    matrixUsername = "test",
                    cognitoUsername = "TestUser",
                    displayName = "Test User",
                    firstName = "Test",
                    lastName = "User",
                    email = "test@signout.io",
                    avatarUrl = null
                ),
                UserMapping(
                    matrixUserId = "@admin:signout.io",
                    matrixUsername = "admin",
                    cognitoUsername = "AdminUser",
                    displayName = "Admin User",
                    firstName = "Admin",
                    lastName = "User",
                    email = "admin@signout.io",
                    avatarUrl = null
                )
            )
            
            defaultMappings.forEach { mapping ->
                userMappings[mapping.matrixUsername.lowercase()] = mapping
            }
            
            Timber.d("DefaultUserMappingService: Initialized with ${defaultMappings.size} default mappings")
        }
    }
} 