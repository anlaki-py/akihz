package akihz.anlaki.dev.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import akihz.anlaki.dev.data.CustomRefreshProfile
import akihz.anlaki.dev.data.OemSettingsStrategy

/** Displays the one-time warning before custom-key discovery is opened. */
@Composable
fun CustomKeysWarning(onAccept: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLeave,
        title = { Text("Experimental advanced feature") },
        text = {
            Text(
                "Incorrect Android settings keys or values can cause display problems. " +
                    "akiHz will test changes temporarily and restore captured values, " +
                    "but this feature is intended for advanced users."
            )
        },
        confirmButton = { TextButton(onClick = onAccept) { Text("I understand") } },
        dismissButton = { TextButton(onClick = onLeave) { Text("Go back") } }
    )
}

/** Collects a manually entered namespace and key name. Only refresh_rate keys are allowed. */
@Composable
fun AddRawKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (OemSettingsStrategy.Namespace, String) -> Unit
) {
    var namespace by remember { mutableStateOf(OemSettingsStrategy.Namespace.SYSTEM) }
    var name by remember { mutableStateOf("") }
    val isRefreshRateKey = CustomRefreshProfile.isRefreshRateKey(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add refresh_rate key") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("Namespace")
                androidx.compose.foundation.layout.Row {
                    OemSettingsStrategy.Namespace.entries.forEach { option ->
                        TextButton(onClick = { namespace = option }) {
                            Text(if (namespace == option) "✓ ${option.name.lowercase()}" else option.name.lowercase())
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Key name") },
                    supportingText = {
                        if (name.isNotBlank() && !isRefreshRateKey) {
                            Text("Key must contain \"refresh_rate\"")
                        }
                    },
                    isError = name.isNotBlank() && !isRefreshRateKey
                )
                if (name.isBlank()) {
                    androidx.compose.material3.Text(
                        "Example: peak_refresh_rate, min_refresh_rate",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && isRefreshRateKey,
                onClick = { onAdd(namespace, name); onDismiss() }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
