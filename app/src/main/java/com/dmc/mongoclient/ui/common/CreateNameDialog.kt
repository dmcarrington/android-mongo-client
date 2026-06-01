package com.dmc.mongoclient.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Single- or two-field dialog used to name a new database or collection.
 * Trimmed values are passed to [onConfirm]; the secondary value is null when
 * [secondaryLabel] is null. Confirm enables once every visible field has at
 * least one non-whitespace character.
 */
@Composable
fun CreateNameDialog(
    title: String,
    primaryLabel: String,
    secondaryLabel: String? = null,
    confirmButtonLabel: String = "Create",
    onConfirm: (primary: String, secondary: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    val canConfirm = primary.isNotBlank() &&
        (secondaryLabel == null || secondary.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = primary,
                    onValueChange = { primary = it },
                    label = { Text(primaryLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(fontFamily = FontFamily.Monospace),
                )
                if (secondaryLabel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secondary,
                        onValueChange = { secondary = it },
                        label = { Text(secondaryLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                            .copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        primary.trim(),
                        if (secondaryLabel != null) secondary.trim() else null,
                    )
                },
                enabled = canConfirm,
            ) { Text(confirmButtonLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
