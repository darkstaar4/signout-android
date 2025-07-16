/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Serializable
enum class DocumentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    UNDER_REVIEW,
    DEACTIVATED
}

@Serializable
enum class DocumentType {
    DRIVERS_LICENSE,
    PASSPORT,
    STATE_ID,
    MILITARY_ID,
    OTHER
}

@Serializable
@Parcelize
data class DocumentReview(
    val id: String,
    val userId: String,
    val userEmail: String,
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val documentType: DocumentType,
    val documentUrl: String,
    val documentFileName: String,
    val status: DocumentStatus,
    val submittedAt: String,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null,
    val isUserActive: Boolean = true,
    val phoneNumber: String? = null,
    val specialty: String? = null,
    val city: String? = null,
    val state: String? = null
) : Parcelable

@Serializable
data class DocumentReviewResult(
    val isSuccess: Boolean,
    val documents: List<DocumentReview> = emptyList(),
    val error: String? = null
)

@Serializable
data class DocumentActionRequest(
    val documentId: String,
    val action: DocumentAction,
    val reviewNotes: String? = null
)

@Serializable
enum class DocumentAction {
    APPROVE,
    REJECT,
    CLEAR_FROM_REVIEW,
    DEACTIVATE_USER
}

@Serializable
data class DocumentActionResult(
    val isSuccess: Boolean,
    val message: String? = null,
    val error: String? = null
) 