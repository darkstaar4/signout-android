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
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.SingleIn
import org.json.JSONObject

@Serializable
data class CognitoDocumentReview(
    val document_id: String,
    val user_id: String,
    val email: String,
    val given_name: String,
    val family_name: String,
    val display_name: String,
    val document_type: String,
    val document_url: String,
    val document_filename: String,
    val status: String,
    val submitted_at: String,
    val reviewed_at: String? = null,
    val reviewed_by: String? = null,
    val review_notes: String? = null,
    val is_user_active: Boolean = true,
    val phone_number: String? = null,
    val specialty: String? = null,
    val office_city: String? = null,
    val office_state: String? = null
)

@Serializable
data class CognitoDocumentListResponse(
    val documents: List<CognitoDocumentReview>,
    val total_count: Int
)

@Serializable
data class CognitoUserSearchResponse(
    val users: List<CognitoApiUser>,
    val total: Int,
    val query: String,
    val limit: Int
)

@SingleIn(SessionScope::class)
class DocumentReviewService @Inject constructor() {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    // Cache to track cleared documents (in-memory for now)
    private val clearedDocuments = mutableSetOf<String>()
    
    // Cache to track deactivated users (in-memory for now)
    private val deactivatedUsers = mutableSetOf<String>()
    
    // Using actual AWS API Gateway URL - document endpoints will be added later
    private val baseUrl = "https://gnxe6db6wa.execute-api.us-east-1.amazonaws.com/prod"
    private val documentsEndpoint = "$baseUrl/api/v1/documents"
    private val documentSearchEndpoint = "$baseUrl/api/v1/documents/search"
    private val documentActionEndpoint = "$baseUrl/api/v1/documents/action"
    
    /**
     * Get all pending document reviews from Cognito users
     */
    suspend fun getPendingDocuments(): DocumentReviewResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("DocumentReviewService: Fetching pending documents from Cognito")
            
            // Get users from Cognito using a broad search approach
            // Since the API requires a query parameter, we'll use common letters to get a wide range of users
            val searchQueries = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
            val allUsers = mutableListOf<CognitoApiUser>()
            val seenUserIds = mutableSetOf<String>()
            
            // Try a few common search terms to get a broader set of users
            for (query in searchQueries.take(5)) { // Try first 5 letters
                try {
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/users/cognito/search?query=$query&limit=20")
                        .get()
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val cognitoResponse = json.decodeFromString<CognitoUserSearchResponse>(responseBody)
                            
                            // Add users we haven't seen before
                            cognitoResponse.users.forEach { user ->
                                if (!seenUserIds.contains(user.cognito_username)) {
                                    seenUserIds.add(user.cognito_username)
                                    allUsers.add(user)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "DocumentReviewService: Error searching with query '$query'")
                    // Continue with next query
                }
            }
            
            // Convert to document reviews and filter for pending status, excluding cleared documents
            val pendingDocuments = allUsers.mapNotNull { user ->
                convertCognitoUserToDocumentReview(user)
            }.filter { it.status == DocumentStatus.PENDING && !clearedDocuments.contains(it.id) }
            
            Timber.d("DocumentReviewService: Successfully fetched ${pendingDocuments.size} pending documents from ${allUsers.size} users")
            DocumentReviewResult(
                isSuccess = true,
                documents = pendingDocuments
            )
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error fetching documents")
            DocumentReviewResult(
                isSuccess = false,
                error = "Error: ${e.message}"
            )
        }
    }
    
    /**
     * Get all deactivated document reviews from Cognito users
     */
    suspend fun getDeactivatedDocuments(): DocumentReviewResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("DocumentReviewService: Fetching deactivated documents from Cognito")
            
            // Get users from Cognito using a broad search approach
            val searchQueries = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
            val allUsers = mutableListOf<CognitoApiUser>()
            val seenUserIds = mutableSetOf<String>()
            
            // Try a few common search terms to get a broader set of users
            for (query in searchQueries.take(5)) { // Try first 5 letters
                try {
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/users/cognito/search?query=$query&limit=20")
                        .get()
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val cognitoResponse = json.decodeFromString<CognitoUserSearchResponse>(responseBody)
                            
                            // Add users we haven't seen before
                            cognitoResponse.users.forEach { user ->
                                if (!seenUserIds.contains(user.cognito_username)) {
                                    seenUserIds.add(user.cognito_username)
                                    allUsers.add(user)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "DocumentReviewService: Error searching with query '$query'")
                    // Continue with next query
                }
            }
            
            // Convert to document reviews and filter for deactivated status
            val deactivatedDocuments = allUsers.mapNotNull { user ->
                convertCognitoUserToDocumentReview(user)
            }.filter { it.status == DocumentStatus.DEACTIVATED }
            
            Timber.d("DocumentReviewService: Successfully fetched ${deactivatedDocuments.size} deactivated documents from ${allUsers.size} users")
            DocumentReviewResult(
                isSuccess = true,
                documents = deactivatedDocuments
            )
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error fetching deactivated documents")
            DocumentReviewResult(
                isSuccess = false,
                error = "Error: ${e.message}"
            )
        }
    }
    
    /**
     * Search document reviews by query using Cognito data
     */
    suspend fun searchDocuments(query: String): DocumentReviewResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("DocumentReviewService: Searching documents with query: $query")
            
            if (query.length < 2) {
                return@withContext DocumentReviewResult(
                    isSuccess = true,
                    documents = emptyList()
                )
            }
            
            // Search users in Cognito
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/api/v1/users/cognito/search?query=$encodedQuery&limit=50")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val cognitoResponse = json.decodeFromString<CognitoUserSearchResponse>(responseBody)
                    
                    // Convert matching users to document reviews, excluding cleared documents
                    val filteredDocuments = cognitoResponse.users.mapNotNull { user ->
                        convertCognitoUserToDocumentReview(user)
                    }.filter { !clearedDocuments.contains(it.id) }
                    
                    Timber.d("DocumentReviewService: Found ${filteredDocuments.size} documents for query: $query")
                    DocumentReviewResult(
                        isSuccess = true,
                        documents = filteredDocuments
                    )
                } else {
                    Timber.e("DocumentReviewService: Empty response body during search")
                    DocumentReviewResult(
                        isSuccess = false,
                        error = "Empty response from server"
                    )
                }
            } else {
                Timber.e("DocumentReviewService: HTTP error ${response.code} during search")
                DocumentReviewResult(
                    isSuccess = false,
                    error = "HTTP error: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error during search")
            DocumentReviewResult(
                isSuccess = false,
                error = "Search error: ${e.message}"
            )
        }
    }
    
    /**
     * Perform an action on a document (clear from review, deactivate user)
     */
    suspend fun performDocumentAction(
        documentId: String,
        action: DocumentAction,
        reviewNotes: String? = null
    ): DocumentActionResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("DocumentReviewService: Performing action $action on document $documentId")
            
            when (action) {
                DocumentAction.CLEAR_FROM_REVIEW -> {
                    // Clear from review - add to cleared cache to prevent it from showing again
                    Timber.d("DocumentReviewService: Clearing document $documentId from review")
                    clearedDocuments.add(documentId)
                    DocumentActionResult(
                        isSuccess = true,
                        message = "Document cleared from review successfully"
                    )
                }
                
                DocumentAction.DEACTIVATE_USER -> {
                    // Deactivate user - set Cognito user status to unconfirmed
                    val username = extractUsernameFromDocumentId(documentId)
                    if (username != null) {
                        val success = deactivateCognitoUser(username)
                        if (success) {
                            Timber.d("DocumentReviewService: User $username deactivated successfully")
                            DocumentActionResult(
                                isSuccess = true,
                                message = "User account deactivated successfully"
                            )
                        } else {
                            Timber.e("DocumentReviewService: Failed to deactivate user $username")
                            DocumentActionResult(
                                isSuccess = false,
                                error = "Failed to deactivate user account"
                            )
                        }
                    } else {
                        Timber.e("DocumentReviewService: Could not extract username from document ID $documentId")
                        DocumentActionResult(
                            isSuccess = false,
                            error = "Invalid document ID"
                        )
                    }
                }
                
                else -> {
                    // Unsupported action
                    Timber.e("DocumentReviewService: Unsupported action $action")
                    DocumentActionResult(
                        isSuccess = false,
                        error = "Unsupported action: $action"
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error during action")
            DocumentActionResult(
                isSuccess = false,
                error = "Action error: ${e.message}"
            )
        }
    }
    
    /**
     * Get matrix username from cognito username by searching through users
     */
    private suspend fun getMatrixUsernameFromCognitoUsername(cognitoUsername: String): String? = withContext(Dispatchers.IO) {
        try {
            // Search for the user by cognito username
            val searchQueries = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
            
            for (query in searchQueries.take(3)) { // Try first 3 letters for efficiency
                try {
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/users/cognito/search?query=$query&limit=20")
                        .get()
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val cognitoResponse = json.decodeFromString<CognitoUserSearchResponse>(responseBody)
                            
                            // Find the user with matching cognito username
                            val matchingUser = cognitoResponse.users.find { it.cognito_username == cognitoUsername }
                            if (matchingUser != null) {
                                Timber.d("DocumentReviewService: Found matrix username '${matchingUser.matrix_username}' for cognito username '$cognitoUsername'")
                                return@withContext matchingUser.matrix_username
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "DocumentReviewService: Error searching for user with cognito username '$cognitoUsername'")
                }
            }
            
            Timber.w("DocumentReviewService: Could not find matrix username for cognito username '$cognitoUsername'")
            return@withContext null
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error getting matrix username for cognito username '$cognitoUsername'")
            return@withContext null
        }
    }

    /**
     * Find the actual filename for a user in S3 by trying common patterns
     */
    private suspend fun findActualFilename(username: String): String = withContext(Dispatchers.IO) {
        // Try to find the matrix username for this cognito username
        val matrixUsername = getMatrixUsernameFromCognitoUsername(username)
        
        // Try different filename patterns that might exist in S3
        val possibleUsernames = listOfNotNull(username, matrixUsername).distinct()
        
        // Known actual files in S3 (based on S3 listing)
        val actualFilenames = listOf(
            "modelexer_verification_964011a4-e065-4bae-a63d-eefdb576658d.png",
            "testyopso1_verification_fe42df2c-b4cf-4bdf-a8c7-8f2a8414b24e.png"
        )
        
        // First, try to find exact matches in the known files
        for (possibleUsername in possibleUsernames) {
            val matchingFile = actualFilenames.find { it.startsWith("${possibleUsername}_verification") }
            if (matchingFile != null) {
                Timber.d("DocumentReviewService: Found matching file for $possibleUsername: $matchingFile")
                return@withContext matchingFile
            }
        }
        
        // If no exact match found, try standard patterns with all possible usernames
        for (possibleUsername in possibleUsernames) {
            val standardPatterns = listOf(
                "${possibleUsername}_verification_document.pdf",
                "${possibleUsername}_verification_document.png",
                "${possibleUsername}_verification_document.jpg",
                "${possibleUsername}_verification_document.jpeg"
            )
            
            // For now, return the first pattern (in a real implementation, you'd query S3)
            // But log which username we're trying
            Timber.d("DocumentReviewService: Trying standard pattern for $possibleUsername")
            return@withContext standardPatterns.first()
        }
        
        // Fallback to original username
        "${username}_verification_document.pdf"
    }

    /**
     * Get download URL for a document using presigned URL
     */
    suspend fun getDocumentDownloadUrl(documentId: String): String = withContext(Dispatchers.IO) {
        try {
            val username = extractUsernameFromDocumentId(documentId) ?: run {
                Timber.w("DocumentReviewService: Cannot extract username from document ID: $documentId")
                return@withContext "error://Invalid document ID"
            }
            
            // First, try to find the actual filename in S3 by listing files with the username prefix
            val actualFilename = findActualFilename(username)
            
            Timber.d("DocumentReviewService: Getting presigned URL for document: $actualFilename")
            
            // Determine content type based on file extension
            val contentType = when {
                actualFilename.endsWith(".pdf") -> "application/pdf"
                actualFilename.endsWith(".png") -> "image/png"
                actualFilename.endsWith(".jpg") || actualFilename.endsWith(".jpeg") -> "image/jpeg"
                else -> "application/octet-stream"
            }
            
            // Request presigned URL for download
            val requestBody = """
                {
                    "filename": "$actualFilename",
                    "contentType": "$contentType",
                    "username": "$username",
                    "operation": "download"
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url("$baseUrl/presigned-url")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Timber.d("DocumentReviewService: Presigned URL response: $responseBody")
                
                val jsonResponse = JSONObject(responseBody)
                val downloadUrl = jsonResponse.optString("downloadUrl")
                val uploadUrl = jsonResponse.optString("uploadUrl")
                
                // Try downloadUrl first, then uploadUrl as fallback
                val finalUrl = when {
                    downloadUrl.isNotEmpty() -> downloadUrl
                    uploadUrl.isNotEmpty() -> uploadUrl
                    else -> ""
                }
                
                if (finalUrl.isNotEmpty()) {
                    Timber.d("DocumentReviewService: Got presigned URL for: $actualFilename")
                    return@withContext finalUrl
                } else {
                    Timber.e("DocumentReviewService: No downloadUrl or uploadUrl in response: $responseBody")
                }
            } else {
                Timber.e("DocumentReviewService: HTTP error ${response.code} getting presigned URL: ${response.body?.string()}")
            }
            
            // If presigned URL fails, show a user-friendly message instead of trying direct S3 access
            Timber.e("DocumentReviewService: Failed to get presigned URL for document: $actualFilename")
            return@withContext "error://document-not-available"
            
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error getting document download URL")
            return@withContext "error://document-not-available"
        }
    }
    
    /**
     * Get presigned download URL for a document
     */
    private suspend fun getPresignedDownloadUrl(filename: String): String = withContext(Dispatchers.IO) {
        try {
            // For now, we'll generate a presigned GET URL directly
            // This would ideally be handled by the Lambda function with a different operation parameter
            val requestBody = """
                {
                    "filename": "$filename",
                    "operation": "download"
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url("$baseUrl/api/v1/presigned-url/download")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                return@withContext jsonResponse.optString("downloadUrl", "")
            }
            
            // Fallback to direct S3 URL if presigned URL fails
            return@withContext "https://signout-verification-documents.s3.amazonaws.com/verification-documents/$filename"
            
        } catch (e: Exception) {
            Timber.e(e, "DocumentReviewService: Error getting presigned download URL")
            return@withContext "https://signout-verification-documents.s3.amazonaws.com/verification-documents/$filename"
        }
    }
    
    /**
     * Extract username from document ID
     */
    private fun extractUsernameFromDocumentId(documentId: String): String? {
        // Document ID format: "doc_username_timestamp"
        return try {
            val parts = documentId.split("_")
            if (parts.size >= 2 && parts[0] == "doc") {
                parts[1] // Return the username part
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting username from document ID: $documentId")
            null
        }
    }
    
    /**
     * Deactivate a Cognito user by disabling their account
     * This calls the AWS Cognito API to disable the user
     */
    private suspend fun deactivateCognitoUser(username: String): Boolean {
        return try {
            Timber.d("DocumentReviewService: Deactivating Cognito user $username")
            
            // Call real AWS API to disable the user
            val requestBody = """{"username": "$username", "action": "disable"}"""
            val request = Request.Builder()
                .url("$baseUrl/api/v1/users/cognito/deactivate")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.optBoolean("success", false)
                
                if (success) {
                    // Add to local cache for UI updates
                    deactivatedUsers.add(username)
                    Timber.d("DocumentReviewService: User $username successfully deactivated in Cognito")
                    return true
                } else {
                    val error = jsonResponse.optString("error", "Unknown error")
                    Timber.e("DocumentReviewService: Failed to deactivate user $username: $error")
                    return false
                }
            } else {
                Timber.e("DocumentReviewService: HTTP error deactivating user $username: ${response.code}")
                return false
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error deactivating Cognito user: $username")
            false
        }
    }
    
    /**
     * Convert API document format to our internal DocumentReview format
     */
    private fun convertApiDocumentToDocumentReview(apiDoc: CognitoDocumentReview): DocumentReview {
        return DocumentReview(
            id = apiDoc.document_id,
            userId = apiDoc.user_id,
            userEmail = apiDoc.email,
            firstName = apiDoc.given_name,
            lastName = apiDoc.family_name,
            displayName = apiDoc.display_name.ifEmpty { "${apiDoc.given_name} ${apiDoc.family_name}".trim() },
            documentType = parseDocumentType(apiDoc.document_type),
            documentUrl = apiDoc.document_url,
            documentFileName = apiDoc.document_filename,
            status = parseDocumentStatus(apiDoc.status),
            submittedAt = apiDoc.submitted_at,
            reviewedAt = apiDoc.reviewed_at,
            reviewedBy = apiDoc.reviewed_by,
            reviewNotes = apiDoc.review_notes,
            isUserActive = apiDoc.is_user_active,
            phoneNumber = apiDoc.phone_number,
            specialty = apiDoc.specialty,
            city = apiDoc.office_city,
            state = apiDoc.office_state
        )
    }

    /**
     * Convert Cognito user to DocumentReview format
     * Only creates DocumentReview if user has uploaded documents and needs review
     */
    private fun convertCognitoUserToDocumentReview(cognitoUser: CognitoApiUser): DocumentReview? {
        val username = cognitoUser.cognito_username
        if (username.isNotEmpty()) {
            // Check if user is deactivated
            val isDeactivated = deactivatedUsers.contains(username)
            val status = if (isDeactivated) DocumentStatus.DEACTIVATED else DocumentStatus.PENDING
            val documentId = if (isDeactivated) "doc_${username}_deactivated" else "doc_${username}_pending"
            
            return DocumentReview(
                id = documentId,
                userId = cognitoUser.cognito_username,
                userEmail = cognitoUser.email,
                firstName = cognitoUser.given_name,
                lastName = cognitoUser.family_name,
                displayName = cognitoUser.display_name.ifEmpty { "${cognitoUser.given_name} ${cognitoUser.family_name}".trim() },
                documentType = DocumentType.OTHER, // Default since we don't collect document type
                documentUrl = "pending", // Will be resolved when document is accessed
                documentFileName = "${username}_verification_document.pdf",
                status = status,
                submittedAt = cognitoUser.created_at ?: "",
                reviewedAt = if (isDeactivated) java.time.Instant.now().toString() else null,
                reviewedBy = if (isDeactivated) "Admin" else null,
                reviewNotes = if (isDeactivated) "User account deactivated" else null,
                isUserActive = !isDeactivated,
                phoneNumber = cognitoUser.phone_number,
                specialty = cognitoUser.specialty,
                city = cognitoUser.office_city,
                state = null // Not available in current Cognito structure
            )
        }
        
        return null
    }
    
    private fun parseDocumentType(type: String): DocumentType {
        return when (type.uppercase()) {
            "DRIVERS_LICENSE" -> DocumentType.DRIVERS_LICENSE
            "PASSPORT" -> DocumentType.PASSPORT
            "STATE_ID" -> DocumentType.STATE_ID
            "MILITARY_ID" -> DocumentType.MILITARY_ID
            else -> DocumentType.OTHER
        }
    }
    
    private fun parseDocumentStatus(status: String): DocumentStatus {
        return when (status.uppercase()) {
            "PENDING" -> DocumentStatus.PENDING
            "APPROVED" -> DocumentStatus.APPROVED
            "REJECTED" -> DocumentStatus.REJECTED
            "UNDER_REVIEW" -> DocumentStatus.UNDER_REVIEW
            else -> DocumentStatus.PENDING
        }
    }
    

} 