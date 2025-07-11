/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.usersearch.api.CognitoUserData
import io.element.android.libraries.usersearch.api.UserDirectoryEntry
import io.element.android.libraries.usersearch.api.UserDirectoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(SessionScope::class)
class DefaultUserDirectoryService @Inject constructor() : UserDirectoryService {
    
    // Add instance identifier for debugging
    private val instanceId = System.currentTimeMillis().toString().takeLast(4)
    
    companion object {
        // Shared in-memory storage across all instances - in production this would be a backend API
        private val userDirectory = ConcurrentHashMap<String, UserDirectoryEntry>()
    }
    
    init {
        Timber.d("UserDirectoryService: Created instance $instanceId")
    }
    
    override suspend fun storeUserProfile(
        matrixUserId: String,
        cognitoData: CognitoUserData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entry = UserDirectoryEntry(
                matrixUserId = matrixUserId,
                cognitoUsername = cognitoData.cognitoUsername,
                displayName = "${cognitoData.givenName} ${cognitoData.familyName}",
                givenName = cognitoData.givenName,
                familyName = cognitoData.familyName,
                email = cognitoData.email,
                specialty = cognitoData.specialty,
                officeCity = cognitoData.officeCity,
                officeState = cognitoData.officeState,
                professionalTitle = cognitoData.professionalTitle,
                phoneNumber = cognitoData.phoneNumber,
                npiNumber = cognitoData.npiNumber,
                avatarUrl = null // Avatar URL is not in CognitoUserData
            )
            
            userDirectory[matrixUserId] = entry
            Timber.d("UserDirectoryService: Stored user profile for $matrixUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "UserDirectoryService: Failed to store user profile for $matrixUserId")
            Result.failure(e)
        }
    }
    
    override suspend fun addUser(userEntry: UserDirectoryEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDirectory[userEntry.matrixUserId] = userEntry
            Timber.d("UserDirectoryService: [Instance $instanceId] Added user to directory: ${userEntry.displayName} (${userEntry.matrixUserId})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "UserDirectoryService: Failed to add user to directory: ${userEntry.matrixUserId}")
            Result.failure(e)
        }
    }
    
    override suspend fun searchUsers(
        query: String,
        limit: Int
    ): Result<List<UserDirectoryEntry>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("UserDirectoryService: [Instance $instanceId] Searching users for query: '$query'")
            Timber.d("UserDirectoryService: [Instance $instanceId] Directory contains ${userDirectory.size} users: ${userDirectory.keys.joinToString()}")
            
            if (query.isBlank()) {
                // Return all cached users if query is empty
                val results = userDirectory.values.take(limit)
                Timber.d("UserDirectoryService: [Instance $instanceId] Returning ${results.size} cached users for empty query")
                return@withContext Result.success(results)
            }
            
            val searchQuery = query.lowercase().trim()
            val results = userDirectory.values.filter { entry ->
                // Search only in firstname, lastname, and username as requested
                // Search in display name (full name)
                entry.displayName.lowercase().contains(searchQuery) ||
                // Search in given name (firstname)
                entry.givenName.lowercase().contains(searchQuery) ||
                // Search in family name (lastname)
                entry.familyName.lowercase().contains(searchQuery) ||
                // Search in Cognito username
                entry.cognitoUsername.lowercase().contains(searchQuery) ||
                // Search by @username pattern
                searchQuery.startsWith("@") && entry.cognitoUsername.lowercase().contains(searchQuery.removePrefix("@")) ||
                // Additional flexible matching for partial words in names
                entry.displayName.lowercase().replace(" ", "").contains(searchQuery.replace(" ", "")) ||
                // Match against individual words in display name
                entry.displayName.lowercase().split(" ").any { word -> 
                    word.contains(searchQuery) || searchQuery.contains(word)
                }
            }.sortedBy { entry ->
                // Sort by relevance: exact matches first, then display name matches
                when {
                    entry.cognitoUsername.lowercase() == searchQuery.removePrefix("@") -> 0
                    entry.displayName.lowercase().startsWith(searchQuery) -> 1
                    entry.givenName.lowercase() == searchQuery -> 2
                    entry.familyName.lowercase() == searchQuery -> 3
                    else -> 4
                }
            }.take(limit)
            
            Timber.d("UserDirectoryService: Found ${results.size} users for query: '$query'")
            Result.success(results)
        } catch (e: Exception) {
            Timber.e(e, "UserDirectoryService: Error searching users for query: '$query'")
            Result.failure(e)
        }
    }
    
    override suspend fun search(
        query: String,
        limit: Long
    ): List<UserDirectoryEntry> {
        return searchUsers(query, limit.toInt()).getOrElse { emptyList() }
    }
    
    override suspend fun getUserProfile(matrixUserId: String): Result<UserDirectoryEntry?> = withContext(Dispatchers.IO) {
        try {
            val entry = userDirectory[matrixUserId]
            Timber.d("UserDirectoryService: Retrieved user profile for $matrixUserId: ${entry?.displayName}")
            Result.success(entry)
        } catch (e: Exception) {
            Timber.e(e, "UserDirectoryService: Failed to get user profile for $matrixUserId")
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserProfile(
        matrixUserId: String,
        cognitoData: CognitoUserData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entry = UserDirectoryEntry(
                matrixUserId = matrixUserId,
                cognitoUsername = cognitoData.cognitoUsername,
                displayName = "${cognitoData.givenName} ${cognitoData.familyName}",
                givenName = cognitoData.givenName,
                familyName = cognitoData.familyName,
                email = cognitoData.email,
                specialty = cognitoData.specialty,
                officeCity = cognitoData.officeCity,
                officeState = cognitoData.officeState,
                professionalTitle = cognitoData.professionalTitle,
                phoneNumber = cognitoData.phoneNumber,
                npiNumber = cognitoData.npiNumber,
                avatarUrl = null // Avatar URL is not in CognitoUserData
            )
            
            userDirectory[matrixUserId] = entry
            Timber.d("UserDirectoryService: Updated user profile for $matrixUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "UserDirectoryService: Failed to update user profile for $matrixUserId")
            Result.failure(e)
        }
    }
} 