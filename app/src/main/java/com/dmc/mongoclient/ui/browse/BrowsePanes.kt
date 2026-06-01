package com.dmc.mongoclient.ui.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmc.mongoclient.domain.model.CollectionSummary
import com.dmc.mongoclient.domain.model.CollectionType
import com.dmc.mongoclient.domain.model.DatabaseSummary
import java.util.Locale

@Composable
fun DatabaseListPane(
    databases: List<DatabaseSummary>,
    selected: String?,
    loading: Boolean,
    onSelect: (String) -> Unit,
    onLongPress: (DatabaseSummary) -> Unit,
    onAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PaneFrame(
        modifier = modifier,
        title = "Databases",
        onAdd = onAdd,
        addContentDescription = "New database",
    ) {
        when {
            loading && databases.isEmpty() -> Centered { CircularProgressIndicator() }
            databases.isEmpty() -> Centered { Text("No databases", style = MaterialTheme.typography.bodyMedium) }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = databases, key = { it.name }) { db ->
                    DatabaseRow(
                        db = db,
                        selected = db.name == selected,
                        onClick = { onSelect(db.name) },
                        onLongPress = { onLongPress(db) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun CollectionListPane(
    database: String?,
    collections: List<CollectionSummary>,
    selected: String?,
    loading: Boolean,
    onSelect: (String) -> Unit,
    onLongPress: (CollectionSummary) -> Unit,
    onAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PaneFrame(
        modifier = modifier,
        title = database ?: "Collections",
        // No add button until a database is selected — collections need one.
        onAdd = if (database != null) onAdd else null,
        addContentDescription = "New collection",
    ) {
        when {
            database == null -> Centered {
                Text("Select a database", style = MaterialTheme.typography.bodyMedium)
            }
            loading && collections.isEmpty() -> Centered { CircularProgressIndicator() }
            collections.isEmpty() -> Centered {
                Text("No collections", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = collections, key = { it.name }) { col ->
                    CollectionRow(
                        col = col,
                        selected = col.name == selected,
                        onClick = { onSelect(col.name) },
                        onLongPress = { onLongPress(col) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatabaseRow(
    db: DatabaseSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onLongPress)
        .background(
            if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Column(modifier = rowMod) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = db.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f, fill = true),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            if (db.empty) {
                Text(
                    text = "empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (db.sizeOnDisk != null && !db.empty) {
            Text(
                text = formatBytes(db.sizeOnDisk),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionRow(
    col: CollectionSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onLongPress)
        .background(
            if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Column(modifier = rowMod) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = col.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f, fill = true),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            if (col.type != CollectionType.COLLECTION) {
                Text(
                    text = col.type.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        col.estimatedDocumentCount?.let { count ->
            Text(
                text = "${formatCount(count)} document(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaneFrame(
    modifier: Modifier,
    title: String,
    onAdd: (() -> Unit)? = null,
    addContentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            if (onAdd != null) {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = addContentDescription)
                }
            }
        }
        HorizontalDivider()
        Box(modifier = Modifier.fillMaxSize(), content = { content() })
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        content()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024.0
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}

private fun formatCount(n: Long): String = String.format(Locale.getDefault(), "%,d", n)
