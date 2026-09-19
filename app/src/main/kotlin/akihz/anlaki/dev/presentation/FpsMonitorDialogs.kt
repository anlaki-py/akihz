package akihz.anlaki.dev.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.fps.OverlayPillColor

/** Human label for a pill color choice. */
internal fun pillColorLabel(color: OverlayPillColor): String =
    when (color) {
        OverlayPillColor.Black -> "Black, white text"
        OverlayPillColor.Green -> "Green, white text"
        OverlayPillColor.Red -> "Red, white text"
        OverlayPillColor.White -> "White, black text"
    }

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
                    fontFamily = FontFamily.Monospace
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

/**
 * Bottom sheet for picking the FPS pill background color.
 *
 * @param selected currently active color
 * @param onSelected invoked when the user picks a color
 * @param onDismiss invoked when the sheet closes without a pick
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PillColorSheet(
    selected: OverlayPillColor,
    onSelected: (OverlayPillColor) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = "Pill color",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            OverlayPillColor.entries.forEach { color ->
                ListItem(
                    headlineContent = { Text(pillColorLabel(color)) },
                    leadingContent = {
                        RadioButton(
                            selected = color == selected,
                            onClick = null
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
