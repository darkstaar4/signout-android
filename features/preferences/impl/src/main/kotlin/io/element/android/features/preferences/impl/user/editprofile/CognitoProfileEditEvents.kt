/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

sealed interface CognitoProfileEditEvents {
    data class UpdatePhoneNumber(val phoneNumber: String) : CognitoProfileEditEvents
    data class UpdateOfficeAddress(val address: String) : CognitoProfileEditEvents
    data class UpdateOfficeCity(val city: String) : CognitoProfileEditEvents
    data class UpdateOfficeState(val state: String) : CognitoProfileEditEvents
    data class UpdateOfficeZip(val zip: String) : CognitoProfileEditEvents
    data class UpdateProfessionalTitle(val title: String) : CognitoProfileEditEvents
    data class UpdateSpecialty(val specialty: String) : CognitoProfileEditEvents
    data class UpdateNpiNumber(val npiNumber: String) : CognitoProfileEditEvents
    data class UpdateCountry(val country: String) : CognitoProfileEditEvents
    data object SaveProfile : CognitoProfileEditEvents
    data object ClearError : CognitoProfileEditEvents
} 