package akihz.anlaki.dev.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.theme.AppThemeMode

/**
 * Displays watchdog, advanced, and app information preferences.
 *
 * @param onResetToDefaults restores system refresh-rate settings
 * @param themeMode currently selected appearance mode
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 * @param modifier layout modifier supplied by the app shell
 */
@Composable
fun SettingsScreen(
    onResetToDefaults: () -> Unit,
    themeMode: AppThemeMode,
    amoledMode: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceLayout(
        label = "Settings",
        modifier = modifier
    ) {
        ThemeSection(
            themeMode = themeMode,
            amoledMode = amoledMode,
            onThemeModeChanged = onThemeModeChanged,
            onAmoledModeChanged = onAmoledModeChanged
        )
        WatchdogSection()
        AdvancedSection(onResetToDefaults = onResetToDefaults)
        AboutSection()
    }
}
