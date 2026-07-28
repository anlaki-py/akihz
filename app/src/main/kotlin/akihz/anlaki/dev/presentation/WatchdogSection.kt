package akihz.anlaki.dev.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceSlider
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
                PreferenceSlider(
                    title = "Check interval",
                    value = intervalMs,
                    onValueChange = { intervalMs = it },
                    onValueChangeFinished = { newValue ->
                        PreferencesHelper.watchdogIntervalMs = newValue.toLong()
                        RefreshRateWatchdogService.restart(context)
                    },
                    valueLabel = "${(intervalMs / 1000).toInt()}s",
                    valueRange = 1000f..30000f,
                    steps = 28,
                    increment = 1000f
                )
            }
        }
    }
}
