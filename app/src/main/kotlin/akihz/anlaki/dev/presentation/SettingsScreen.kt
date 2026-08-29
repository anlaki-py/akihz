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

/**
 * Displays appearance, background, engine, update and about preferences.
 *
 * Layout follows semantic grouping for balanced card sizes.
 *
 * @param onResetToDefaults restores system refresh-rate settings
 * @param onOpenCustomKeys opens custom refresh-rate key configuration
 * @param themeMode currently selected appearance mode
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param blurEnabled whether progressive edge blur is enabled
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 * @param onBlurEnabledChanged invoked when progressive blur changes
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
    amoledMode: Boolean,
    blurEnabled: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    onBlurEnabledChanged: (Boolean) -> Unit,
    debugOptionsUnlocked: Boolean,
    onDebugOptionsUnlocked: () -> Unit,
    onOpenDebugSettings: () -> Unit,
    modifier: Modifier = Modifier,
    autoCheckRequest: Int = 0,
    keepAliveEnabled: Boolean = true,
    onKeepAliveEnabledChanged: (Boolean) -> Unit = {}
) {
    var versionTapCount by remember { mutableIntStateOf(0) }
    PreferenceLayout(
        label = "Settings",
        modifier = modifier
    ) {
        ThemeSection(
            themeMode = themeMode,
            amoledMode = amoledMode,
            blurEnabled = blurEnabled,
            onThemeModeChanged = onThemeModeChanged,
            onAmoledModeChanged = onAmoledModeChanged,
            onBlurEnabledChanged = onBlurEnabledChanged
        )
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
                    description = "Home screen tuning, performance monitoring, and crash logs",
                    icon = Icons.Default.DeveloperMode,
                    onClick = onOpenDebugSettings
                )
            }
        }
    }
}
