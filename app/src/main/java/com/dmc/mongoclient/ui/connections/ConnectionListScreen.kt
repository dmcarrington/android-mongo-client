package com.dmc.mongoclient.ui.connections

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmc.mongoclient.domain.model.SavedConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionListScreen(
    onAddConnection: () -> Unit,
    onEditConnection: (Long) -> Unit,
    onConnected: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ConnectionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<SavedConnection?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is ConnectionListEvent.ConnectedTo -> {
                viewModel.consumeEvent()
                onConnected(e.id)
            }
            is ConnectionListEvent.ConnectFailed -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Connections") },
                actions = {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                overflowOpen = false
                                onOpenSettings()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddConnection) {
                Icon(Icons.Default.Add, contentDescription = "Add connection")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading -> Unit
            state.connections.isEmpty() -> EmptyState(innerPadding)
            else -> ConnectionList(
                connections = state.connections,
                contentPadding = innerPadding,
                onConnect = viewModel::connect,
                onEdit = onEditConnection,
                onDelete = { pendingDelete = it },
            )
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete connection?") },
            text = { Text("\"${target.displayName}\" will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("No saved connections", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap + to add a MongoDB connection URI.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ConnectionList(
    connections: List<SavedConnection>,
    contentPadding: PaddingValues,
    onConnect: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (SavedConnection) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = connections, key = { it.id }) { conn ->
            ConnectionCard(
                connection = conn,
                onConnect = { onConnect(conn.id) },
                onEdit = { onEdit(conn.id) },
                onDelete = { onDelete(conn) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        item { Spacer(Modifier.size(72.dp)) }
    }
}

@Composable
private fun ConnectionCard(
    connection: SavedConnection,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(connection.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            Text(
                text = connection.redactedUri,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            Row(onConnect = onConnect, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun Row(onConnect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onConnect) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Connect")
        }
    }
}
