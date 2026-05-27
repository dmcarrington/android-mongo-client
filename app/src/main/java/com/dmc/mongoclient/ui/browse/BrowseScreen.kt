package com.dmc.mongoclient.ui.browse

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.rememberUpdatedState
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

    val widthClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass
    val isStacked = widthClass != WindowWidthSizeClass.EXPANDED

    // Pane stack state for the stacked layouts (Compact + Medium). Lives at
    // the screen level so both the top-bar back arrow and the system back
    // handler can step through it consistently.
    var compactPane by rememberSaveable { mutableStateOf(CompactPane.DATABASES) }

    LaunchedEffect(state.disconnected) {
        if (state.disconnected) onDisconnected()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val handleBack = rememberUpdatedState {
        if (isStacked && compactPane != CompactPane.DATABASES) {
            compactPane = when (compactPane) {
                CompactPane.CONTENT -> CompactPane.COLLECTIONS
                CompactPane.COLLECTIONS -> CompactPane.DATABASES
                CompactPane.DATABASES -> CompactPane.DATABASES
            }
        } else {
            viewModel.disconnect(onDisconnected)
        }
    }

    // System back uses the same logic as the top-bar arrow.
    BackHandler { handleBack.value() }

    val onSelectDatabase: (String) -> Unit = { name ->
        viewModel.selectDatabase(name)
        if (isStacked) compactPane = CompactPane.COLLECTIONS
    }
    val onSelectCollection: (String) -> Unit = { name ->
        viewModel.selectCollection(name)
        if (isStacked) compactPane = CompactPane.CONTENT
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle(state, isStacked, compactPane)) },
                navigationIcon = {
                    IconButton(onClick = { handleBack.value() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (isStacked) {
                StackedPanes(
                    state = state,
                    pane = compactPane,
                    onSelectDatabase = onSelectDatabase,
                    onSelectCollection = onSelectCollection,
                    onDropDatabase = { viewModel.requestDrop(DropTarget.Database(it.name)) },
                    onDropCollection = { db, col -> viewModel.requestDrop(DropTarget.Collection(db, col.name)) },
                )
            } else {
                ThreePane(
                    state = state,
                    onSelectDatabase = onSelectDatabase,
                    onSelectCollection = onSelectCollection,
                    onDropDatabase = { viewModel.requestDrop(DropTarget.Database(it.name)) },
                    onDropCollection = { db, col -> viewModel.requestDrop(DropTarget.Collection(db, col.name)) },
                )
            }
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

private fun topBarTitle(state: BrowseUiState, stacked: Boolean, pane: CompactPane): String {
    if (!stacked) return state.selectedDatabase ?: "Browse"
    return when (pane) {
        CompactPane.DATABASES -> "Databases"
        CompactPane.COLLECTIONS -> state.selectedDatabase ?: "Collections"
        CompactPane.CONTENT -> state.selectedCollection ?: state.selectedDatabase ?: "Browse"
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
    pane: CompactPane,
    onSelectDatabase: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onDropDatabase: (com.dmc.mongoclient.domain.model.DatabaseSummary) -> Unit,
    onDropCollection: (String, com.dmc.mongoclient.domain.model.CollectionSummary) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().fillMaxWidth()) {
        when (pane) {
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
