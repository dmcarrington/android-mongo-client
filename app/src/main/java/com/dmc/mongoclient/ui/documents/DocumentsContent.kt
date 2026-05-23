package com.dmc.mongoclient.ui.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmc.mongoclient.domain.model.DocumentRef
import com.dmc.mongoclient.ui.common.highlightedJson
import com.dmc.mongoclient.ui.common.rememberJsonHighlightTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsContent(
    database: String?,
    collection: String?,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(database, collection) {
        viewModel.setLocation(database, collection)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (state.collection != null && state.mode is ContentMode.List) {
                FloatingActionButton(onClick = viewModel::openInsert) {
                    Icon(Icons.Default.Add, contentDescription = "Insert document")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.collection == null -> EmptyMessage("Select a collection")
                state.mode is ContentMode.List -> DocumentList(state, viewModel)
                state.mode is ContentMode.Detail -> DocumentDetail(state, viewModel)
                state.mode is ContentMode.Edit -> DocumentEditor(state, viewModel)
            }
        }
    }

    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete document?") },
            text = { Text("This document will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DocumentList(state: DocumentsUiState, vm: DocumentsViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        QueryBar(state, vm)
        HorizontalDivider()
        StatusHeader(state)
        HorizontalDivider()
        Box(modifier = Modifier.fillMaxSize().weight(1f, fill = true)) {
            when {
                state.loading && state.page == null -> Centered { CircularProgressIndicator() }
                state.page == null -> EmptyMessage("Tap Run to fetch documents")
                state.page!!.documents.isEmpty() -> EmptyMessage("No documents match")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Pad the bottom so the FAB doesn't sit on top of the last card.
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.page!!.documents, key = { it.id.hashCode() }) { ref ->
                        DocumentCard(ref = ref, onClick = { vm.openDetail(ref) })
                    }
                }
            }
        }
        Pager(state, vm)
    }
}

@Composable
private fun StatusHeader(state: DocumentsUiState) {
    val page = state.page
    val rangeText = when {
        page == null -> "—"
        page.documents.isEmpty() -> "0"
        else -> "${page.skip + 1}–${page.skip + page.documents.size}"
    }
    val totalText = when {
        state.total != null -> " of ${formatCount(state.total)}"
        state.totalLoading -> " of …"
        else -> ""
    }
    Text(
        text = "Showing $rangeText$totalText",
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatCount(n: Long): String = java.text.NumberFormat.getInstance().format(n)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueryBar(state: DocumentsUiState, vm: DocumentsViewModel) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide query" else "Show query")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = vm::runQuery, enabled = !state.loading) {
                Text(if (state.loading) "…" else "Run")
            }
        }
        if (expanded) {
            JsonField(
                label = "Filter",
                value = state.filterText,
                onChange = vm::setFilterText,
                error = state.filterError,
                placeholder = "{}",
            )
            JsonField(
                label = "Projection",
                value = state.projectionText,
                onChange = vm::setProjectionText,
                error = state.projectionError,
                placeholder = "{ \"name\": 1, \"_id\": 0 }",
            )
            JsonField(
                label = "Sort",
                value = state.sortText,
                onChange = vm::setSortText,
                error = state.sortError,
                placeholder = "{ \"_id\": -1 }",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = false,
        maxLines = 3,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        ),
        textStyle = TextStyle(fontFamily = FontFamily.Monospace),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun DocumentCard(ref: DocumentRef, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
    ) {
        Text(
            text = highlightedJson(ref.prettyJson),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Pager(state: DocumentsUiState, vm: DocumentsViewModel) {
    HorizontalDivider()
    // Buttons stay anchored to the left; the right side is empty space the FAB
    // can float over without hiding anything. Count moved to the StatusHeader.
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = vm::prevPage,
            enabled = state.skip > 0 && !state.loading,
        ) { Text("Prev") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = vm::nextPage,
            enabled = state.page?.mightHaveMore == true && !state.loading,
        ) { Text("Next") }
    }
}

@Composable
private fun DocumentDetail(state: DocumentsUiState, vm: DocumentsViewModel) {
    val ref = (state.mode as ContentMode.Detail).ref
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Document", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::requestDelete) { Text("Delete") }
            TextButton(onClick = vm::openEdit) { Text("Edit") }
            TextButton(onClick = vm::closeDetailOrEditor) { Text("Close") }
        }
        HorizontalDivider()
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = highlightedJson(ref.prettyJson),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentEditor(state: DocumentsUiState, vm: DocumentsViewModel) {
    val mode = state.mode as ContentMode.Edit
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (mode.replacing == null) "Insert document" else "Edit document",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::formatEditorJson) { Text("Format") }
            TextButton(onClick = vm::closeDetailOrEditor, enabled = !state.saving) { Text("Cancel") }
            Button(
                onClick = vm::saveEditor,
                enabled = !state.saving && state.editorParseError == null,
            ) { Text(if (state.saving) "Saving…" else "Save") }
        }
        HorizontalDivider()
        OutlinedTextField(
            value = state.editorText,
            onValueChange = vm::setEditorText,
            modifier = Modifier.fillMaxSize().padding(12.dp),
            isError = state.editorParseError != null,
            supportingText = { state.editorParseError?.let { Text(it) } },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            visualTransformation = rememberJsonHighlightTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Centered { Text(text, style = MaterialTheme.typography.bodyMedium) }
}
