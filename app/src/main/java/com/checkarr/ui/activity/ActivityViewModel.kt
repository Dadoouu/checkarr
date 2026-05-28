package com.checkarr.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import com.checkarr.data.repository.RadarrRepository
import com.checkarr.data.repository.SonarrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ActivityTab { QUEUE, HISTORY }

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val radarrRepo = RadarrRepository(ApiClient())
    private val sonarrRepo = SonarrRepository(ApiClient())

    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _activeTab = MutableStateFlow(ActivityTab.QUEUE)
    private val _toast = MutableStateFlow<String?>(null)

    val queueItems: StateFlow<List<QueueItem>> = _queueItems
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems
    val isLoading: StateFlow<Boolean> = _isLoading
    val error: StateFlow<String?> = _error
    val activeTab: StateFlow<ActivityTab> = _activeTab
    val toast: StateFlow<String?> = _toast

    fun loadQueue(radarrInstance: Instance?, sonarrInstance: Instance?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val items = mutableListOf<QueueItem>()
            radarrInstance?.let { inst ->
                radarrRepo.getQueue(inst).onSuccess { items.addAll(it.records) }
            }
            sonarrInstance?.let { inst ->
                sonarrRepo.getQueue(inst).onSuccess { items.addAll(it.records) }
            }
            _queueItems.value = items
            _isLoading.value = false
        }
    }

    fun loadHistory(radarrInstance: Instance?, sonarrInstance: Instance?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val items = mutableListOf<HistoryItem>()
            radarrInstance?.let { inst ->
                radarrRepo.getHistory(inst).onSuccess { items.addAll(it.records) }
            }
            sonarrInstance?.let { inst ->
                sonarrRepo.getHistory(inst).onSuccess { items.addAll(it.records) }
            }
            _historyItems.value = items.sortedByDescending { it.date }
            _isLoading.value = false
        }
    }

    fun removeFromQueue(radarrInstance: Instance?, sonarrInstance: Instance?, item: QueueItem, blacklist: Boolean) {
        viewModelScope.launch {
            val inst = if (item.movieId != null) radarrInstance else sonarrInstance
            inst?.let {
                val repo = if (item.movieId != null) radarrRepo else sonarrRepo
                if (item.movieId != null) {
                    radarrRepo.removeQueueItem(it, item.id, blacklist).fold(
                        onSuccess = {
                            _queueItems.value = _queueItems.value.filter { q -> q.id != item.id }
                            _toast.value = "Removed from queue"
                        },
                        onFailure = { e -> _error.value = e.message }
                    )
                } else {
                    sonarrRepo.removeQueueItem(it, item.id, blacklist).fold(
                        onSuccess = {
                            _queueItems.value = _queueItems.value.filter { q -> q.id != item.id }
                            _toast.value = "Removed from queue"
                        },
                        onFailure = { e -> _error.value = e.message }
                    )
                }
            }
        }
    }

    fun setActiveTab(tab: ActivityTab) { _activeTab.value = tab }
    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }
}
