package akihz.anlaki.dev.presentation.glass

import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.json.JSONException

/** Imports pasted glass JSON through [onImport], or closes through [onDismiss]. */
@Composable
internal fun GlassPresetImportDialog(
    onDismiss: () -> Unit,
    onImport: (NavbarGlassSettings) -> Unit
) {
    var json by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import glass preset") },
        text = {
            OutlinedTextField(
                value = json,
                onValueChange = {
                    if (it.length <= 65_536) {
                        json = it
                        error = null
                    } else {
                        error = "Preset is too large. Limit it to 65,536 characters."
                    }
                },
                label = { Text("Preset JSON") },
                minLines = 4,
                maxLines = 10,
                isError = error != null,
                supportingText = { Text(error ?: "Applies glass controls. Keeps your current app theme.") }
            )
        },
        confirmButton = {
            TextButton(
                enabled = json.isNotBlank(),
                onClick = {
                    val preset = try {
                        NavbarGlassSettings.fromJson(json)
                    } catch (_: JSONException) {
                        error = "Invalid JSON. Paste a complete glass preset."
                        null
                    } catch (failure: IllegalArgumentException) {
                        error = failure.message ?: "Unsupported glass preset."
                        null
                    }
                    if (preset != null) {
                        onImport(preset)
                        onDismiss()
                    }
                }
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
