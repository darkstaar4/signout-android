/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.anvilannotations.ContributesNode
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import android.content.Intent
import android.net.Uri

@ContributesNode(SessionScope::class)
class DocumentDetailNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val matrixClient: MatrixClient,
    private val documentReviewService: DocumentReviewService,
) : Node(buildContext, plugins = plugins) {

    data class Inputs(
        val document: DocumentReview,
    ) : NodeInputs

    interface Callback : NodeInputs {
        fun onBackClick()
        fun onDocumentActionCompleted()
    }

    @Composable
    override fun View(modifier: Modifier) {
        val callback = inputs<Callback>()
        val inputs = inputs<Inputs>()
        DocumentDetailView(
            modifier = modifier,
            document = inputs.document,
            onBackClick = callback::onBackClick,
            onDocumentActionCompleted = callback::onDocumentActionCompleted,
            matrixClient = matrixClient,
            documentReviewService = documentReviewService
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentDetailView(
    modifier: Modifier = Modifier,
    document: DocumentReview,
    onBackClick: () -> Unit,
    onDocumentActionCompleted: () -> Unit,
    matrixClient: MatrixClient,
    documentReviewService: DocumentReviewService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var documentUrl by remember { mutableStateOf<String?>(null) }
    
    // Dialog states
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    // Load document download URL
    LaunchedEffect(document.id) {
        scope.launch {
            documentUrl = documentReviewService.getDocumentDownloadUrl(document.id)
            Timber.d("DocumentDetail: Got download URL for document ${document.id}")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Document Review") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "User Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    InfoRow("Name", document.displayName)
                    InfoRow("Email", document.userEmail)
                    document.phoneNumber?.let { InfoRow("Phone", it) }
                    document.specialty?.let { InfoRow("Specialty", it) }
                    document.city?.let { city ->
                        val location = if (document.state != null) "$city, ${document.state}" else city
                        InfoRow("Location", location)
                    }
                    InfoRow("User Status", if (document.isUserActive) "Active" else "Inactive")
                }
            }

            // Document Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Document Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    InfoRow("File Name", document.documentFileName)
                    InfoRow("Status", document.status.name.replace("_", " "))
                    InfoRow("Submitted", document.submittedAt)
                }
            }

            // Document Viewer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Document Viewer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = {
                            documentUrl?.let { url ->
                                if (url.startsWith("error://")) {
                                    errorMessage = when (url) {
                                        "error://document-not-available" -> "Document is not available or access failed. Please contact support."
                                        else -> "Document access error"
                                    }
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to open document")
                                        errorMessage = "Failed to open document: ${e.message}"
                                    }
                                }
                            } ?: run {
                                errorMessage = "Document URL not available"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = documentUrl != null && !documentUrl!!.startsWith("error://")
                    ) {
                        Text("View Document")
                    }
                    
                    if (documentUrl == null) {
                        Text(
                            text = "Loading document...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (documentUrl!!.startsWith("error://")) {
                        Text(
                            text = "Document not available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Action Buttons Card
            if (document.status == DocumentStatus.PENDING || document.status == DocumentStatus.UNDER_REVIEW) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Review Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Clear from Review Button
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear from Review")
                        }
                        
                        // Deactivate User Button
                        OutlinedButton(
                            onClick = { showDeactivateDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deactivate User Account")
                        }
                    }
                }
            }

            // Status Messages
            successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Green
                    )
                }
            }

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Action Dialogs


    if (showClearDialog) {
        ConfirmationDialog(
            content = "Are you sure you want to clear ${document.displayName} from document review? This will remove the document from the review queue.",
            title = "Clear from Review",
            onDismiss = { showClearDialog = false },
            onSubmitClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    
                    try {
                        val result = documentReviewService.performDocumentAction(
                            document.id,
                            DocumentAction.CLEAR_FROM_REVIEW
                        )
                        
                        if (result.isSuccess) {
                            successMessage = "User cleared from review successfully"
                            onDocumentActionCompleted()
                        } else {
                            errorMessage = result.error ?: "Failed to clear from review"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error clearing from review: ${e.message}"
                    } finally {
                        isLoading = false
                        showClearDialog = false
                    }
                }
            }
        )
    }

    if (showDeactivateDialog) {
        ConfirmationDialog(
            content = "Are you sure you want to deactivate ${document.displayName}'s account? This will prevent them from logging in and disable their access to the application.",
            title = "Deactivate User Account",
            onDismiss = { showDeactivateDialog = false },
            onSubmitClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    
                    try {
                        val result = documentReviewService.performDocumentAction(
                            document.id,
                            DocumentAction.DEACTIVATE_USER
                        )
                        
                        if (result.isSuccess) {
                            successMessage = "User account deactivated successfully"
                            onDocumentActionCompleted()
                        } else {
                            errorMessage = result.error ?: "Failed to deactivate user"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error deactivating user: ${e.message}"
                    } finally {
                        isLoading = false
                        showDeactivateDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f)
        )
    }
}

 