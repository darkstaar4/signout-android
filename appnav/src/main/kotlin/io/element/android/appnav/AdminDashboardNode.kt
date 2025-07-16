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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.components.dialogs.AlertDialog
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
import javax.inject.Inject

// Broadcast Message Data Classes
data class BroadcastMessage(
    val id: String,
    val title: String,
    val content: String,
    val targetAudience: TargetAudience,
    val urgency: MessageUrgency,
    val isDismissable: Boolean,
    val launchDateTime: LocalDateTime,
    val activeDuration: Long, // in hours
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: LocalDateTime
)

enum class TargetAudience {
    ALL_USERS,
    ADMINS_ONLY,
    SPECIFIC_USERS
}

enum class MessageUrgency {
    CRITICAL,
    ROUTINE
}

@ContributesNode(SessionScope::class)
class AdminDashboardNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val matrixClient: MatrixClient,
) : Node(buildContext, plugins = plugins) {

    interface Callback : NodeInputs {
        fun onBackClick()
        fun onUserManagementClick()
        fun onDocumentReviewClick()
    }

    @Composable
    override fun View(modifier: Modifier) {
        val callback = inputs<Callback>()
        AdminDashboardView(
            modifier = modifier,
            onBackClick = callback::onBackClick,
            onUserManagementClick = callback::onUserManagementClick,
            onDocumentReviewClick = callback::onDocumentReviewClick,
            matrixClient = matrixClient
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardView(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onDocumentReviewClick: () -> Unit,
    matrixClient: MatrixClient
) {
    var showUserManagement by remember { mutableStateOf(false) }
    var showDocumentReview by remember { mutableStateOf(false) }
    var showSystemSettings by remember { mutableStateOf(false) }
    var showBroadcastMessages by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showCreateMessageDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<String?>(null) }
    var selectedMessageTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Inactive
    
    val scope = rememberCoroutineScope()
    var userList by remember { mutableStateOf<List<MatrixUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Broadcast Messages State
    var broadcastMessages by remember { 
        mutableStateOf(
            listOf(
                BroadcastMessage(
                    id = "1",
                    title = "System Maintenance",
                    content = "The system will be under maintenance from 2:00 AM to 4:00 AM PST tomorrow.",
                    targetAudience = TargetAudience.ALL_USERS,
                    urgency = MessageUrgency.CRITICAL,
                    isDismissable = false,
                    launchDateTime = LocalDateTime.now().plusHours(2),
                    activeDuration = 24,
                    isActive = true,
                    createdBy = "nabil.baig@gmail.com",
                    createdAt = LocalDateTime.now()
                ),
                BroadcastMessage(
                    id = "2",
                    title = "New Feature Update",
                    content = "We've added new messaging features. Check them out!",
                    targetAudience = TargetAudience.ALL_USERS,
                    urgency = MessageUrgency.ROUTINE,
                    isDismissable = true,
                    launchDateTime = LocalDateTime.now().minusHours(1),
                    activeDuration = 48,
                    isActive = true,
                    createdBy = "nabil.baig@gmail.com",
                    createdAt = LocalDateTime.now().minusHours(2)
                ),
                BroadcastMessage(
                    id = "3",
                    title = "Admin Policy Update",
                    content = "New admin policies have been implemented. Please review.",
                    targetAudience = TargetAudience.ADMINS_ONLY,
                    urgency = MessageUrgency.ROUTINE,
                    isDismissable = true,
                    launchDateTime = LocalDateTime.now().minusDays(1),
                    activeDuration = 12,
                    isActive = false,
                    createdBy = "nabil.baig@gmail.com",
                    createdAt = LocalDateTime.now().minusDays(2)
                )
            )
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = { BackButton(onClick = onBackClick) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ElementTheme.colors.bgSubtleSecondary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🔧 Admin Dashboard",
                            style = ElementTheme.typography.fontHeadingSmMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Welcome, nabil.baig@gmail.com!",
                            style = ElementTheme.typography.fontBodyMdRegular,
                            color = ElementTheme.colors.textSecondary
                        )
                    }
                }
            }

            // User Management Section
            item {
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
                            text = "👥 User Management",
                            style = ElementTheme.typography.fontHeadingSmMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage users, deactivate accounts, and view user details",
                            style = ElementTheme.typography.fontBodySmRegular,
                            color = ElementTheme.colors.textSecondary
                        )
                        
                        Button(
                            onClick = onUserManagementClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open User Management")
                        }
                    }
                }
            }

            // Document Review Section
            item {
                AdminSectionCard(
                    title = "📄 Document Review",
                    description = "Review and approve user documents and credentials",
                    isExpanded = showDocumentReview,
                    onToggle = { showDocumentReview = !showDocumentReview }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocumentReviewItem(
                            documentName = "Medical License - Dr. John Smith",
                            status = "Pending Review",
                            isUrgent = true
                        )
                        DocumentReviewItem(
                            documentName = "NPI Certificate - Dr. Sarah Johnson",
                            status = "Approved",
                            isUrgent = false
                        )
                        DocumentReviewItem(
                            documentName = "Board Certification - Dr. Mike Wilson",
                            status = "Requires Revision",
                            isUrgent = false
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = onDocumentReviewClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Document Review")
                        }
                    }
                }
            }

            // System Settings Section
            item {
                AdminSectionCard(
                    title = "⚙️ System Settings",
                    description = "Configure system-wide settings and preferences",
                    isExpanded = showSystemSettings,
                    onToggle = { showSystemSettings = !showSystemSettings }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemSettingItem(
                            title = "User Registration",
                            description = "Manage user registration settings",
                            value = "Enabled"
                        )
                        SystemSettingItem(
                            title = "Document Verification",
                            description = "Configure document verification requirements",
                            value = "Required"
                        )
                        SystemSettingItem(
                            title = "Session Timeout",
                            description = "Set session timeout duration",
                            value = "24 hours"
                        )
                    }
                }
            }

            // Broadcast Messages Section
            item {
                AdminSectionCard(
                    title = "📢 Broadcast Messages",
                    description = "Manage broadcast messages to users and admins",
                    isExpanded = showBroadcastMessages,
                    onToggle = { showBroadcastMessages = !showBroadcastMessages }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Create Message Button
                        Button(
                            onClick = { showCreateMessageDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Create New Message")
                        }
                        
                        // Active/Inactive Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedMessageTab = 0 },
                                modifier = Modifier.weight(1f),
                                colors = if (selectedMessageTab == 0) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("Active Messages")
                            }
                            Button(
                                onClick = { selectedMessageTab = 1 },
                                modifier = Modifier.weight(1f),
                                colors = if (selectedMessageTab == 1) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("Inactive Messages")
                            }
                        }
                        
                        // Message List
                        val filteredMessages = if (selectedMessageTab == 0) {
                            broadcastMessages.filter { it.isActive }
                        } else {
                            broadcastMessages.filter { !it.isActive }
                        }
                        
                        if (filteredMessages.isEmpty()) {
                            Text(
                                text = if (selectedMessageTab == 0) "No active messages" else "No inactive messages",
                                style = ElementTheme.typography.fontBodyMdRegular,
                                color = ElementTheme.colors.textSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            filteredMessages.forEach { message ->
                                BroadcastMessageItem(
                                    message = message,
                                    onToggleActive = { messageId ->
                                        broadcastMessages = broadcastMessages.map {
                                            if (it.id == messageId) it.copy(isActive = !it.isActive) else it
                                        }
                                    },
                                    onDelete = { messageId ->
                                        broadcastMessages = broadcastMessages.filter { it.id != messageId }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🚀 Quick Actions",
                            style = ElementTheme.typography.fontHeadingSmMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* TODO: Implement */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Backup Data")
                            }
                            Button(
                                onClick = { /* TODO: Implement */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export Logs")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* TODO: Implement */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Send Broadcast")
                            }
                            Button(
                                onClick = { /* TODO: Implement */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("System Status")
                            }
                        }
                    }
                }
            }
        }
    }

    // Deactivate User Dialog
    if (showDeactivateDialog && selectedUser != null) {
        AlertDialog(
            content = "Are you sure you want to deactivate user: $selectedUser?",
            title = "Deactivate User",
            onDismiss = {
                showDeactivateDialog = false
                selectedUser = null
            }
        )
    }
    
    // Create Message Dialog
    if (showCreateMessageDialog) {
        CreateMessageDialog(
            onDismiss = { showCreateMessageDialog = false },
            onCreateMessage = { message ->
                broadcastMessages = broadcastMessages + message
                showCreateMessageDialog = false
            }
        )
    }
}

@Composable
private fun AdminSectionCard(
    title: String,
    description: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = ElementTheme.typography.fontHeadingSmMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) CompoundIcons.ChevronUp() else CompoundIcons.ChevronDown(),
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                content()
            }
        }
    }
}

@Composable
private fun UserListItem(
    user: MatrixUser,
    onDeactivateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ElementTheme.colors.bgSubtleSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName ?: "Unknown User",
                    style = ElementTheme.typography.fontBodyMdRegular,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.userId.value,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary
                )
            }
            
            Button(
                onClick = onDeactivateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElementTheme.colors.bgCriticalPrimary
                )
            ) {
                Text("Deactivate")
            }
        }
    }
}

@Composable
private fun DocumentReviewItem(
    documentName: String,
    status: String,
    isUrgent: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) ElementTheme.colors.bgCriticalSubtle else ElementTheme.colors.bgSubtleSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = documentName,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = status,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = if (isUrgent) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textSecondary
                )
            }
            
            if (isUrgent) {
                Text(
                    text = "URGENT",
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textCriticalPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SystemSettingItem(
    title: String,
    description: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ElementTheme.colors.bgSubtleSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary
                )
            }
            
            Text(
                text = value,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BroadcastMessageItem(
    message: BroadcastMessage,
    onToggleActive: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ElementTheme.colors.bgSubtleSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = message.title,
                            style = ElementTheme.typography.fontBodyMdRegular,
                            fontWeight = FontWeight.Bold
                        )
                        if (message.urgency == MessageUrgency.CRITICAL) {
                            Text(
                                text = "CRITICAL",
                                style = ElementTheme.typography.fontBodySmRegular,
                                color = ElementTheme.colors.textCriticalPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = message.content,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Target: ${message.targetAudience.name}",
                            style = ElementTheme.typography.fontBodySmRegular,
                            color = ElementTheme.colors.textSecondary
                        )
                        Text(
                            text = "Dismissable: ${if (message.isDismissable) "Yes" else "No"}",
                            style = ElementTheme.typography.fontBodySmRegular,
                            color = ElementTheme.colors.textSecondary
                        )
                    }
                    
                    Text(
                        text = "Launch: ${message.launchDateTime}",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                    
                    Text(
                        text = "Duration: ${message.activeDuration} hours",
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { onToggleActive(message.id) }
                    ) {
                        Text(if (message.isActive) "Deactivate" else "Activate")
                    }
                    TextButton(
                        onClick = { onDelete(message.id) }
                    ) {
                        Text("Delete", color = ElementTheme.colors.textCriticalPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMessageDialog(
    onDismiss: () -> Unit,
    onCreateMessage: (BroadcastMessage) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf(TargetAudience.ALL_USERS) }
    var selectedUrgency by remember { mutableStateOf(MessageUrgency.ROUTINE) }
    var isDismissable by remember { mutableStateOf(true) }
    var activeDuration by remember { mutableStateOf("24") }
    var launchDate by remember { mutableStateOf("") }
    var launchTime by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Broadcast Message") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Message Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Message Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                
                item {
                    Text(
                        text = "Target Audience",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        fontWeight = FontWeight.Medium
                    )
                    Column {
                        TargetAudience.values().forEach { audience ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedAudience == audience,
                                        onClick = { selectedAudience = audience }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAudience == audience,
                                    onClick = { selectedAudience = audience }
                                )
                                Text(
                                    text = audience.name.replace("_", " ").lowercase().let { it.first().uppercase() + it.drop(1) },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Message Urgency",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        fontWeight = FontWeight.Medium
                    )
                    Column {
                        MessageUrgency.values().forEach { urgency ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedUrgency == urgency,
                                        onClick = { selectedUrgency = urgency }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUrgency == urgency,
                                    onClick = { selectedUrgency = urgency }
                                )
                                Text(
                                    text = urgency.name.lowercase().let { it.first().uppercase() + it.drop(1) },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDismissable,
                            onCheckedChange = { isDismissable = it }
                        )
                        Text(
                            text = "Message is dismissable by users",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = activeDuration,
                        onValueChange = { activeDuration = it },
                        label = { Text("Active Duration (hours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = launchDate,
                        onValueChange = { launchDate = it },
                        label = { Text("Launch Date (YYYY-MM-DD)") },
                        placeholder = { Text("2024-01-15") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = launchTime,
                        onValueChange = { launchTime = it },
                        label = { Text("Launch Time PST (HH:MM)") },
                        placeholder = { Text("14:30") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val launchDateTime = try {
                            if (launchDate.isNotBlank() && launchTime.isNotBlank()) {
                                LocalDateTime.parse("${launchDate}T${launchTime}:00")
                            } else {
                                LocalDateTime.now()
                            }
                        } catch (e: Exception) {
                            LocalDateTime.now()
                        }
                        
                        val message = BroadcastMessage(
                            id = System.currentTimeMillis().toString(),
                            title = title,
                            content = content,
                            targetAudience = selectedAudience,
                            urgency = selectedUrgency,
                            isDismissable = isDismissable,
                            launchDateTime = launchDateTime,
                            activeDuration = activeDuration.toLongOrNull() ?: 24L,
                            isActive = true,
                            createdBy = "nabil.baig@gmail.com",
                            createdAt = LocalDateTime.now()
                        )
                        onCreateMessage(message)
                    }
                }
            ) {
                Text("Create Message")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 