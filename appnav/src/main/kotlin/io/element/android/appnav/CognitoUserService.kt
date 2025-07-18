/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.SingleIn

@Serializable
data class CognitoApiUser(
    val matrix_user_id: String,
    val matrix_username: String,
    val cognito_username: String,
    val given_name: String,
    val family_name: String,
    val display_name: String,
    val email: String,
    val specialty: String? = null,
    val office_city: String? = null,
    val npi_number: String? = null,
    val phone_number: String? = null,
    val avatar_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Add Cognito status fields
    val user_status: String? = null,
    val is_enabled: Boolean = true,
    val is_active: Boolean = true,
    // Add approval status for document review
    val approval_status: String = "pending"
)

@Serializable
data class CognitoSearchResponse(
    val users: List<CognitoApiUser>,
    val total: Int,
    val query: String,
    val limit: Int
)

@SingleIn(SessionScope::class)
class CognitoUserService @Inject constructor() {
    
    companion object {
        private const val API_BASE_URL = "https://gnxe6db6wa.execute-api.us-east-1.amazonaws.com/prod"
        private const val USER_SEARCH_ENDPOINT = "$API_BASE_URL/api/v1/users/cognito/search"
        private const val USER_DISCOVERY_ENDPOINT = "$API_BASE_URL/api/v1/users/cognito/discover"
        private const val REQUEST_TIMEOUT = 30L
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    data class UserSearchResult(
        val users: List<CognitoUser>,
        val isSuccess: Boolean,
        val error: String? = null
    )
    
    data class UserDiscoveryResult(
        val user: CognitoUser?,
        val isSuccess: Boolean,
        val error: String? = null
    )
    
    /**
     * Search for users in Cognito User Pool
     */
    suspend fun searchUsers(query: String, limit: Int = 50): UserSearchResult {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "$USER_SEARCH_ENDPOINT?query=$encodedQuery&limit=$limit"
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                Timber.d("Searching users with query: $query, limit: $limit")
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Timber.e("User search failed: ${response.code} - $errorBody")
                        return@withContext UserSearchResult(
                            users = emptyList(),
                            isSuccess = false,
                            error = "Search failed: ${response.code}"
                        )
                    }
                    
                    val responseBody = response.body?.string() ?: ""
                    val searchResponse = json.decodeFromString<CognitoSearchResponse>(responseBody)
                    
                    val cognitoUsers = searchResponse.users.map { apiUser ->
                        convertApiUserToCognitoUser(apiUser)
                    }
                    
                    Timber.d("Found ${cognitoUsers.size} users")
                    
                    UserSearchResult(
                        users = cognitoUsers,
                        isSuccess = true
                    )
                }
            } catch (e: IOException) {
                Timber.e(e, "Network error during user search")
                UserSearchResult(
                    users = emptyList(),
                    isSuccess = false,
                    error = "Network error: ${e.message}"
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during user search")
                UserSearchResult(
                    users = emptyList(),
                    isSuccess = false,
                    error = "Search error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get all users by searching with a broad query
     */
    suspend fun getAllUsers(): UserSearchResult {
        return withContext(Dispatchers.IO) {
            try {
                // Use a broad search query to get all users
                // We'll try multiple common patterns to get a comprehensive list
                val searchQueries = listOf("", "a", "e", "i", "o", "u") // Common letters
                val allUsers = mutableListOf<CognitoUser>()
                val seenUsernames = mutableSetOf<String>()
                
                for (query in searchQueries) {
                    val result = searchUsers(query, 60) // Cognito API limit
                    if (result.isSuccess) {
                        result.users.forEach { user ->
                            if (!seenUsernames.contains(user.username)) {
                                seenUsernames.add(user.username)
                                allUsers.add(user)
                            }
                        }
                    }
                }
                
                Timber.d("Retrieved ${allUsers.size} total users")
                
                UserSearchResult(
                    users = allUsers,
                    isSuccess = true
                )
            } catch (e: Exception) {
                Timber.e(e, "Error getting all users")
                UserSearchResult(
                    users = emptyList(),
                    isSuccess = false,
                    error = "Error getting users: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Discover a specific user by Matrix ID
     */
    suspend fun discoverUser(matrixUserId: String): UserDiscoveryResult {
        return withContext(Dispatchers.IO) {
            try {
                val encodedMatrixId = URLEncoder.encode(matrixUserId, "UTF-8")
                val url = "$USER_DISCOVERY_ENDPOINT?matrix_user_id=$encodedMatrixId"
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                Timber.d("Discovering user with Matrix ID: $matrixUserId")
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Timber.e("User discovery failed: ${response.code} - $errorBody")
                        return@withContext UserDiscoveryResult(
                            user = null,
                            isSuccess = false,
                            error = "Discovery failed: ${response.code}"
                        )
                    }
                    
                    val responseBody = response.body?.string() ?: ""
                    val apiUser = json.decodeFromString<CognitoApiUser>(responseBody)
                    
                    val cognitoUser = convertApiUserToCognitoUser(apiUser)
                    
                    Timber.d("Discovered user: ${cognitoUser.username}")
                    
                    UserDiscoveryResult(
                        user = cognitoUser,
                        isSuccess = true
                    )
                }
            } catch (e: IOException) {
                Timber.e(e, "Network error during user discovery")
                UserDiscoveryResult(
                    user = null,
                    isSuccess = false,
                    error = "Network error: ${e.message}"
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during user discovery")
                UserDiscoveryResult(
                    user = null,
                    isSuccess = false,
                    error = "Discovery error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Placeholder for user activation/deactivation
     * Note: This would require additional AWS Lambda functions to handle user status changes
     */
    suspend fun toggleUserStatus(userId: String, activate: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // For now, this is a placeholder since we don't have a backend endpoint
                // In a real implementation, this would call an AWS Lambda function to:
                // 1. Update user status in Cognito (enable/disable user)
                // 2. Update Matrix user status if needed
                
                Timber.d("Toggle user status for $userId to ${if (activate) "active" else "inactive"}")
                
                // Simulate API call delay
                kotlinx.coroutines.delay(1000)
                
                // Return success for now
                true
            } catch (e: Exception) {
                Timber.e(e, "Error toggling user status")
                false
            }
        }
    }
    
    /**
     * Convert API user format to our internal CognitoUser format
     */
    private fun convertApiUserToCognitoUser(apiUser: CognitoApiUser): CognitoUser {
        return CognitoUser(
            id = apiUser.cognito_username,
            username = apiUser.matrix_username,
            email = apiUser.email,
            firstName = apiUser.given_name,
            lastName = apiUser.family_name,
            displayName = apiUser.display_name.ifEmpty { "${apiUser.given_name} ${apiUser.family_name}".trim() },
            city = apiUser.office_city ?: "Unknown",
            state = "Unknown", // Not provided by API
            country = "Unknown", // Not provided by API
            specialty = apiUser.specialty ?: "Unknown",
            professionalTitle = "Unknown", // Not provided by API
            phoneNumber = apiUser.phone_number ?: "Unknown",
            isActive = apiUser.is_active, // Use actual Cognito status
            createdAt = apiUser.created_at?.substring(0, 10) ?: "Unknown", // Extract date part
            lastSignIn = apiUser.updated_at?.substring(0, 10) ?: "Unknown", // Use updated_at as proxy
            matrixUserId = apiUser.matrix_user_id
        )
    }
} 