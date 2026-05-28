package com.checkarr.ui.activity

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
import com.checkarr.data.models.HistoryItem
import com.checkarr.data.models.Instance
import com.checkarr.data.models.QueueItem
import com.checkarr.navigation.Screen
import com.checkarr.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    radarrInstance: Instance?,
    sonarrInstance: Instance?,
    navController: NavController,
    viewModel: ActivityViewModel = viewModel()
) {
    val queueItems by viewModel.queueItems.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val error by viewModel.error.collectAsState()
    val toast by viewModel.toast.collectAsState()

    LaunchedEffect(radarrInstance, sonarrInstance) {
        viewModel.loadQueue(radarrInstance, sonarrInstance)
    }

    LaunchedEffect(activeTab) {
        if (activeTab == ActivityTab.HISTORY) {
            viewModel.loadHistory(radarrInstance, sonarrInstance)
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
                    title = { Text("Activity") },
                    actions = {
                        IconButton(onClick = {
                            if (activeTab == ActivityTab.QUEUE) viewModel.loadQueue(radarrInstance, sonarrInstance)
                            else viewModel.loadHistory(radarrInstance, sonarrInstance)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
                TabRow(selectedTabIndex = activeTab.ordinal) {
                    Tab(
                        selected = activeTab == ActivityTab.QUEUE,
                        onClick = { viewModel.setActiveTab(ActivityTab.QUEUE) },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Queue")
                                if (queueItems.isNotEmpty()) {
                                    Badge { Text(queueItems.size.toString()) }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == ActivityTab.HISTORY,
                        onClick = { viewModel.setActiveTab(ActivityTab.HISTORY) },
                        text = { Text("History") }
                    )
                }
            }
        },
        snackbarHost = {
            if (toast != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(toast!!) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                radarrInstance == null && sonarrInstance == null -> NoInstanceConfigured("Radarr or Sonarr") {
                    navController.navigate(Screen.Settings.route)
                }
                isLoading -> LoadingOverlay()
                else -> {
                    Column {
                        error?.let {
                            ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
                        }
                        when (activeTab) {
                            ActivityTab.QUEUE -> QueueTab(
                                items = queueItems,
                                onRemove = { item, blacklist ->
                                    viewModel.removeFromQueue(radarrInstance, sonarrInstance, item, blacklist)
                                }
                            )
                            ActivityTab.HISTORY -> HistoryTab(items = historyItems)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTab(items: List<QueueItem>, onRemove: (QueueItem, Boolean) -> Unit) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CloudDownload,
            title = "Queue Empty",
            subtitle = "No downloads in progress."
        )
    } else {
        LazyColumn {
            items(items, key = { it.id }) { item ->
                QueueItemRow(item = item, onRemove = { blacklist -> onRemove(item, blacklist) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun QueueItemRow(item: QueueItem, onRemove: (Boolean) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = queueStatusColor(item.status)
                    )
                    item.timeleft?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = item.sizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.status.lowercase() == "downloading") {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                if (item.statusMessages.isNotEmpty()) {
                    item.statusMessages.firstOrNull()?.messages?.firstOrNull()?.let { msg ->
                        Text(text = msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { showMenu = false; onRemove(false) }
                    )
                    DropdownMenuItem(
                        text = { Text("Blacklist & Remove") },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemove(true) }
                    )
                }
            }
        }
    )
}

@Composable
private fun queueStatusColor(status: String): Color = when (status.lowercase()) {
    "downloading" -> MaterialTheme.colorScheme.primary
    "completed" -> Color(0xFF22C55E)
    "failed" -> MaterialTheme.colorScheme.error
    "warning" -> Color(0xFFF97316)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun HistoryTab(items: List<HistoryItem>) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            title = "No History",
            subtitle = "No recent activity found."
        )
    } else {
        LazyColumn {
            items(items, key = { it.id }) { item ->
                HistoryItemRow(item = item)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryItemRow(item: HistoryItem) {
    ListItem(
        headlineContent = {
            Text(item.sourceTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.eventTypeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = historyEventColor(item.eventType)
                )
                item.quality?.let { q ->
                    Text(
                        text = q.quality.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = item.date.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = historyEventIcon(item.eventType),
                contentDescription = null,
                tint = historyEventColor(item.eventType)
            )
        }
    )
}

@Composable
private fun historyEventColor(eventType: String): Color = when (eventType.lowercase()) {
    "grabbed" -> MaterialTheme.colorScheme.primary
    "downloadfolderimported", "moviefileimported" -> Color(0xFF22C55E)
    "downloadfailed" -> MaterialTheme.colorScheme.error
    "moviefiledeleted" -> Color(0xFFF97316)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun historyEventIcon(eventType: String) = when (eventType.lowercase()) {
    "grabbed" -> Icons.Default.Download
    "downloadfolderimported", "moviefileimported" -> Icons.Default.CheckCircle
    "downloadfailed" -> Icons.Default.Error
    "moviefiledeleted" -> Icons.Default.Delete
    else -> Icons.Default.History
}
