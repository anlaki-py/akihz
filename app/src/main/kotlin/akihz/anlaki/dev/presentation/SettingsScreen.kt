package akihz.anlaki.dev.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import akihz.anlaki.dev.presentation.theme.ThemeSection

/**
 * Displays appearance, background, engine, update and about preferences.
 *
 * Layout follows semantic grouping for balanced card sizes.
 *
 * @param onResetToDefaults restores system refresh-rate settings
 * @param onOpenCustomKeys opens custom refresh-rate key configuration
 * @param themeMode currently selected appearance mode
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onOpenThemePreferences opens dynamic color and theme preferences
 * @param onOpenDebugSettings opens the hidden home-screen tuning page
 * @param debugOptionsUnlocked whether the persistent developer entry is visible
 * @param onDebugOptionsUnlocked persists the developer entry after six version taps
 * @param autoCheckRequest changes when an update notification opens this screen
 * @param keepAliveEnabled whether the keep-alive notification is enabled
 * @param onKeepAliveEnabledChanged invoked when the keep-alive toggle changes
 * @param modifier layout modifier supplied by the app shell
 */
@Composable
fun SettingsScreen(
    onResetToDefaults: () -> Unit,
    onOpenCustomKeys: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenThemePreferences: () -> Unit,
    debugOptionsUnlocked: Boolean,
    onDebugOptionsUnlocked: () -> Unit,
    onOpenDebugSettings: () -> Unit,
    modifier: Modifier = Modifier,
    autoCheckRequest: Int = 0,
    keepAliveEnabled: Boolean = true,
    onKeepAliveEnabledChanged: (Boolean) -> Unit = {},
    onOpenFpsMonitor: () -> Unit = {}
) {
    var versionTapCount by remember { mutableIntStateOf(0) }
    PreferenceLayout(
        label = "Settings",
        modifier = modifier
    ) {
        ThemeSection(
            themeMode = themeMode,
            onThemeModeChanged = onThemeModeChanged,
            onOpenThemePreferences = onOpenThemePreferences
        )
        FpsMonitorSection(onOpenFpsMonitor = onOpenFpsMonitor)
        BackgroundSection(
            keepAliveEnabled = keepAliveEnabled,
            onKeepAliveEnabledChanged = onKeepAliveEnabledChanged
        )
        RefreshRateEngineSection(
            onResetToDefaults = onResetToDefaults,
            onOpenCustomKeys = onOpenCustomKeys
        )
        UpdatesSection(autoCheckRequest = autoCheckRequest)
        AboutSection(
            onVersionClick = {
                if (!debugOptionsUnlocked) {
                    versionTapCount++
                    if (versionTapCount >= 6) {
                        versionTapCount = 0
                        onDebugOptionsUnlocked()
                    }
                }
            }
        )
        DiagnosticsSection()
        if (debugOptionsUnlocked) {
            PreferenceGroup(heading = "Developer") {
                PreferenceTemplate(
                    title = "Debug options",
                    description = "Layout controls and app logs",
                    icon = Icons.Default.DeveloperMode,
                    onClick = onOpenDebugSettings
                )
            }
        }
    }
}
