@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.seerr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.checkarr.data.models.*
import com.checkarr.ui.shared.*

enum class SeerrTab { REQUESTS, SEARCH }

@Composable
fun SeerrScreen(
    url: String?,
    apiKey: String?,
    viewModel: SeerrViewModel = viewModel()
) {
    val requests by viewModel.filteredRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.error.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val currentFilter by viewModel.filterStatus.collectAsState()

    var selectedTab by remember { mutableStateOf(SeerrTab.REQUESTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<SeerrRequest?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val isConfigured = !url.isNullOrBlank() && !apiKey.isNullOrBlank()

    LaunchedEffect(url, apiKey) {
        if (isConfigured) viewModel.load(url!!, apiKey!!)
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    val filterOptions = listOf("all" to "All", "pending" to "Pending", "approved" to "Approved",
        "declined" to "Declined", "available" to "Available")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Seerr") },
                    actions = {
                        if (isConfigured) {
                            if (selectedTab == SeerrTab.REQUESTS) {
                                Box {
                                    IconButton(onClick = { showFilterMenu = true }) {
                                        Icon(Icons.Default.FilterList, contentDescription = "Filter",
                                            tint = if (currentFilter != "all") MaterialTheme.colorScheme.primary else LocalContentColor.current)
                                    }
                                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                        filterOptions.forEach { (key, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                leadingIcon = {
                                                    if (currentFilter == key) Icon(Icons.Default.Check, null,
                                                        tint = MaterialTheme.colorScheme.primary)
                                                },
                                                onClick = { viewModel.setFilter(key); showFilterMenu = false }
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.load(url!!, apiKey!!) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    SeerrTab.values().forEach { tab ->
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
        if (!isConfigured) {
            UnconfiguredState(serviceName = "Seerr", icon = Icons.Default.VideoCall, color = Color(0xFF7C3AED))
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            error?.let {
                ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
            }

            when (selectedTab) {
                SeerrTab.REQUESTS -> RequestsTab(
                    requests = requests,
                    isLoading = isLoading,
                    onApprove = { viewModel.approveRequest(url!!, apiKey!!, it) },
                    onDecline = { viewModel.declineRequest(url!!, apiKey!!, it) },
                    onDelete = { req -> showDeleteConfirm = req },
                    onRetry = { viewModel.retryRequest(url!!, apiKey!!, it) }
                )
                SeerrTab.SEARCH -> SearchTab(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        if (it.length >= 2) viewModel.search(url!!, apiKey!!, it)
                        else viewModel.clearSearch()
                    },
                    results = searchResults,
                    isSearching = isSearching
                )
            }
        }
    }

    showDeleteConfirm?.let { req ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Request") },
            text = { Text("Are you sure you want to delete this request?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRequest(url!!, apiKey!!, req.id)
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
private fun RequestsTab(
    requests: List<SeerrRequest>,
    isLoading: Boolean,
    onApprove: (Int) -> Unit,
    onDecline: (Int) -> Unit,
    onDelete: (SeerrRequest) -> Unit,
    onRetry: (Int) -> Unit
) {
    when {
        isLoading -> LoadingOverlay()
        requests.isEmpty() -> EmptyState(icon = Icons.Default.VideoCall, title = "No Requests", subtitle = "No requests found.")
        else -> LazyColumn {
            items(requests) { request ->
                RequestItem(
                    request = request,
                    onApprove = { onApprove(request.id) },
                    onDecline = { onDecline(request.id) },
                    onDelete = { onDelete(request) },
                    onRetry = { onRetry(request.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RequestItem(
    request: SeerrRequest,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val statusColor = Color(request.statusColor)

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = request.media?.let { "TMDb: ${it.tmdbId ?: it.tvdbId}" } ?: "Request #${request.id}",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusBadge(request.statusLabel, statusColor)
                if (request.is4k) StatusBadge("4K", Color(0xFF8B5CF6))
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                request.requestedBy?.let {
                    Text("Requested by ${it.displayName}", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(request.type.replaceFirstChar { it.uppercase() }, Color(0xFF3B82F6))
                    Text(request.createdAt.take(10), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (request.status == 1) { // pending
                    IconButton(onClick = onApprove) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Approve",
                            tint = Color(0xFF22C55E))
                    }
                    IconButton(onClick = onDecline) {
                        Icon(Icons.Default.Cancel, contentDescription = "Decline",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (request.status == 1) {
                            DropdownMenuItem(
                                text = { Text("Approve") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E)) },
                                onClick = { showMenu = false; onApprove() }
                            )
                            DropdownMenuItem(
                                text = { Text("Decline") },
                                leadingIcon = { Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDecline() }
                            )
                        }
                        if (request.status in listOf(3, 5)) {
                            DropdownMenuItem(
                                text = { Text("Retry") },
                                leadingIcon = { Icon(Icons.Default.Replay, null) },
                                onClick = { showMenu = false; onRetry() }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SeerrSearchResult>,
    isSearching: Boolean
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search movies and shows…") },
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
                Text("Search for movies and TV shows to request them",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            results.isEmpty() && query.length >= 2 -> EmptyState(
                icon = Icons.Default.SearchOff, title = "No Results", subtitle = "No results found for \"$query\""
            )
            else -> LazyColumn {
                items(results) { result ->
                    SearchResultItem(result = result)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: SeerrSearchResult) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(result.displayTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (result.displayDate.isNotBlank()) {
                    Text(result.displayDate, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result.overview?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(
                        if (result.mediaType == "movie") "Movie" else "TV",
                        if (result.mediaType == "movie") Color(0xFFFFAC33) else Color(0xFF00D4FF)
                    )
                    if (result.mediaInfo != null) {
                        StatusBadge("In Library", Color(0xFF22C55E))
                    }
                    if (result.voteAverage > 0) {
                        Text("★ ${"%.1f".format(result.voteAverage)}", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24))
                    }
                }
            }
        },
        leadingContent = {
            result.posterUrl?.let { url ->
                Card(shape = MaterialTheme.shapes.small, modifier = Modifier.size(width = 48.dp, height = 72.dp)) {
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                }
            } ?: Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(width = 48.dp, height = 72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
