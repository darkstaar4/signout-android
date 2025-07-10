/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.usersearch.api.CognitoUserIntegrationService
import io.element.android.libraries.usersearch.api.UserMappingService
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class DefaultCognitoUserIntegrationService @Inject constructor(
    private val matrixClient: MatrixClient,
    private val userMappingService: UserMappingService,
) : CognitoUserIntegrationService {

    private val awsApiBaseUrl = "https://gnxe6db6wa.execute-api.us-east-1.amazonaws.com/prod"
    private val okHttpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun populateCurrentUserMapping() {
        try {
            Timber.d("CognitoUserIntegrationService: Populating current user mapping")
            val currentUserId = matrixClient.sessionId.value
            val currentUsername = currentUserId.substringAfter("@").substringBefore(":")
            
            // Check if we already have the current user mapping
            val existingMapping = userMappingService.getUserMapping(currentUsername)
            if (existingMapping != null) {
                Timber.d("CognitoUserIntegrationService: Current user mapping already exists: ${existingMapping.displayName}")
                return
            }

            try {
                // Try to get user data from AWS API
                val awsUser = fetchUserFromAwsApi(currentUsername)
                if (awsUser != null) {
                    userMappingService.addUserFromCognitoData(
                        matrixUserId = awsUser.matrix_user_id,
                        matrixUsername = awsUser.matrix_username,
                        cognitoUsername = awsUser.cognito_username ?: awsUser.matrix_username,
                        givenName = awsUser.given_name,
                        familyName = awsUser.family_name,
                        email = awsUser.email ?: "${awsUser.matrix_username}@signout.io",
                        specialty = awsUser.specialty,
                        officeCity = awsUser.office_city,
                        avatarUrl = awsUser.avatar_url
                    )
                    Timber.d("CognitoUserIntegrationService: Added current user from AWS: ${awsUser.display_name}")
                } else {
                    // Create fallback mapping with proper display name
                    val displayName = createDisplayNameFromUsername(currentUsername)
                    val fallbackMapping = UserMapping(
                        matrixUserId = currentUserId,
                        matrixUsername = currentUsername,
                        cognitoUsername = currentUsername,
                        displayName = displayName,
                        firstName = displayName,
                        lastName = "",
                        email = "",
                        specialty = null,
                        officeCity = null,
                        avatarUrl = null
                    )
                    userMappingService.addUserMapping(fallbackMapping)
                    Timber.d("CognitoUserIntegrationService: Added current user fallback mapping: ${fallbackMapping.displayName}")
                }
            } catch (e: Exception) {
                Timber.e(e, "CognitoUserIntegrationService: Error fetching current user data, creating fallback")
                // Create fallback mapping with proper display name
                val displayName = createDisplayNameFromUsername(currentUsername)
                val fallbackMapping = UserMapping(
                    matrixUserId = currentUserId,
                    matrixUsername = currentUsername,
                    cognitoUsername = currentUsername,
                    displayName = displayName,
                    firstName = displayName,
                    lastName = "",
                    email = "",
                    specialty = null,
                    officeCity = null,
                    avatarUrl = null
                )
                userMappingService.addUserMapping(fallbackMapping)
                Timber.d("CognitoUserIntegrationService: Added current user fallback mapping after error: ${fallbackMapping.displayName}")
            }
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error populating current user mapping")
        }
    }
    
    override suspend fun discoverUserMapping(matrixUserId: String, matrixDisplayName: String?) {
        try {
            val username = matrixUserId.substringAfter("@").substringBefore(":")
            Timber.d("CognitoUserIntegrationService: [DISCOVER] Starting discovery for user $username (matrixUserId: $matrixUserId)")
            Timber.d("CognitoUserIntegrationService: [DISCOVER] Matrix display name: '$matrixDisplayName'")
            
            // Check if we already have this user
            val existingMapping = userMappingService.getUserMapping(username)
            if (existingMapping != null) {
                Timber.d("CognitoUserIntegrationService: [DISCOVER] User mapping already exists for $username: ${existingMapping.displayName}")
                return
            }
            
            try {
                // Try to get user data from AWS API
                val awsUser = fetchUserFromAwsApi(username)
                if (awsUser != null) {
                    userMappingService.addUserFromCognitoData(
                        matrixUserId = awsUser.matrix_user_id,
                        matrixUsername = awsUser.matrix_username,
                        cognitoUsername = awsUser.cognito_username ?: awsUser.matrix_username,
                        givenName = awsUser.given_name,
                        familyName = awsUser.family_name,
                        email = awsUser.email ?: "${awsUser.matrix_username}@signout.io",
                        specialty = awsUser.specialty,
                        officeCity = awsUser.office_city,
                        avatarUrl = awsUser.avatar_url
                    )
                    Timber.d("CognitoUserIntegrationService: Discovered and added user mapping: ${awsUser.display_name}")
                } else {
                    // Create fallback mapping with proper display name
                    val displayName = createDisplayNameFromUsername(username)
                    val fallbackMapping = UserMapping(
                        matrixUserId = matrixUserId,
                        matrixUsername = username,
                        cognitoUsername = username,
                        displayName = displayName,
                        firstName = displayName,
                        lastName = "",
                        email = "",
                        specialty = null,
                        officeCity = null,
                        avatarUrl = null
                    )
                    userMappingService.addUserMapping(fallbackMapping)
                    Timber.d("CognitoUserIntegrationService: Created fallback mapping for $username: ${fallbackMapping.displayName}")
                }
            } catch (e: Exception) {
                Timber.w(e, "CognitoUserIntegrationService: AWS API error for user '$username', creating fallback")
                // Create fallback mapping with proper display name
                val displayName = createDisplayNameFromUsername(username)
                val fallbackMapping = UserMapping(
                    matrixUserId = matrixUserId,
                    matrixUsername = username,
                    cognitoUsername = username,
                    displayName = displayName,
                    firstName = displayName,
                    lastName = "",
                    email = "",
                    specialty = null,
                    officeCity = null,
                    avatarUrl = null
                )
                userMappingService.addUserMapping(fallbackMapping)
                Timber.d("CognitoUserIntegrationService: Created fallback mapping after error for $username: ${fallbackMapping.displayName}")
            }
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error discovering user mapping")
        }
    }
    
    override suspend fun extractMappingFromMatrixData(
        matrixUserId: String, 
        matrixDisplayName: String?, 
        matrixUsername: String
    ) {
        try {
            Timber.d("CognitoUserIntegrationService: Extracting mapping from Matrix data for $matrixUsername")
            
            // Check if we already have this user
            val existingMapping = userMappingService.getUserMapping(matrixUsername)
            if (existingMapping != null) {
                return
            }
            
            // Try to get from AWS backend first
            val awsUser = fetchUserFromAwsApi(matrixUsername)
            if (awsUser != null) {
                userMappingService.addUserFromCognitoData(
                    matrixUserId = awsUser.matrix_user_id,
                    matrixUsername = awsUser.matrix_username,
                    cognitoUsername = awsUser.cognito_username ?: awsUser.matrix_username,
                    givenName = awsUser.given_name,
                    familyName = awsUser.family_name,
                    email = awsUser.email ?: "${awsUser.matrix_username}@signout.io",
                    specialty = awsUser.specialty,
                    officeCity = awsUser.office_city,
                    avatarUrl = awsUser.avatar_url
                )
                return
            }
            
            // Fallback to Matrix display name extraction
            if (matrixDisplayName != null && matrixDisplayName.isNotBlank()) {
                val parts = matrixDisplayName.split(" ")
                val firstName = parts.firstOrNull()
                val lastName = parts.drop(1).joinToString(" ").ifEmpty { null }
                
                userMappingService.addUserFromCognitoData(
                    matrixUserId = matrixUserId,
                    matrixUsername = matrixUsername,
                    cognitoUsername = matrixUsername,
                    givenName = firstName ?: matrixUsername,
                    familyName = lastName ?: "User",
                    email = "$matrixUsername@signout.io",
                    specialty = null,
                    officeCity = null,
                    avatarUrl = null
                )
                
                Timber.d("CognitoUserIntegrationService: Extracted mapping from Matrix data: $matrixDisplayName")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error extracting mapping from Matrix data")
        }
    }
    
    private suspend fun fetchUserFromAwsApi(username: String): AwsUser? = withContext(Dispatchers.IO) {
        try {
            val url = "$awsApiBaseUrl/api/v1/users/cognito/discover?matrix_user_id=@$username:signout.io"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Timber.d("CognitoUserIntegrationService: AWS API response for user '$username': $responseBody")
                    return@withContext json.decodeFromString<AwsUser>(responseBody)
                }
            } else {
                Timber.w("CognitoUserIntegrationService: AWS API error for user '$username': ${response.code}")
            }
            
            null
        } catch (e: Exception) {
            Timber.w(e, "CognitoUserIntegrationService: Error fetching user '$username' from AWS API")
            null
        }
    }
    
    /**
     * Creates a display name from a Matrix username by capitalizing and handling special cases
     */
    private fun createDisplayNameFromUsername(username: String): String {
        return when (username.lowercase()) {
            "racexcars" -> "RaceX Cars"
            "nbaig" -> "Nabil Baig"
            else -> {
                // Convert camelCase or snake_case to proper title case
                username.replace("_", " ")
                    .replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
                    .split(" ")
                    .joinToString(" ") { word -> 
                        word.lowercase().replaceFirstChar { it.uppercase() }
                    }
            }
        }
    }
}

@Serializable
data class AwsUser(
    val matrix_user_id: String,
    val matrix_username: String,
    val cognito_username: String?,
    val given_name: String,
    val family_name: String,
    val display_name: String,
    val email: String?,
    val specialty: String?,
    val office_city: String?,
    val npi_number: String?,
    val phone_number: String?,
    val avatar_url: String?,
    val created_at: String? = null,
    val updated_at: String? = null
) 