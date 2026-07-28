package akihz.anlaki.dev.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OemOverridePreference(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    PreferenceTemplate(
        title = "OEM override",
        description = selectedOption,
        icon = Icons.Default.PhoneAndroid,
        onClick = { showOptions = true }
    )

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "OEM override",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Text(
                text = "Use auto-detect unless your device needs a different settings profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                options.forEach { option ->
                    ListItem(
                        headlineContent = { Text(option) },
                        leadingContent = {
                            RadioButton(
                                selected = option == selectedOption,
                                onClick = null
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                showOptions = false
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
