package com.dmc.mongoclient.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.mongoclient.data.settings.AppSettings
import com.dmc.mongoclient.data.settings.AppSettingsSnapshot
import com.dmc.mongoclient.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: AppSettings,
) : ViewModel() {

    val state: StateFlow<AppSettingsSnapshot> = settings.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettingsSnapshot(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setShowSystemDbsDefault(value: Boolean) {
        viewModelScope.launch { settings.setShowSystemDbsDefault(value) }
    }
}
