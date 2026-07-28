package akihz.anlaki.dev.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.presentation.components.PreferenceLayout

/**
 * Displays watchdog, advanced, and app information preferences.
 *
 * @param onResetToDefaults restores system refresh-rate settings
 * @param modifier layout modifier supplied by the app shell
 */
@Composable
fun SettingsScreen(
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceLayout(
        label = "Settings",
        modifier = modifier
    ) {
        WatchdogSection()
        AdvancedSection(onResetToDefaults = onResetToDefaults)
        AboutSection()
    }
}
