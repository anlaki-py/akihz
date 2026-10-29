package akihz.anlaki.dev.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.presentation.theme.LocalDiagnosticFont

/** Copies diagnostics text to the clipboard. */
internal fun copyFpsDiagnostics(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FPS Monitor diagnostics", text))
}

/** Shares diagnostics text through the system chooser. */
internal fun shareFpsDiagnostics(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "FPS Monitor diagnostics")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share diagnostics"))
}

/**
 * Dialog showing the FPS debug log with copy and share actions.
 *
 * @param logText text to show
 * @param onCopy invoked for the copy button
 * @param onShare invoked for the share button
 * @param onDismiss invoked when the dialog closes
 */
@Composable
internal fun FpsDebugLogDialog(
    logText: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FPS Monitor diagnostics") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = logText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = LocalDiagnosticFont.current
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) { Text("Copy") }
        },
        dismissButton = {
            TextButton(onClick = onShare) { Text("Share") }
        }
    )
}
