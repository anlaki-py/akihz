package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.CustomRefreshProfile
import akihz.anlaki.dev.data.CustomSettingsKey

/**
 * Edits roles and per-rate values for one selected settings key.
 *
 * @param setting selected custom key
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(setting.name)
                    Text(setting.namespace.name.lowercase())
                }
                IconButton(onClick = onRemove, enabled = enabled) {
                    Text("×")
                }
            }
            RoleToggle("Read for validation", setting.canRead, enabled) {
                onRolesChanged(it, setting.canWrite)
            }
            RoleToggle("Write when switching", setting.canWrite, enabled) {
                onRolesChanged(setting.canRead, it)
            }
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
    }
}

@Composable
private fun RoleToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.padding(top = 12.dp))
        Switch(checked = checked, onCheckedChange = onChanged, enabled = enabled)
    }
}
