package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.OemSettingsStrategy
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.BatteryOptimizationHelper
import akihz.anlaki.dev.utils.PreferencesHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSection(onResetToDefaults: () -> Unit) {
    val context = LocalContext.current
    val oemNames = OemSettingsStrategy.getSupportedOemNames()
    var selectedOem by remember { mutableStateOf(PreferencesHelper.oemOverride.ifBlank { "Auto-detect" }) }
    var showDebug by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val batteryUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

    PreferenceGroup(heading = "Advanced") {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "OEM Override",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedOem,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    oemNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedOem = name
                                PreferencesHelper.oemOverride = if (name == "Auto-detect") "" else name
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        PreferenceTemplate(
            title = "Battery optimization",
            description = if (batteryUnrestricted) {
                "Unrestricted — background service can run reliably"
            } else {
                "Tap to exclude akiHz from battery restrictions"
            },
            onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }
        )

        PreferenceTemplate(
            title = "Reset to defaults",
            description = "Restore adaptive refresh rate (system defaults)",
            onClick = { showResetConfirm = true }
        )

        PreferenceTemplate(
            title = if (showDebug) "Hide debug info" else "Show debug info",
            onClick = { showDebug = !showDebug }
        )

        if (showDebug) {
            DebugInfo()
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to defaults") },
            text = {
                Text(
                    "This clears OEM refresh rate settings and restores adaptive mode. " +
                    "Your akiHz preferences are kept."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetToDefaults()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DebugInfo() {
    val strategy = OemSettingsStrategy.resolve()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            "Detected OEM: ${android.os.Build.MANUFACTURER}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Brand: ${android.os.Build.BRAND}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Model: ${android.os.Build.MODEL}",
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Read keys:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        strategy.readKeys.forEach {
            Text(
                "  ${it.namespace.name.lowercase()}/${it.key}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Write keys:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        strategy.writeKeys.forEach {
            Text(
                "  ${it.namespace.name.lowercase()}/${it.key}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
