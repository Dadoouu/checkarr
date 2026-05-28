@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.movies

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkarr.data.models.*
import com.checkarr.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchScreen(
    instance: Instance?,
    qualityProfiles: List<QualityProfile>,
    rootFolders: List<RootFolder>,
    navController: NavController,
    viewModel: MoviesViewModel = viewModel()
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val existingMovies by viewModel.movies.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf<Movie?>(null) }

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
                        placeholder = { Text("Search for movies…") },
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
                    title = "Search Movies",
                    subtitle = "Type to search for movies to add."
                )
                searchResults.isEmpty() && !isSearching -> EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Results",
                    subtitle = "No movies found for \"$query\"."
                )
                else -> {
                    LazyColumn {
                        items(searchResults) { movie ->
                            val isAdded = existingMovies.any { it.tmdbId == movie.tmdbId }
                            MovieSearchRow(
                                movie = movie,
                                isAdded = isAdded,
                                onAdd = { if (!isAdded) showAddDialog = movie }
                            )
                        }
                    }
                }
            }
        }
    }

    showAddDialog?.let { movie ->
        AddMovieDialog(
            movie = movie,
            qualityProfiles = qualityProfiles,
            rootFolders = rootFolders,
            onAdd = { qpId, rfPath, monitored ->
                instance?.let { inst ->
                    viewModel.addMovie(inst, movie, qpId, rfPath, monitored)
                }
                showAddDialog = null
            },
            onDismiss = { showAddDialog = null }
        )
    }
}

@Composable
private fun MovieSearchRow(movie: Movie, isAdded: Boolean, onAdd: () -> Unit) {
    ListItem(
        headlineContent = { Text(movie.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                "${movie.year}${if (movie.studio != null) " · ${movie.studio}" else ""}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            AsyncImage(
                model = movie.remotePoster,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp)
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
private fun AddMovieDialog(
    movie: Movie,
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
        title = { Text("Add ${movie.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (qualityProfiles.isNotEmpty()) {
                    var qpExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = qpExpanded,
                        onExpandedChange = { qpExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedQp?.name ?: "Select quality profile",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Quality Profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qpExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = qpExpanded, onDismissRequest = { qpExpanded = false }) {
                            qualityProfiles.forEach { qp ->
                                DropdownMenuItem(
                                    text = { Text(qp.name) },
                                    onClick = { selectedQp = qp; qpExpanded = false }
                                )
                            }
                        }
                    }
                }
                if (rootFolders.isNotEmpty()) {
                    var rfExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = rfExpanded,
                        onExpandedChange = { rfExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRf?.path ?: "Select root folder",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Root Folder") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rfExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = rfExpanded, onDismissRequest = { rfExpanded = false }) {
                            rootFolders.forEach { rf ->
                                DropdownMenuItem(
                                    text = { Text(rf.path) },
                                    onClick = { selectedRf = rf; rfExpanded = false }
                                )
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
            ) { Text("Add Movie") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieReleasesScreen(
    movieId: Int,
    instance: Instance?,
    navController: NavController,
    viewModel: MoviesViewModel = viewModel()
) {
    val releases by viewModel.releases.collectAsState()
    val isLoading by viewModel.isLoadingReleases.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var sortBySize by remember { mutableStateOf(false) }

    LaunchedEffect(movieId, instance) {
        instance?.let { viewModel.loadReleases(it, movieId) }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    val sortedReleases = remember(releases, sortBySize) {
        if (sortBySize) releases.sortedByDescending { it.size }
        else releases.sortedWith(compareBy({ it.rejected }, { -it.age }))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Releases") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { sortBySize = !sortBySize }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
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
                isLoading -> LoadingOverlay()
                releases.isEmpty() -> EmptyState(
                    icon = Icons.Default.Download,
                    title = "No Releases",
                    subtitle = "No releases found."
                )
                else -> LazyColumn {
                    items(sortedReleases) { release ->
                        ReleaseRow(
                            release = release,
                            onDownload = {
                                instance?.let { inst ->
                                    viewModel.downloadRelease(inst, release)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseRow(release: MovieRelease, onDownload: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = release.quality.quality.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = release.sizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = release.indexer,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = release.ageFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (release.seeders != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${release.seeders}S",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "${release.leechers ?: 0}L",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (!release.rejected) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Rejected",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (release.rejected && release.rejections.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = release.rejections.joinToString(", ") { it.reason },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
