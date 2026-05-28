@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkarr.data.models.Instance
import com.checkarr.data.models.Movie
import com.checkarr.navigation.Screen
import com.checkarr.ui.shared.*

@Composable
fun MoviesScreen(
    instance: Instance?,
    navController: NavController,
    viewModel: MoviesViewModel = viewModel()
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(instance) {
        instance?.let { viewModel.load(it) }
    }
    LaunchedEffect(toast) {
        if (toast != null) { kotlinx.coroutines.delay(2000); viewModel.clearToast() }
    }

    Scaffold(
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearch(it) },
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Rechercher des films…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Trier")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                if (filter != MovieFilter.ALL) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                                contentDescription = "Filtrer"
                            )
                        }
                        IconButton(onClick = { navController.navigate(Screen.MovieSearch.route) }) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {}
        },
        snackbarHost = {
            if (toast != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(toast!!) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                instance == null -> NoInstanceConfigured("Radarr") { navController.navigate(Screen.Settings.route) }
                isLoading && movies.isEmpty() -> LoadingOverlay()
                else -> {
                    Column {
                        error?.let { ErrorBanner(it) { viewModel.clearError() } }
                        if (movies.isEmpty() && !isLoading) {
                            EmptyState(
                                icon = Icons.Default.Movie,
                                title = "Aucun film",
                                subtitle = if (filter != MovieFilter.ALL) "Aucun film ne correspond au filtre." else "Aucun film dans cette instance."
                            )
                        } else {
                            // Stats bar
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val downloaded = movies.count { it.hasFile }
                                val missing = movies.count { it.monitored && !it.hasFile }
                                Text("${movies.size} films", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$downloaded téléchargés", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
                                if (missing > 0) {
                                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$missing manquants", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                            LazyColumn {
                                items(movies, key = { it.id }) { movie ->
                                    MovieListRow(movie = movie, onClick = {
                                        navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Filtrer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                MovieFilter.values().forEach { f ->
                    ListItem(
                        headlineContent = { Text(filterLabel(f)) },
                        leadingContent = {
                            if (filter == f) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            else Spacer(Modifier.size(24.dp))
                        },
                        modifier = Modifier.clickable { viewModel.setFilter(f); showFilterSheet = false }
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Trier par", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                MovieSortOption.values().forEach { s ->
                    ListItem(
                        headlineContent = { Text(sortLabel(s)) },
                        leadingContent = {
                            if (sortOption == s) {
                                Icon(
                                    if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null, tint = MaterialTheme.colorScheme.primary
                                )
                            } else Spacer(Modifier.size(24.dp))
                        },
                        modifier = Modifier.clickable { viewModel.setSort(s); showSortSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieListRow(movie: Movie, onClick: () -> Unit) {
    val statusColor = when {
        movie.hasFile -> Color(0xFF22C55E)
        movie.monitored -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Poster
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (movie.remotePoster != null) {
                AsyncImage(
                    model = movie.remotePoster,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${movie.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (movie.runtime > 0) {
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(movie.runtimeFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (movie.genres.isNotEmpty()) {
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(movie.genres.take(2).joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            movie.hasFile -> "Téléchargé"
                            movie.monitored -> "Manquant"
                            else -> "Non suivi"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                movie.movieFile?.quality?.quality?.name?.let { quality ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(quality, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (movie.sizeOnDisk > 0) {
                    Text(movie.fileSizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Chevron
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(start = 80.dp), thickness = 0.5.dp)
}

private fun filterLabel(f: MovieFilter) = when (f) {
    MovieFilter.ALL -> "Tous"
    MovieFilter.MONITORED -> "Suivi"
    MovieFilter.UNMONITORED -> "Non suivi"
    MovieFilter.MISSING -> "Manquants"
    MovieFilter.DOWNLOADED -> "Téléchargés"
}

private fun sortLabel(s: MovieSortOption) = when (s) {
    MovieSortOption.TITLE -> "Titre"
    MovieSortOption.YEAR -> "Année"
    MovieSortOption.ADDED -> "Ajouté"
    MovieSortOption.SIZE -> "Taille"
    MovieSortOption.STATUS -> "Statut"
}
