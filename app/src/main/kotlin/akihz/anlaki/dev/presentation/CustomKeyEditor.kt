package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.CustomRefreshProfile
import akihz.anlaki.dev.data.CustomSettingsKey
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/**
 * Edits roles and per-rate values for one selected refresh_rate key.
 * Only refresh_rate keys are surfaced by the surrounding screen.
 *
 * @param setting selected custom key containing refresh_rate
 * @param rates refresh rates exposed by the profile
 * @param enabled whether editing controls are enabled
 * @param onRolesChanged receives updated read and write roles
 * @param onValueChanged receives a rate and its raw setting value
 * @param onRemove removes this key from the profile
 */
@Composable
fun CustomKeyEditor(
    setting: CustomSettingsKey,
    rates: List<Float>,
    enabled: Boolean,
    onRolesChanged: (Boolean, Boolean) -> Unit,
    onValueChanged: (Float, String) -> Unit,
    onRemove: () -> Unit
) {
    PreferenceGroup(heading = setting.id) {
        PreferenceTemplate(
            title = "Read for validation",
            description = "Read key to verify current rate",
            icon = Icons.Default.Visibility,
            checked = setting.canRead,
            onCheckedChange = if (enabled) {
                { onRolesChanged(it, setting.canWrite) }
            } else null
        )
        PreferenceTemplate(
            title = "Write when switching",
            description = "Write key when applying a rate",
            icon = Icons.Default.Edit,
            checked = setting.canWrite,
            onCheckedChange = if (enabled) {
                { onRolesChanged(setting.canRead, it) }
            } else null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Values per rate",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            rates.forEach { rate ->
                OutlinedTextField(
                    value = setting.values[CustomRefreshProfile.rateKey(rate)].orEmpty(),
                    onValueChange = { onValueChanged(rate, it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true,
                    label = { Text("${CustomRefreshProfile.rateLabel(rate)} Hz value") }
                )
            }
        }
        PreferenceTemplate(
            title = "Remove key",
            description = "Delete ${setting.name} from profile",
            icon = Icons.Default.Delete,
            onClick = if (enabled) onRemove else null
        )
    }
}
