/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.features.customauth.impl.auth.CognitoAuthService
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch
import javax.inject.Inject

class CognitoProfileEditPresenter @Inject constructor(
    private val cognitoAuthService: CognitoAuthService,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<CognitoProfileEditState> {

    @Composable
    override fun present(): CognitoProfileEditState {
        android.util.Log.d("elementx", "CognitoProfileEditPresenter: present() called")
        val coroutineScope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        
        // Form fields
        var phoneNumber by remember { mutableStateOf("") }
        var officeAddress by remember { mutableStateOf("") }
        var officeCity by remember { mutableStateOf("") }
        var officeState by remember { mutableStateOf("") }
        var officeZip by remember { mutableStateOf("") }
        var professionalTitle by remember { mutableStateOf("") }
        var specialty by remember { mutableStateOf("") }
        var npiNumber by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }
        
        // Read-only fields
        var givenName by remember { mutableStateOf("") }
        var familyName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }

        // Load initial data
        LaunchedEffect(Unit) {
            isLoading = true
            try {
                val userDetails = cognitoAuthService.getCurrentUserDetails()
                userDetails?.let { details ->
                    val attrs = details.attributes?.attributes ?: emptyMap()
                    phoneNumber = attrs["phone_number"] ?: ""
                    officeAddress = attrs["custom:office_address"] ?: ""
                    officeCity = attrs["custom:office_city"] ?: ""
                    officeState = attrs["custom:office_state"] ?: ""
                    officeZip = attrs["custom:office_zip"] ?: ""
                    professionalTitle = attrs["custom:professional_title"] ?: ""
                    specialty = attrs["custom:specialty"] ?: ""
                    npiNumber = attrs["custom:npi_number"] ?: ""
                    country = attrs["custom:country"] ?: ""
                    
                    givenName = attrs["given_name"] ?: ""
                    familyName = attrs["family_name"] ?: ""
                    email = attrs["email"] ?: ""
                }
            } catch (e: Exception) {
                error = "Failed to load profile: ${e.message}"
            } finally {
                isLoading = false
            }
        }

        fun handleEvent(event: CognitoProfileEditEvents) {
            when (event) {
                is CognitoProfileEditEvents.UpdatePhoneNumber -> {
                    phoneNumber = event.phoneNumber
                }
                is CognitoProfileEditEvents.UpdateOfficeAddress -> {
                    officeAddress = event.address
                }
                is CognitoProfileEditEvents.UpdateOfficeCity -> {
                    officeCity = event.city
                }
                is CognitoProfileEditEvents.UpdateOfficeState -> {
                    officeState = event.state
                }
                is CognitoProfileEditEvents.UpdateOfficeZip -> {
                    officeZip = event.zip
                }
                is CognitoProfileEditEvents.UpdateProfessionalTitle -> {
                    professionalTitle = event.title
                }
                is CognitoProfileEditEvents.UpdateSpecialty -> {
                    specialty = event.specialty
                }
                is CognitoProfileEditEvents.UpdateNpiNumber -> {
                    npiNumber = event.npiNumber
                }
                is CognitoProfileEditEvents.UpdateCountry -> {
                    country = event.country
                }
                is CognitoProfileEditEvents.SaveProfile -> {
                    coroutineScope.launch {
                        isLoading = true
                        error = null
                        try {
                            val attributesToUpdate = mapOf(
                                "phone_number" to phoneNumber,
                                "custom:office_address" to officeAddress,
                                "custom:office_city" to officeCity,
                                "custom:office_state" to officeState,
                                "custom:office_zip" to officeZip,
                                "custom:professional_title" to professionalTitle,
                                "custom:specialty" to specialty,
                                "custom:npi_number" to npiNumber,
                                "custom:country" to country
                            )
                            
                            val success = cognitoAuthService.updateUserAttributes(attributesToUpdate)
                            if (success) {
                                snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_success))
                            } else {
                                error = "Failed to save profile: Update operation failed"
                            }
                        } catch (e: Exception) {
                            error = "Failed to save profile: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
                is CognitoProfileEditEvents.ClearError -> {
                    error = null
                }
            }
        }

        return CognitoProfileEditState(
            // Read-only fields
            firstName = givenName,
            lastName = familyName,
            email = email,
            // Editable fields
            phoneNumber = phoneNumber,
            officeAddress = officeAddress,
            officeCity = officeCity,
            officeState = officeState,
            officeZip = officeZip,
            professionalTitle = professionalTitle,
            specialty = specialty,
            npiNumber = npiNumber,
            country = country,
            // UI state
            isLoading = isLoading,
            error = error,
            eventSink = ::handleEvent
        )
    }
} 