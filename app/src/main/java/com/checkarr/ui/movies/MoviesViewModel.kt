package com.checkarr.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import com.checkarr.data.repository.RadarrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MovieSortOption { TITLE, YEAR, ADDED, SIZE, STATUS }
enum class MovieFilter { ALL, MONITORED, UNMONITORED, MISSING, DOWNLOADED }

class MoviesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = RadarrRepository(ApiClient())

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(MovieSortOption.TITLE)
    private val _sortAscending = MutableStateFlow(true)
    private val _filter = MutableStateFlow(MovieFilter.ALL)
    private val _toast = MutableStateFlow<String?>(null)

    val isLoading: StateFlow<Boolean> = _isLoading
    val error: StateFlow<String?> = _error
    val searchQuery: StateFlow<String> = _searchQuery
    val sortOption: StateFlow<MovieSortOption> = _sortOption
    val sortAscending: StateFlow<Boolean> = _sortAscending
    val filter: StateFlow<MovieFilter> = _filter
    val toast: StateFlow<String?> = _toast

    val movies: StateFlow<List<Movie>> = combine(
        _movies, _searchQuery, _sortOption, _sortAscending, _filter
    ) { movies, query, sort, asc, filter ->
        var result = movies
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.originalTitle.contains(query, ignoreCase = true) ||
                it.year.toString().contains(query)
            }
        }
        result = when (filter) {
            MovieFilter.MONITORED -> result.filter { it.monitored }
            MovieFilter.UNMONITORED -> result.filter { !it.monitored }
            MovieFilter.MISSING -> result.filter { it.monitored && !it.hasFile }
            MovieFilter.DOWNLOADED -> result.filter { it.hasFile }
            MovieFilter.ALL -> result
        }
        result = when (sort) {
            MovieSortOption.TITLE -> result.sortedBy { it.sortTitle }
            MovieSortOption.YEAR -> result.sortedBy { it.year }
            MovieSortOption.ADDED -> result.sortedBy { it.added }
            MovieSortOption.SIZE -> result.sortedBy { it.sizeOnDisk }
            MovieSortOption.STATUS -> result.sortedBy { it.status }
        }
        if (!asc) result = result.reversed()
        result
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _qualityProfiles = MutableStateFlow<List<QualityProfile>>(emptyList())
    private val _rootFolders = MutableStateFlow<List<RootFolder>>(emptyList())
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val qualityProfiles: StateFlow<List<QualityProfile>> = _qualityProfiles
    val rootFolders: StateFlow<List<RootFolder>> = _rootFolders
    val tags: StateFlow<List<Tag>> = _tags

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    val searchResults: StateFlow<List<Movie>> = _searchResults
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _releases = MutableStateFlow<List<MovieRelease>>(emptyList())
    private val _isLoadingReleases = MutableStateFlow(false)
    val releases: StateFlow<List<MovieRelease>> = _releases
    val isLoadingReleases: StateFlow<Boolean> = _isLoadingReleases

    fun load(instance: Instance) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getMovies(instance).fold(
                onSuccess = { _movies.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun loadMetadata(instance: Instance) {
        viewModelScope.launch {
            repo.getQualityProfiles(instance).onSuccess { _qualityProfiles.value = it }
            repo.getRootFolders(instance).onSuccess { _rootFolders.value = it }
            repo.getTags(instance).onSuccess { _tags.value = it }
        }
    }

    fun searchOnline(instance: Instance, query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            repo.lookupMovie(instance, query).onSuccess { _searchResults.value = it }
            _isSearching.value = false
        }
    }

    fun clearSearchResults() { _searchResults.value = emptyList() }

    fun loadReleases(instance: Instance, movieId: Int) {
        viewModelScope.launch {
            _isLoadingReleases.value = true
            repo.getReleases(instance, movieId).fold(
                onSuccess = { _releases.value = it.sortedByDescending { r -> !r.rejected } },
                onFailure = { _error.value = it.message }
            )
            _isLoadingReleases.value = false
        }
    }

    fun downloadRelease(instance: Instance, release: MovieRelease) {
        viewModelScope.launch {
            repo.downloadRelease(instance, release.guid, 0).fold(
                onSuccess = { _toast.value = "Added to queue" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun toggleMonitor(instance: Instance, movie: Movie) {
        viewModelScope.launch {
            val updated = movie.copy(monitored = !movie.monitored)
            repo.updateMovie(instance, updated).fold(
                onSuccess = {
                    val list = _movies.value.toMutableList()
                    val idx = list.indexOfFirst { m -> m.id == movie.id }
                    if (idx >= 0) list[idx] = it
                    _movies.value = list
                    _toast.value = if (it.monitored) "Monitoring" else "Not monitoring"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun searchMovie(instance: Instance, movieId: Int) {
        viewModelScope.launch {
            repo.searchMovie(instance, movieId).fold(
                onSuccess = { _toast.value = "Search started" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun addMovie(instance: Instance, movie: Movie, qualityProfileId: Int, rootFolderPath: String, monitored: Boolean) {
        viewModelScope.launch {
            repo.addMovie(instance, movie, qualityProfileId, rootFolderPath, monitored).fold(
                onSuccess = {
                    _movies.value = _movies.value + it
                    _toast.value = "${it.title} added"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun deleteMovie(instance: Instance, id: Int, deleteFiles: Boolean) {
        viewModelScope.launch {
            repo.deleteMovie(instance, id, deleteFiles).fold(
                onSuccess = {
                    _movies.value = _movies.value.filter { it.id != id }
                    _toast.value = "Movie deleted"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun setSearch(query: String) { _searchQuery.value = query }
    fun setSort(sort: MovieSortOption) {
        if (_sortOption.value == sort) _sortAscending.value = !_sortAscending.value
        else { _sortOption.value = sort; _sortAscending.value = true }
    }
    fun setFilter(f: MovieFilter) { _filter.value = f }
    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }
}
