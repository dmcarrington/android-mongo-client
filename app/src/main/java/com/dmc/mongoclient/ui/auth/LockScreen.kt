package com.dmc.mongoclient.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    statusMessage: String? = null,
    onUnlock: () -> Unit,
) {
    // Auto-trigger the biometric prompt on first composition so the user
    // doesn't need an extra tap on cold start. If they cancel, the manual
    // Unlock button below stays available.
    LaunchedEffect(Unit) { onUnlock() }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(16.dp))
        Text("Sextant is locked", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(8.dp))
        Text(
            text = statusMessage ?: "Authenticate to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(32.dp))
        Button(onClick = onUnlock) {
            Text("Unlock")
        }
    }
}
