package com.checkarr.ui.maintainerr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.repository.MaintainerrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MaintainerrViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MaintainerrRepository()

    private val _collections = MutableStateFlow<List<MaintainerrRuleGroup>>(emptyList())

    private val _selectedCollectionMedia = MutableStateFlow<List<MaintainerrMedia>>(emptyList())
    private val _storageMetrics = MutableStateFlow<MaintainerrStorageMetrics?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isLoadingMedia = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _toast = MutableStateFlow<String?>(null)

    val collections: StateFlow<List<MaintainerrRuleGroup>> = _collections
    val selectedCollectionMedia: StateFlow<List<MaintainerrMedia>> = _selectedCollectionMedia
    val storageMetrics: StateFlow<MaintainerrStorageMetrics?> = _storageMetrics
    val isLoading: StateFlow<Boolean> = _isLoading
    val isLoadingMedia: StateFlow<Boolean> = _isLoadingMedia
    val error: StateFlow<String?> = _error
    val toast: StateFlow<String?> = _toast

    fun load(url: String, apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getCollections(url, apiKey).fold(
                onSuccess = { _collections.value = it },
                onFailure = { _error.value = it.message }
            )
            repo.getStorageMetrics(url, apiKey).onSuccess { _storageMetrics.value = it }
            _isLoading.value = false
        }
    }

    fun loadCollectionMedia(url: String, apiKey: String, collectionId: Int) {
        viewModelScope.launch {
            _isLoadingMedia.value = true
            repo.getCollectionMedia(url, apiKey, collectionId).fold(
                onSuccess = { _selectedCollectionMedia.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoadingMedia.value = false
        }
    }

    fun handleCollections(url: String, apiKey: String) {
        viewModelScope.launch {
            repo.handleCollections(url, apiKey).fold(
                onSuccess = { _toast.value = "Collection handling triggered" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun handleCollection(url: String, apiKey: String, collectionId: Int) {
        viewModelScope.launch {
            repo.handleCollection(url, apiKey, collectionId).fold(
                onSuccess = { _toast.value = "Collection handling started" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun runRules(url: String, apiKey: String) {
        viewModelScope.launch {
            repo.runRules(url, apiKey).fold(
                onSuccess = { _toast.value = "Rules execution triggered" },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun removeMediaFromCollection(url: String, apiKey: String, collectionId: Int, mediaId: Int) {
        viewModelScope.launch {
            repo.removeMediaFromCollection(url, apiKey, collectionId, mediaId).fold(
                onSuccess = {
                    _selectedCollectionMedia.value = _selectedCollectionMedia.value.filter { it.id != mediaId }
                    _toast.value = "Media removed from collection"
                },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun clearError() { _error.value = null }
    fun clearToast() { _toast.value = null }
    fun clearMedia() { _selectedCollectionMedia.value = emptyList() }
}
