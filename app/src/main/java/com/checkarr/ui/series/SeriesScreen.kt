@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.series

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
import com.checkarr.data.models.Series
import com.checkarr.navigation.Screen
import com.checkarr.ui.shared.*

@Composable
fun SeriesScreen(
    instance: Instance?,
    navController: NavController,
    viewModel: SeriesViewModel = viewModel()
) {
    val series by viewModel.series.collectAsState()
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
                placeholder = { Text("Rechercher des séries…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showSortSheet = true }) { Icon(Icons.Default.Sort, contentDescription = "Trier") }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(if (filter != SeriesFilter.ALL) Icons.Default.FilterAlt else Icons.Default.FilterAltOff, contentDescription = "Filtrer")
                        }
                        IconButton(onClick = { navController.navigate(Screen.SeriesSearch.route) }) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {}
        },
        snackbarHost = {
            if (toast != null) { Snackbar(modifier = Modifier.padding(16.dp)) { Text(toast!!) } }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                instance == null -> NoInstanceConfigured("Sonarr") { navController.navigate(Screen.Settings.route) }
                isLoading && series.isEmpty() -> LoadingOverlay()
                else -> {
                    Column {
                        error?.let { ErrorBanner(it) { viewModel.clearError() } }
                        if (series.isEmpty() && !isLoading) {
                            EmptyState(icon = Icons.Default.Tv, title = "Aucune série", subtitle = "Aucune série dans cette instance.")
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("${series.size} séries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val continuing = series.count { it.status == "continuing" }
                                Text("$continuing en cours", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00D4FF))
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                            LazyColumn {
                                items(series, key = { it.id }) { s ->
                                    SeriesListRow(series = s, onClick = {
                                        navController.navigate(Screen.SeriesDetail.createRoute(s.id))
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
                SeriesFilter.values().forEach { f ->
                    ListItem(
                        headlineContent = { Text(seriesFilterLabel(f)) },
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
                SeriesSortOption.values().forEach { s ->
                    ListItem(
                        headlineContent = { Text(seriesSortLabel(s)) },
                        leadingContent = {
                            if (sortOption == s) {
                                Icon(if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
fun SeriesListRow(series: Series, onClick: () -> Unit) {
    val statusColor = when (series.status) {
        "continuing" -> Color(0xFF22C55E)
        "ended" -> Color(0xFF6B7280)
        else -> MaterialTheme.colorScheme.primary
    }
    val episodeProgress = if ((series.statistics?.totalEpisodeCount ?: 0) > 0)
        (series.statistics?.episodeFileCount ?: 0).toFloat() / (series.statistics?.totalEpisodeCount ?: 1) else 0f

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.width(52.dp).height(78.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val poster = series.images.firstOrNull { it.coverType == "poster" }?.remoteUrl
            if (poster != null) {
                AsyncImage(model = poster, contentDescription = series.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(series.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${series.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (series.status == "continuing") "En cours" else "Terminée", style = MaterialTheme.typography.bodySmall, color = statusColor)
                series.network?.let {
                    Text("· $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            series.statistics?.let { stats ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { episodeProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = if (episodeProgress >= 1f) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${stats.episodeFileCount}/${stats.totalEpisodeCount} épisodes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(start = 80.dp), thickness = 0.5.dp)
}

private fun seriesFilterLabel(f: SeriesFilter): String = when (f) {
    SeriesFilter.ALL -> "Toutes"
    SeriesFilter.MONITORED -> "Suivies"
    SeriesFilter.UNMONITORED -> "Non suivies"
    SeriesFilter.CONTINUING -> "En cours"
    SeriesFilter.ENDED -> "Terminées"
    else -> f.name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun seriesSortLabel(s: SeriesSortOption) = when (s) {
    SeriesSortOption.TITLE -> "Titre"
    SeriesSortOption.YEAR -> "Année"
    SeriesSortOption.ADDED -> "Ajouté"
    SeriesSortOption.STATUS -> "Statut"
    else -> s.name.lowercase().replaceFirstChar { it.uppercase() }
}

