package com.checkarr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.AppSettings
import com.checkarr.data.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = AppSettings(application)

    val appConfig: StateFlow<AppConfig> = settings.appConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

    val theme: StateFlow<String> = settings.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val accentColor: StateFlow<String> = settings.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "blue")

    // Derive instances from AppConfig
    val selectedRadarrInstance: StateFlow<Instance?> = appConfig.map { cfg ->
        val sc = cfg.serviceConfigs[ServiceType.RADARR.name]
        if (sc?.enabled == true && sc.url.isNotBlank() && sc.apiKey.isNotBlank())
            sc.toInstance(ServiceType.RADARR) else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedSonarrInstance: StateFlow<Instance?> = appConfig.map { cfg ->
        val sc = cfg.serviceConfigs[ServiceType.SONARR.name]
        if (sc?.enabled == true && sc.url.isNotBlank() && sc.apiKey.isNotBlank())
            sc.toInstance(ServiceType.SONARR) else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedJellyfinInstance: StateFlow<JellyfinInstance?> = appConfig.map { cfg ->
        val sc = cfg.serviceConfigs[ServiceType.JELLYFIN.name]
        if (sc?.enabled == true && sc.url.isNotBlank() && sc.apiKey.isNotBlank())
            sc.toJellyfinInstance() else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveAppConfig(config: AppConfig) {
        viewModelScope.launch { settings.saveAppConfig(config) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch { settings.setAccentColor(color) }
    }
}
