@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.series

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
import androidx.navigation.NavController
import com.checkarr.data.models.Instance
import com.checkarr.data.models.MovieRelease
import com.checkarr.ui.shared.*

@Composable
fun EpisodeReleasesScreen(
    episodeId: Int,
    instance: Instance?,
    navController: NavController,
    viewModel: SeriesViewModel = viewModel()
) {
    val releases by viewModel.episodeReleases.collectAsState()
    val isLoading by viewModel.isLoadingEpisodeReleases.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val error by viewModel.error.collectAsState()

    var filterRejected by remember { mutableStateOf(false) }
    var sortBySize by remember { mutableStateOf(false) }

    LaunchedEffect(episodeId, instance) {
        instance?.let { viewModel.loadEpisodeReleases(it, episodeId) }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    val filteredReleases = releases
        .let { if (filterRejected) it.filter { r -> !r.rejected } else it }
        .let { if (sortBySize) it.sortedByDescending { r -> r.size } else it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interactive Search") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { filterRejected = !filterRejected }) {
                        Icon(
                            if (filterRejected) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = "Filter Rejected",
                            tint = if (filterRejected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { sortBySize = !sortBySize }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort",
                            tint = if (sortBySize) MaterialTheme.colorScheme.primary else LocalContentColor.current)
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
            error?.let {
                ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
            }
            when {
                isLoading -> LoadingOverlay()
                filteredReleases.isEmpty() -> EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Releases",
                    subtitle = if (filterRejected) "No approved releases found." else "No releases found for this episode."
                )
                else -> LazyColumn {
                    item {
                        Text(
                            text = "${filteredReleases.size} release(s) found",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredReleases) { release ->
                        EpisodeReleaseItem(
                            release = release,
                            onDownload = {
                                instance?.let { inst ->
                                    viewModel.downloadEpisodeRelease(inst, release)
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
private fun EpisodeReleaseItem(release: MovieRelease, onDownload: () -> Unit) {
    var showRejections by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = release.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = if (release.rejected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(release.quality.quality.name, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(release.sizeFormatted, style = MaterialTheme.typography.labelSmall)
                    Text(release.ageFormatted, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(release.indexer, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary)
                    if (release.protocol == "torrent") {
                        release.seeders?.let {
                            Text("↑$it", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
                        }
                        release.leechers?.let {
                            Text("↓$it", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                        }
                    }
                }
                if (release.rejected && release.rejections.isNotEmpty()) {
                    TextButton(
                        onClick = { showRejections = !showRejections },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            if (showRejections) "Hide rejections" else "Show ${release.rejections.size} rejection(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (showRejections) {
                        release.rejections.forEach { rejection ->
                            Text("• ${rejection.reason}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        trailingContent = {
            if (!release.rejected) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Icon(Icons.Default.Block, contentDescription = "Rejected",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = if (release.rejected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            ListItemDefaults.colors()
        }
    )
}
