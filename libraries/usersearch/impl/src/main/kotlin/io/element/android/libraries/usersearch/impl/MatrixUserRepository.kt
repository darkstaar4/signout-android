/*
 * Copyright 2024 New Vector Ltd.
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
import io.element.android.libraries.usersearch.api.CognitoUserIntegrationService
import io.element.android.libraries.usersearch.api.UserDirectoryService
import io.element.android.libraries.usersearch.api.UserListDataSource
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import timber.log.Timber

@ContributesBinding(SessionScope::class)
class MatrixUserRepository @Inject constructor(
    private val client: MatrixClient,
    private val dataSource: UserListDataSource,
    private val userMappingService: UserMappingService,
    private val userDirectoryService: UserDirectoryService,
    private val cognitoUserIntegrationService: CognitoUserIntegrationService,
) : UserRepository {
    
    init {
        // Initialize the user mapping service
        userMappingService.initialize()
        Timber.d("MatrixUserRepository: UserMappingService initialized")
        
        // Populate current user's Cognito data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cognitoUserIntegrationService.populateCurrentUserMapping()
                Timber.d("MatrixUserRepository: Current user mapping populated")
            } catch (e: Exception) {
                Timber.w(e, "MatrixUserRepository: Failed to populate current user mapping")
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
            
            // First, search in backend user directory (real Cognito data)
            val directoryResults = userDirectoryService.searchUsers(query).getOrElse { emptyList() }
            Timber.d("MatrixUserRepository: Found ${directoryResults.size} users in backend directory for query: '$query'")
            
            // Convert directory results to UserSearchResult
            for (directoryEntry in directoryResults) {
                try {
                    // Try to get the Matrix user profile from server
                    val matrixUser = dataSource.getProfile(UserId(directoryEntry.matrixUserId))
                    
                    if (matrixUser != null) {
                        // Create UserMapping directly from directory entry with enhanced Cognito data
                        val mapping = UserMapping(
                            matrixUserId = directoryEntry.matrixUserId,
                            matrixUsername = directoryEntry.cognitoUsername,
                            cognitoUsername = directoryEntry.cognitoUsername,
                            firstName = directoryEntry.givenName,
                            lastName = directoryEntry.familyName,
                            displayName = directoryEntry.displayName,
                            email = directoryEntry.email,
                            specialty = directoryEntry.specialty,
                            officeCity = directoryEntry.officeCity,
                            avatarUrl = directoryEntry.avatarUrl
                        )
                        
                        val enhancedUser = matrixUser.copy(
                            displayName = directoryEntry.displayName
                        )
                        results.add(UserSearchResult(enhancedUser, mapping))
                        Timber.d("MatrixUserRepository: Found user from backend directory: ${directoryEntry.displayName} (${directoryEntry.matrixUserId})")
                        Timber.d("MatrixUserRepository: UserMapping specialty: ${mapping?.specialty}, officeCity: ${mapping?.officeCity}")
                    } else {
                        // Create offline user from directory data
                        val offlineUser = MatrixUser(
                            userId = UserId(directoryEntry.matrixUserId),
                            displayName = directoryEntry.displayName,
                            avatarUrl = null
                        )
                        val mapping = userMappingService.getUserMapping(directoryEntry.cognitoUsername)
                        results.add(UserSearchResult(offlineUser, mapping))
                        Timber.d("MatrixUserRepository: Created offline user from directory: ${directoryEntry.displayName}")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "MatrixUserRepository: Error processing directory entry for ${directoryEntry.matrixUserId}")
                }
            }
            
            // Fallback: search in local mapping database if no backend results
            if (results.isEmpty()) {
                val mappingResults = userMappingService.searchUsers(query)
                Timber.d("MatrixUserRepository: Fallback to local mappings - found ${mappingResults.size} mappings for query: '$query'")
            
                for (mapping in mappingResults) {
                    try {
                        // Try to get the Matrix user profile from server first
                        val matrixUser = dataSource.getProfile(UserId(mapping.matrixUserId))
                        
                        if (matrixUser != null) {
                            // Use server data with our enhanced information
                            val enhancedUser = matrixUser.copy(
                                displayName = mapping.displayName
                            )
                            results.add(UserSearchResult(enhancedUser, mapping))
                            Timber.d("MatrixUserRepository: Found user from server: ${mapping.displayName} (${mapping.matrixUserId})")
                        } else {
                            // Create user from mapping data directly (offline mode)
                            val offlineUser = MatrixUser(
                                userId = UserId(mapping.matrixUserId),
                                displayName = mapping.displayName,
                                avatarUrl = mapping.avatarUrl
                            )
                            results.add(UserSearchResult(offlineUser, mapping))
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
                        results.add(UserSearchResult(fallbackUser, mapping))
                    }
                }
            }
            
            // If no results from mapping, try Matrix server search
            if (results.isEmpty() && !query.startsWith("@")) {
                Timber.d("MatrixUserRepository: No local results, trying Matrix server search")
                val matrixResults = dataSource
                    .search(query, MAXIMUM_SEARCH_RESULTS)
                    .filter { !client.isMe(it.userId) }
                
                Timber.d("MatrixUserRepository: Matrix server returned ${matrixResults.size} results")
                
                // Enhance Matrix results with mapping data if available, or discover new mappings
                for (matrixUser in matrixResults) {
                    val username = extractUsernameFromMatrixId(matrixUser.userId.value)
                    var mapping = userMappingService.getUserMapping(username)
                    
                    if (mapping == null) {
                        // Try to discover and create a mapping for this user
                        cognitoUserIntegrationService.discoverUserMapping(
                            matrixUser.userId.value, 
                            matrixUser.displayName
                        )
                        // Check again after discovery attempt
                        mapping = userMappingService.getUserMapping(username)
                    }
                    
                    if (mapping != null) {
                        val enhancedUser = matrixUser.copy(
                            displayName = mapping.displayName
                        )
                        results.add(UserSearchResult(enhancedUser, mapping))
                        Timber.d("MatrixUserRepository: Enhanced Matrix user with mapping: ${mapping.displayName}")
                    } else {
                        results.add(UserSearchResult(matrixUser, null))
                        Timber.d("MatrixUserRepository: Added Matrix user without mapping: ${matrixUser.displayName ?: matrixUser.userId.value}")
                    }
                }
            }
            
            // Handle direct Matrix ID queries
            if (shouldQueryProfile && results.none { it.matrixUser.userId.value == query }) {
                Timber.d("MatrixUserRepository: Handling direct Matrix ID query: $query")
                val profileUser = dataSource.getProfile(UserId(query))
                if (profileUser != null) {
                    val username = extractUsernameFromMatrixId(query)
                    var mapping = userMappingService.getUserMapping(username)
                    
                    if (mapping == null) {
                        // Try to discover mapping for direct profile query
                        cognitoUserIntegrationService.discoverUserMapping(query, profileUser.displayName)
                        mapping = userMappingService.getUserMapping(username)
                    }
                    
                    results.add(0, UserSearchResult(profileUser, mapping))
                } else {
                    results.add(0, UserSearchResult(MatrixUser(UserId(query)), null, isUnresolved = true))
                }
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
