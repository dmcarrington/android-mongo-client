package com.dmc.mongoclient.ui.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmc.mongoclient.domain.model.ConnectionTestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionEditScreen(
    onClose: () -> Unit,
    viewModel: ConnectionEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) "New Connection" else "Edit Connection") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = state.canSave,
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.uri,
                onValueChange = viewModel::onUriChange,
                label = { Text("Connection URI") },
                placeholder = { Text("mongodb+srv://user:pass@cluster.mongodb.net/") },
                singleLine = false,
                maxLines = 4,
                isError = state.parseError != null,
                supportingText = {
                    when {
                        state.parseError != null -> Text(state.parseError!!)
                        state.redactedPreview.isNotEmpty() -> Text(
                            text = "Preview: ${state.redactedPreview}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
                visualTransformation = if (state.showPassword) {
                    VisualTransformation.None
                } else {
                    UriPasswordTransformation
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = if (state.showPassword) "Hide password" else "Show password",
                        )
                    }
                },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::test,
                    enabled = !state.testing && state.uri.isNotBlank() && state.parseError == null,
                ) {
                    Text(if (state.testing) "Testing…" else "Test")
                }
            }

            when (val r = state.testResult) {
                is ConnectionTestResult.Ok -> Text(
                    "Connected. ${r.databaseCount} database(s), ${r.durationMs} ms.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                is ConnectionTestResult.Error -> Text(
                    r.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                null -> Spacer(Modifier.size(0.dp))
            }
        }
    }
}

/**
 * Replaces the password segment of a `mongodb[+srv]://user:password@host/...`
 * URI with bullet characters of the same length, so the visible string is the
 * same length as the source and the offset mapping stays identity (cursor
 * positions and selection survive editing).
 */
private object UriPasswordTransformation : VisualTransformation {
    private val regex = Regex(
        """^(mongodb(?:\+srv)?://)([^:/?#@\s]+):([^@\s]+)@""",
        RegexOption.IGNORE_CASE,
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val match = regex.find(original)
            ?: return TransformedText(text, OffsetMapping.Identity)
        val pwdRange = match.groups[3]!!.range
        val masked = buildString {
            append(original, 0, pwdRange.first)
            repeat(pwdRange.last - pwdRange.first + 1) { append('•') }
            append(original, pwdRange.last + 1, original.length)
        }
        return TransformedText(AnnotatedString(masked), OffsetMapping.Identity)
    }
}
