/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

data class CognitoProfileEditState(
    // Read-only fields
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    
    // Editable fields
    val phoneNumber: String = "",
    val officeAddress: String = "",
    val officeCity: String = "",
    val officeState: String = "",
    val officeZip: String = "",
    val professionalTitle: String = "",
    val specialty: String = "",
    val npiNumber: String = "",
    val country: String = "",
    
    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    
    val eventSink: (CognitoProfileEditEvents) -> Unit = {},
) 