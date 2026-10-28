package akihz.anlaki.dev.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/**
 * Entry that opens the Surface FPS Monitor page.
 */
@Composable
fun FpsMonitorSection(
    onOpenFpsMonitor: () -> Unit
) {
    PreferenceGroup(heading = "FPS Monitor") {
        PreferenceTemplate(
            title = "Surface FPS Monitor",
            description = "Show a floating FPS counter",
            icon = Icons.Default.Speed,
            onClick = onOpenFpsMonitor
        )
    }
}
