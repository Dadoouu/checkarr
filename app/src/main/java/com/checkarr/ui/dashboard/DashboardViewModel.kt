package com.checkarr.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardState(
    val isLoading: Boolean = false,
    // Radarr
    val movieCount: Int = 0,
    val movieMissing: Int = 0,
    val movieQueue: List<QueueItem> = emptyList(),
    val radarrDisk: List<DiskSpace> = emptyList(),
    // Sonarr
    val seriesCount: Int = 0,
    val episodesMissing: Int = 0,
    val sonarrQueue: List<QueueItem> = emptyList(),
    val sonarrDisk: List<DiskSpace> = emptyList(),
    // Jellyfin
    val jellyfinSessions: List<JellyfinSession> = emptyList(),
    val jellyfinSystemInfo: JellyfinSystemInfo? = null,
    // qBittorrent
    val torrents: List<QBitTorrent> = emptyList(),
    val qbitServerState: QBitServerState? = null,
    // Seerr
    val pendingRequests: List<SeerrRequest> = emptyList(),
    // Errors per service
    val errors: Map<String, String> = emptyMap()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val radarrRepo = RadarrRepository(com.checkarr.data.network.ApiClient())
    private val sonarrRepo = SonarrRepository(com.checkarr.data.network.ApiClient())
    private val jellyfinRepo = JellyfinRepository()
    private val qbitRepo = QBittorrentRepository()
    private val seerrRepo = SeerrRepository()

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    fun refresh(
        radarrInstance: Instance?,
        sonarrInstance: Instance?,
        jellyfinInstance: JellyfinInstance?,
        appConfig: AppConfig
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val errors = mutableMapOf<String, String>()

            // Radarr
            if (radarrInstance != null) {
                launch {
                    radarrRepo.getMovies(radarrInstance).onSuccess { movies ->
                        _state.value = _state.value.copy(
                            movieCount = movies.size,
                            movieMissing = movies.count { it.monitored && !it.hasFile }
                        )
                    }.onFailure { errors["radarr"] = it.message ?: "Erreur Radarr" }
                }
                launch {
                    radarrRepo.getQueue(radarrInstance).onSuccess {
                        _state.value = _state.value.copy(movieQueue = it.records)
                    }
                }
                launch {
                    radarrRepo.getDiskSpace(radarrInstance).onSuccess {
                        _state.value = _state.value.copy(radarrDisk = it)
                    }
                }
            }

            // Sonarr
            if (sonarrInstance != null) {
                launch {
                    sonarrRepo.getSeries(sonarrInstance).onSuccess { series ->
                        _state.value = _state.value.copy(seriesCount = series.size)
                    }.onFailure { errors["sonarr"] = it.message ?: "Erreur Sonarr" }
                }
                launch {
                    sonarrRepo.getQueue(sonarrInstance).onSuccess {
                        _state.value = _state.value.copy(sonarrQueue = it.records)
                    }
                }
                launch {
                    sonarrRepo.getDiskSpace(sonarrInstance).onSuccess {
                        _state.value = _state.value.copy(sonarrDisk = it)
                    }
                }
            }

            // Jellyfin
            if (jellyfinInstance != null) {
                launch {
                    jellyfinRepo.getSessions(jellyfinInstance).onSuccess {
                        _state.value = _state.value.copy(jellyfinSessions = it)
                    }
                }
                launch {
                    jellyfinRepo.getSystemInfo(jellyfinInstance).onSuccess {
                        _state.value = _state.value.copy(jellyfinSystemInfo = it)
                    }
                }
            }

            // qBittorrent
            val qbitConfig = appConfig.serviceConfigs[ServiceType.QBITTORRENT.name]
            if (qbitConfig?.enabled == true && qbitConfig.url.isNotBlank()) {
                launch {
                    qbitRepo.login(qbitConfig.url, "admin", qbitConfig.password)
                    qbitRepo.getTorrents(qbitConfig.url).onSuccess {
                        _state.value = _state.value.copy(torrents = it)
                    }.onFailure { errors["qbit"] = it.message ?: "Erreur qBittorrent" }
                    qbitRepo.getServerState(qbitConfig.url).onSuccess {
                        _state.value = _state.value.copy(qbitServerState = it.server_state)
                    }
                }
            }

            // Seerr
            val seerrConfig = appConfig.serviceConfigs[ServiceType.SEERR.name]
            if (seerrConfig?.enabled == true && seerrConfig.url.isNotBlank() && seerrConfig.apiKey.isNotBlank()) {
                launch {
                    seerrRepo.getPendingRequests(seerrConfig.url, seerrConfig.apiKey).onSuccess {
                        _state.value = _state.value.copy(pendingRequests = it.results)
                    }
                }
            }

            _state.value = _state.value.copy(isLoading = false, errors = errors)
        }
    }
}
