package akihz.anlaki.dev.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import akihz.anlaki.dev.domain.update.UpdateCheckFrequency

/** Lets the user choose how often automatic update checks run. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateFrequencyDialog(
    selected: UpdateCheckFrequency,
    onSelect: (UpdateCheckFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = "Automatic update checks",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            UpdateCheckFrequency.entries.forEach { frequency ->
                ListItem(
                    headlineContent = { Text(frequency.label) },
                    supportingContent = {
                        Text(
                            if (frequency == UpdateCheckFrequency.Never) {
                                "Only check when requested"
                            } else {
                                "Check approximately ${frequency.label.lowercase()}"
                            }
                        )
                    },
                    leadingContent = {
                        RadioButton(selected = frequency == selected, onClick = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(frequency) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
