package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Introduces the app's independent-project status before first use.
 *
 * @param onAccept called when the user acknowledges the notice and wants to continue
 */
@Composable
fun FirstRunNoticeDialog(
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("A quick note before you start") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "akiHz is a small, independently made open-source app. It uses " +
                        "Shizuku to change Android's refresh-rate settings."
                )
                Text(
                    "Phones handle these settings differently, so compatibility and results " +
                        "can vary. The app is shared as-is, without guaranteed support, " +
                        "maintenance, or future updates."
                )
                Text(
                    "If something doesn't work as expected, you can use Reset to defaults in " +
                        "Settings. By continuing, you understand this and choose to use akiHz " +
                        "on your device."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Continue")
            }
        }
    )
}
