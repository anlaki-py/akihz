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
 * Displays advanced and app information preferences.
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
    modifier: Modifier = Modifier
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
        AdvancedSection(
            onResetToDefaults = onResetToDefaults,
            onOpenCustomKeys = onOpenCustomKeys
        )
        if (debugOptionsUnlocked) {
            PreferenceGroup(heading = "Developer") {
                PreferenceTemplate(
                    title = "Debug options",
                    description = "Tune home-screen sizing and Material 3 Expressive shapes",
                    icon = Icons.Default.DeveloperMode,
                    onClick = onOpenDebugSettings
                )
            }
        }
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
    }
}
