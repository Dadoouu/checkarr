package com.checkarr.ui.jellyfin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.repository.JellyfinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class JellyfinTab { SESSIONS, RECENT, RESUME, ACTIVITY, STATS }

class JellyfinViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = JellyfinRepository()

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _sessions = MutableStateFlow<List<JellyfinSession>>(emptyList())
    private val _recentlyAdded = MutableStateFlow<List<JellyfinItem>>(emptyList())
    private val _resumeItems = MutableStateFlow<List<JellyfinItem>>(emptyList())
    private val _activityLog = MutableStateFlow<List<JellyfinActivity>>(emptyList())
    private val _systemInfo = MutableStateFlow<JellyfinSystemInfo?>(null)
    private val _drives = MutableStateFlow<List<JellyfinDriveInfo>>(emptyList())
    private val _selectedTab = MutableStateFlow(JellyfinTab.SESSIONS)

    val isLoading: StateFlow<Boolean> = _isLoading
    val error: StateFlow<String?> = _error
    val sessions: StateFlow<List<JellyfinSession>> = _sessions
    val recentlyAdded: StateFlow<List<JellyfinItem>> = _recentlyAdded
    val resumeItems: StateFlow<List<JellyfinItem>> = _resumeItems
    val activityLog: StateFlow<List<JellyfinActivity>> = _activityLog
    val systemInfo: StateFlow<JellyfinSystemInfo?> = _systemInfo
    val drives: StateFlow<List<JellyfinDriveInfo>> = _drives
    val selectedTab: StateFlow<JellyfinTab> = _selectedTab

    fun setTab(tab: JellyfinTab) { _selectedTab.value = tab }
    fun clearError() { _error.value = null }

    fun load(instance: JellyfinInstance) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Load all sections in parallel
            val sessionsJob = launch {
                repo.getSessions(instance).onSuccess { _sessions.value = it }
                    .onFailure { if (_error.value == null) _error.value = it.message }
            }
            val recentJob = launch {
                repo.getRecentlyAdded(instance).onSuccess { _recentlyAdded.value = it.items }
            }
            val resumeJob = launch {
                repo.getResumeItems(instance).onSuccess { _resumeItems.value = it.items }
            }
            val activityJob = launch {
                repo.getActivityLog(instance).onSuccess { _activityLog.value = it.items }
            }
            val systemJob = launch {
                repo.getSystemInfo(instance).onSuccess { _systemInfo.value = it }
            }
            val drivesJob = launch {
                repo.getDrives(instance).onSuccess { _drives.value = it }
            }

            sessionsJob.join()
            recentJob.join()
            resumeJob.join()
            activityJob.join()
            systemJob.join()
            drivesJob.join()

            _isLoading.value = false
        }
    }
}
