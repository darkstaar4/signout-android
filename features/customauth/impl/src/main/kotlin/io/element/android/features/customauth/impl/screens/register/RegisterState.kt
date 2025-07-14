/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.register

data class RegisterState(
    val username: String,
    val usernameValidationState: UsernameValidationState,
    val email: String,
    val confirmEmail: String,
    val firstName: String,
    val lastName: String,
    val professionalTitle: String,
    val specialty: String,
    val phoneNumber: String,
    val password: String,
    val confirmPassword: String,
    val npiNumber: String,
    val country: String,
    val officeAddress: String,
    val officeCity: String,
    val officeState: String,
    val officeZip: String,
    val verificationDocumentUri: android.net.Uri?,
    val isUploadingDocument: Boolean,
    val verificationDocumentUrl: String?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val fieldErrors: Map<String, String>,
    val canSubmit: Boolean,
    val isRegistrationSuccessful: Boolean,
    val registrationUsername: String?,
    val eventSink: (RegisterEvents) -> Unit,
)

sealed interface UsernameValidationState {
    data object Idle : UsernameValidationState

    data object Validating : UsernameValidationState

    data object Available : UsernameValidationState

    data class Unavailable(val message: String) : UsernameValidationState

    data class Error(val message: String) : UsernameValidationState
}

sealed interface RegisterEvents {
    data class SetUsername(val username: String) : RegisterEvents

    data object ValidateUsername : RegisterEvents

    data class SetEmail(val email: String) : RegisterEvents

    data class SetConfirmEmail(val confirmEmail: String) : RegisterEvents

    data class SetFirstName(val firstName: String) : RegisterEvents

    data class SetLastName(val lastName: String) : RegisterEvents

    data class SetProfessionalTitle(val professionalTitle: String) : RegisterEvents

    data class SetSpecialty(val specialty: String) : RegisterEvents

    data class SetPhoneNumber(val phoneNumber: String) : RegisterEvents

    data class SetPassword(val password: String) : RegisterEvents

    data class SetConfirmPassword(val confirmPassword: String) : RegisterEvents

    data class SetNpiNumber(val npiNumber: String) : RegisterEvents

    data class SetCountry(val country: String) : RegisterEvents

    data class SetOfficeAddress(val officeAddress: String) : RegisterEvents

    data class SetOfficeCity(val officeCity: String) : RegisterEvents

    data class SetOfficeState(val officeState: String) : RegisterEvents

    data class SetOfficeZip(val officeZip: String) : RegisterEvents

    data class SetVerificationDocument(val uri: android.net.Uri?) : RegisterEvents

    data object UploadVerificationDocument : RegisterEvents

    data object Submit : RegisterEvents

    data object ClearError : RegisterEvents
} 
