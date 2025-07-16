package io.element.android.features.admin.api

import kotlinx.coroutines.flow.Flow

data class AdminUser(
    val id: String,
    val email: String,
    val name: String,
    val familyName: String,
    val givenName: String,
    val phoneNumber: String?,
    val specialty: String?,
    val professionalTitle: String?,
    val country: String?,
    val verificationDocumentUrl: String?,
    val matrixUsername: String?,
    val userStatus: String,
    val createdDate: String,
    val lastModifiedDate: String
)

interface AdminService {
    suspend fun getAllUsers(): Result<List<AdminUser>>
    suspend fun downloadDocument(documentUrl: String): Result<ByteArray>
} 