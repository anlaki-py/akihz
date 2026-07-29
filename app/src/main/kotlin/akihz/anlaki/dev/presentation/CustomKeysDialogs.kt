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

/** Collects a manually entered namespace and key name. */
@Composable
fun AddRawKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (OemSettingsStrategy.Namespace, String) -> Unit
) {
    var namespace by remember { mutableStateOf(OemSettingsStrategy.Namespace.SYSTEM) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add raw settings key") },
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
                    label = { Text("Key name") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onAdd(namespace, name); onDismiss() }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
