package com.dmc.mongoclient.ui.connections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.mongoclient.data.mongo.ConnectionStringParser
import com.dmc.mongoclient.data.mongo.ParseResult
import com.dmc.mongoclient.domain.model.ConnectionTestResult
import com.dmc.mongoclient.domain.repo.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionEditUiState(
    val id: Long? = null,
    val displayName: String = "",
    val uri: String = "",
    val parseError: String? = null,
    val redactedPreview: String = "",
    val testing: Boolean = false,
    val testResult: ConnectionTestResult? = null,
    val showPassword: Boolean = false,
    val loading: Boolean = true,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = !loading && !testing && displayName.isNotBlank() && uri.isNotBlank() && parseError == null
}

@HiltViewModel
class ConnectionEditViewModel @Inject constructor(
    private val repo: ConnectionRepository,
    private val parser: ConnectionStringParser,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Route arg is a String so we can distinguish "new" from a numeric id.
    private val argId: Long? = savedStateHandle.get<String>("id")
        ?.takeIf { it != "new" }
        ?.toLongOrNull()

    private val _state = MutableStateFlow(ConnectionEditUiState(id = argId, loading = argId != null))
    val state: StateFlow<ConnectionEditUiState> = _state.asStateFlow()

    init {
        if (argId != null) {
            viewModelScope.launch {
                val existing = repo.getById(argId)
                if (existing != null) {
                    _state.update {
                        it.copy(
                            displayName = existing.displayName,
                            uri = existing.uri,
                            redactedPreview = existing.redactedUri,
                            loading = false,
                        )
                    }
                } else {
                    _state.update { it.copy(loading = false) }
                }
            }
        } else {
            _state.update { it.copy(loading = false) }
        }
    }

    fun onDisplayNameChange(value: String) {
        _state.update { it.copy(displayName = value) }
    }

    fun onUriChange(value: String) {
        val parse = if (value.isBlank()) {
            null to ""
        } else when (val r = parser.parse(value)) {
            is ParseResult.Ok -> null to r.redactedForDisplay
            is ParseResult.Error -> r.message to parser.redactPassword(value)
        }
        _state.update {
            it.copy(uri = value, parseError = parse.first, redactedPreview = parse.second, testResult = null)
        }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    fun test() {
        val uri = _state.value.uri.trim().takeIf { it.isNotEmpty() } ?: return
        _state.update { it.copy(testing = true, testResult = null) }
        viewModelScope.launch {
            val result = repo.test(uri)
            _state.update { it.copy(testing = false, testResult = result) }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            if (s.id == null) {
                repo.save(s.displayName.trim(), s.uri.trim())
            } else {
                repo.update(s.id, s.displayName.trim(), s.uri.trim())
            }
            _state.update { it.copy(saved = true) }
        }
    }
}
