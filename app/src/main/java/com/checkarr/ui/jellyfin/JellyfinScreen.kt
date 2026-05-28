@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.jellyfin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.navigation.NavController
import com.checkarr.data.models.*
import com.checkarr.ui.shared.EmptyState
import com.checkarr.ui.shared.ErrorBanner
import com.checkarr.ui.shared.LoadingOverlay
import com.checkarr.ui.shared.NoInstanceConfigured
import com.checkarr.ui.shared.SectionHeader

@Composable
fun JellyfinScreen(
    instance: JellyfinInstance?,
    navController: NavController,
    viewModel: JellyfinViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    LaunchedEffect(instance) {
        instance?.let { viewModel.load(it) }
    }

    if (instance == null) {
        NoInstanceConfigured("Jellyfin") {
            navController.navigate("settings")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jellyfin") },
                actions = {
                    IconButton(onClick = { viewModel.load(instance) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            error?.let {
                ErrorBanner(it) { viewModel.clearError() }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp
            ) {
                JellyfinTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(tab.label) },
                        icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            if (isLoading) {
                LoadingOverlay()
            } else {
                when (selectedTab) {
                    JellyfinTab.SESSIONS -> SessionsTab(viewModel, instance)
                    JellyfinTab.RECENT -> RecentTab(viewModel, instance)
                    JellyfinTab.RESUME -> ResumeTab(viewModel, instance)
                    JellyfinTab.ACTIVITY -> ActivityTab(viewModel)
                    JellyfinTab.STATS -> StatsTab(viewModel)
                }
            }
        }
    }
}

val JellyfinTab.label: String get() = when (this) {
    JellyfinTab.SESSIONS -> "En cours"
    JellyfinTab.RECENT -> "Récents"
    JellyfinTab.RESUME -> "Reprendre"
    JellyfinTab.ACTIVITY -> "Activité"
    JellyfinTab.STATS -> "Stats"
}

val JellyfinTab.icon: ImageVector get() = when (this) {
    JellyfinTab.SESSIONS -> Icons.Default.PlayCircle
    JellyfinTab.RECENT -> Icons.Default.NewReleases
    JellyfinTab.RESUME -> Icons.Default.History
    JellyfinTab.ACTIVITY -> Icons.Default.List
    JellyfinTab.STATS -> Icons.Default.BarChart
}

// ── Sessions ────────────────────────────────────────────────────────────────

@Composable
fun SessionsTab(viewModel: JellyfinViewModel, instance: JellyfinInstance) {
    val sessions by viewModel.sessions.collectAsState()
    val active = sessions.filter { it.nowPlayingItem != null }

    if (active.isEmpty()) {
        EmptyState(
            icon = Icons.Default.PlayCircleOutline,
            title = "Aucune lecture en cours",
            subtitle = "Personne ne regarde quelque chose en ce moment."
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(active) { session ->
            SessionCard(session = session, baseUrl = instance.url)
        }
    }
}

@Composable
fun SessionCard(session: JellyfinSession, baseUrl: String) {
    val item = session.nowPlayingItem ?: return
    val playState = session.playState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${session.client} · ${session.deviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (playState?.isPaused == true) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("En pause", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Badge(containerColor = Color(0xFF22C55E).copy(alpha = 0.15f)) {
                        Text("▶ Lecture", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.runTimeTicks > 0 && playState != null) {
                val progress = playState.positionTicks.toFloat() / item.runTimeTicks
                val posMin = (playState.positionTicks / 600_000_000).toInt()
                val totalMin = item.runtimeMinutes
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = if (playState.isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${posMin}min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${totalMin}min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Recently Added ───────────────────────────────────────────────────────────

@Composable
fun RecentTab(viewModel: JellyfinViewModel, instance: JellyfinInstance) {
    val items by viewModel.recentlyAdded.collectAsState()

    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.NewReleases,
            title = "Aucun ajout récent",
            subtitle = "Les médias récemment ajoutés apparaîtront ici."
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(items) { item ->
            JellyfinItemRow(item = item, baseUrl = instance.url)
        }
    }
}

// ── Resume ───────────────────────────────────────────────────────────────────

@Composable
fun ResumeTab(viewModel: JellyfinViewModel, instance: JellyfinInstance) {
    val items by viewModel.resumeItems.collectAsState()

    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            title = "Rien à reprendre",
            subtitle = "Les lectures en cours apparaîtront ici."
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(items) { item ->
            val progress = item.userData?.let { ud ->
                if (item.runTimeTicks > 0) ud.playCount.toFloat() else 0f
            } ?: 0f
            JellyfinItemRow(item = item, baseUrl = instance.url)
        }
    }
}

@Composable
fun JellyfinItemRow(item: JellyfinItem, baseUrl: String) {
    val typeIcon = when (item.type) {
        "Movie" -> Icons.Default.Movie
        "Episode" -> Icons.Default.Tv
        else -> Icons.Default.VideoFile
    }
    val typeColor = when (item.type) {
        "Movie" -> Color(0xFFFFAC33)
        "Episode" -> Color(0xFF00D4FF)
        else -> MaterialTheme.colorScheme.primary
    }

    ListItem(
        headlineContent = {
            Text(item.displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item.productionYear?.let { Text("$it", style = MaterialTheme.typography.labelSmall) }
                if (item.runtimeMinutes > 0) {
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.runtimeMinutes}min", style = MaterialTheme.typography.labelSmall)
                }
                item.communityRating?.let { rating ->
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFEAB308))
                    Text(String.format("%.1f", rating), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp))
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
}

// ── Activity Log ─────────────────────────────────────────────────────────────

@Composable
fun ActivityTab(viewModel: JellyfinViewModel) {
    val log by viewModel.activityLog.collectAsState()

    if (log.isEmpty()) {
        EmptyState(
            icon = Icons.Default.List,
            title = "Aucune activité",
            subtitle = "Le journal d'activité du serveur apparaîtra ici."
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(log) { entry ->
            ActivityEntryRow(entry)
        }
    }
}

@Composable
fun ActivityEntryRow(entry: JellyfinActivity) {
    val (icon, color) = when {
        entry.type.contains("AuthenticationSuccess", ignoreCase = true) -> Icons.Default.Login to Color(0xFF22C55E)
        entry.type.contains("AuthenticationFailed", ignoreCase = true) -> Icons.Default.NoEncryption to Color(0xFFEF4444)
        entry.type.contains("PlaybackStart", ignoreCase = true) -> Icons.Default.PlayArrow to Color(0xFF3B82F6)
        entry.type.contains("PlaybackStop", ignoreCase = true) -> Icons.Default.Stop to Color(0xFF6B7280)
        entry.severity == "Error" -> Icons.Default.Error to Color(0xFFEF4444)
        entry.severity == "Warning" -> Icons.Default.Warning to Color(0xFFF97316)
        else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = {
            Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Column {
                if (entry.shortOverview.isNotBlank()) {
                    Text(entry.shortOverview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    entry.userName?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatJellyfinDate(entry.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), thickness = 0.5.dp)
}

// ── Stats ────────────────────────────────────────────────────────────────────

@Composable
fun StatsTab(viewModel: JellyfinViewModel) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val drives by viewModel.drives.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {

        // Server info
        systemInfo?.let { info ->
            item {
                SectionHeader("Serveur")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatRow(Icons.Default.Dns, "Nom", info.serverName)
                        StatRow(Icons.Default.Info, "Version", info.version)
                        StatRow(Icons.Default.Computer, "Système", info.operatingSystem)
                    }
                }
            }
        }

        // Active sessions count
        item {
            SectionHeader("Activité")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val activeSessions = sessions.count { it.nowPlayingItem != null }
                    StatRow(Icons.Default.PlayCircle, "Lectures actives", "$activeSessions")
                    StatRow(Icons.Default.People, "Sessions totales", "${sessions.size}")
                    StatRow(Icons.Default.NewReleases, "Ajouts récents", "${recentlyAdded.size}")
                }
            }
        }

        // Disk usage
        if (drives.isNotEmpty()) {
            item { SectionHeader("Stockage") }
            items(drives) { drive ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(drive.name.ifBlank { drive.path }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                "${drive.formatSize(drive.freeSpace)} libre",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { drive.usedPercent.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = when {
                                drive.usedPercent > 0.9f -> MaterialTheme.colorScheme.error
                                drive.usedPercent > 0.75f -> Color(0xFFF97316)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(drive.formatSize(drive.usedSpace), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(drive.formatSize(drive.totalSpace), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

fun formatJellyfinDate(dateStr: String): String {
    return try {
        val instant = java.time.Instant.parse(dateStr)
        val local = java.time.ZoneId.systemDefault().let { instant.atZone(it) }
        "${local.dayOfMonth}/${local.monthValue} ${local.hour}:${local.minute.toString().padStart(2,'0')}"
    } catch (e: Exception) {
        dateStr.take(16).replace("T", " ")
    }
}
