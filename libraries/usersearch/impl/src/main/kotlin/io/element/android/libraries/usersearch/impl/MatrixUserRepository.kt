/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.usersearch.api.UserDirectoryService
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResponse
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import io.element.android.libraries.usersearch.impl.network.AwsUserSearchService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class MatrixUserRepository @Inject constructor(
    private val matrixClient: MatrixClient,
    private val userDirectoryService: UserDirectoryService,
    private val userMappingService: UserMappingService,
    private val awsUserSearchService: AwsUserSearchService,
    private val dispatchers: CoroutineDispatchers,
) : UserRepository {

    // Legacy search method for backward compatibility
    override fun search(query: String): Flow<UserSearchResultState> = flow {
        emit(UserSearchResultState(results = emptyList(), isSearching = true))
        
        try {
            val response = searchUsers(query, 50)
            val results = response.results.map { matrixUser ->
                // Extract username from Matrix ID: @username:domain -> username
                val username = matrixUser.userId.value.substringAfter("@").substringBefore(":")
                Timber.d("UserRepository: Looking for user mapping for username: '$username' from Matrix ID: '${matrixUser.userId.value}'")
                
                // Try to get user mapping for enhanced display
                val userMapping = userMappingService.getUserMapping(username)
                
                if (userMapping != null) {
                    Timber.d("UserRepository: Found user mapping for '$username': ${userMapping.displayName}")
                } else {
                    Timber.w("UserRepository: No user mapping found for '$username'")
                }
                
                UserSearchResult(
                    matrixUser = matrixUser,
                    userMapping = userMapping,
                    isUnresolved = userMapping == null
                )
            }
            
            Timber.d("UserRepository: Legacy search completed with ${results.size} results")
            emit(UserSearchResultState(results = results, isSearching = false))
        } catch (e: Exception) {
            Timber.e(e, "UserRepository: Error in legacy search - ${e.message}")
            Timber.e("UserRepository: Exception details: ${e.javaClass.simpleName}")
            // Even if there's an error, try to return cached results
            try {
                val cachedResponse = getCachedUsers(50, query)
                val cachedResults = cachedResponse.results.map { matrixUser ->
                    val username = matrixUser.userId.value.substringAfter("@").substringBefore(":")
                    val userMapping = userMappingService.getUserMapping(username)
                    
                    UserSearchResult(
                        matrixUser = matrixUser,
                        userMapping = userMapping,
                        isUnresolved = userMapping == null
                    )
                }
                emit(UserSearchResultState(results = cachedResults, isSearching = false))
            } catch (cacheException: Exception) {
                Timber.e(cacheException, "UserRepository: Error in cached search fallback - ${cacheException.message}")
                Timber.e("UserRepository: Cache exception details: ${cacheException.javaClass.simpleName}")
                emit(UserSearchResultState(results = emptyList(), isSearching = false))
            }
        }
    }

    override suspend fun searchUsers(query: String, limit: Long): UserSearchResponse = withContext(dispatchers.io) {
        Timber.d("UserRepository: Searching for users with query: '$query', limit: $limit")
        
        val results = mutableListOf<MatrixUser>()
        val addedUserIds = mutableSetOf<String>()
        
        // If query is empty or less than 2 characters, show cached users only
        if (query.isBlank()) {
            Timber.d("UserRepository: Empty query, showing all cached users")
            return@withContext getCachedUsers(limit)
        }
        
        if (query.length < 2) {
            Timber.d("UserRepository: Query too short (${query.length} chars), showing cached users matching query")
            return@withContext getCachedUsers(limit, query)
        }
        
        // For 2+ characters, prioritize AWS backend search
        try {
            Timber.d("UserRepository: Query length ${query.length}, searching AWS backend first")
            val awsUsers = awsUserSearchService.searchUsers(query)
            Timber.d("UserRepository: Found ${awsUsers.size} users from AWS backend")
            
            // Convert AWS users to MatrixUser objects and add to results
            awsUsers.forEach { awsUser ->
                val matrixUser = MatrixUser(
                    userId = UserId(awsUser.matrixUserId),
                    displayName = awsUser.displayName,
                    avatarUrl = awsUser.avatarUrl
                )
                results.add(matrixUser)
                addedUserIds.add(matrixUser.userId.value)
                
                // Add to local directory and user mapping for future caching
                userDirectoryService.addUser(awsUser)
                
                // Add to user mapping service
                userMappingService.addUserFromCognitoData(
                    matrixUserId = awsUser.matrixUserId,
                    matrixUsername = awsUser.matrixUserId.substringAfter("@").substringBefore(":"),
                    cognitoUsername = awsUser.cognitoUsername,
                    givenName = awsUser.givenName,
                    familyName = awsUser.familyName,
                    email = awsUser.email,
                    specialty = awsUser.specialty,
                    officeCity = awsUser.officeCity,
                    avatarUrl = awsUser.avatarUrl
                )
                
                Timber.d("UserRepository: Added AWS user to results: ${awsUser.displayName} (${awsUser.matrixUserId})")
            }
        } catch (e: Exception) {
            Timber.w(e, "UserRepository: Failed to search AWS backend, falling back to local search")
        }
        
        // Add cached users that match but weren't found in AWS search
        val cachedUsers = getCachedUsers(limit, query)
        cachedUsers.results.forEach { cachedUser ->
            if (!addedUserIds.contains(cachedUser.userId.value)) {
                results.add(cachedUser)
                addedUserIds.add(cachedUser.userId.value)
                Timber.d("UserRepository: Added cached user to results: ${cachedUser.displayName} (${cachedUser.userId.value})")
            }
        }
        
        // Fallback to Matrix server search if still no results
        if (results.isEmpty()) {
            try {
                Timber.d("UserRepository: No results from AWS/local search, trying Matrix server search")
                val matrixSearchResult = matrixClient.searchUsers(query, limit)
                matrixSearchResult.fold(
                    onSuccess = { searchResults ->
                        searchResults.results.forEach { matrixUser ->
                            if (!addedUserIds.contains(matrixUser.userId.value)) {
                                results.add(matrixUser)
                                addedUserIds.add(matrixUser.userId.value)
                            }
                        }
                        Timber.d("UserRepository: Found ${searchResults.results.size} users from Matrix server")
                    },
                    onFailure = { error ->
                        Timber.w(error, "UserRepository: Matrix server search failed")
                    }
                )
            } catch (e: Exception) {
                Timber.w(e, "UserRepository: Matrix server search failed")
            }
        }
        
        // Take only the requested limit
        val limitedResults = results.take(limit.toInt())
        
        Timber.d("UserRepository: Returning ${limitedResults.size} total users for query: '$query'")
        
        UserSearchResponse(
            results = limitedResults,
            limited = limitedResults.size >= limit
        )
    }
    
    private suspend fun getCachedUsers(limit: Long, query: String = ""): UserSearchResponse {
        val results = mutableListOf<MatrixUser>()
        val addedUserIds = mutableSetOf<String>()
        
        // Get users from mapping service first (these have enhanced data)
        val mappedUsers = if (query.isBlank()) {
            userMappingService.searchUsers("", limit) // Get all cached mappings
        } else {
            userMappingService.searchUsers(query, limit)
        }
        
        mappedUsers.forEach { mapping ->
            val matrixUser = MatrixUser(
                userId = UserId(mapping.matrixUserId),
                displayName = mapping.displayName,
                avatarUrl = mapping.avatarUrl
            )
            results.add(matrixUser)
            addedUserIds.add(matrixUser.userId.value)
            Timber.d("UserRepository: Added cached mapped user: ${mapping.displayName} (${mapping.matrixUserId})")
        }
        
        // Get users from local directory
        val localUsers = if (query.isBlank()) {
            userDirectoryService.search("", limit) // Get all cached users
        } else {
            userDirectoryService.search(query, limit)
        }
        
        localUsers.forEach { localUser ->
            if (!addedUserIds.contains(localUser.matrixUserId)) {
                val matrixUser = MatrixUser(
                    userId = UserId(localUser.matrixUserId),
                    displayName = localUser.displayName,
                    avatarUrl = localUser.avatarUrl
                )
                results.add(matrixUser)
                addedUserIds.add(matrixUser.userId.value)
                Timber.d("UserRepository: Added cached local user: ${localUser.displayName} (${localUser.matrixUserId})")
            }
        }
        
        val limitedResults = results.take(limit.toInt())
        Timber.d("UserRepository: Returning ${limitedResults.size} cached users for query: '$query'")
        
        return UserSearchResponse(
            results = limitedResults,
            limited = limitedResults.size >= limit
        )
    }
}
