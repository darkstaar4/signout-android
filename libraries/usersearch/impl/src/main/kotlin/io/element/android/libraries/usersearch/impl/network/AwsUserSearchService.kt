package io.element.android.libraries.usersearch.impl.network

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.usersearch.api.UserDirectoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

interface AwsUserSearchService {
    suspend fun searchUsers(query: String): List<UserDirectoryEntry>
}

@ContributesBinding(SessionScope::class)
class DefaultAwsUserSearchService @Inject constructor(
    private val httpClient: OkHttpClient
) : AwsUserSearchService {
    
    companion object {
        private const val AWS_API_BASE_URL = "https://gnxe6db6wa.execute-api.us-east-1.amazonaws.com/prod"
        private const val USER_SEARCH_ENDPOINT = "/api/v1/users/cognito/search"
    }
    
    override suspend fun searchUsers(query: String): List<UserDirectoryEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            Timber.d("AwsUserSearchService: Empty query, returning empty results")
            return@withContext emptyList()
        }
        
        try {
            Timber.d("AwsUserSearchService: Searching for users with query: '$query'")
            
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("gnxe6db6wa.execute-api.us-east-1.amazonaws.com")
                .addPathSegment("prod")
                .addPathSegment("api")
                .addPathSegment("v1")
                .addPathSegment("users")
                .addPathSegment("cognito")
                .addPathSegment("search")
                .addQueryParameter("query", query)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Timber.w("AwsUserSearchService: API call failed with status: ${response.code}")
                return@withContext emptyList()
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Timber.w("AwsUserSearchService: Empty response body")
                return@withContext emptyList()
            }
            
            Timber.d("AwsUserSearchService: API response: $responseBody")
            
            val results = parseSearchResponse(responseBody)
            Timber.d("AwsUserSearchService: Parsed ${results.size} users from API response")
            
            return@withContext results
            
        } catch (e: Exception) {
            Timber.e(e, "AwsUserSearchService: Error searching users")
            return@withContext emptyList()
        }
    }
    
    private fun parseSearchResponse(responseBody: String): List<UserDirectoryEntry> {
        try {
            val jsonResponse = JSONObject(responseBody)
            val users = jsonResponse.optJSONArray("users") ?: return emptyList()
            
            val results = mutableListOf<UserDirectoryEntry>()
            
            for (i in 0 until users.length()) {
                val userObj = users.getJSONObject(i)
                
                val matrixUserId = userObj.optString("matrix_user_id")
                val cognitoUsername = userObj.optString("cognito_username")
                val givenName = userObj.optString("given_name")
                val familyName = userObj.optString("family_name")
                val email = userObj.optString("email")
                val specialty = userObj.optString("specialty")
                val officeCity = userObj.optString("office_city")
                
                if (matrixUserId.isNotEmpty() && cognitoUsername.isNotEmpty()) {
                    val displayName = if (givenName.isNotEmpty() && familyName.isNotEmpty()) {
                        "$givenName $familyName"
                    } else {
                        cognitoUsername
                    }
                    
                    val userEntry = UserDirectoryEntry(
                        matrixUserId = matrixUserId,
                        cognitoUsername = cognitoUsername,
                        givenName = givenName,
                        familyName = familyName,
                        displayName = displayName,
                        email = email,
                        specialty = specialty,
                        officeCity = officeCity,
                        avatarUrl = null
                    )
                    
                    results.add(userEntry)
                    Timber.d("AwsUserSearchService: Parsed user: $displayName ($matrixUserId)")
                }
            }
            
            return results
            
        } catch (e: Exception) {
            Timber.e(e, "AwsUserSearchService: Error parsing search response")
            return emptyList()
        }
    }
} 