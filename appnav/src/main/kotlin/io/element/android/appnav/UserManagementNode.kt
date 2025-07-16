/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.anvilannotations.ContributesNode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Cognito User Data Class
data class CognitoUser(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val city: String,
    val state: String,
    val country: String,
    val specialty: String,
    val professionalTitle: String,
    val phoneNumber: String,
    val isActive: Boolean,
    val createdAt: String,
    val lastSignIn: String,
    val matrixUserId: String?
)

@ContributesNode(SessionScope::class)
class UserManagementNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val matrixClient: MatrixClient,
    private val cognitoUserService: CognitoUserService,
) : Node(buildContext, plugins = plugins) {

    interface Callback : NodeInputs {
        fun onBackClick()
    }

    @Composable
    override fun View(modifier: Modifier) {
        val callback = inputs<Callback>()
        UserManagementView(
            modifier = modifier,
            onBackClick = callback::onBackClick,
            matrixClient = matrixClient,
            cognitoUserService = cognitoUserService
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserManagementView(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    matrixClient: MatrixClient,
    cognitoUserService: CognitoUserService
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showUserDetails by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<CognitoUser?>(null) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    var userList by remember { mutableStateOf<List<CognitoUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load users from Cognito
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val result = cognitoUserService.getAllUsers()
                if (result.isSuccess) {
                    userList = result.users
                    Timber.d("Loaded ${result.users.size} users from Cognito")
                } else {
                    errorMessage = result.error ?: "Failed to load users"
                    Timber.e("Failed to load users: ${result.error}")
                }
            } catch (e: Exception) {
                errorMessage = "Error loading users: ${e.message}"
                Timber.e(e, "Exception loading users")
            } finally {
                isLoading = false
            }
        }
    }

    // Debounced search - only search after user stops typing for 500ms
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            // Debounce the search by 500ms
            kotlinx.coroutines.delay(500)
            
            isLoading = true
            errorMessage = null
            
            scope.launch {
                try {
                    val result = cognitoUserService.searchUsers(searchQuery)
                    if (result.isSuccess) {
                        userList = result.users
                        Timber.d("Search found ${result.users.size} users")
                    } else {
                        errorMessage = result.error ?: "Search failed"
                        Timber.e("Search failed: ${result.error}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Search error: ${e.message}"
                    Timber.e(e, "Exception during search")
                } finally {
                    isLoading = false
                }
            }
        } else if (searchQuery.isBlank()) {
            // If search is cleared, reload all users immediately
            isLoading = true
            errorMessage = null
            
            scope.launch {
                try {
                    val result = cognitoUserService.getAllUsers()
                    if (result.isSuccess) {
                        userList = result.users
                        Timber.d("Reloaded ${result.users.size} users")
                    } else {
                        errorMessage = result.error ?: "Failed to reload users"
                        Timber.e("Failed to reload users: ${result.error}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error reloading users: ${e.message}"
                    Timber.e(e, "Exception reloading users")
                } finally {
                    isLoading = false
                }
            }
        } else {
            // Less than 2 characters - clear the list immediately
            userList = emptyList()
            isLoading = false
        }
    }

    // Filter users based on selected filter (Active/Inactive)
    val filteredUsers = remember(selectedFilter, userList) {
        userList.filter { user ->
            when (selectedFilter) {
                "Active" -> user.isActive
                "Inactive" -> !user.isActive
                else -> true
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = { BackButton(onClick = onBackClick) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search and Filter Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ElementTheme.colors.bgSubtleSecondary
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🔍 Search Users",
                        style = ElementTheme.typography.fontHeadingSmMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search users (min 2 characters)") },
                        placeholder = { Text("Enter name, email, username, specialty...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    
                    // Filter buttons and refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Active", "Inactive").forEach { filter ->
                            Button(
                                onClick = { selectedFilter = filter },
                                modifier = Modifier.weight(1f),
                                colors = if (selectedFilter == filter) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                },
                                enabled = !isLoading
                            ) {
                                Text(filter)
                            }
                        }
                        
                        // Refresh button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    searchQuery = "" // Clear search
                                    
                                    try {
                                        val result = cognitoUserService.getAllUsers()
                                        if (result.isSuccess) {
                                            userList = result.users
                                            Timber.d("Refreshed ${result.users.size} users")
                                        } else {
                                            errorMessage = result.error ?: "Failed to refresh users"
                                            Timber.e("Failed to refresh users: ${result.error}")
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Refresh error: ${e.message}"
                                        Timber.e(e, "Exception during refresh")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text("🔄")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${filteredUsers.size} users ${if (searchQuery.isNotBlank()) "found" else "total"}",
                            style = ElementTheme.typography.fontBodySmRegular,
                            color = ElementTheme.colors.textSecondary
                        )
                        
                        if (isLoading) {
                            Text(
                                text = "Loading...",
                                style = ElementTheme.typography.fontBodySmRegular,
                                color = ElementTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // User List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = ElementTheme.colors.textCriticalPrimary,
                    style = ElementTheme.typography.fontBodySmRegular
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserListItem(
                            user = user,
                            onUserClick = {
                                selectedUser = user
                                showUserDetails = true
                            },
                            onDeactivateClick = {
                                selectedUser = user
                                showDeactivateDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // User Details Dialog
    if (showUserDetails && selectedUser != null) {
        UserDetailsDialog(
            user = selectedUser!!,
            onDismiss = { 
                showUserDetails = false
                selectedUser = null
            },
            onDeactivate = {
                showUserDetails = false
                showDeactivateDialog = true
            }
        )
    }

    // Deactivate User Dialog
    if (showDeactivateDialog && selectedUser != null) {
        ConfirmationDialog(
            content = "Are you sure you want to ${if (selectedUser!!.isActive) "deactivate" else "activate"} user: ${selectedUser!!.displayName}?",
            title = "${if (selectedUser!!.isActive) "Deactivate" else "Activate"} User",
            submitText = if (selectedUser!!.isActive) "Deactivate" else "Activate",
            onSubmitClick = {
                val user = selectedUser!!
                val newStatus = !user.isActive
                
                scope.launch {
                    isLoading = true
                    try {
                        val success = cognitoUserService.toggleUserStatus(user.id, newStatus)
                        if (success) {
                            // Update local user list
                            userList = userList.map { 
                                if (it.id == user.id) it.copy(isActive = newStatus) else it 
                            }
                            Timber.d("User ${user.displayName} ${if (newStatus) "activated" else "deactivated"}")
                        } else {
                            errorMessage = "Failed to ${if (newStatus) "activate" else "deactivate"} user"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                        Timber.e(e, "Exception toggling user status")
                    } finally {
                        isLoading = false
                        showDeactivateDialog = false
                        selectedUser = null
                    }
                }
            },
            onDismiss = {
                showDeactivateDialog = false
                selectedUser = null
            }
        )
    }
}

@Composable
private fun UserListItem(
    user: CognitoUser,
    onUserClick: () -> Unit,
    onDeactivateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) {
                ElementTheme.colors.bgSubtleSecondary
            } else {
                ElementTheme.colors.bgSubtleSecondary.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = user.displayName,
                            style = ElementTheme.typography.fontBodyMdRegular,
                            fontWeight = FontWeight.Bold
                        )
                        if (!user.isActive) {
                            Text(
                                text = "INACTIVE",
                                style = ElementTheme.typography.fontBodySmRegular,
                                color = ElementTheme.colors.textCriticalPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = "${user.professionalTitle} • ${user.specialty}",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                    
                    Text(
                        text = user.email,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                    
                    Text(
                        text = "${user.city}, ${user.state}, ${user.country}",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                    
                    Text(
                        text = "Last sign in: ${user.lastSignIn}",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onUserClick
                    ) {
                        Text("View Details")
                    }
                    TextButton(
                        onClick = onDeactivateClick
                    ) {
                        Text(
                            text = if (user.isActive) "Deactivate" else "Activate",
                            color = if (user.isActive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textSuccessPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserDetailsDialog(
    user: CognitoUser,
    onDismiss: () -> Unit,
    onDeactivate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    UserDetailRow("Display Name", user.displayName)
                    UserDetailRow("Username", user.username)
                    UserDetailRow("Email", user.email)
                    UserDetailRow("First Name", user.firstName)
                    UserDetailRow("Last Name", user.lastName)
                    UserDetailRow("Professional Title", user.professionalTitle)
                    UserDetailRow("Specialty", user.specialty)
                    UserDetailRow("Phone Number", user.phoneNumber)
                    UserDetailRow("City", user.city)
                    UserDetailRow("State", user.state)
                    UserDetailRow("Country", user.country)
                    UserDetailRow("Matrix User ID", user.matrixUserId ?: "Not linked")
                    UserDetailRow("Status", if (user.isActive) "Active" else "Inactive")
                    UserDetailRow("Created At", user.createdAt)
                    UserDetailRow("Last Sign In", user.lastSignIn)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDeactivate) {
                Text(if (user.isActive) "Deactivate" else "Activate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun UserDetailRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
} 