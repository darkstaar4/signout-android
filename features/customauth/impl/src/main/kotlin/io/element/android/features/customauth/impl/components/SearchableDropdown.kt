/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.element.android.features.customauth.impl.data.DropdownOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    options: List<DropdownOption>,
    modifier: Modifier = Modifier,
    searchable: Boolean = true,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredOptions =
        if (searchable && searchQuery.isNotEmpty()) {
            options.filter { it.label.contains(searchQuery, ignoreCase = true) }
        } else {
            options
        }

    val selectedOption = options.find { it.value == value }

    if (searchable && options.size > 10) {
        // Use Dialog for large searchable lists
        Box(modifier = modifier) {
            OutlinedTextField(
                value = selectedOption?.label ?: "",
                onValueChange = { },
                placeholder = { Text(placeholder) },
                readOnly = true,
                enabled = false, // Disable to prevent interaction
                leadingIcon = {
                    // Show icon if selected option has one
                    selectedOption?.iconResId?.let { iconResId ->
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            if (enabled) {
                                expanded = true
                                searchQuery = ""
                            }
                        },
                shape = RoundedCornerShape(8.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface, // Same as enabled to hide disabled state
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface, // Normal text color when disabled
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // Normal placeholder color
                    ),
                interactionSource = remember { MutableInteractionSource() },
            )

            if (expanded) {
                Dialog(
                    onDismissRequest = {
                        expanded = false
                        searchQuery = ""
                    },
                ) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            // Header
                            Text(
                                text = placeholder,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done,
                                    ),
                                keyboardActions =
                                    KeyboardActions(
                                        onDone = { /* Handle search */ },
                                    ),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                singleLine = true,
                            )

                            // Options List
                            if (filteredOptions.isEmpty()) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No options found",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 300.dp),
                                ) {
                                    items(filteredOptions) { option ->
                                        DropdownOptionItem(
                                            option = option,
                                            isSelected = option.value == value,
                                            onClick = {
                                                onValueChange(option.value)
                                                expanded = false
                                                searchQuery = ""
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Use ExposedDropdownMenuBox for smaller lists
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = modifier,
        ) {
            OutlinedTextField(
                value = selectedOption?.label ?: "",
                onValueChange = { },
                placeholder = { Text(placeholder) },
                readOnly = true,
                enabled = enabled,
                leadingIcon = {
                    // Show icon if selected option has one
                    selectedOption?.iconResId?.let { iconResId ->
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(8.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Show icon if available
                                option.iconResId?.let { iconResId ->
                                    Image(
                                        painter = painterResource(id = iconResId),
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Text(
                                    text = option.label,
                                    fontSize = 15.sp,
                                    color = if (option.value == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (option.value == value) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        },
                        onClick = {
                            onValueChange(option.value)
                            expanded = false
                        },
                        modifier =
                            if (option.value == value) {
                                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                Modifier
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownOptionItem(
    option: DropdownOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Show icon if available
            option.iconResId?.let { iconResId ->
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = option.label,
                fontSize = 15.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(3.dp),
                            ),
                )
            }
        }
    }
} 
