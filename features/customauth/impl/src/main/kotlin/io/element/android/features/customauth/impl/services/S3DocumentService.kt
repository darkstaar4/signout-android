package io.element.android.features.customauth.impl.services

import android.content.Context
import android.net.Uri
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

interface S3DocumentService {
    suspend fun uploadDocument(
        uri: Uri,
        username: String,
        documentType: String = "verification"
    ): Result<String>
}

@Serializable
data class PresignedUrlRequest(
    val filename: String,
    val contentType: String,
    val username: String
)

@Serializable
data class PresignedUrlResponse(
    val uploadUrl: String,
    val fileUrl: String
)

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DefaultS3DocumentService @Inject constructor(
    @ApplicationContext private val context: Context
) : S3DocumentService {
    
    companion object {
        private const val AWS_API_BASE_URL = "https://gnxe6db6wa.execute-api.us-east-1.amazonaws.com/prod"
    }
    
    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun uploadDocument(
        uri: Uri,
        username: String,
        documentType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Generate unique filename
            val fileExtension = getFileExtension(uri)
            val fileName = "${username}_${documentType}_${UUID.randomUUID()}.$fileExtension"
            val contentType = getContentType(uri)
            
            // Step 1: Get presigned URL from AWS API
            val presignedUrlRequest = PresignedUrlRequest(
                filename = fileName,
                contentType = contentType,
                username = username
            )
            
            val presignedResponse = getPresignedUrl(presignedUrlRequest)
            
            // Step 2: Upload file to S3 using presigned URL
            val tempFile = createTempFileFromUri(uri)
            
            try {
                val uploadRequest = Request.Builder()
                    .url(presignedResponse.uploadUrl)
                    .put(tempFile.asRequestBody(contentType.toMediaType()))
                    .build()
                
                val uploadResponse = httpClient.newCall(uploadRequest).execute()
                
                if (uploadResponse.isSuccessful) {
                    Timber.d("Document uploaded successfully: ${presignedResponse.fileUrl}")
                    Result.success(presignedResponse.fileUrl)
                } else {
                    Timber.e("Failed to upload document: ${uploadResponse.code} ${uploadResponse.message}")
                    Result.failure(Exception("Upload failed: ${uploadResponse.code} ${uploadResponse.message}"))
                }
                
            } finally {
                tempFile.delete()
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload document")
            Result.failure(e)
        }
    }
    
    private suspend fun getPresignedUrl(request: PresignedUrlRequest): PresignedUrlResponse {
        val requestBody = json.encodeToString(PresignedUrlRequest.serializer(), request)
        
        val httpRequest = Request.Builder()
            .url("$AWS_API_BASE_URL/presigned-url")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = httpClient.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to get presigned URL: ${response.code} ${response.message}")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        return json.decodeFromString(PresignedUrlResponse.serializer(), responseBody)
    }
    
    private fun getContentType(uri: Uri): String {
        val contentResolver = context.contentResolver
        return contentResolver.getType(uri) ?: "application/octet-stream"
    }
    
    private fun getFileExtension(uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> {
                // Fallback to extracting from URI
                val lastSegment = uri.lastPathSegment
                if (lastSegment?.contains(".") == true) {
                    lastSegment.substringAfterLast(".")
                } else {
                    "jpg" // Default fallback
                }
            }
        }
    }
    
    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream for URI: $uri")
        
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
} 