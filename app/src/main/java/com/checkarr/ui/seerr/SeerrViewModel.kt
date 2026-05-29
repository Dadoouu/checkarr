package com.checkarr.ui.seerr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.repository.SeerrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SeerrViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SeerrRepository()

    private val _requests = MutableStateFlow<List<SeerrRequest>>(emptyList())
    private val _searchResults = MutableStateFlow<List<SeerrSearchResult>>(emptyList())
    private val _users = MutableStateFlow<List<SeerrUser>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _toast = MutableStateFlow<String?>(null)
    private val _filterStatus = MutableStateFlow("all")

    val requests: StateFlow<List<SeerrRequest>> = _requests
    val searchResults: StateFlow<List<SeerrSearchResult>> = _searchResults
    val users: StateFlow<List<SeerrUser>> = _users
    val isLoading: StateFlow<Boolean> = _isLoading
    val isSearching: StateFlow<Boolean> = _isSearching
    val error: StateFlow<String?> = _error
    val toast: StateFlow<String?> = _toast
    val filterStatus: StateFlow<String> = _filterStatus

    val filteredRequests: StateFlow<List<SeerrRequest>> = combine(_requests, _filterStatus) { reqs, filter ->
        if (filter == "all") reqs else reqs.filter { r ->
            when (filter) {
                "pending" -> r.status == 1
                "approved" -> r.status == 2
                "declined" -> r.status == 3
                "available" -> r.status == 4
                else -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun load(url: String, apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getRequests(url, apiKey).fold(
                onSuccess = { _requests.value = it.results },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun search(url: String, apiKey: String, query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            repo.search(url, apiKey, query).fold(
                onSuccess = { _searchResults.value = it.results },
                onFailure = { _error.value = it.message }
            )
            _isSearching.value = false
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    fun approveRequest(url: String, apiKey: String, requestId: Int) {
        viewModelScope.launch {
            repo.approveRequest(url, apiKey, requestId).fold(
                onSuccess = { updated ->
                    _requests.value = _requests.value.map { if (it.id == requestId) updated else it }
                    _toast.value = "Request approved"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun declineRequest(url: String, apiKey: String, requestId: Int) {
        viewModelScope.launch {
            repo.declineRequest(url, apiKey, requestId).fold(
                onSuccess = { updated ->
                    _requests.value = _requests.value.map { if (it.id == requestId) updated else it }
                    _toast.value = "Request declined"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun deleteRequest(url: String, apiKey: String, requestId: Int) {
        viewModelScope.launch {
            repo.deleteRequest(url, apiKey, requestId).fold(
                onSuccess = {
                    _requests.value = _requests.value.filter { it.id != requestId }
                    _toast.value = "Request deleted"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun retryRequest(url: String, apiKey: String, requestId: Int) {
        viewModelScope.launch {
            repo.retryRequest(url, apiKey, requestId).fold(
                onSuccess = { _toast.value = "Request retried" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun setFilter(filter: String) { _filterStatus.value = filter }
    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }
}
