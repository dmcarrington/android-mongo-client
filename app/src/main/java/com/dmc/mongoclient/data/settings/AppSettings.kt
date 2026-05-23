package com.dmc.mongoclient.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettingsSnapshot(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showSystemDbsDefault: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(
    private val context: android.content.Context,
) {
    val flow: Flow<AppSettingsSnapshot> = context.dataStore.data.map { prefs ->
        AppSettingsSnapshot(
            themeMode = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            showSystemDbsDefault = prefs[KEY_SYSTEM_DBS] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setShowSystemDbsDefault(value: Boolean) {
        context.dataStore.edit { it[KEY_SYSTEM_DBS] = value }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_SYSTEM_DBS = booleanPreferencesKey("show_system_dbs_default")
    }
}
