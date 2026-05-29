@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.prowlarr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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

enum class ProwlarrTab { INDEXERS, SEARCH, HISTORY }

@Composable
fun ProwlarrScreen(
    instance: Instance?,
    viewModel: ProwlarrViewModel = viewModel()
) {
    val indexers by viewModel.indexers.collectAsState()
    val indexerStats by viewModel.indexerStats.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.error.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var selectedTab by remember { mutableStateOf(ProwlarrTab.INDEXERS) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<ProwlarrIndexer?>(null) }

    LaunchedEffect(instance) {
        instance?.let {
            viewModel.load(it)
            viewModel.loadHistory(it)
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Prowlarr") },
                    actions = {
                        instance?.let { inst ->
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Test All Indexers") },
                                        leadingIcon = { Icon(Icons.Default.NetworkCheck, null) },
                                        onClick = { showMenu = false; viewModel.testAllIndexers(inst) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = { showMenu = false; viewModel.load(inst) }
                                    )
                                }
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    ProwlarrTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            if (toast != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(toast!!) }
            }
        }
    ) { padding ->
        if (instance == null) {
            UnconfiguredState(serviceName = "Prowlarr", icon = Icons.Default.ManageSearch, color = Color(0xFFFF6B35))
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            error?.let {
                ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
            }

            when (selectedTab) {
                ProwlarrTab.INDEXERS -> IndexersTab(
                    indexers = indexers,
                    indexerStats = indexerStats,
                    isLoading = isLoading,
                    onDelete = { showDeleteConfirm = it }
                )
                ProwlarrTab.SEARCH -> SearchTab(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        instance?.let { inst -> if (it.length >= 3) viewModel.search(inst, it) else viewModel.clearSearch() }
                    },
                    results = searchResults,
                    isSearching = isSearching
                )
                ProwlarrTab.HISTORY -> HistoryTab(history = history)
            }
        }
    }

    showDeleteConfirm?.let { indexer ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Indexer") },
            text = { Text("Delete \"${indexer.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        instance?.let { inst -> viewModel.deleteIndexer(inst, indexer.id) }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun IndexersTab(
    indexers: List<ProwlarrIndexer>,
    indexerStats: ProwlarrIndexerStats?,
    isLoading: Boolean,
    onDelete: (ProwlarrIndexer) -> Unit
) {
    when {
        isLoading -> LoadingOverlay()
        indexers.isEmpty() -> EmptyState(icon = Icons.Default.ManageSearch, title = "No Indexers", subtitle = "No indexers configured in Prowlarr.")
        else -> {
            val enabled = indexers.count { it.enable }
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(label = "Total", value = "${indexers.size}")
                    InfoChip(label = "Enabled", value = "$enabled", color = Color(0xFF22C55E))
                    InfoChip(label = "Disabled", value = "${indexers.size - enabled}", color = Color(0xFF6B7280))
                }
                HorizontalDivider()
                LazyColumn {
                    items(indexers) { indexer ->
                        IndexerItem(
                            indexer = indexer,
                            stat = indexerStats?.indexers?.firstOrNull { it.indexerId == indexer.id },
                            onDelete = { onDelete(indexer) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexerItem(indexer: ProwlarrIndexer, stat: ProwlarrIndexerStat?, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(indexer.name, fontWeight = FontWeight.SemiBold)
                StatusBadge(
                    if (indexer.enable) "Enabled" else "Disabled",
                    if (indexer.enable) Color(0xFF22C55E) else Color(0xFF6B7280)
                )
                StatusBadge(
                    if (indexer.protocol == "usenet") "NZB" else "Torrent",
                    Color(0xFF3B82F6)
                )
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (indexer.supportsSearch) StatusBadge("Search", Color(0xFF8B5CF6))
                    if (indexer.supportsRss) StatusBadge("RSS", Color(0xFFFF6B35))
                }
                stat?.let { s ->
                    Text(
                        "Queries: ${s.numberOfQueries} · Grabs: ${s.numberOfGrabs} · Avg ${s.averageResponseTime}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<ProwlarrSearchResult>,
    isSearching: Boolean
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search all indexers…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        when {
            isSearching -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            query.isBlank() -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Enter at least 3 characters to search across all indexers",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            results.isEmpty() && query.length >= 3 -> EmptyState(
                icon = Icons.Default.SearchOff, title = "No Results", subtitle = "No results found for \"$query\""
            )
            else -> LazyColumn {
                item {
                    Text("${results.size} results", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                items(results) { result ->
                    SearchResultItem(result = result)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: ProwlarrSearchResult) {
    ListItem(
        headlineContent = {
            Text(result.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(result.sizeFormatted, style = MaterialTheme.typography.labelSmall)
                    Text(result.ageFormatted, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(result.indexer, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary)
                }
                if (result.protocol == "torrent") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.seeders?.let { Text("↑$it", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E)) }
                        result.leechers?.let { Text("↓$it", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444)) }
                    }
                }
            }
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (result.protocol == "torrent") Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (result.protocol == "torrent") "T" else "U",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result.protocol == "torrent") Color(0xFF22C55E) else Color(0xFF3B82F6))
                }
            }
        }
    )
}

@Composable
private fun HistoryTab(history: ProwlarrHistoryResponse?) {
    if (history == null || history.records.isEmpty()) {
        EmptyState(icon = Icons.Default.History, title = "No History", subtitle = "No indexer history found.")
        return
    }
    LazyColumn {
        item {
            Text("${history.totalRecords} total records",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        items(history.records) { item ->
            ListItem(
                headlineContent = { Text(item.indexer, fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.eventType, style = MaterialTheme.typography.labelSmall)
                        Text(item.date.take(16), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                trailingContent = {
                    Icon(
                        if (item.successful) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (item.successful) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                    )
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
