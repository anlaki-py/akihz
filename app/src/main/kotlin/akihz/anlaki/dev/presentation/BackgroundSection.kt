package akihz.anlaki.dev.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.BatteryOptimizationHelper

/**
 * Background execution controls.
 *
 * @param keepAliveEnabled whether the keep-alive foreground notification is enabled
 * @param onKeepAliveEnabledChanged invoked when the toggle changes
 */
@Composable
fun BackgroundSection(
    keepAliveEnabled: Boolean,
    onKeepAliveEnabledChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    PreferenceGroup(heading = "Background") {
        PreferenceTemplate(
            title = "Keep alive notification",
            description = "Keep foreground notification for reliable tile switching",
            icon = Icons.Default.Notifications,
            checked = keepAliveEnabled,
            onCheckedChange = onKeepAliveEnabledChanged
        )
        PreferenceTemplate(
            title = "Allow background running",
            description = "Exclude akiHz from Android battery optimization",
            icon = Icons.Default.BatterySaver,
            onClick = { BatteryOptimizationHelper.requestExemption(context) }
        )
    }
}
