/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.customauth.impl.R
import io.element.android.features.customauth.impl.components.PhoneNumberInput
import io.element.android.features.customauth.impl.components.SearchableDropdown
import io.element.android.features.customauth.impl.data.DropdownData

@Composable
private fun FieldError(
    fieldName: String,
    fieldErrors: Map<String, String>,
) {
    fieldErrors[fieldName]?.let { error ->
        Text(
            text = error,
            color = Color(0xFFEF4444),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DocumentUploadSection(
    documentUri: android.net.Uri?,
    isUploading: Boolean,
    isUploaded: Boolean,
    onDocumentSelected: (android.net.Uri?) -> Unit,
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        onDocumentSelected(uri)
    }

    Column {
        // Title with warning icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Verification Required",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Healthcare Verification Required *",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
            )
        }

        // Description
        Text(
            text = "Upload proof of healthcare credentials (ID badge, license copy, etc.). Your account will be deactivated if we cannot verify your healthcare professional status.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Upload button/status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isUploading) {
                    launcher.launch("*/*")
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUploaded -> Color(0xFFECFDF5)
                    isUploading -> Color(0xFFF8FAFC)
                    else -> Color(0xFFF8FAFC)
                }
            ),
            border = BorderStroke(
                1.dp,
                when {
                    isUploaded -> Color(0xFF10B981)
                    isUploading -> Color(0xFF94A3B8)
                    else -> Color(0xFFE2E8F0)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isUploaded -> "✓ Document Uploaded"
                            isUploading -> "Uploading..."
                            documentUri != null -> "Document Selected"
                            else -> "Select Document"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isUploaded -> Color(0xFF10B981)
                            isUploading -> Color(0xFF64748B)
                            else -> Color(0xFF0F172A)
                        }
                    )
                    
                    Text(
                        text = when {
                            isUploaded -> "Ready to proceed with registration"
                            isUploading -> "Please wait while we upload your document"
                            documentUri != null -> "Tap to upload or select a different document"
                            else -> "Tap to select a document (PDF, JPG, PNG)"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF0EA5E9)
                    )
                } else if (isUploaded) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Uploaded",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onNavigateToLogin: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Helper function to get field colors based on error state
    @Composable
    fun getFieldColors(fieldName: String) = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color(0xFFF8FAFC),
        focusedIndicatorColor = if (state.fieldErrors.containsKey(fieldName)) Color(0xFFEF4444) else Color(0xFF0EA5E9),
        unfocusedIndicatorColor = if (state.fieldErrors.containsKey(fieldName)) Color(0xFFEF4444) else Color(0xFFE2E8F0),
    )

    // Handle successful registration
    if (state.isRegistrationSuccessful) {
        onNavigateToCredentials()
    }

    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    focusManager.clearFocus()
                },
        color = Color.White,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo Container
            Box(
                modifier =
                    Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.signout_square_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Register for Signout",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Start your secure healthcare communication journey",
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Security Info
            Card(
                modifier = Modifier.widthIn(max = 320.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = Color(0xFF0EA5E9),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted, secure, and HIPAA-compliant",
                        color = Color(0xFF0EA5E9),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                // Username Field
                Text(
                    text = "Username *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.username,
                    onValueChange = { state.eventSink(RegisterEvents.SetUsername(it)) },
                    placeholder = { Text("Choose a unique username") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("username"),
                    trailingIcon = {
                        when (state.usernameValidationState) {
                            is UsernameValidationState.Validating -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF0EA5E9),
                                )
                            }
                            is UsernameValidationState.Available -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Username available",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            is UsernameValidationState.Unavailable, is UsernameValidationState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Username unavailable",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            else -> null
                        }
                    },
                )

                // Username validation message
                when (state.usernameValidationState) {
                    is UsernameValidationState.Available -> {
                        Text(
                            text = "✓ Username is available",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    is UsernameValidationState.Unavailable -> {
                        Text(
                            text = state.usernameValidationState.message,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    is UsernameValidationState.Error -> {
                        Text(
                            text = state.usernameValidationState.message,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                Text(
                    text = "Email Address *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { state.eventSink(RegisterEvents.SetEmail(it)) },
                    placeholder = { Text("you@email.com") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("email"),
                )

                // Email error message
                FieldError("email", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Email Field
                Text(
                    text = "Confirm Email Address *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.confirmEmail,
                    onValueChange = { state.eventSink(RegisterEvents.SetConfirmEmail(it)) },
                    placeholder = { Text("Confirm your email") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("confirmEmail"),
                )

                // Confirm Email error message
                FieldError("confirmEmail", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // First Name Field
                Text(
                    text = "First Name *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { state.eventSink(RegisterEvents.SetFirstName(it)) },
                    placeholder = { Text("First Name") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("firstName"),
                )

                // First Name error message
                FieldError("firstName", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Last Name Field
                Text(
                    text = "Last Name *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = { state.eventSink(RegisterEvents.SetLastName(it)) },
                    placeholder = { Text("Last Name") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("lastName"),
                )

                // Last Name error message
                FieldError("lastName", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Professional Title Dropdown
                Text(
                    text = "Professional Title *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SearchableDropdown(
                    value = state.professionalTitle,
                    onValueChange = { state.eventSink(RegisterEvents.SetProfessionalTitle(it)) },
                    placeholder = "Select title...",
                    options = DropdownData.PROFESSIONAL_TITLES,
                    searchable = false,
                )

                // Professional Title error message
                FieldError("professionalTitle", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Specialty Dropdown
                Text(
                    text = "Specialty *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SearchableDropdown(
                    value = state.specialty,
                    onValueChange = { state.eventSink(RegisterEvents.SetSpecialty(it)) },
                    placeholder = "Select specialty...",
                    options = DropdownData.MEDICAL_SPECIALTIES,
                    searchable = true,
                )

                // Specialty error message
                FieldError("specialty", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Field
                Text(
                    text = "Phone Number *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                PhoneNumberInput(
                    value = state.phoneNumber,
                    onValueChange = { state.eventSink(RegisterEvents.SetPhoneNumber(it)) },
                    placeholder = "Enter phone number",
                )

                // Phone Number error message
                FieldError("phoneNumber", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                Text(
                    text = "Password *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { state.eventSink(RegisterEvents.SetPassword(it)) },
                    placeholder = { Text("Password (min 8 characters)") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = Color(0xFF64748B),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("password"),
                )

                // Password error message
                FieldError("password", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                Text(
                    text = "Confirm Password *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = { state.eventSink(RegisterEvents.SetConfirmPassword(it)) },
                    placeholder = { Text("Confirm Password") },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                tint = Color(0xFF64748B),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("confirmPassword"),
                )

                // Confirm Password error message
                FieldError("confirmPassword", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // NPI Number Field (Optional)
                Text(
                    text = "NPI Number (Optional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.npiNumber,
                    onValueChange = { state.eventSink(RegisterEvents.SetNpiNumber(it)) },
                    placeholder = { Text("10-digit NPI") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("npiNumber"),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Country Dropdown
                Text(
                    text = "Country *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SearchableDropdown(
                    value = state.country,
                    onValueChange = {
                        state.eventSink(RegisterEvents.SetCountry(it))
                        // Reset state when country changes
                        state.eventSink(RegisterEvents.SetOfficeState(""))
                    },
                    placeholder = "Select country...",
                    options = DropdownData.COUNTRIES,
                    searchable = true,
                )

                // Country error message
                FieldError("country", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Office Address Field
                Text(
                    text = "Office Address *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.officeAddress,
                    onValueChange = { state.eventSink(RegisterEvents.SetOfficeAddress(it)) },
                    placeholder = { Text("123 Main St") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("officeAddress"),
                )

                // Office Address error message
                FieldError("officeAddress", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // City Field
                Text(
                    text = "City *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.officeCity,
                    onValueChange = { state.eventSink(RegisterEvents.SetOfficeCity(it)) },
                    placeholder = { Text("City") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("officeCity"),
                )

                // City error message
                FieldError("officeCity", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // State Field (conditional for USA/Canada)
                val stateOptions = DropdownData.getStatesForCountry(state.country)
                val requiresState = DropdownData.countryRequiresState(state.country)

                Text(
                    text = if (requiresState) "State/Province *" else "State/Province (Optional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (stateOptions.isNotEmpty()) {
                    SearchableDropdown(
                        value = state.officeState,
                        onValueChange = { state.eventSink(RegisterEvents.SetOfficeState(it)) },
                        placeholder = if (state.country == "USA") "Select state..." else "Select province...",
                        options = stateOptions,
                        searchable = true,
                    )
                } else {
                    OutlinedTextField(
                        value = state.officeState,
                        onValueChange = { state.eventSink(RegisterEvents.SetOfficeState(it)) },
                        placeholder = { Text("State/Province") },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = getFieldColors("officeState"),
                    )
                }

                // State error message
                FieldError("officeState", state.fieldErrors)

                Spacer(modifier = Modifier.height(16.dp))

                // Zip Code Field
                Text(
                    text = "Zip Code *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.officeZip,
                    onValueChange = { state.eventSink(RegisterEvents.SetOfficeZip(it)) },
                    placeholder = { Text("Zip Code") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (state.canSubmit) {
                                    state.eventSink(RegisterEvents.Submit)
                                }
                            },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = getFieldColors("officeZip"),
                )

                // Zip Code error message
                FieldError("officeZip", state.fieldErrors)

                Spacer(modifier = Modifier.height(24.dp))

                // Verification Document Upload Section
                DocumentUploadSection(
                    documentUri = state.verificationDocumentUri,
                    isUploading = state.isUploadingDocument,
                    isUploaded = state.verificationDocumentUrl != null,
                    onDocumentSelected = { uri ->
                        state.eventSink(RegisterEvents.SetVerificationDocument(uri))
                        // Auto-upload when document is selected
                        if (uri != null) {
                            state.eventSink(RegisterEvents.UploadVerificationDocument)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                // Register Button
                Button(
                    onClick = {
                        state.eventSink(RegisterEvents.Submit)
                    },
                    enabled = state.canSubmit,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0EA5E9),
                            disabledContainerColor = Color(0xFF94A3B8),
                        ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "Register",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Back to Login
                TextButton(
                    onClick = onNavigateToLogin,
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color(0xFF64748B),
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "Sign in",
                        color = Color(0xFF0EA5E9),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // USA Pride Banner
                USAPrideBanner()
            }
        }
    }
}

@Composable
private fun USAPrideBanner() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFF0F9FF),
            ),
        border = BorderStroke(1.dp, Color(0xFFE0F2FE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🇺🇸",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 8.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Designed & Made in the USA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0EA5E9),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Your data is protected by US technology and servers",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    ElementTheme {
        RegisterScreen(
            state =
                RegisterState(
                    username = "",
                    usernameValidationState = UsernameValidationState.Idle,
                    email = "",
                    confirmEmail = "",
                    firstName = "",
                    lastName = "",
                    professionalTitle = "",
                    specialty = "",
                    phoneNumber = "",
                    password = "",
                    confirmPassword = "",
                    npiNumber = "",
                    country = "",
                    officeAddress = "",
                    officeCity = "",
                    officeState = "",
                    officeZip = "",
                    verificationDocumentUri = null,
                    isUploadingDocument = false,
                    verificationDocumentUrl = null,
                    isLoading = false,
                    errorMessage = null,
                    fieldErrors = emptyMap(),
                    canSubmit = false,
                    isRegistrationSuccessful = false,
                    registrationUsername = null,
                    eventSink = {},
                ),
            onNavigateToLogin = {},
            onNavigateToCredentials = {},
        )
    }
} 
