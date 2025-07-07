/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.features.customauth.impl.data.CountryCode
import io.element.android.features.customauth.impl.data.DropdownData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Enter phone number",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var selectedCountry by remember { mutableStateOf(DropdownData.COUNTRY_CODES[0]) } // Default to US
    var phoneNumber by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }

    // Helper functions
    fun parsePhoneNumber(fullNumber: String): Pair<CountryCode, String>? {
        val cleanNumber = fullNumber.replace(Regex("[^\\d+]"), "")
        val country = DropdownData.COUNTRY_CODES.find { cleanNumber.startsWith(it.dialCode) }
        return if (country != null) {
            val numberPart = cleanNumber.substring(country.dialCode.length)
            Pair(country, numberPart)
        } else {
            null
        }
    }

    fun formatPhoneNumber(
        number: String,
        countryCode: String,
    ): String {
        val digits = number.replace(Regex("\\D"), "")
        return when (countryCode) {
            "US", "CA" -> {
                // Format as (XXX) XXX-XXXX
                when {
                    digits.length <= 3 -> digits
                    digits.length <= 6 -> "(${digits.take(3)}) ${digits.drop(3)}"
                    else -> "(${digits.take(3)}) ${digits.drop(3).take(3)}-${digits.drop(6).take(4)}"
                }
            }
            "MX" -> {
                // Format as XXX XXX XXXX
                when {
                    digits.length <= 3 -> digits
                    digits.length <= 6 -> "${digits.take(3)} ${digits.drop(3)}"
                    digits.length <= 9 -> "${digits.take(3)} ${digits.drop(3).take(3)} ${digits.drop(6)}"
                    else -> "${digits.take(3)} ${digits.drop(3).take(3)} ${digits.drop(6).take(4)}"
                }
            }
            "GB" -> {
                // Format as XXXXX XXXXXX
                when {
                    digits.length <= 5 -> digits
                    else -> "${digits.take(5)} ${digits.drop(5).take(6)}"
                }
            }
            else -> digits // Default formatting for other countries
        }
    }

    // Parse the full phone number when value changes
    LaunchedEffect(value) {
        if (value.isNotEmpty()) {
            parsePhoneNumber(value)?.let { (country, number) ->
                selectedCountry = country
                phoneNumber = formatPhoneNumber(number, country.code)
            }
        }
    }

    fun handlePhoneNumberChange(text: String) {
        // Get current cursor position before formatting
        val currentDigits = phoneNumber.replace(Regex("\\D"), "")
        val newDigits = text.replace(Regex("\\D"), "")
        
        // Only process if digits changed (not just formatting)
        if (currentDigits != newDigits) {
            // Limit digits based on country format
            val maxDigits = when (selectedCountry.code) {
                "US", "CA" -> 10
                "MX" -> 10
                "GB" -> 11
                else -> 15 // International standard
            }
            
            val limitedDigits = newDigits.take(maxDigits)
            val formatted = formatPhoneNumber(limitedDigits, selectedCountry.code)
            phoneNumber = formatted

            // Create the full E.164 format
            val fullNumber = if (limitedDigits.isNotEmpty()) {
                "${selectedCountry.dialCode}$limitedDigits"
            } else {
                ""
            }
            onValueChange(fullNumber)
        }
    }

    fun handleCountryChange(countryCode: String) {
        val country = DropdownData.COUNTRY_CODES.find { it.code == countryCode }
        if (country != null) {
            selectedCountry = country
            // Reformat the phone number for the new country
            val digits = phoneNumber.replace(Regex("\\D"), "")
            val formatted = formatPhoneNumber(digits, country.code)
            phoneNumber = formatted

            // Update the full number
            val fullNumber = "${country.dialCode}$digits"
            onValueChange(fullNumber)
        }
        showCountryPicker = false
    }

    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Country Code Picker
            ExposedDropdownMenuBox(
                expanded = showCountryPicker,
                onExpandedChange = { showCountryPicker = it },
                modifier = Modifier.width(120.dp),
            ) {
                OutlinedTextField(
                    value = "${selectedCountry.flag} ${selectedCountry.dialCode}",
                    onValueChange = { },
                    readOnly = true,
                    enabled = enabled,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCountryPicker)
                    },
                    modifier =
                        Modifier
                            .width(120.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            disabledContainerColor = Color(0xFFF1F5F9),
                            focusedIndicatorColor = Color(0xFF0EA5E9),
                            unfocusedIndicatorColor = Color(0xFFE2E8F0),
                            disabledIndicatorColor = Color(0xFFE2E8F0),
                        ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                )

                ExposedDropdownMenu(
                    expanded = showCountryPicker,
                    onDismissRequest = { showCountryPicker = false },
                ) {
                    DropdownData.COUNTRY_CODES.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = country.flag,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Text(
                                        text = "${country.dialCode} ${country.name}",
                                        fontSize = 14.sp,
                                        color = if (country.code == selectedCountry.code) Color(0xFF0EA5E9) else Color(0xFF0F172A),
                                        fontWeight = if (country.code == selectedCountry.code) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            },
                            onClick = { handleCountryChange(country.code) },
                            modifier =
                                if (country.code == selectedCountry.code) {
                                    Modifier.background(Color(0xFFF0F9FF))
                                } else {
                                    Modifier
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = ::handlePhoneNumberChange,
                placeholder = { Text(placeholder) },
                enabled = enabled,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { /* Handle done */ },
                    ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        disabledContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color(0xFF0EA5E9),
                        unfocusedIndicatorColor = Color(0xFFE2E8F0),
                        disabledIndicatorColor = Color(0xFFE2E8F0),
                    ),
                maxLines = 1,
            )
        }
    }
}
