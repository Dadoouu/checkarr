package com.checkarr.ui.prowlarr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import com.checkarr.data.repository.ProwlarrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProwlarrViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ProwlarrRepository(ApiClient())

    private val _indexers = MutableStateFlow<List<ProwlarrIndexer>>(emptyList())
    private val _indexerStats = MutableStateFlow<ProwlarrIndexerStats?>(null)
    private val _searchResults = MutableStateFlow<List<ProwlarrSearchResult>>(emptyList())
    private val _history = MutableStateFlow<ProwlarrHistoryResponse?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _toast = MutableStateFlow<String?>(null)

    val indexers: StateFlow<List<ProwlarrIndexer>> = _indexers
    val indexerStats: StateFlow<ProwlarrIndexerStats?> = _indexerStats
    val searchResults: StateFlow<List<ProwlarrSearchResult>> = _searchResults
    val history: StateFlow<ProwlarrHistoryResponse?> = _history
    val isLoading: StateFlow<Boolean> = _isLoading
    val isSearching: StateFlow<Boolean> = _isSearching
    val error: StateFlow<String?> = _error
    val toast: StateFlow<String?> = _toast

    fun load(instance: Instance) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getIndexers(instance).fold(
                onSuccess = { _indexers.value = it },
                onFailure = { _error.value = it.message }
            )
            repo.getIndexerStats(instance).onSuccess { _indexerStats.value = it }
            _isLoading.value = false
        }
    }

    fun search(instance: Instance, query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            repo.search(instance, query).fold(
                onSuccess = { _searchResults.value = it },
                onFailure = { _error.value = it.message }
            )
            _isSearching.value = false
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    fun loadHistory(instance: Instance) {
        viewModelScope.launch {
            repo.getHistory(instance).onSuccess { _history.value = it }
        }
    }

    fun deleteIndexer(instance: Instance, id: Int) {
        viewModelScope.launch {
            repo.deleteIndexer(instance, id).fold(
                onSuccess = {
                    _indexers.value = _indexers.value.filter { it.id != id }
                    _toast.value = "Indexer deleted"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun testAllIndexers(instance: Instance) {
        viewModelScope.launch {
            repo.testAllIndexers(instance).fold(
                onSuccess = { _toast.value = "Test all indexers started" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }
}
