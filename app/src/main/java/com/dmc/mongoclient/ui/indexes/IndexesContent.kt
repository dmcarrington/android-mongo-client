package com.dmc.mongoclient.ui.indexes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmc.mongoclient.domain.model.IndexInfo
import com.dmc.mongoclient.ui.common.highlightedJson

@Composable
fun IndexesContent(
    database: String?,
    collection: String?,
    modifier: Modifier = Modifier,
    viewModel: IndexesViewModel = hiltViewModel(),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                collection == null -> Centered { Text("Select a collection") }
                state.loading && state.indexes.isEmpty() -> Centered { CircularProgressIndicator() }
                state.indexes.isEmpty() -> Centered { Text("No indexes") }
                else -> IndexList(state.indexes, contentPadding = PaddingValues(12.dp))
            }
        }
    }
}

@Composable
private fun IndexList(indexes: List<IndexInfo>, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = indexes, key = { it.name }) { index ->
            IndexCard(index)
        }
    }
}

@Composable
private fun IndexCard(index: IndexInfo) {
    var expanded by rememberSaveable(index.name) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(index.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            Text(
                text = keyDisplay(index),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.size(8.dp))
            FlowChips(index)
            if (expanded) {
                Spacer(Modifier.size(12.dp))
                HorizontalDivider()
                Spacer(Modifier.size(8.dp))
                Text(
                    text = highlightedJson(index.raw),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(index: IndexInfo) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (index.unique) Chip("unique")
        if (index.sparse) Chip("sparse")
        index.ttlSeconds?.let { Chip("TTL ${it}s") }
        if (index.partialFilterExpression != null) Chip("partial")
    }
}

@Composable
private fun Chip(label: String) {
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(),
    )
}

private fun keyDisplay(index: IndexInfo): String =
    index.keys.joinToString(", ") { key ->
        val direction = when (val v = key.value) {
            is Number -> when (v.toDouble()) {
                1.0 -> "↑"
                -1.0 -> "↓"
                else -> v.toString()
            }
            else -> v.toString()
        }
        "${key.field} $direction"
    }

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        content()
    }
}
