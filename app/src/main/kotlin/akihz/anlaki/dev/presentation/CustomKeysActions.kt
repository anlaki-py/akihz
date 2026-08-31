package akihz.anlaki.dev.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Confirmation countdown displayed while temporary custom values are active. */
@Composable
fun TestCountdownDialog(seconds: Int, onConfirm: () -> Unit, onReject: () -> Unit) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text("Keep this test result?") },
        text = {
            Text(
                "Is the display working at the expected rate? Original values will be restored in $seconds seconds."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("It worked") } },
        dismissButton = { TextButton(onClick = onReject) { Text("Restore now") } }
    )
}
