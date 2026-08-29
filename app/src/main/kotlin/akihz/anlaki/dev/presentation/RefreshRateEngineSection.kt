package akihz.anlaki.dev.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import akihz.anlaki.dev.data.OemSettingsStrategy
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.PreferencesHelper

/**
 * Refresh-rate engine controls.
 *
 * @param onResetToDefaults restores adaptive system refresh-rate settings
 * @param onOpenCustomKeys opens experimental custom-key configuration
 */
@Composable
fun RefreshRateEngineSection(
    onResetToDefaults: () -> Unit,
    onOpenCustomKeys: () -> Unit
) {
    val oemNames = OemSettingsStrategy.getSupportedOemNames()
    var selectedOem by remember { mutableStateOf(PreferencesHelper.oemOverride.ifBlank { "Auto-detect" }) }
    var showResetConfirm by remember { mutableStateOf(false) }

    PreferenceGroup(heading = "Refresh rate") {
        PreferenceTemplate(
            title = "Custom refresh-rate keys",
            description = "Experimental discovery and manual key mappings",
            icon = Icons.Default.Build,
            onClick = onOpenCustomKeys
        )
        OemOverridePreference(
            options = oemNames,
            selectedOption = selectedOem,
            onOptionSelected = { name ->
                selectedOem = name
                PreferencesHelper.oemOverride = if (name == "Auto-detect") "" else name
            }
        )
        PreferenceTemplate(
            title = "Reset to defaults",
            description = "Restore adaptive refresh rate (system defaults)",
            icon = Icons.Default.RestartAlt,
            onClick = { showResetConfirm = true }
        )
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
