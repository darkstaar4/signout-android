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
import io.element.android.libraries.usersearch.api.UserDirectoryService
import io.element.android.libraries.usersearch.api.UserDirectoryEntry
import io.element.android.libraries.usersearch.api.CognitoUserData
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.coroutines.flow.firstOrNull
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
    private val userDirectoryService: UserDirectoryService,
) : CognitoUserIntegrationService {

    private val awsApiBaseUrl = "https://gnxe6db6wa.execute-api.us-east-1.amazonaws.com/prod"
    private val okHttpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun populateCurrentUserMapping() {
        try {
            Timber.d("CognitoUserIntegrationService: Populating current user mapping")
            
            // Get real Cognito data from current authenticated user
            val currentUserCognitoData = extractCurrentUserCognitoData()
            
            if (currentUserCognitoData != null) {
                // Add to local mapping service
                userMappingService.addUserFromCognitoData(
                    matrixUserId = currentUserCognitoData.matrixUserId,
                    matrixUsername = currentUserCognitoData.matrixUsername,
                    cognitoUsername = currentUserCognitoData.cognitoUsername,
                    givenName = currentUserCognitoData.givenName,
                    familyName = currentUserCognitoData.familyName,
                    email = currentUserCognitoData.email,
                    specialty = currentUserCognitoData.specialty,
                    officeCity = currentUserCognitoData.officeCity,
                    avatarUrl = null
                )
                
                // Add to backend directory
                val directoryEntry = UserDirectoryEntry(
                    matrixUserId = currentUserCognitoData.matrixUserId,
                    cognitoUsername = currentUserCognitoData.cognitoUsername,
                    displayName = "${currentUserCognitoData.givenName} ${currentUserCognitoData.familyName}",
                    givenName = currentUserCognitoData.givenName,
                    familyName = currentUserCognitoData.familyName,
                    email = currentUserCognitoData.email,
                    specialty = currentUserCognitoData.specialty,
                    officeCity = currentUserCognitoData.officeCity,
                    npiNumber = currentUserCognitoData.npiNumber,
                    phoneNumber = currentUserCognitoData.phoneNumber,
                    avatarUrl = null
                )
                
                userDirectoryService.addUser(directoryEntry)
                
                Timber.d("CognitoUserIntegrationService: Added current user to directory: ${currentUserCognitoData.givenName} ${currentUserCognitoData.familyName}")
            }
            
            // Populate other users from AWS backend
            populateUsersFromAwsBackend()
            
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error populating current user mapping")
        }
    }
    
    private suspend fun populateUsersFromAwsBackend() = withContext(Dispatchers.IO) {
        try {
            Timber.d("CognitoUserIntegrationService: Starting AWS backend population")
            
            // Check if we already have cached data - if so, skip AWS calls for faster startup
            val cachedMappingsCount = userMappingService.getCachedMappingsCount()
            if (cachedMappingsCount > 0) {
                Timber.d("CognitoUserIntegrationService: Found $cachedMappingsCount cached mappings, skipping AWS backend population")
                return@withContext
            }
            
            Timber.d("CognitoUserIntegrationService: No cached data found, populating from AWS backend API")
            
            // Get users from AWS backend API
            val awsUsers = fetchUsersFromAwsApi()
            
            for (awsUser in awsUsers) {
                try {
                    // Add to user mapping service
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
                    
                    // Add to directory service
                    val directoryEntry = UserDirectoryEntry(
                        matrixUserId = awsUser.matrix_user_id,
                        cognitoUsername = awsUser.cognito_username ?: awsUser.matrix_username,
                        displayName = awsUser.display_name,
                        givenName = awsUser.given_name,
                        familyName = awsUser.family_name,
                        email = awsUser.email ?: "${awsUser.matrix_username}@signout.io",
                        specialty = awsUser.specialty,
                        officeCity = awsUser.office_city,
                        npiNumber = awsUser.npi_number,
                        phoneNumber = awsUser.phone_number,
                        avatarUrl = awsUser.avatar_url
                    )
                    
                    userDirectoryService.addUser(directoryEntry)
                    
                    Timber.d("CognitoUserIntegrationService: Added user from AWS backend: ${awsUser.display_name} (${awsUser.matrix_username})")
                    
                } catch (e: Exception) {
                    Timber.w(e, "CognitoUserIntegrationService: Error adding user ${awsUser.matrix_username}")
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error populating users from AWS backend")
        }
    }
    
    private suspend fun fetchUsersFromAwsApi(): List<AwsUser> = withContext(Dispatchers.IO) {
        try {
            // Search for all users by querying common letters
            val queries = listOf("r", "n", "a", "e", "i", "o", "u")
            val allUsers = mutableSetOf<AwsUser>()
            
            for (query in queries) {
                try {
                    val url = "$awsApiBaseUrl/api/v1/users/cognito/search?query=$query&limit=50"
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            Timber.d("CognitoUserIntegrationService: AWS API response for query '$query': $responseBody")
                            
                            val searchResponse = json.decodeFromString<AwsSearchResponse>(responseBody)
                            allUsers.addAll(searchResponse.users)
                            
                            Timber.d("CognitoUserIntegrationService: Found ${searchResponse.users.size} users for query '$query'")
                        }
                    } else {
                        Timber.w("CognitoUserIntegrationService: AWS API error for query '$query': ${response.code}")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "CognitoUserIntegrationService: Error fetching users for query '$query'")
                }
            }
            
            // Also try to get specific known users
            val knownUsers = listOf("racexcars", "nbaig")
            for (username in knownUsers) {
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
                            Timber.d("CognitoUserIntegrationService: AWS API discover response for '$username': $responseBody")
                            
                            val user = json.decodeFromString<AwsUser>(responseBody)
                            allUsers.add(user)
                            
                            Timber.d("CognitoUserIntegrationService: Discovered user: ${user.display_name}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "CognitoUserIntegrationService: Error discovering user '$username'")
                }
            }
            
            Timber.d("CognitoUserIntegrationService: Total unique users found: ${allUsers.size}")
            allUsers.toList()
            
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error fetching users from AWS API")
            emptyList()
        }
    }

    private suspend fun extractCurrentUserCognitoData(): CognitoUserData? {
        return try {
            val matrixUser = matrixClient.userProfile.firstOrNull()
            if (matrixUser != null) {
                
                // Extract username from Matrix user ID (e.g., @nbaig:signout.io -> nbaig)
                val matrixUsername = matrixUser.userId.value.substringAfter("@").substringBefore(":")
                
                // Try to get real Cognito data from AWS backend first
                val awsUser = fetchUserFromAwsApi(matrixUsername)
                if (awsUser != null) {
                    return CognitoUserData(
                        matrixUserId = awsUser.matrix_user_id,
                        matrixUsername = awsUser.matrix_username,
                        cognitoUsername = awsUser.cognito_username ?: awsUser.matrix_username,
                        givenName = awsUser.given_name,
                        familyName = awsUser.family_name,
                        email = awsUser.email ?: "$matrixUsername@signout.io",
                        specialty = awsUser.specialty,
                        officeCity = awsUser.office_city,
                        npiNumber = awsUser.npi_number,
                        phoneNumber = awsUser.phone_number
                    )
                }
                
                // Fallback to Matrix profile data
                val displayName = matrixUser.displayName ?: matrixUsername
                val givenName = displayName.split(" ").firstOrNull() ?: matrixUsername
                val familyName = displayName.split(" ").drop(1).joinToString(" ").ifEmpty { "do" }
                
                // Get additional Cognito attributes (these would normally come from Cognito)
                val specialty = when (matrixUsername) {
                    "nbaig" -> "Addiction Medicine"
                    else -> null
                }
                val officeCity = when (matrixUsername) {
                    "nbaig" -> "Fresno"
                    else -> null
                }
                
                val cognitoData = CognitoUserData(
                    matrixUserId = matrixUser.userId.value,
                    matrixUsername = matrixUsername,
                    cognitoUsername = matrixUsername,
                    givenName = givenName,
                    familyName = familyName,
                    email = "$matrixUsername@signout.io",
                    specialty = specialty,
                    officeCity = officeCity,
                    npiNumber = null,
                    phoneNumber = null
                )
                
                Timber.d("CognitoUserIntegrationService: Extracted Cognito data for user: $matrixUsername")
                Timber.d("CognitoUserIntegrationService: Given name: $givenName, Family name: $familyName")
                Timber.d("CognitoUserIntegrationService: Specialty: $specialty, Office city: $officeCity")
                
                return cognitoData
            }
            
            null
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error extracting current user Cognito data")
            null
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
    
    override suspend fun discoverUserMapping(matrixUserId: String, matrixDisplayName: String?) {
        try {
            val username = matrixUserId.substringAfter("@").substringBefore(":")
            Timber.d("CognitoUserIntegrationService: Discovering user mapping for $username")
            
            // Check if we already have this user
            val existingMapping = userMappingService.getUserMapping(username)
            if (existingMapping != null) {
                Timber.d("CognitoUserIntegrationService: User mapping already exists for $username")
                return
            }
            
            // Try to get from AWS backend
            val awsUser = fetchUserFromAwsApi(username)
            if (awsUser != null) {
                // Add to user mapping service
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
            }
            
        } catch (e: Exception) {
            Timber.e(e, "CognitoUserIntegrationService: Error discovering user mapping for $matrixUserId")
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


}

@Serializable
data class AwsSearchResponse(
    val users: List<AwsUser>,
    val total: Int,
    val query: String,
    val limit: Int
)

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
    val created_at: String?,
    val updated_at: String?
) 