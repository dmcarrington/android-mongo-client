package com.dmc.mongoclient.ui.indexes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.mongoclient.data.mongo.toUserMessage
import com.dmc.mongoclient.domain.model.IndexInfo
import com.dmc.mongoclient.domain.repo.BrowseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IndexesUiState(
    val database: String? = null,
    val collection: String? = null,
    val loading: Boolean = false,
    val indexes: List<IndexInfo> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class IndexesViewModel @Inject constructor(
    private val repo: BrowseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IndexesUiState())
    val state: StateFlow<IndexesUiState> = _state.asStateFlow()

    fun setLocation(database: String?, collection: String?) {
        val cur = _state.value
        if (cur.database == database && cur.collection == collection) return
        _state.value = IndexesUiState(database = database, collection = collection)
        if (database != null && collection != null) refresh()
    }

    fun refresh() {
        val s = _state.value
        val db = s.database ?: return
        val col = s.collection ?: return
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repo.listIndexes(db, col) }
                .onSuccess { indexes ->
                    _state.update { it.copy(indexes = indexes, loading = false) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(loading = false, errorMessage = t.toUserMessage())
                    }
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
