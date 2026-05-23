package com.dmc.mongoclient.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.mongoclient.data.mongo.MongoClientHolder
import com.dmc.mongoclient.data.mongo.isConnectionLost
import com.dmc.mongoclient.data.mongo.toUserMessage
import com.dmc.mongoclient.data.settings.AppSettings
import com.dmc.mongoclient.domain.model.CollectionSummary
import com.dmc.mongoclient.domain.model.DatabaseSummary
import com.dmc.mongoclient.domain.repo.BrowseRepository
import com.dmc.mongoclient.domain.repo.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DropTarget {
    data class Database(val name: String) : DropTarget
    data class Collection(val database: String, val name: String) : DropTarget
}

data class BrowseUiState(
    val loadingDatabases: Boolean = false,
    val loadingCollections: Boolean = false,
    val databases: List<DatabaseSummary> = emptyList(),
    val collections: List<CollectionSummary> = emptyList(),
    val selectedDatabase: String? = null,
    val selectedCollection: String? = null,
    val showSystemDbs: Boolean = false,
    val pendingDrop: DropTarget? = null,
    val errorMessage: String? = null,
    /** True when the active MongoClient has gone away (process death or explicit disconnect). */
    val disconnected: Boolean = false,
    /** True when the most recent driver call failed with a socket / timeout error. */
    val connectionLost: Boolean = false,
    val reconnecting: Boolean = false,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val browseRepo: BrowseRepository,
    private val connectionRepo: ConnectionRepository,
    private val clientHolder: MongoClientHolder,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        if (clientHolder.activeClient() == null) {
            _state.update { it.copy(disconnected = true) }
        } else {
            viewModelScope.launch {
                val defaults = appSettings.flow.first()
                _state.update { it.copy(showSystemDbs = defaults.showSystemDbsDefault) }
                refreshDatabases()
            }
        }
    }

    fun refreshDatabases() {
        _state.update { it.copy(loadingDatabases = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { browseRepo.listDatabases(_state.value.showSystemDbs) }
                .onSuccess { dbs ->
                    _state.update { it.copy(databases = dbs, loadingDatabases = false) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(loadingDatabases = false, errorMessage = t.toUserMessage(), connectionLost = t.isConnectionLost() || it.connectionLost)
                    }
                }
        }
    }

    fun selectDatabase(name: String) {
        if (_state.value.selectedDatabase == name) return
        _state.update {
            it.copy(
                selectedDatabase = name,
                selectedCollection = null,
                collections = emptyList(),
                loadingCollections = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching { browseRepo.listCollections(name) }
                .onSuccess { cols ->
                    _state.update { it.copy(collections = cols, loadingCollections = false) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(loadingCollections = false, errorMessage = t.toUserMessage(), connectionLost = t.isConnectionLost() || it.connectionLost)
                    }
                }
        }
    }

    fun selectCollection(name: String) {
        _state.update { it.copy(selectedCollection = name) }
    }

    fun toggleShowSystemDbs() {
        _state.update { it.copy(showSystemDbs = !it.showSystemDbs) }
        refreshDatabases()
    }

    fun requestDrop(target: DropTarget) {
        _state.update { it.copy(pendingDrop = target) }
    }

    fun cancelDrop() {
        _state.update { it.copy(pendingDrop = null) }
    }

    fun confirmDrop() {
        val target = _state.value.pendingDrop ?: return
        _state.update { it.copy(pendingDrop = null) }
        viewModelScope.launch {
            runCatching {
                when (target) {
                    is DropTarget.Database -> browseRepo.dropDatabase(target.name)
                    is DropTarget.Collection -> browseRepo.dropCollection(target.database, target.name)
                }
            }.onFailure { t ->
                _state.update {
                    it.copy(errorMessage = t.toUserMessage(), connectionLost = t.isConnectionLost() || it.connectionLost)
                }
            }
            refreshDatabases()
            // If we just dropped the currently-open database, clear its collections.
            if (target is DropTarget.Database && _state.value.selectedDatabase == target.name) {
                _state.update {
                    it.copy(selectedDatabase = null, selectedCollection = null, collections = emptyList())
                }
            } else if (_state.value.selectedDatabase != null) {
                // Refresh collections for the still-selected database.
                selectDatabaseForceReload(_state.value.selectedDatabase!!)
            }
        }
    }

    private fun selectDatabaseForceReload(name: String) {
        _state.update {
            it.copy(selectedCollection = null, collections = emptyList(), loadingCollections = true)
        }
        viewModelScope.launch {
            runCatching { browseRepo.listCollections(name) }
                .onSuccess { cols ->
                    _state.update { it.copy(collections = cols, loadingCollections = false) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(loadingCollections = false, errorMessage = t.toUserMessage(), connectionLost = t.isConnectionLost() || it.connectionLost)
                    }
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun disconnect(onDone: () -> Unit) {
        viewModelScope.launch {
            connectionRepo.closeActive()
            _state.update { it.copy(disconnected = true) }
            onDone()
        }
    }

    fun reconnect() {
        val connectionId = clientHolder.activeConnectionId()
        if (connectionId == null) {
            _state.update { it.copy(disconnected = true) }
            return
        }
        _state.update { it.copy(reconnecting = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { connectionRepo.openActive(connectionId) }
                .onSuccess {
                    _state.update { it.copy(reconnecting = false, connectionLost = false) }
                    refreshDatabases()
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(reconnecting = false, errorMessage = t.toUserMessage())
                    }
                }
        }
    }
}
