package com.dmc.mongoclient.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dmc.mongoclient.ui.documents.DocumentsContent
import com.dmc.mongoclient.ui.indexes.IndexesContent

private enum class CollectionTab { DOCUMENTS, INDEXES }

/**
 * Wraps the right-pane / content view with a Documents | Indexes tab row.
 * Tab state is local and resets to Documents whenever the selected
 * (database, collection) changes.
 */
@Composable
fun CollectionContent(
    database: String?,
    collection: String?,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(CollectionTab.DOCUMENTS) }

    LaunchedEffect(database, collection) {
        tab = CollectionTab.DOCUMENTS
    }

    if (collection == null) {
        // Skip the tab row entirely until a collection is selected — there's
        // nothing meaningful to switch between.
        DocumentsContent(database = database, collection = null, modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab.ordinal) {
            Tab(
                selected = tab == CollectionTab.DOCUMENTS,
                onClick = { tab = CollectionTab.DOCUMENTS },
                text = { Text("Documents") },
            )
            Tab(
                selected = tab == CollectionTab.INDEXES,
                onClick = { tab = CollectionTab.INDEXES },
                text = { Text("Indexes") },
            )
        }
        HorizontalDivider()
        Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                CollectionTab.DOCUMENTS -> DocumentsContent(
                    database = database,
                    collection = collection,
                    modifier = Modifier.fillMaxSize(),
                )
                CollectionTab.INDEXES -> IndexesContent(
                    database = database,
                    collection = collection,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
