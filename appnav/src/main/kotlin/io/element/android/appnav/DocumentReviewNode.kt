/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.navmodel.backstack.BackStack
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.architecture.inputs
import io.element.android.anvilannotations.ContributesNode
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ContributesNode(SessionScope::class)
class DocumentReviewNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val matrixClient: MatrixClient,
    private val documentReviewService: DocumentReviewService,
) : Node(buildContext, plugins = plugins) {

    interface Callback : NodeInputs {
        fun onBackClick()
        fun onDocumentClick(document: DocumentReview)
    }

    @Composable
    override fun View(modifier: Modifier) {
        val callback = inputs<Callback>()
        DocumentReviewView(
            modifier = modifier,
            onBackClick = callback::onBackClick,
            onDocumentClick = callback::onDocumentClick,
            matrixClient = matrixClient,
            documentReviewService = documentReviewService
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentReviewView(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onDocumentClick: (DocumentReview) -> Unit,
    matrixClient: MatrixClient,
    documentReviewService: DocumentReviewService
) {
    val scope = rememberCoroutineScope()
    var documentList by remember { mutableStateOf<List<DocumentReview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Pending") }

    // Load documents on first launch and when filter changes
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val result = documentReviewService.getPendingDocuments()
                if (result.isSuccess) {
                    documentList = result.documents
                    Timber.d("DocumentReview: Loaded ${result.documents.size} pending documents")
                } else {
                    errorMessage = result.error ?: "Failed to load documents"
                    Timber.e("DocumentReview: Failed to load documents: ${result.error}")
                }
            } catch (e: Exception) {
                errorMessage = "Error loading documents: ${e.message}"
                Timber.e(e, "DocumentReview: Exception loading documents")
            } finally {
                isLoading = false
            }
        }
    }

    // Load documents when filter changes
    LaunchedEffect(selectedFilter) {
        if (selectedFilter == "Deactivated") {
            isLoading = true
            errorMessage = null
            
            scope.launch {
                try {
                    val result = documentReviewService.getDeactivatedDocuments()
                    if (result.isSuccess) {
                        documentList = result.documents
                        Timber.d("DocumentReview: Loaded ${result.documents.size} deactivated documents")
                    } else {
                        errorMessage = result.error ?: "Failed to load deactivated documents"
                        Timber.e("DocumentReview: Failed to load deactivated documents: ${result.error}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error loading deactivated documents: ${e.message}"
                    Timber.e(e, "DocumentReview: Exception loading deactivated documents")
                } finally {
                    isLoading = false
                }
            }
        } else if (selectedFilter == "Pending") {
            isLoading = true
            errorMessage = null
            
            scope.launch {
                try {
                    val result = documentReviewService.getPendingDocuments()
                    if (result.isSuccess) {
                        documentList = result.documents
                        Timber.d("DocumentReview: Loaded ${result.documents.size} pending documents")
                    } else {
                        errorMessage = result.error ?: "Failed to load pending documents"
                        Timber.e("DocumentReview: Failed to load pending documents: ${result.error}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error loading pending documents: ${e.message}"
                    Timber.e(e, "DocumentReview: Exception loading pending documents")
                } finally {
                    isLoading = false
                }
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
                    val result = documentReviewService.searchDocuments(searchQuery)
                    if (result.isSuccess) {
                        documentList = result.documents
                        Timber.d("DocumentReview: Search found ${result.documents.size} documents")
                    } else {
                        errorMessage = result.error ?: "Search failed"
                        Timber.e("DocumentReview: Search failed: ${result.error}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Search error: ${e.message}"
                    Timber.e(e, "DocumentReview: Exception during search")
                } finally {
                    isLoading = false
                }
            }
        } else if (searchQuery.isBlank()) {
            // If search is cleared, reload pending documents
            isLoading = true
            errorMessage = null
            
            scope.launch {
                try {
                    val result = documentReviewService.getPendingDocuments()
                    if (result.isSuccess) {
                        documentList = result.documents
                        Timber.d("DocumentReview: Reloaded ${result.documents.size} pending documents")
                    } else {
                        errorMessage = result.error ?: "Failed to reload documents"
                        Timber.e("DocumentReview: Failed to reload documents: ${result.error}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Error reloading documents: ${e.message}"
                    Timber.e(e, "DocumentReview: Exception reloading documents")
                } finally {
                    isLoading = false
                }
            }
        } else {
            // Less than 2 characters - clear the list immediately
            documentList = emptyList()
            isLoading = false
        }
    }

    // Filter documents based on selected filter
    val filteredDocuments = remember(selectedFilter, documentList) {
        documentList.filter { document ->
            when (selectedFilter) {
                "Pending" -> document.status == DocumentStatus.PENDING
                "Deactivated" -> document.status == DocumentStatus.DEACTIVATED
                else -> true
            }
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
            },
            actions = {
                IconButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            try {
                                val result = documentReviewService.getPendingDocuments()
                                if (result.isSuccess) {
                                    documentList = result.documents
                                    Timber.d("DocumentReview: Refreshed ${result.documents.size} documents")
                                } else {
                                    errorMessage = result.error ?: "Failed to refresh"
                                    Timber.e("DocumentReview: Refresh failed: ${result.error}")
                                }
                            } catch (e: Exception) {
                                errorMessage = "Refresh error: ${e.message}"
                                Timber.e(e, "DocumentReview: Exception during refresh")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search documents (min 2 characters)") },
                placeholder = { Text("Enter name, email, file name...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            )

            // Filter buttons and document count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pending", "Deactivated").forEach { filter ->
                        Button(
                            onClick = { selectedFilter = filter },
                            modifier = Modifier.weight(1f),
                            colors = if (selectedFilter == filter) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text(filter)
                        }
                    }
                }
            }

            // Document count
            Text(
                text = "Showing ${filteredDocuments.size} documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error message
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

            // Documents List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDocuments) { document ->
                    DocumentReviewItem(
                        document = document,
                        onClick = { onDocumentClick(document) }
                    )
                }
                
                if (filteredDocuments.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "No pending documents found"
                                } else {
                                    "No documents found for \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentReviewItem(
    document: DocumentReview,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Status chip
                val statusColor = when (document.status) {
                    DocumentStatus.PENDING -> MaterialTheme.colorScheme.primary
                    DocumentStatus.UNDER_REVIEW -> MaterialTheme.colorScheme.secondary
                    DocumentStatus.APPROVED -> Color.Green
                    DocumentStatus.REJECTED -> MaterialTheme.colorScheme.error
                    DocumentStatus.DEACTIVATED -> Color.Gray
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = document.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontSize = 10.sp
                    )
                }
            }

            // Email and document type
            Text(
                text = document.userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "File: ${document.documentFileName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Additional info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Submitted: ${document.submittedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (document.specialty != null) {
                    Text(
                        text = document.specialty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
} 