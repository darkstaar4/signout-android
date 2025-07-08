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
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

@ContributesBinding(SessionScope::class)
class MatrixUserRepository @Inject constructor(
    private val client: MatrixClient,
    private val dataSource: UserListDataSource,
    private val cognitoUserSearchService: CognitoUserSearchService
) : UserRepository {
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
            // Handle @username searches (without domain)
            if (query.startsWith("@") && !query.contains(":")) {
                val username = query.substring(1) // Remove @
                val matrixUserId = "@$username:signout.io"
                
                Timber.d("Searching for @username: $username -> $matrixUserId")
                
                val matrixUser = dataSource.getProfile(UserId(matrixUserId))
                if (matrixUser != null) {
                    // Create enhanced user with Cognito display name
                    val enhancedUser = enhanceUserWithCognitoData(matrixUser, username)
                    results.add(UserSearchResult(enhancedUser))
                    Timber.d("Found user by @username: ${enhancedUser.displayName}")
                }
            } else {
                // Regular Matrix server search
                val matrixResults = dataSource
                    .search(query, MAXIMUM_SEARCH_RESULTS)
                    .filter { !client.isMe(it.userId) }
                
                // Enhance all Matrix users with Cognito data
                for (matrixUser in matrixResults) {
                    val username = extractUsernameFromMatrixId(matrixUser.userId.value)
                    val enhancedUser = enhanceUserWithCognitoData(matrixUser, username)
                    results.add(UserSearchResult(enhancedUser))
                }
                
                // Also search by real name using Cognito
                val realNameResults = searchByRealName(query)
                results.addAll(realNameResults)
            }
            
            // Handle direct Matrix ID queries
            if (shouldQueryProfile && results.none { it.matrixUser.userId.value == query }) {
                results.add(
                    0,
                    dataSource.getProfile(UserId(query))
                        ?.let { UserSearchResult(it) }
                        ?: UserSearchResult(MatrixUser(UserId(query)), isUnresolved = true)
                )
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error in search")
        }

        return UserSearchResultState(
            results = results.distinctBy { it.matrixUser.userId }.take(MAXIMUM_SEARCH_RESULTS.toInt()), 
            isSearching = false
        )
    }
    
    private suspend fun enhanceUserWithCognitoData(matrixUser: MatrixUser, username: String): MatrixUser = withContext(Dispatchers.IO) {
        try {
            // Try to get Cognito user data for this username
            val cognitoDisplayName = cognitoUserSearchService.getCognitoDisplayName(username)
            
            if (cognitoDisplayName != null) {
                // Return MatrixUser with Cognito display name but keep Matrix user ID
                MatrixUser(
                    userId = matrixUser.userId,
                    displayName = cognitoDisplayName,
                    avatarUrl = matrixUser.avatarUrl
                )
            } else {
                // Fallback to original Matrix user
                matrixUser
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to enhance user with Cognito data")
            matrixUser
        }
    }
    
    private suspend fun searchByRealName(query: String): List<UserSearchResult> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Searching by real name: $query")
            
            val results = mutableListOf<UserSearchResult>()
            
            // Use the Cognito service to search by real name
            val cognitoResults = cognitoUserSearchService.searchUsersByRealName(query)
            
            for (cognitoUser in cognitoResults) {
                try {
                    val matrixUserId = "@${cognitoUser.username}:signout.io"
                    val matrixUser = dataSource.getProfile(UserId(matrixUserId))
                    if (matrixUser != null) {
                        // Create enhanced user with Cognito display name
                        val enhancedUser = MatrixUser(
                            userId = matrixUser.userId,
                            displayName = cognitoUser.displayName,
                            avatarUrl = matrixUser.avatarUrl
                        )
                        results.add(UserSearchResult(enhancedUser))
                        Timber.d("Found user by real name search: ${enhancedUser.displayName}")
                    }
                } catch (e: Exception) {
                    // Ignore individual lookup failures
                    Timber.d("Failed to lookup Matrix user for ${cognitoUser.username}: ${e.message}")
                }
            }
            
            results
        } catch (e: Exception) {
            Timber.e(e, "Failed to search by real name")
            emptyList()
        }
    }
    
    private fun extractUsernameFromMatrixId(matrixId: String): String {
        // Extract username from @username:domain format
        return matrixId.removePrefix("@").substringBefore(":")
    }
    
    private fun formatMatrixUsername(username: String): String {
        // Use the same formatting logic as MatrixIntegrationService
        return username.lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .take(32)
    }

    companion object {
        private const val DEBOUNCE_TIME_MILLIS = 250L
        private const val MINIMUM_SEARCH_LENGTH = 2 // Reduced for @username searches
        private const val MAXIMUM_SEARCH_RESULTS = 10L
    }
}
