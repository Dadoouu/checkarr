@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.checkarr.ui.qbittorrent

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.checkarr.data.models.*
import com.checkarr.data.repository.QBittorrentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QBittorrentViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = QBittorrentRepository()
    private val _torrents = MutableStateFlow<List<QBitTorrent>>(emptyList())
    private val _serverState = MutableStateFlow<QBitServerState?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _filter = MutableStateFlow("all")

    val isLoading: StateFlow<Boolean> = _isLoading
    val error: StateFlow<String?> = _error
    val filter: StateFlow<String> = _filter
    val serverState: StateFlow<QBitServerState?> = _serverState

    val torrents: StateFlow<List<QBitTorrent>> = combine(_torrents, _filter) { list, f ->
        when (f) {
            "downloading" -> list.filter { it.state == "downloading" || it.state == "stalledDL" }
            "uploading" -> list.filter { it.state == "uploading" || it.state == "stalledUP" }
            "paused" -> list.filter { it.state.contains("paused") }
            "error" -> list.filter { it.state == "error" || it.state == "missingFiles" }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFilter(f: String) { _filter.value = f }
    fun clearError() { _error.value = null }

    fun load(config: ServiceConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.login(config.url, config.username.ifBlank { "admin" }, config.password)
            repo.getTorrents(config.url).fold(
                onSuccess = { _torrents.value = it },
                onFailure = { _error.value = it.message }
            )
            repo.getServerState(config.url).onSuccess { _serverState.value = it.server_state }
            _isLoading.value = false
        }
    }

    fun pause(config: ServiceConfig, hash: String) {
        viewModelScope.launch { repo.pauseTorrent(config.url, hash); load(config) }
    }

    fun resume(config: ServiceConfig, hash: String) {
        viewModelScope.launch { repo.resumeTorrent(config.url, hash); load(config) }
    }

    fun delete(config: ServiceConfig, hash: String, deleteFiles: Boolean) {
        viewModelScope.launch { repo.deleteTorrent(config.url, hash, deleteFiles); load(config) }
    }
}

@Composable
fun QBittorrentScreen(config: ServiceConfig?, viewModel: QBittorrentViewModel = viewModel()) {
    val torrents by viewModel.torrents.collectAsState()
    val serverState by viewModel.serverState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val filter by viewModel.filter.collectAsState()

    LaunchedEffect(config) { config?.let { viewModel.load(it) } }

    if (config == null || !config.enabled) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("qBittorrent non configuré", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("qBittorrent") },
                actions = {
                    IconButton(onClick = { viewModel.load(config) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) { Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
            }

            // Speed bar
            serverState?.let { srv ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        SpeedItem(Icons.Default.Download, "↓ ${srv.dlSpeedFormatted}", Color(0xFF3B82F6))
                        SpeedItem(Icons.Default.Upload, "↑ ${srv.upSpeedFormatted}", Color(0xFF22C55E))
                        SpeedItem(Icons.Default.Download, "${torrents.size} torrents", MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Filter chips
            val filters = listOf("all" to "Tous", "downloading" to "DL", "uploading" to "UL", "paused" to "En pause", "error" to "Erreur")
            Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { viewModel.setFilter(key) }, label = { Text(label) })
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (torrents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun torrent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(torrents, key = { it.hash }) { torrent ->
                        TorrentRow(torrent = torrent, config = config, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TorrentRow(torrent: QBitTorrent, config: ServiceConfig, viewModel: QBittorrentViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val stateColor = Color(torrent.stateColor)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(torrent.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(stateColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text(torrent.stateLabel, style = MaterialTheme.typography.labelSmall, color = stateColor) }
                    Text(torrent.sizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (torrent.dlspeed > 0) Text("↓ ${torrent.dlSpeedFormatted}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF3B82F6))
                    if (torrent.upspeed > 0) Text("↑ ${torrent.upSpeedFormatted}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (torrent.state.contains("paused")) {
                        DropdownMenuItem(text = { Text("Reprendre") }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                            onClick = { viewModel.resume(config, torrent.hash); showMenu = false })
                    } else {
                        DropdownMenuItem(text = { Text("Pause") }, leadingIcon = { Icon(Icons.Default.Pause, null) },
                            onClick = { viewModel.pause(config, torrent.hash); showMenu = false })
                    }
                    DropdownMenuItem(text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showDeleteDialog = true; showMenu = false })
                }
            }
        }
        if (torrent.progress < 1f && torrent.state == "downloading") {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { torrent.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
            )
            Text("${(torrent.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(thickness = 0.5.dp)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer ?") },
            text = { Text(torrent.name) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(config, torrent.hash, true); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Supprimer avec fichiers") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.delete(config, torrent.hash, false); showDeleteDialog = false }) { Text("Garder fichiers") }
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
                }
            }
        )
    }
}
