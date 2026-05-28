package com.checkarr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.checkarr.data.models.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "checkarr_settings")

class AppSettings(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val APP_CONFIG_KEY = stringPreferencesKey("app_config")
        val THEME_KEY = stringPreferencesKey("theme")
        val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
    }

    val appConfigFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        val raw = prefs[APP_CONFIG_KEY] ?: return@map AppConfig()
        try { json.decodeFromString<AppConfig>(raw) } catch (e: Exception) { AppConfig() }
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[THEME_KEY] ?: "system" }
    val accentColorFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[ACCENT_COLOR_KEY] ?: "blue" }

    suspend fun saveAppConfig(config: AppConfig) {
        context.dataStore.edit { prefs -> prefs[APP_CONFIG_KEY] = json.encodeToString(config) }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = theme }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { prefs -> prefs[ACCENT_COLOR_KEY] = color }
    }
}
