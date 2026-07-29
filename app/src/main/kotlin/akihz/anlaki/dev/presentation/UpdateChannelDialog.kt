package akihz.anlaki.dev.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.domain.update.UpdateChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateChannelDialog(
    selected: UpdateChannel,
    onSelect: (UpdateChannel) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = "Update channel",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
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
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(channel) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
