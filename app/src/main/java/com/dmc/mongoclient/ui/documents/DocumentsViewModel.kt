package com.dmc.mongoclient.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.mongoclient.data.mongo.JsonFormat
import com.dmc.mongoclient.data.mongo.toUserMessage
import com.dmc.mongoclient.domain.model.DocumentRef
import com.dmc.mongoclient.domain.model.FindPage
import com.dmc.mongoclient.domain.model.FindRequest
import com.dmc.mongoclient.domain.repo.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

private const val DEFAULT_PAGE_SIZE = 25

sealed interface ContentMode {
    object List : ContentMode
    data class Detail(val ref: DocumentRef) : ContentMode
    data class Edit(val initialJson: String, val replacing: DocumentRef?) : ContentMode
}

data class DocumentsUiState(
    val database: String? = null,
    val collection: String? = null,
    val filterText: String = "{}",
    val projectionText: String = "",
    val sortText: String = "",
    val filterError: String? = null,
    val projectionError: String? = null,
    val sortError: String? = null,
    val skip: Int = 0,
    val limit: Int = DEFAULT_PAGE_SIZE,
    val loading: Boolean = false,
    val page: FindPage? = null,
    /** Total document count for the current filter. Null while loading or if the count call fails. */
    val total: Long? = null,
    val totalLoading: Boolean = false,
    val mode: ContentMode = ContentMode.List,
    val editorText: String = "",
    val editorParseError: String? = null,
    val saving: Boolean = false,
    val pendingDelete: DocumentRef? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repo: DocumentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentsUiState())
    val state: StateFlow<DocumentsUiState> = _state.asStateFlow()

    fun setLocation(database: String?, collection: String?) {
        val cur = _state.value
        if (cur.database == database && cur.collection == collection) return
        _state.update {
            DocumentsUiState(
                database = database,
                collection = collection,
                // Carry the filter/projection/sort defaults forward; everything else resets.
            )
        }
        if (database != null && collection != null) runQuery()
    }

    fun setFilterText(value: String) = _state.update { it.copy(filterText = value, filterError = null) }
    fun setProjectionText(value: String) = _state.update { it.copy(projectionText = value, projectionError = null) }
    fun setSortText(value: String) = _state.update { it.copy(sortText = value, sortError = null) }

    fun runQuery() {
        val s = _state.value
        if (s.database == null || s.collection == null) return
        if (!validateQueryFields()) return
        _state.update { it.copy(loading = true, errorMessage = null, skip = 0) }
        loadCurrentPage()
    }

    fun nextPage() {
        val s = _state.value
        if (s.page?.mightHaveMore != true) return
        _state.update { it.copy(skip = it.skip + it.limit, loading = true) }
        loadCurrentPage()
    }

    fun prevPage() {
        val s = _state.value
        if (s.skip == 0) return
        _state.update { it.copy(skip = (it.skip - it.limit).coerceAtLeast(0), loading = true) }
        loadCurrentPage()
    }

    private fun loadCurrentPage() {
        val s = _state.value
        val db = s.database ?: return
        val col = s.collection ?: return
        viewModelScope.launch {
            runCatching {
                repo.find(
                    db,
                    col,
                    FindRequest(
                        filter = s.filterText,
                        projection = s.projectionText.takeIf { it.isNotBlank() },
                        sort = s.sortText.takeIf { it.isNotBlank() },
                        skip = s.skip,
                        limit = s.limit,
                    ),
                )
            }.onSuccess { page ->
                _state.update { it.copy(page = page, loading = false) }
            }.onFailure { t ->
                _state.update { it.copy(loading = false, errorMessage = t.toUserMessage()) }
            }
        }
        refreshTotal(db, col, s.filterText)
    }

    private fun refreshTotal(db: String, col: String, filterText: String) {
        _state.update { it.copy(total = null, totalLoading = true) }
        viewModelScope.launch {
            val result = runCatching {
                // Empty/default filter → metadata-based count (fast).
                // Anything else → exact countDocuments (can be slow on large
                // unindexed filters but always accurate).
                if (filterText.isBlank() || filterText.trim() == "{}") {
                    repo.estimatedCount(db, col)
                } else {
                    repo.countDocuments(db, col, filterText)
                }
            }
            _state.update {
                it.copy(total = result.getOrNull(), totalLoading = false)
            }
        }
    }

    private fun validateQueryFields(): Boolean {
        val s = _state.value
        val filterErr = jsonError(s.filterText.ifBlank { "{}" })
        val projErr = if (s.projectionText.isBlank()) null else jsonError(s.projectionText)
        val sortErr = if (s.sortText.isBlank()) null else jsonError(s.sortText)
        _state.update {
            it.copy(filterError = filterErr, projectionError = projErr, sortError = sortErr)
        }
        return filterErr == null && projErr == null && sortErr == null
    }

    private fun jsonError(text: String): String? = try {
        JsonFormat.parseObject(text); null
    } catch (e: IllegalArgumentException) {
        e.message
    }

    fun openDetail(ref: DocumentRef) {
        _state.update { it.copy(mode = ContentMode.Detail(ref)) }
    }

    fun openInsert() {
        // Pre-seed the editor with a freshly-minted ObjectId so the user can
        // see (and optionally edit) what `_id` will be assigned. The driver
        // would generate one server-side if omitted, but Compass-style
        // explicit pre-fill makes it obvious and copyable.
        val template = buildInsertTemplate()
        _state.update {
            it.copy(
                mode = ContentMode.Edit(initialJson = template, replacing = null),
                editorText = template,
                editorParseError = null,
            )
        }
    }

    private fun buildInsertTemplate(): String {
        val oid = ObjectId.get().toHexString()
        return "{\n  \"_id\": { \"\$oid\": \"$oid\" },\n  \n}"
    }

    fun openEdit() {
        val ref = (_state.value.mode as? ContentMode.Detail)?.ref ?: return
        _state.update {
            it.copy(
                mode = ContentMode.Edit(initialJson = ref.prettyJson, replacing = ref),
                editorText = ref.prettyJson,
                editorParseError = null,
            )
        }
    }

    fun closeDetailOrEditor() {
        _state.update {
            it.copy(mode = ContentMode.List, editorText = "", editorParseError = null)
        }
    }

    fun setEditorText(value: String) {
        _state.update { it.copy(editorText = value, editorParseError = null) }
    }

    fun formatEditorJson() {
        val text = _state.value.editorText
        val doc = try {
            JsonFormat.parseObject(text)
        } catch (e: IllegalArgumentException) {
            _state.update { it.copy(editorParseError = e.message) }
            return
        }
        _state.update { it.copy(editorText = JsonFormat.render(doc), editorParseError = null) }
    }

    fun saveEditor() {
        val s = _state.value
        val mode = s.mode as? ContentMode.Edit ?: return
        val db = s.database ?: return
        val col = s.collection ?: return
        val text = s.editorText
        val err = jsonError(text)
        if (err != null) {
            _state.update { it.copy(editorParseError = err) }
            return
        }
        _state.update { it.copy(saving = true, editorParseError = null) }
        viewModelScope.launch {
            runCatching {
                if (mode.replacing == null) {
                    repo.insertOne(db, col, text)
                } else {
                    repo.replaceOne(db, col, mode.replacing, text)
                }
            }.onSuccess {
                _state.update {
                    it.copy(saving = false, mode = ContentMode.List, editorText = "")
                }
                loadCurrentPage()
            }.onFailure { t ->
                _state.update { it.copy(saving = false, errorMessage = t.toUserMessage()) }
            }
        }
    }

    fun requestDelete() {
        val ref = (_state.value.mode as? ContentMode.Detail)?.ref ?: return
        _state.update { it.copy(pendingDelete = ref) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val ref = _state.value.pendingDelete ?: return
        val s = _state.value
        val db = s.database ?: return
        val col = s.collection ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            runCatching { repo.deleteOne(db, col, ref) }
                .onSuccess {
                    _state.update { it.copy(mode = ContentMode.List) }
                    loadCurrentPage()
                }
                .onFailure { t ->
                    _state.update { it.copy(errorMessage = t.toUserMessage()) }
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

}
