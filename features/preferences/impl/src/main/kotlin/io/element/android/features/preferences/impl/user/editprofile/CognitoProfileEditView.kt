/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.customauth.impl.components.PhoneNumberInput
import io.element.android.features.customauth.impl.components.SearchableDropdown
import io.element.android.features.customauth.impl.data.DropdownData as ProfileDropdownData
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.*
import io.element.android.libraries.ui.strings.CommonStrings

// Dropdown data constants matching the registration form
private object ProfileDropdownData {
    val PROFESSIONAL_TITLES = ProfileDropdownData.PROFESSIONAL_TITLES
    val COUNTRIES = ProfileDropdownData.COUNTRIES  
    val MEDICAL_SPECIALTIES = ProfileDropdownData.MEDICAL_SPECIALTIES
    
    fun getStatesForCountry(countryValue: String) = ProfileDropdownData.getStatesForCountry(countryValue)
    fun countryRequiresState(countryValue: String) = ProfileDropdownData.countryRequiresState(countryValue)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CognitoProfileEditView(
    state: CognitoProfileEditState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                withDismissAction = true,
            )
        }
    }

    @Composable
    fun getFieldColors() = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    BackButton(onClick = onBackClick)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                text = "Edit Profile Information",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Form Fields
            Column(
                modifier = Modifier.widthIn(max = 400.dp),
            ) {
                // Read-only fields section
                Text(
                    text = "Personal Information (Read-only)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // First Name (Read-only)
                Text(
                    text = "First Name",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Last Name (Read-only)
                Text(
                    text = "Last Name",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email (Read-only)
                Text(
                    text = "Email Address",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Editable fields section
                Text(
                    text = "Professional Information (Editable)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Professional Title Dropdown
                Text(
                    text = "Professional Title *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SearchableDropdown(
                    value = state.professionalTitle,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateProfessionalTitle(it)) },
                    placeholder = "Select title...",
                    options = ProfileDropdownData.PROFESSIONAL_TITLES,
                    searchable = false,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Specialty Dropdown
                Text(
                    text = "Specialty *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SearchableDropdown(
                    value = state.specialty,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateSpecialty(it)) },
                    placeholder = "Select specialty...",
                    options = ProfileDropdownData.MEDICAL_SPECIALTIES,
                    searchable = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number
                Text(
                    text = "Phone Number *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                PhoneNumberInput(
                    value = state.phoneNumber,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdatePhoneNumber(it)) },
                    placeholder = "Enter phone number",
                )

                Spacer(modifier = Modifier.height(16.dp))

                // NPI Number
                Text(
                    text = "NPI Number",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.npiNumber,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateNpiNumber(it)) },
                    placeholder = { Text("10-digit NPI") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Office Information section
                Text(
                    text = "Office Information (Editable)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Country Dropdown
                Text(
                    text = "Country *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SearchableDropdown(
                    value = state.country,
                    onValueChange = { 
                        state.eventSink(CognitoProfileEditEvents.UpdateCountry(it))
                        // Reset state when country changes
                        state.eventSink(CognitoProfileEditEvents.UpdateOfficeState(""))
                    },
                    placeholder = "Select country...",
                    options = ProfileDropdownData.COUNTRIES,
                    searchable = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Office Address
                Text(
                    text = "Office Address *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.officeAddress,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateOfficeAddress(it)) },
                    placeholder = { Text("123 Main St") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Office City
                Text(
                    text = "City *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.officeCity,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateOfficeCity(it)) },
                    placeholder = { Text("City") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // State/Province Field (conditional for USA/Canada)
                val stateOptions = ProfileDropdownData.getStatesForCountry(state.country)
                val requiresState = ProfileDropdownData.countryRequiresState(state.country)

                Text(
                    text = if (requiresState) "State/Province *" else "State/Province (Optional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (stateOptions.isNotEmpty()) {
                    SearchableDropdown(
                        value = state.officeState,
                        onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateOfficeState(it)) },
                        placeholder = if (state.country == "USA") "Select state..." else "Select province...",
                        options = stateOptions,
                        searchable = true,
                    )
                } else {
                    OutlinedTextField(
                        value = state.officeState,
                        onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateOfficeState(it)) },
                        placeholder = { Text("State/Province") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getFieldColors(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zip Code
                Text(
                    text = "Zip Code *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.officeZip,
                    onValueChange = { state.eventSink(CognitoProfileEditEvents.UpdateOfficeZip(it)) },
                    placeholder = { Text("Zip Code") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = { state.eventSink(CognitoProfileEditEvents.SaveProfile) },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (state.isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Saving...")
                        }
                    } else {
                        Text(
                            text = "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun CognitoProfileEditViewPreview() = ElementPreview {
    CognitoProfileEditView(
        state = CognitoProfileEditState(
            // Read-only fields
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            // Editable fields
            phoneNumber = "+1234567890",
            officeAddress = "123 Main St",
            officeCity = "Anytown",
            officeState = "CA",
            officeZip = "12345",
            professionalTitle = "MD",
            specialty = "Cardiology",
            npiNumber = "1234567890",
            country = "USA",
            // UI state
            isLoading = false,
            error = null,
            eventSink = { }
        ),
        onBackClick = { }
    )
} 