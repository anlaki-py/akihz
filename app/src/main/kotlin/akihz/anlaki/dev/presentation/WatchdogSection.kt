package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.RefreshRateWatchdogService

/**
 * Displays controls for configuring the refresh-rate watchdog service.
 */
@Composable
fun WatchdogSection() {
    val context = LocalContext.current
    var watchdogEnabled by remember { mutableStateOf(PreferencesHelper.watchdogEnabled) }
    var aggressive by remember { mutableStateOf(PreferencesHelper.watchdogAggressive) }
    var intervalMs by remember { mutableFloatStateOf(PreferencesHelper.watchdogIntervalMs.toFloat()) }
    var showInfo by remember { mutableStateOf(false) }

    PreferenceGroup(heading = "Watchdog") {
        PreferenceTemplate(
            title = "Enable watchdog",
            description = "Monitor and re-apply refresh rate automatically (untested on all devices)",
            icon = Icons.Default.Shield,
            checked = watchdogEnabled,
            onCheckedChange = {
                watchdogEnabled = it
                PreferencesHelper.watchdogEnabled = it
                RefreshRateWatchdogService.restart(context)
            }
        )

        if (watchdogEnabled) {
            PreferenceTemplate(
                title = "Aggressive mode",
                description = "Check every 500ms (uses more battery)",
                icon = Icons.Default.Bolt,
                checked = aggressive,
                onCheckedChange = {
                    aggressive = it
                    PreferencesHelper.watchdogAggressive = it
                    RefreshRateWatchdogService.restart(context)
                }
            )

            if (!aggressive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Check interval: ${(intervalMs / 1000).toInt()}s",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Slider(
                    value = intervalMs,
                    onValueChange = { intervalMs = it },
                    onValueChangeFinished = {
                        PreferencesHelper.watchdogIntervalMs = intervalMs.toLong()
                        RefreshRateWatchdogService.restart(context)
                    },
                    valueRange = 1000f..30000f,
                    steps = 28,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Watchdog") },
            text = {
                Text(
                    "The watchdog service monitors your current refresh rate and re-applies " +
                    "your desired rate when the system overrides it."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("OK")
                }
            }
        )
    }
}
