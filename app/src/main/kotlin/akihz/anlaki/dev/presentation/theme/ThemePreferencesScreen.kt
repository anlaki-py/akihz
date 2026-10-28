package akihz.anlaki.dev.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import akihz.anlaki.dev.data.PreferencesHelper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/**
 * Shows theme preferences in the same first-level page used by RikkaHub.
 *
 * @param dynamicColor whether wallpaper colors override selected color schemes
 * @param amoledMode whether dark surfaces use pure black
 * @param themedAppIcon whether the launcher icon uses Material colors
 * @param onThemedAppIconChanged persists launcher icon changes
 * @param onDynamicColorChanged persists dynamic color changes
 * @param onAmoledModeChanged persists AMOLED changes
 * @param onOpenColorSchemes opens the second-level color scheme page
 * @param onBack returns to Settings
 * @param modifier page modifier
 */
@Composable
fun ThemePreferencesScreen(
    dynamicColor: Boolean,
    amoledMode: Boolean,
    themedAppIcon: Boolean,
    onThemedAppIconChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    onOpenColorSchemes: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceLayout(
        label = "Theme",
        modifier = modifier,
        navigationIcon = Icons.Default.ArrowBack,
        navigationContentDescription = "Back",
        onNavigationClick = onBack
    ) {
        PreferenceGroup(heading = "App icon") {
            PreferenceTemplate(
                title = "Material app icon",
                description = "Use wallpaper colors for the app icon",
                icon = Icons.Default.Apps,
                checked = themedAppIcon,
                onCheckedChange = onThemedAppIconChanged
            )
        }
        PreferenceGroup(heading = "Theme") {
            PreferenceTemplate(
                title = "Dynamic color",
                description = "Generate app colors from your wallpaper",
                icon = Icons.Default.Wallpaper,
                checked = dynamicColor,
                onCheckedChange = onDynamicColorChanged
            )
            PreferenceTemplate(
                title = "Color schemes",
                description = "Customize the app color scheme",
                icon = Icons.Default.Palette,
                onClick = onOpenColorSchemes
            )
            PreferenceTemplate(
                title = "Pitch-black AMOLED",
                description = "Use pure-black surfaces whenever dark mode is active",
                icon = Icons.Default.DarkMode,
                checked = amoledMode,
                onCheckedChange = onAmoledModeChanged
            )
        }
        PreferenceGroup(heading = "Font") {
            PreferenceTemplate(
                title = "Cute Dino everywhere",
                description = "Use the home title font for all app text",
                checked = LocalCuteDinoEnabled.current,
                onCheckedChange = { PreferencesHelper.cuteDinoEverywhere = it }
            )
        }
    }
}
