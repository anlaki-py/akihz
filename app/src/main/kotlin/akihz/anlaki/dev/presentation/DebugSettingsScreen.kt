package akihz.anlaki.dev.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/**
 * Displays debug categories. The app back stack owns navigation to each detail page.
 *
 * @param onBack returns to Settings
 * @param onOpenHomeScreen opens home-screen tuning
 * @param onOpenPerformance opens performance monitoring
 * @param onOpenCrashLogs opens crash reports
 * @param modifier page layout modifier
 */
@Composable
internal fun DebugSettingsScreen(
    onBack: () -> Unit,
    onOpenHomeScreen: () -> Unit,
    onOpenPerformance: () -> Unit,
    onOpenCrashLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceLayout(
        label = "Debug options",
        modifier = modifier,
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back to settings",
        onNavigationClick = onBack
    ) {
        PreferenceGroup(heading = "Interface") {
            PreferenceTemplate(
                title = "Home screen",
                description = "Adjust text, buttons, and spacing",
                icon = Icons.Default.Home,
                onClick = onOpenHomeScreen
            )
        }
        PreferenceGroup(heading = "Diagnostics") {
            PreferenceTemplate(
                title = "Performance monitoring",
                description = "Record CPU, memory, and refresh rate",
                icon = Icons.Default.Speed,
                onClick = onOpenPerformance
            )
            PreferenceTemplate(
                title = "Error logs",
                description = "View and share app logs",
                icon = Icons.Default.BugReport,
                onClick = onOpenCrashLogs
            )
        }
    }
}
