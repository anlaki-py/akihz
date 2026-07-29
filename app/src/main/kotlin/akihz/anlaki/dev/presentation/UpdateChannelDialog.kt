package akihz.anlaki.dev.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.domain.update.UpdateChannel

@Composable
internal fun UpdateChannelDialog(
    selected: UpdateChannel,
    onSelect: (UpdateChannel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update channel") },
        text = {
            Column {
                UpdateChannel.entries.forEach { channel ->
                    ListItem(
                        headlineContent = { Text(channel.label) },
                        supportingContent = {
                            Text(
                                if (channel == UpdateChannel.Stable) {
                                    "Stable releases only"
                                } else {
                                    "Betas and stable releases"
                                }
                            )
                        },
                        leadingContent = {
                            RadioButton(selected = channel == selected, onClick = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(channel) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                Text("Cancel")
            }
        }
    )
}
