@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.series

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkarr.data.models.*
import com.checkarr.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesSearchScreen(
    instance: Instance?,
    navController: NavController,
    viewModel: SeriesViewModel = viewModel()
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val existingSeries by viewModel.series.collectAsState()
    val qualityProfiles by viewModel.qualityProfiles.collectAsState()
    val rootFolders by viewModel.rootFolders.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf<Series?>(null) }

    LaunchedEffect(instance) {
        instance?.let { viewModel.loadMetadata(it) }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            instance?.let { inst ->
                                if (it.length >= 2) viewModel.searchOnline(inst, it)
                                else viewModel.clearSearchResults()
                            }
                        },
                        placeholder = { Text("Search for series…") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        Box(modifier = Modifier.padding(padding)) {
            when {
                isSearching -> LoadingOverlay()
                query.isBlank() -> EmptyState(
                    icon = Icons.Default.Search,
                    title = "Search Series",
                    subtitle = "Type to search for series to add."
                )
                searchResults.isEmpty() && !isSearching -> EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Results",
                    subtitle = "No series found for \"$query\"."
                )
                else -> LazyColumn {
                    items(searchResults) { s ->
                        val isAdded = existingSeries.any { it.tvdbId == s.tvdbId }
                        SeriesSearchRow(
                            series = s,
                            isAdded = isAdded,
                            onAdd = { if (!isAdded) showAddDialog = s }
                        )
                    }
                }
            }
        }
    }

    showAddDialog?.let { s ->
        AddSeriesDialog(
            series = s,
            qualityProfiles = qualityProfiles,
            rootFolders = rootFolders,
            onAdd = { qpId, rfPath, monitored ->
                instance?.let { inst ->
                    viewModel.addSeries(inst, s, qpId, rfPath, monitored)
                }
                showAddDialog = null
            },
            onDismiss = { showAddDialog = null }
        )
    }
}

@Composable
private fun SeriesSearchRow(series: Series, isAdded: Boolean, onAdd: () -> Unit) {
    ListItem(
        headlineContent = { Text(series.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                "${series.year}${if (series.network != null) " · ${series.network}" else ""}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            AsyncImage(
                model = series.remotePoster,
                contentDescription = series.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(48.dp).height(72.dp)
            )
        },
        trailingContent = {
            if (isAdded) {
                Icon(Icons.Default.Check, contentDescription = "Added", tint = MaterialTheme.colorScheme.primary)
            } else {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onAdd)
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}

@Composable
private fun AddSeriesDialog(
    series: Series,
    qualityProfiles: List<QualityProfile>,
    rootFolders: List<RootFolder>,
    onAdd: (Int, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedQp by remember { mutableStateOf(qualityProfiles.firstOrNull()) }
    var selectedRf by remember { mutableStateOf(rootFolders.firstOrNull()) }
    var monitored by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${series.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (qualityProfiles.isNotEmpty()) {
                    var qpExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = qpExpanded, onExpandedChange = { qpExpanded = it }) {
                        OutlinedTextField(
                            value = selectedQp?.name ?: "Select quality profile",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Quality Profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qpExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = qpExpanded, onDismissRequest = { qpExpanded = false }) {
                            qualityProfiles.forEach { qp ->
                                DropdownMenuItem(text = { Text(qp.name) }, onClick = { selectedQp = qp; qpExpanded = false })
                            }
                        }
                    }
                }
                if (rootFolders.isNotEmpty()) {
                    var rfExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = rfExpanded, onExpandedChange = { rfExpanded = it }) {
                        OutlinedTextField(
                            value = selectedRf?.path ?: "Select root folder",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Root Folder") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rfExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = rfExpanded, onDismissRequest = { rfExpanded = false }) {
                            rootFolders.forEach { rf ->
                                DropdownMenuItem(text = { Text(rf.path) }, onClick = { selectedRf = rf; rfExpanded = false })
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = monitored, onCheckedChange = { monitored = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Monitor")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qpId = selectedQp?.id ?: qualityProfiles.firstOrNull()?.id ?: 1
                    val rfPath = selectedRf?.path ?: rootFolders.firstOrNull()?.path ?: ""
                    onAdd(qpId, rfPath, monitored)
                },
                enabled = selectedQp != null && selectedRf != null
            ) { Text("Add Series") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
