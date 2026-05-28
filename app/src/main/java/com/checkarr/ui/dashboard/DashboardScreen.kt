@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.checkarr.data.models.*
import com.checkarr.ui.shared.SectionHeader

@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit = {},
    radarrInstance: Instance?,
    sonarrInstance: Instance?,
    jellyfinInstance: JellyfinInstance?,
    appConfig: AppConfig,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(radarrInstance, sonarrInstance, jellyfinInstance) {
        viewModel.refresh(radarrInstance, sonarrInstance, jellyfinInstance, appConfig)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableau de bord") },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.refresh(radarrInstance, sonarrInstance, jellyfinInstance, appConfig) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Service status cards row
            item {
                SectionHeader("Services")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceStatusChip("Radarr", radarrInstance != null, Color(0xFFFFAC33), modifier = Modifier.weight(1f))
                    ServiceStatusChip("Sonarr", sonarrInstance != null, Color(0xFF00D4FF), modifier = Modifier.weight(1f))
                    ServiceStatusChip("Jellyfin", jellyfinInstance != null, Color(0xFF00A4DC), modifier = Modifier.weight(1f))
                    val qbitEnabled = appConfig.serviceConfigs[ServiceType.QBITTORRENT.name]?.enabled == true
                    ServiceStatusChip("qBit", qbitEnabled, Color(0xFF6BBF4E), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            // Media stats
            if (radarrInstance != null || sonarrInstance != null) {
                item {
                    SectionHeader("Médiathèque")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (radarrInstance != null) {
                            StatCard(
                                icon = Icons.Default.Movie,
                                label = "Films",
                                value = "${state.movieCount}",
                                sub = "${state.movieMissing} manquants",
                                color = Color(0xFFFFAC33),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (sonarrInstance != null) {
                            StatCard(
                                icon = Icons.Default.Tv,
                                label = "Séries",
                                value = "${state.seriesCount}",
                                sub = "${state.sonarrQueue.size} en queue",
                                color = Color(0xFF00D4FF),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Active downloads
            val activeQueue = (state.movieQueue + state.sonarrQueue).filter {
                it.status.lowercase() == "downloading"
            }
            if (activeQueue.isNotEmpty()) {
                item { SectionHeader("Téléchargements en cours (${activeQueue.size})") }
                items(activeQueue.take(5)) { item ->
                    QueueItemRow(item)
                }
            }

            // qBittorrent stats
            state.qbitServerState?.let { srv ->
                item {
                    SectionHeader("qBittorrent")
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SpeedStat(Icons.Default.Download, "↓", srv.dlSpeedFormatted, Color(0xFF3B82F6))
                            SpeedStat(Icons.Default.Upload, "↑", srv.upSpeedFormatted, Color(0xFF22C55E))
                            val activeTorrents = state.torrents.count { it.state == "downloading" }
                            SpeedStat(Icons.Default.Download, "Actifs", "$activeTorrents", Color(0xFF6BBF4E))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Jellyfin sessions
            val activeSessions = state.jellyfinSessions.filter { it.nowPlayingItem != null }
            if (activeSessions.isNotEmpty()) {
                item { SectionHeader("Lectures Jellyfin (${activeSessions.size})") }
                items(activeSessions) { session ->
                    JellyfinSessionRow(session)
                }
            }

            // Pending Seerr requests
            if (state.pendingRequests.isNotEmpty()) {
                item { SectionHeader("Demandes en attente (${state.pendingRequests.size})") }
                items(state.pendingRequests.take(5)) { req ->
                    SeerrRequestRow(req)
                }
            }

            // Disk space
            val allDisks = state.radarrDisk.distinctBy { it.path }
            if (allDisks.isNotEmpty()) {
                item {
                    SectionHeader("Stockage")
                    allDisks.forEach { disk ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(disk.path.takeLast(30), style = MaterialTheme.typography.bodySmall)
                                    Text("${disk.freeFormatted} libre", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { disk.usedPercent },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                    color = when {
                                        disk.usedPercent > 0.9f -> MaterialTheme.colorScheme.error
                                        disk.usedPercent > 0.75f -> Color(0xFFF97316)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${disk.totalFormatted} total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Errors
            if (state.errors.isNotEmpty()) {
                item {
                    SectionHeader("Erreurs")
                    state.errors.forEach { (service, error) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("$service: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceStatusChip(name: String, connected: Boolean, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (connected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(8.dp).background(
                    if (connected) color else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, color = if (connected) color else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatCard(icon: ImageVector, label: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun SpeedStat(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QueueItemRow(item: QueueItem) {
    ListItem(
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Column {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(2.dp))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    Text(item.sizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        leadingContent = {
            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
}

@Composable
fun JellyfinSessionRow(session: JellyfinSession) {
    val item = session.nowPlayingItem ?: return
    ListItem(
        headlineContent = { Text(item.displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Text(
                "${session.userName} · ${session.client}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                if (session.playState?.isPaused == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF00A4DC),
                modifier = Modifier.size(20.dp)
            )
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
}

@Composable
fun SeerrRequestRow(req: SeerrRequest) {
    ListItem(
        headlineContent = { Text("Demande #${req.id}", style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Text(
                "Par ${req.requestedBy?.displayName ?: "Inconnu"} · ${req.statusLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
}
