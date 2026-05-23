package com.dmc.mongoclient.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.dmc.mongoclient.ui.common.TypeToConfirmDialog

private enum class CompactPane { DATABASES, COLLECTIONS, CONTENT }

@Composable
private fun ConnectionLostBanner(
    reconnecting: Boolean,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "Connection lost",
                style = MaterialTheme.typography.bodyMedium,
                modifier = androidx.compose.ui.Modifier.weight(1f),
            )
            TextButton(onClick = onDisconnect, enabled = !reconnecting) {
                Text("Exit")
            }
            TextButton(onClick = onReconnect, enabled = !reconnecting) {
                Text(if (reconnecting) "Reconnecting…" else "Reconnect")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onDisconnected: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
    adaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var overflowOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.disconnected) {
        if (state.disconnected) onDisconnected()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedDatabase ?: "Browse") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.disconnect(onDisconnected) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Disconnect")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshDatabases) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    TextButton(onClick = { overflowOpen = true }) {
                        Text(if (state.showSystemDbs) "All DBs" else "User DBs")
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (state.showSystemDbs) "Hide system DBs" else "Show system DBs") },
                            onClick = {
                                overflowOpen = false
                                viewModel.toggleShowSystemDbs()
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (state.connectionLost) {
                ConnectionLostBanner(
                    reconnecting = state.reconnecting,
                    onReconnect = viewModel::reconnect,
                    onDisconnect = { viewModel.disconnect(onDisconnected) },
                )
            }
            BrowseLayout(
                state = state,
                widthClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass,
                onSelectDatabase = viewModel::selectDatabase,
                onSelectCollection = viewModel::selectCollection,
                onDropDatabase = { viewModel.requestDrop(DropTarget.Database(it.name)) },
                onDropCollection = { db, col -> viewModel.requestDrop(DropTarget.Collection(db, col.name)) },
            )
        }
    }

    state.pendingDrop?.let { drop ->
        when (drop) {
            is DropTarget.Database -> TypeToConfirmDialog(
                title = "Drop database",
                message = "This will permanently delete \"${drop.name}\" and every collection it contains. This cannot be undone.",
                confirmationText = drop.name,
                onConfirm = viewModel::confirmDrop,
                onDismiss = viewModel::cancelDrop,
            )
            is DropTarget.Collection -> TypeToConfirmDialog(
                title = "Drop collection",
                message = "This will permanently delete \"${drop.database}/${drop.name}\". This cannot be undone.",
                confirmationText = drop.name,
                onConfirm = viewModel::confirmDrop,
                onDismiss = viewModel::cancelDrop,
            )
        }
    }
}

@Composable
private fun BrowseLayout(
    state: BrowseUiState,
    widthClass: WindowWidthSizeClass,
    onSelectDatabase: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onDropDatabase: (com.dmc.mongoclient.domain.model.DatabaseSummary) -> Unit,
    onDropCollection: (String, com.dmc.mongoclient.domain.model.CollectionSummary) -> Unit,
) {
    when (widthClass) {
        WindowWidthSizeClass.EXPANDED -> ThreePane(
            state, onSelectDatabase, onSelectCollection, onDropDatabase, onDropCollection,
        )
        // Medium fits a two-pane (dbs+collections) view but not three.
        // Falling through to stacked keeps documents reachable; the dedicated
        // dbs-as-dropdown design is deferred polish.
        else -> StackedPanes(
            state, onSelectDatabase, onSelectCollection, onDropDatabase, onDropCollection,
        )
    }
}

@Composable
private fun ThreePane(
    state: BrowseUiState,
    onSelectDatabase: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onDropDatabase: (com.dmc.mongoclient.domain.model.DatabaseSummary) -> Unit,
    onDropCollection: (String, com.dmc.mongoclient.domain.model.CollectionSummary) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        DatabaseListPane(
            databases = state.databases,
            selected = state.selectedDatabase,
            loading = state.loadingDatabases,
            onSelect = onSelectDatabase,
            onLongPress = onDropDatabase,
            modifier = Modifier.width(280.dp),
        )
        VerticalDivider()
        CollectionListPane(
            database = state.selectedDatabase,
            collections = state.collections,
            selected = state.selectedCollection,
            loading = state.loadingCollections,
            onSelect = onSelectCollection,
            onLongPress = { col -> state.selectedDatabase?.let { onDropCollection(it, col) } },
            modifier = Modifier.width(320.dp),
        )
        VerticalDivider()
        CollectionContent(
            database = state.selectedDatabase,
            collection = state.selectedCollection,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StackedPanes(
    state: BrowseUiState,
    onSelectDatabase: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onDropDatabase: (com.dmc.mongoclient.domain.model.DatabaseSummary) -> Unit,
    onDropCollection: (String, com.dmc.mongoclient.domain.model.CollectionSummary) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(CompactPane.DATABASES) }

    // When a selection is made, advance to the next pane. When selection clears,
    // ensure we don't render a pane that has no data to show.
    LaunchedEffect(state.selectedDatabase) {
        if (state.selectedDatabase != null && page == CompactPane.DATABASES) {
            page = CompactPane.COLLECTIONS
        } else if (state.selectedDatabase == null) {
            page = CompactPane.DATABASES
        }
    }
    LaunchedEffect(state.selectedCollection) {
        if (state.selectedCollection != null && page != CompactPane.CONTENT) {
            page = CompactPane.CONTENT
        }
    }

    BackHandler(enabled = page != CompactPane.DATABASES) {
        page = when (page) {
            CompactPane.CONTENT -> CompactPane.COLLECTIONS
            CompactPane.COLLECTIONS -> CompactPane.DATABASES
            CompactPane.DATABASES -> CompactPane.DATABASES
        }
    }

    Box(modifier = Modifier.fillMaxSize().fillMaxWidth()) {
        when (page) {
            CompactPane.DATABASES -> DatabaseListPane(
                databases = state.databases,
                selected = state.selectedDatabase,
                loading = state.loadingDatabases,
                onSelect = onSelectDatabase,
                onLongPress = onDropDatabase,
                modifier = Modifier.fillMaxSize(),
            )
            CompactPane.COLLECTIONS -> CollectionListPane(
                database = state.selectedDatabase,
                collections = state.collections,
                selected = state.selectedCollection,
                loading = state.loadingCollections,
                onSelect = onSelectCollection,
                onLongPress = { col -> state.selectedDatabase?.let { onDropCollection(it, col) } },
                modifier = Modifier.fillMaxSize(),
            )
            CompactPane.CONTENT -> CollectionContent(
                database = state.selectedDatabase,
                collection = state.selectedCollection,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
