package com.checkarr.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkarr.data.models.*
import com.checkarr.navigation.Screen
import com.checkarr.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    instance: Instance?,
    navController: NavController,
    viewModel: SeriesViewModel = viewModel()
) {
    val allSeries by viewModel.series.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val error by viewModel.error.collectAsState()

    val series = allSeries.firstOrNull { it.id == seriesId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteFiles by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(series?.title ?: "Series") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    series?.let { s ->
                        instance?.let { inst ->
                            IconButton(onClick = { viewModel.toggleMonitor(inst, s) }) {
                                Icon(
                                    if (s.monitored) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (s.monitored) "Unmonitor" else "Monitor",
                                    tint = if (s.monitored) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { viewModel.searchSeries(inst, s.id) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Series") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { showMenu = false; showDeleteDialog = true }
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
        if (series == null) {
            LoadingOverlay()
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                error?.let {
                    ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
                }
                SeriesHero(series = series)
                SeriesInfoSection(series = series)
                SeriesSeasonsSection(
                    series = series,
                    onSeasonClick = { seasonNumber ->
                        navController.navigate(Screen.SeasonDetail.createRoute(series.id, seasonNumber))
                    }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Series") },
            text = {
                Column {
                    Text("Are you sure you want to delete \"${series?.title}\"?")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteFiles, onCheckedChange = { deleteFiles = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Delete series files")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        instance?.let { inst ->
                            series?.let { s ->
                                viewModel.deleteSeries(inst, s.id, deleteFiles)
                            }
                        }
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SeriesHero(series: Series) {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        series.remoteFanart?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)))
        )

        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.width(90.dp).height(135.dp)
            ) {
                series.remotePoster?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = series.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = series.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${series.year}${if (series.network != null) " · ${series.network}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (series.monitored) StatusBadge("Monitored", Color(0xFF22C55E))
                    else StatusBadge("Unmonitored", Color(0xFF6C6C70))
                    val statusColor = if (series.status == "ended") Color(0xFF6C6C70) else Color(0xFF22C55E)
                    StatusBadge(series.statusLabel, statusColor)
                }
            }
        }
    }
}

@Composable
private fun SeriesInfoSection(series: Series) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (series.overview.isNotBlank()) {
            Text(text = series.overview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
        if (series.genres.isNotEmpty()) {
            Text(text = series.genres.joinToString(", "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
        }
        SectionHeader("Details")
        HorizontalDivider()
        if (series.runtime > 0) MetadataRow("Runtime", series.runtimeFormatted)
        series.certification?.let { MetadataRow("Certification", it) }
        MetadataRow("Status", series.statusLabel)
        series.network?.let { MetadataRow("Network", it) }
        series.firstAired?.let { MetadataRow("First Aired", it.take(10)) }
        series.statistics?.let { stats ->
            MetadataRow("Episodes", "${stats.episodeFileCount} / ${stats.totalEpisodeCount}")
            if (stats.sizeOnDisk > 0) {
                val gb = stats.sizeOnDisk / (1024.0 * 1024.0 * 1024.0)
                MetadataRow("Size on Disk", "%.1f GB".format(gb))
            }
        }
        MetadataRow("Path", series.path)
    }
}

@Composable
private fun SeriesSeasonsSection(series: Series, onSeasonClick: (Int) -> Unit) {
    val visibleSeasons = series.seasons.filter { it.seasonNumber > 0 }.sortedByDescending { it.seasonNumber }
    if (visibleSeasons.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Seasons")
        visibleSeasons.forEach { season ->
            SeasonRow(season = season, onClick = { onSeasonClick(season.seasonNumber) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun SeasonRow(season: Season, onClick: () -> Unit) {
    val stats = season.statistics
    ListItem(
        headlineContent = { Text("Season ${season.seasonNumber}") },
        supportingContent = {
            stats?.let {
                Column {
                    Text(
                        "${it.episodeFileCount} / ${it.totalEpisodeCount} episodes",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (it.totalEpisodeCount > 0) {
                        LinearProgressIndicator(
                            progress = { (it.episodeFileCount.toFloat() / it.totalEpisodeCount.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (season.monitored) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Monitored", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Unmonitored", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDetailScreen(
    seriesId: Int,
    seasonNumber: Int,
    instance: Instance?,
    navController: NavController,
    viewModel: SeriesViewModel = viewModel()
) {
    val episodes by viewModel.episodes.collectAsState()
    val isLoading by viewModel.isLoadingEpisodes.collectAsState()
    val toast by viewModel.toast.collectAsState()

    LaunchedEffect(seriesId, seasonNumber, instance) {
        instance?.let { viewModel.loadEpisodes(it, seriesId, seasonNumber) }
    }

    val seasonEpisodes = episodes.filter { it.seasonNumber == seasonNumber }.sortedByDescending { it.episodeNumber }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Season $seasonNumber") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    instance?.let { inst ->
                        IconButton(onClick = {
                            val ids = seasonEpisodes.map { it.id }
                            viewModel.searchEpisodes(inst, ids)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Season")
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
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> LoadingOverlay()
                seasonEpisodes.isEmpty() -> EmptyState(
                    icon = Icons.Default.Tv,
                    title = "No Episodes",
                    subtitle = "No episodes found for this season."
                )
                else -> LazyColumn {
                    items(seasonEpisodes) { episode ->
                        EpisodeRow(
                            episode = episode,
                            onSearch = {
                                instance?.let { inst ->
                                    viewModel.searchEpisodes(inst, listOf(episode.id))
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onSearch: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = "${episode.episodeIdentifier} · ${episode.title.ifBlank { "TBA" }}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                episode.airDate?.let {
                    Text(it.take(10), style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (episode.hasFile) StatusBadge("Downloaded", Color(0xFF3B82F6))
                    else if (!episode.airDate.isNullOrBlank()) StatusBadge("Missing", Color(0xFFEF4444))
                    if (!episode.monitored) StatusBadge("Unmonitored", Color(0xFF6C6C70))
                }
            }
        },
        trailingContent = {
            if (!episode.hasFile) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
