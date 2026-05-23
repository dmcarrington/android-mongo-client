package com.dmc.mongoclient.ui.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.mongoclient.data.mongo.toUserMessage
import com.dmc.mongoclient.domain.model.SavedConnection
import com.dmc.mongoclient.domain.repo.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectionListUiState(
    val connections: List<SavedConnection> = emptyList(),
    val loading: Boolean = true,
)

sealed interface ConnectionListEvent {
    data class ConnectedTo(val id: Long) : ConnectionListEvent
    data class ConnectFailed(val message: String) : ConnectionListEvent
}

@HiltViewModel
class ConnectionListViewModel @Inject constructor(
    private val repo: ConnectionRepository,
) : ViewModel() {

    val uiState: StateFlow<ConnectionListUiState> = repo.observeAll()
        .map { ConnectionListUiState(connections = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionListUiState(),
        )

    private val _events = MutableStateFlow<ConnectionListEvent?>(null)
    val events: StateFlow<ConnectionListEvent?> = _events.asStateFlow()

    fun consumeEvent() {
        _events.value = null
    }

    fun connect(id: Long) {
        viewModelScope.launch {
            runCatching { repo.openActive(id) }
                .onSuccess { _events.value = ConnectionListEvent.ConnectedTo(id) }
                .onFailure { _events.value = ConnectionListEvent.ConnectFailed(it.toUserMessage()) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}
