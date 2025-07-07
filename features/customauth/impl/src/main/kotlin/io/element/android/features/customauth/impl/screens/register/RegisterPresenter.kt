/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.features.customauth.impl.auth.CognitoAuthService
import io.element.android.features.customauth.impl.auth.CognitoAuthService.UserData
import io.element.android.features.customauth.impl.auth.MatrixIntegrationService
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegisterPresenter
    @Inject
    constructor(
        private val cognitoAuthService: CognitoAuthService,
        private val matrixIntegrationService: MatrixIntegrationService,
    ) : Presenter<RegisterState> {
        
        companion object {
            // More permissive email regex that allows most valid email formats
            private fun isValidEmail(email: String): Boolean {
                // Basic email pattern that's more permissive than Android's default
                val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                return emailRegex.matches(email)
            }
        }
        
        @Composable
        override fun present(): RegisterState {
            val coroutineScope = rememberCoroutineScope()

            var username by remember { mutableStateOf("") }
            var usernameValidationState by remember { mutableStateOf<UsernameValidationState>(UsernameValidationState.Idle) }
            var usernameValidationJob by remember { mutableStateOf<Job?>(null) }
            var email by remember { mutableStateOf("") }
            var confirmEmail by remember { mutableStateOf("") }
            var firstName by remember { mutableStateOf("") }
            var lastName by remember { mutableStateOf("") }
            var professionalTitle by remember { mutableStateOf("") }
            var specialty by remember { mutableStateOf("") }
            var phoneNumber by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var npiNumber by remember { mutableStateOf("") }
            var country by remember { mutableStateOf("") }
            var officeAddress by remember { mutableStateOf("") }
            var officeCity by remember { mutableStateOf("") }
            var officeState by remember { mutableStateOf("") }
            var officeZip by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var isRegistrationSuccessful by remember { mutableStateOf(false) }
            var registrationUsername by remember { mutableStateOf<String?>(null) }

            val canSubmit =
                remember(
                    username, email, confirmEmail, firstName, lastName, professionalTitle,
                    specialty, phoneNumber, password, confirmPassword, country,
                    officeAddress, officeCity, officeZip, isLoading, usernameValidationState,
                ) {
                    username.isNotBlank() &&
                        usernameValidationState is UsernameValidationState.Available &&
                        email.isNotBlank() &&
                        confirmEmail.isNotBlank() &&
                        firstName.isNotBlank() &&
                        lastName.isNotBlank() &&
                        professionalTitle.isNotBlank() &&
                        specialty.isNotBlank() &&
                        phoneNumber.isNotBlank() &&
                        password.isNotBlank() &&
                        confirmPassword.isNotBlank() &&
                        country.isNotBlank() &&
                        officeAddress.isNotBlank() &&
                        officeCity.isNotBlank() &&
                        officeZip.isNotBlank() &&
                        (country != "USA" || officeState.isNotBlank()) &&
                        !isLoading
                }

            fun validateUsernameWithDelay() {
                // Cancel previous validation job
                usernameValidationJob?.cancel()

                if (username.isBlank()) {
                    usernameValidationState = UsernameValidationState.Idle
                    return
                }

                usernameValidationJob =
                    coroutineScope.launch {
                        // Debounce for 500ms to avoid excessive API calls
                        delay(500)

                        usernameValidationState = UsernameValidationState.Validating

                        try {
                            val result = cognitoAuthService.validateUsername(username)
                            usernameValidationState =
                                if (result.isAvailable) {
                                    UsernameValidationState.Available
                                } else {
                                    UsernameValidationState.Unavailable(result.error ?: "Username is not available")
                                }
                        } catch (e: Exception) {
                            usernameValidationState =
                                UsernameValidationState.Error(
                                    e.message ?: "Failed to validate username",
                                )
                        }
                    }
            }

            fun handleEvents(event: RegisterEvents) {
                when (event) {
                    is RegisterEvents.SetUsername -> {
                        username = event.username
                        // Reset validation state and start new validation
                        usernameValidationState = UsernameValidationState.Idle
                        validateUsernameWithDelay()
                    }
                    is RegisterEvents.ValidateUsername -> {
                        validateUsernameWithDelay()
                    }
                    is RegisterEvents.SetEmail -> email = event.email
                    is RegisterEvents.SetConfirmEmail -> confirmEmail = event.confirmEmail
                    is RegisterEvents.SetFirstName -> firstName = event.firstName
                    is RegisterEvents.SetLastName -> lastName = event.lastName
                    is RegisterEvents.SetProfessionalTitle -> professionalTitle = event.professionalTitle
                    is RegisterEvents.SetSpecialty -> specialty = event.specialty
                    is RegisterEvents.SetPhoneNumber -> phoneNumber = event.phoneNumber
                    is RegisterEvents.SetPassword -> password = event.password
                    is RegisterEvents.SetConfirmPassword -> confirmPassword = event.confirmPassword
                    is RegisterEvents.SetNpiNumber -> npiNumber = event.npiNumber
                    is RegisterEvents.SetCountry -> country = event.country
                    is RegisterEvents.SetOfficeAddress -> officeAddress = event.officeAddress
                    is RegisterEvents.SetOfficeCity -> officeCity = event.officeCity
                    is RegisterEvents.SetOfficeState -> officeState = event.officeState
                    is RegisterEvents.SetOfficeZip -> officeZip = event.officeZip
                    is RegisterEvents.ClearError -> errorMessage = null
                    is RegisterEvents.Submit -> {
                        coroutineScope.launch {
                            // Clear previous errors
                            fieldErrors = emptyMap()
                            errorMessage = null
                            
                            val errors = mutableMapOf<String, String>()
                            
                            // Field-specific validation
                            if (username.isBlank()) {
                                errors["username"] = "Username is required"
                            } else if (usernameValidationState !is UsernameValidationState.Available) {
                                errors["username"] = "Please choose a valid username"
                            }
                            
                            if (email.isBlank()) {
                                errors["email"] = "Email is required"
                            } else if (!isValidEmail(email)) {
                                errors["email"] = "Please enter a valid email address"
                            }
                            
                            if (confirmEmail.isBlank()) {
                                errors["confirmEmail"] = "Please confirm your email"
                            } else if (email != confirmEmail) {
                                errors["confirmEmail"] = "Emails do not match"
                            }
                            
                            if (firstName.isBlank()) {
                                errors["firstName"] = "First name is required"
                            }
                            
                            if (lastName.isBlank()) {
                                errors["lastName"] = "Last name is required"
                            }
                            
                            if (professionalTitle.isBlank()) {
                                errors["professionalTitle"] = "Professional title is required"
                            }
                            
                            if (specialty.isBlank()) {
                                errors["specialty"] = "Specialty is required"
                            }
                            
                            if (phoneNumber.isBlank()) {
                                errors["phoneNumber"] = "Phone number is required"
                            } else {
                                // Extract just the digits for validation
                                val phoneDigits = phoneNumber.replace(Regex("[^\\d]"), "")
                                // Check if it starts with a valid country code and has enough digits
                                when {
                                    phoneDigits.startsWith("1") && phoneDigits.length != 11 -> {
                                        // US/Canada numbers should be exactly 11 digits (1 + 10)
                                        errors["phoneNumber"] = "US/Canada phone numbers must be 10 digits"
                                    }
                                    phoneDigits.startsWith("52") && phoneDigits.length != 12 -> {
                                        // Mexico numbers should be exactly 12 digits (52 + 10)
                                        errors["phoneNumber"] = "Mexico phone numbers must be 10 digits"
                                    }
                                    phoneDigits.startsWith("44") && phoneDigits.length != 13 -> {
                                        // UK numbers should be exactly 13 digits (44 + 11)
                                        errors["phoneNumber"] = "UK phone numbers must be 11 digits"
                                    }
                                    phoneDigits.length < 10 -> {
                                        // General minimum length check
                                        errors["phoneNumber"] = "Please enter a valid phone number"
                                    }
                                }
                            }
                            
                            if (password.isBlank()) {
                                errors["password"] = "Password is required"
                            } else if (password.length < 8) {
                                errors["password"] = "Password must be at least 8 characters long"
                            } else if (!password.matches(Regex(".*[A-Z].*"))) {
                                errors["password"] = "Password must contain at least one uppercase letter"
                            } else if (!password.matches(Regex(".*[a-z].*"))) {
                                errors["password"] = "Password must contain at least one lowercase letter"
                            } else if (!password.matches(Regex(".*\\d.*"))) {
                                errors["password"] = "Password must contain at least one number"
                            }
                            
                            if (confirmPassword.isBlank()) {
                                errors["confirmPassword"] = "Please confirm your password"
                            } else if (password != confirmPassword) {
                                errors["confirmPassword"] = "Passwords do not match"
                            }
                            
                            if (country.isBlank()) {
                                errors["country"] = "Country is required"
                            }
                            
                            if (officeAddress.isBlank()) {
                                errors["officeAddress"] = "Office address is required"
                            }
                            
                            if (officeCity.isBlank()) {
                                errors["officeCity"] = "City is required"
                            }
                            
                            if (officeZip.isBlank()) {
                                errors["officeZip"] = "Zip code is required"
                            }
                            
                            if (country == "USA" && officeState.isBlank()) {
                                errors["officeState"] = "State is required when USA is selected"
                            }
                            
                            if (country == "Canada" && officeState.isBlank()) {
                                errors["officeState"] = "Province is required when Canada is selected"
                            }
                            
                            // If there are validation errors, show them
                            if (errors.isNotEmpty()) {
                                fieldErrors = errors
                                return@launch
                            }

                            isLoading = true

                            try {
                                isLoading = true
                                
                                // Create UserData without Matrix credentials (they'll be created after verification)
                                val userData = UserData(
                                    username = username,
                                    email = email,
                                    firstName = firstName,
                                    lastName = lastName,
                                    phoneNumber = phoneNumber,
                                    professionalTitle = professionalTitle,
                                    specialty = specialty,
                                    npiNumber = npiNumber.ifEmpty { null },
                                    country = country,
                                    officeAddress = officeAddress,
                                    officeCity = officeCity,
                                    officeState = officeState,
                                    officeZip = officeZip,
                                    matrixUsername = null,
                                    matrixPassword = null
                                )
                                
                                // Only register with Cognito (no Matrix integration yet)
                                val result = cognitoAuthService.registerWithoutMatrix(userData, password)
                                
                                if (result.isSuccess) {
                                    // Store user data for verification screen
                                    // Registration successful - navigate to verification screen
                                    isRegistrationSuccessful = true
                                    registrationUsername = username
                                } else {
                                    // Handle field-specific errors from service
                                    result.fieldError?.let { (fieldName, fieldMessage) ->
                                        fieldErrors = mapOf(fieldName to fieldMessage)
                                    }
                                    errorMessage = result.error ?: "Registration failed. Please try again."
                                }
                            } catch (e: Exception) {
                                errorMessage = "An unexpected error occurred: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            }

            return RegisterState(
                username = username,
                usernameValidationState = usernameValidationState,
                email = email,
                confirmEmail = confirmEmail,
                firstName = firstName,
                lastName = lastName,
                professionalTitle = professionalTitle,
                specialty = specialty,
                phoneNumber = phoneNumber,
                password = password,
                confirmPassword = confirmPassword,
                npiNumber = npiNumber,
                country = country,
                officeAddress = officeAddress,
                officeCity = officeCity,
                officeState = officeState,
                officeZip = officeZip,
                isLoading = isLoading,
                errorMessage = errorMessage,
                fieldErrors = fieldErrors,
                canSubmit = canSubmit,
                isRegistrationSuccessful = isRegistrationSuccessful,
                registrationUsername = registrationUsername,
                eventSink = ::handleEvents,
            )
        }
    } 
