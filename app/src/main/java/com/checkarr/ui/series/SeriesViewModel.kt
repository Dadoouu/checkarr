package com.checkarr.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import com.checkarr.data.repository.SonarrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SeriesSortOption { TITLE, YEAR, ADDED, SIZE, STATUS }
enum class SeriesFilter { ALL, MONITORED, UNMONITORED, CONTINUING, ENDED, MISSING }

class SeriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SonarrRepository(ApiClient())

    private val _series = MutableStateFlow<List<Series>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SeriesSortOption.TITLE)
    private val _sortAscending = MutableStateFlow(true)
    private val _filter = MutableStateFlow(SeriesFilter.ALL)
    private val _toast = MutableStateFlow<String?>(null)

    val isLoading: StateFlow<Boolean> = _isLoading
    val error: StateFlow<String?> = _error
    val searchQuery: StateFlow<String> = _searchQuery
    val sortOption: StateFlow<SeriesSortOption> = _sortOption
    val sortAscending: StateFlow<Boolean> = _sortAscending
    val filter: StateFlow<SeriesFilter> = _filter
    val toast: StateFlow<String?> = _toast

    val series: StateFlow<List<Series>> = combine(
        _series, _searchQuery, _sortOption, _sortAscending, _filter
    ) { series, query, sort, asc, filter ->
        var result = series
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.year.toString().contains(query) ||
                (it.network ?: "").contains(query, ignoreCase = true)
            }
        }
        result = when (filter) {
            SeriesFilter.MONITORED -> result.filter { it.monitored }
            SeriesFilter.UNMONITORED -> result.filter { !it.monitored }
            SeriesFilter.CONTINUING -> result.filter { it.status == "continuing" }
            SeriesFilter.ENDED -> result.filter { it.status == "ended" }
            SeriesFilter.MISSING -> result.filter { it.monitored && (it.statistics?.percentOfEpisodes ?: 0.0) < 100.0 }
            SeriesFilter.ALL -> result
        }
        result = when (sort) {
            SeriesSortOption.TITLE -> result.sortedBy { it.sortTitle }
            SeriesSortOption.YEAR -> result.sortedBy { it.year }
            SeriesSortOption.ADDED -> result.sortedBy { it.added }
            SeriesSortOption.SIZE -> result.sortedBy { it.statistics?.sizeOnDisk ?: 0L }
            SeriesSortOption.STATUS -> result.sortedBy { it.status }
        }
        if (!asc) result = result.reversed()
        result
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    private val _isLoadingEpisodes = MutableStateFlow(false)
    val episodes: StateFlow<List<Episode>> = _episodes
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes

    private val _searchResults = MutableStateFlow<List<Series>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    val searchResults: StateFlow<List<Series>> = _searchResults
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _qualityProfiles = MutableStateFlow<List<QualityProfile>>(emptyList())
    private val _rootFolders = MutableStateFlow<List<RootFolder>>(emptyList())
    val qualityProfiles: StateFlow<List<QualityProfile>> = _qualityProfiles
    val rootFolders: StateFlow<List<RootFolder>> = _rootFolders

    fun load(instance: Instance) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getSeries(instance).fold(
                onSuccess = { _series.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun loadEpisodes(instance: Instance, seriesId: Int, seasonNumber: Int? = null) {
        viewModelScope.launch {
            _isLoadingEpisodes.value = true
            repo.getEpisodes(instance, seriesId, seasonNumber).fold(
                onSuccess = { _episodes.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoadingEpisodes.value = false
        }
    }

    fun loadMetadata(instance: Instance) {
        viewModelScope.launch {
            repo.getQualityProfiles(instance).onSuccess { _qualityProfiles.value = it }
            repo.getRootFolders(instance).onSuccess { _rootFolders.value = it }
        }
    }

    fun searchOnline(instance: Instance, query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            repo.lookupSeries(instance, query).onSuccess { _searchResults.value = it }
            _isSearching.value = false
        }
    }

    fun clearSearchResults() { _searchResults.value = emptyList() }

    fun toggleMonitor(instance: Instance, s: Series) {
        viewModelScope.launch {
            val updated = s.copy(monitored = !s.monitored)
            repo.updateSeries(instance, updated).fold(
                onSuccess = {
                    val list = _series.value.toMutableList()
                    val idx = list.indexOfFirst { it.id == s.id }
                    if (idx >= 0) list[idx] = it
                    _series.value = list
                    _toast.value = if (it.monitored) "Monitoring" else "Not monitoring"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun searchSeries(instance: Instance, seriesId: Int) {
        viewModelScope.launch {
            repo.searchSeries(instance, seriesId).fold(
                onSuccess = { _toast.value = "Search started" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun searchEpisodes(instance: Instance, episodeIds: List<Int>) {
        viewModelScope.launch {
            repo.searchEpisodes(instance, episodeIds).fold(
                onSuccess = { _toast.value = "Search started" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun addSeries(instance: Instance, s: Series, qualityProfileId: Int, rootFolderPath: String, monitored: Boolean) {
        viewModelScope.launch {
            repo.addSeries(instance, s, qualityProfileId, rootFolderPath, monitored).fold(
                onSuccess = {
                    _series.value = _series.value + it
                    _toast.value = "${it.title} added"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun deleteSeries(instance: Instance, id: Int, deleteFiles: Boolean) {
        viewModelScope.launch {
            repo.deleteSeries(instance, id, deleteFiles).fold(
                onSuccess = {
                    _series.value = _series.value.filter { it.id != id }
                    _toast.value = "Series deleted"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun setSearch(query: String) { _searchQuery.value = query }
    fun setSort(sort: SeriesSortOption) {
        if (_sortOption.value == sort) _sortAscending.value = !_sortAscending.value
        else { _sortOption.value = sort; _sortAscending.value = true }
    }
    fun setFilter(f: SeriesFilter) { _filter.value = f }
    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }
}
