@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.maintainerr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.checkarr.data.models.*
import com.checkarr.ui.shared.*
import androidx.compose.material.icons.automirrored.filled.List

@Composable
fun MaintainerrScreen(url: String?, apiKey: String?, viewModel: MaintainerrViewModel = viewModel()) {
    val collections by viewModel.collections.collectAsState()
    val selectedMedia by viewModel.selectedCollectionMedia.collectAsState()
    val storageMetrics by viewModel.storageMetrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMedia by viewModel.isLoadingMedia.collectAsState()
    val error by viewModel.error.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var selectedCollection by remember { mutableStateOf<MaintainerrRuleGroup?>(null) }
    var showMediaSheet by remember { mutableStateOf(false) }

    LaunchedEffect(url, apiKey) {
        if (!url.isNullOrBlank() && !apiKey.isNullOrBlank()) viewModel.load(url, apiKey)
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maintainerr") },
                actions = {
                    url?.let { u ->
                        if (u.isNotBlank()) {
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Run Rules") },
                                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                                        onClick = { showMenu = false; viewModel.runRules(u, apiKey!!) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Handle All Collections") },
                                        leadingIcon = { Icon(Icons.Default.CleaningServices, null) },
                                        onClick = { showMenu = false; viewModel.handleCollections(u, apiKey!!) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = { showMenu = false; viewModel.load(u, apiKey!!) }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            if (toast != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(toast!!) }
            }
        }
    ) { padding ->
        if (url.isNullOrBlank() || apiKey.isNullOrBlank()) {
            UnconfiguredState(serviceName = "Maintainerr", icon = Icons.Default.CleaningServices, color = Color(0xFFEC4899))
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            error?.let {
                ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
            }

            when {
                isLoading -> LoadingOverlay()
                else -> {
                    storageMetrics?.let { metrics ->
                        StorageMetricsCard(metrics = metrics)
                    }
                    if (collections.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.CleaningServices,
                            title = "No Collections",
                            subtitle = "No Maintainerr collections found."
                        )
                    } else {
                        LazyColumn {
                            items(collections) { ruleGroup ->
                                CollectionItem(
                                    ruleGroup = ruleGroup,
                                    onViewMedia = {
                                        selectedCollection = ruleGroup
                                        viewModel.loadCollectionMedia(url, apiKey!!, ruleGroup.collection?.id ?: ruleGroup.id)
                                        showMediaSheet = true
                                    },
                                    onHandle = {
                                        viewModel.handleCollection(url, apiKey!!, ruleGroup.collection?.id ?: ruleGroup.id)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMediaSheet) {
        ModalBottomSheet(onDismissRequest = {
            showMediaSheet = false
            viewModel.clearMedia()
        }) {
            CollectionMediaSheet(
                collection = selectedCollection,
                media = selectedMedia,
                isLoading = isLoadingMedia,
                onRemoveMedia = { mediaId ->
                    url?.let { u ->
                        selectedCollection?.let { c ->
                            viewModel.removeMediaFromCollection(u, apiKey!!, c.id, mediaId)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun StorageMetricsCard(metrics: MaintainerrStorageMetrics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Storage Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("${metrics.collectionCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Collections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
                Column {
                    Text("${metrics.mediaCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Media Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
                metrics.collectionSummary?.let { summary ->
                    Column {
                        Text(summary.totalFormatted, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Reclaimable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionItem(
    ruleGroup: MaintainerrRuleGroup,
    onViewMedia: () -> Unit,
    onHandle: () -> Unit
) {
    val collection = ruleGroup.collection
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ruleGroup.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!ruleGroup.isActive) StatusBadge("Inactive", Color(0xFF6B7280))
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ruleGroup.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(ruleGroup.dataType.replaceFirstChar { it.uppercase() }, Color(0xFFEC4899))
                    collection?.deleteAfterDays?.let { StatusBadge("$it days", Color(0xFFEF4444)) }
                    collection?.let { StatusBadge(it.arrActionLabel, Color(0xFF8B5CF6)) }
                    Text("${ruleGroup.rules.size} rule(s)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onViewMedia) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Media",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onHandle) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Handle",
                        tint = Color(0xFFEC4899))
                }
            }
        }
    )
}

@Composable
private fun CollectionMediaSheet(
    collection: MaintainerrRuleGroup?,
    media: List<MaintainerrMedia>,
    isLoading: Boolean,
    onRemoveMedia: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = collection?.name ?: "Collection Media",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider()
        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            media.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No media in this collection", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn {
                items(media) { item ->
                    ListItem(
                        headlineContent = { Text("Plex ID: ${item.plexId}") },
                        supportingContent = {
                            Column {
                                item.addDate?.let { Text("Added: ${it.take(10)}", style = MaterialTheme.typography.bodySmall) }
                                if (item.isManual) {
                                    Text("Manually added", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onRemoveMedia(item.id) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
