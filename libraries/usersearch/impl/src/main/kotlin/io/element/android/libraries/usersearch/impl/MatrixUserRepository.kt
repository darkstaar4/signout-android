/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.usersearch.api.UserListDataSource
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ContributesBinding(SessionScope::class)
class MatrixUserRepository @Inject constructor(
    private val client: MatrixClient,
    private val dataSource: UserListDataSource,
    private val userMappingService: UserMappingService
) : UserRepository {
    
    init {
        // Initialize the user mapping service with default data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMappingService.initializeWithDefaultData()
                Timber.d("MatrixUserRepository: UserMappingService initialized with default data")
            } catch (e: Exception) {
                Timber.w(e, "MatrixUserRepository: Failed to initialize UserMappingService")
            }
        }
    }
    
    override fun search(query: String): Flow<UserSearchResultState> = flow {
        val shouldQueryProfile = MatrixPatterns.isUserId(query) && !client.isMe(UserId(query))
        val shouldFetchSearchResults = query.length >= MINIMUM_SEARCH_LENGTH
        
        // For @username searches (without domain), we'll handle them specially
        val isAtUsernameSearch = query.startsWith("@") && !query.contains(":")
        
        if (shouldQueryProfile || shouldFetchSearchResults || isAtUsernameSearch) {
            emit(UserSearchResultState(isSearching = true, results = emptyList()))
        }
        
        if (shouldFetchSearchResults || isAtUsernameSearch) {
            val results = fetchSearchResults(query, shouldQueryProfile)
            emit(results)
        }
    }

    private suspend fun fetchSearchResults(query: String, shouldQueryProfile: Boolean): UserSearchResultState {
        // Debounce
        delay(DEBOUNCE_TIME_MILLIS)
        
        val results = mutableListOf<UserSearchResult>()
        
        try {
            Timber.d("MatrixUserRepository: Searching for query: '$query'")
            
            // First, search in our local mapping database
            val mappingResults = userMappingService.searchUsers(query)
            Timber.d("MatrixUserRepository: Found ${mappingResults.size} mappings for query: '$query'")
            
            for (mapping in mappingResults) {
                try {
                    // Try to get the Matrix user profile from server first
                    val matrixUser = dataSource.getProfile(UserId(mapping.matrixUserId))
                    
                    if (matrixUser != null) {
                        // Use server data with our display name
                        val enhancedUser = MatrixUser(
                            userId = matrixUser.userId,
                            displayName = mapping.displayName,
                            avatarUrl = matrixUser.avatarUrl ?: mapping.avatarUrl
                        )
                        results.add(UserSearchResult(enhancedUser))
                        Timber.d("MatrixUserRepository: Found user from server: ${mapping.displayName} (${mapping.matrixUserId})")
                    } else {
                        // Create user from mapping data directly (offline mode)
                        val offlineUser = MatrixUser(
                            userId = UserId(mapping.matrixUserId),
                            displayName = mapping.displayName,
                            avatarUrl = mapping.avatarUrl
                        )
                        results.add(UserSearchResult(offlineUser))
                        Timber.d("MatrixUserRepository: Created offline user: ${mapping.displayName} (${mapping.matrixUserId})")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "MatrixUserRepository: Error getting Matrix profile for ${mapping.matrixUserId}, creating offline user")
                    
                    // Create user from mapping data as fallback
                    val fallbackUser = MatrixUser(
                        userId = UserId(mapping.matrixUserId),
                        displayName = mapping.displayName,
                        avatarUrl = mapping.avatarUrl
                    )
                    results.add(UserSearchResult(fallbackUser))
                }
            }
            
            // If no results from mapping, try Matrix server search
            if (results.isEmpty() && !query.startsWith("@")) {
                Timber.d("MatrixUserRepository: No local results, trying Matrix server search")
                val matrixResults = dataSource
                    .search(query, MAXIMUM_SEARCH_RESULTS)
                    .filter { !client.isMe(it.userId) }
                
                Timber.d("MatrixUserRepository: Matrix server returned ${matrixResults.size} results")
                
                // Enhance Matrix results with mapping data if available
                for (matrixUser in matrixResults) {
                    val username = extractUsernameFromMatrixId(matrixUser.userId.value)
                    val mapping = userMappingService.getUserMapping(username)
                    
                    val enhancedUser = if (mapping != null) {
                        MatrixUser(
                            userId = matrixUser.userId,
                            displayName = mapping.displayName,
                            avatarUrl = matrixUser.avatarUrl ?: mapping.avatarUrl
                        )
                    } else {
                        matrixUser
                    }
                    
                    results.add(UserSearchResult(enhancedUser))
                }
            }
            
            // Handle direct Matrix ID queries
            if (shouldQueryProfile && results.none { it.matrixUser.userId.value == query }) {
                Timber.d("MatrixUserRepository: Handling direct Matrix ID query: $query")
                results.add(
                    0,
                    dataSource.getProfile(UserId(query))
                        ?.let { UserSearchResult(it) }
                        ?: UserSearchResult(MatrixUser(UserId(query)), isUnresolved = true)
                )
            }
            
        } catch (e: Exception) {
            Timber.e(e, "MatrixUserRepository: Error in search")
        }

        val finalResults = results.distinctBy { it.matrixUser.userId }.take(MAXIMUM_SEARCH_RESULTS.toInt())
        Timber.d("MatrixUserRepository: Returning ${finalResults.size} final results for query: '$query'")
        
        return UserSearchResultState(
            results = finalResults, 
            isSearching = false
        )
    }
    
    private fun extractUsernameFromMatrixId(matrixId: String): String {
        // Extract username from @username:domain format
        return matrixId.removePrefix("@").substringBefore(":")
    }

    companion object {
        private const val DEBOUNCE_TIME_MILLIS = 250L
        private const val MINIMUM_SEARCH_LENGTH = 2 // Reduced for @username searches
        private const val MAXIMUM_SEARCH_RESULTS = 10L
    }
}
