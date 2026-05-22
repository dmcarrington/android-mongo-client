package com.dmc.mongoclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.dmc.mongoclient.ui.theme.MongoClientTheme
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.connection.TransportSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import javax.net.ssl.SSLContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.Document
import org.conscrypt.Conscrypt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MongoClientTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpikeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpikeScreen() {
    var uri by remember { mutableStateOf("") }
    var output by remember {
        mutableStateOf("Enter a MongoDB connection URI and tap Connect.")
    }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("MongoDB Connection Spike") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uri,
                onValueChange = { uri = it },
                label = { Text("Connection URI") },
                placeholder = { Text("mongodb+srv://user:pass@cluster.mongodb.net/") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                enabled = !busy,
            )

            Button(
                onClick = {
                    busy = true
                    output = "Connecting…"
                    scope.launch {
                        output = runProbe(uri.trim())
                        busy = false
                    }
                },
                enabled = uri.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Working…" else "Connect")
            }

            Text("Output", style = MaterialTheme.typography.titleMedium)
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}

private suspend fun runProbe(uri: String): String = withContext(Dispatchers.IO) {
    val client = try {
        // The driver's default async transport uses a vendored TlsChannel that
        // mis-flips NIO buffers on Android — SSLEngine.unwrap() never advances.
        // Netty's transport sidesteps it entirely. Conscrypt-backed SSLContext
        // still feeds into Netty's SslHandler.
        val sslContext = SSLContext.getInstance("TLS", Conscrypt.newProvider())
            .apply { init(null, null, null) }
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .applyToSslSettings { it.context(sslContext) }
            .transportSettings(TransportSettings.nettyBuilder().build())
            .build()
        MongoClient.create(settings)
    } catch (t: Throwable) {
        return@withContext buildString {
            appendLine("Failed to create MongoClient:")
            append(t.stackTraceToString())
        }
    }

    try {
        buildString {
            val ping = client.getDatabase("admin").runCommand(Document("ping", 1))
            appendLine("ping → $ping")
            appendLine()
            appendLine("Databases:")
            client.listDatabaseNames().collect { name ->
                appendLine("  • $name")
            }
        }
    } catch (t: Throwable) {
        buildString {
            appendLine("Probe failed:")
            append(t.stackTraceToString())
        }
    } finally {
        runCatching { client.close() }
    }
}
