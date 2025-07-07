/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.verification

data class VerificationState(
    val username: String,
    val email: String,
    val verificationCode: String,
    val isLoading: Boolean,
    val isResending: Boolean,
    val errorMessage: String?,
    val canSubmit: Boolean,
    val isVerificationSuccessful: Boolean,
    val isMatrixAccountCreated: Boolean,
    val eventSink: (VerificationEvents) -> Unit
)

sealed interface VerificationEvents {
    data class SetVerificationCode(val code: String) : VerificationEvents
    data object Submit : VerificationEvents
    data object ResendCode : VerificationEvents
    data object ClearError : VerificationEvents
    data object Continue : VerificationEvents
} 