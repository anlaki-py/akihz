package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Applies the saved app theme and observes changes while the service stays open. */
@Composable
internal fun FpsOverlayTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { PreferencesHelper.apply { init(context) } }
    val changes by preferences.preferenceChanges.collectAsState()
    val mode = remember(changes) { preferences.themeMode }
    val amoled = remember(changes) { preferences.amoledMode }
    val dynamic = remember(changes) { preferences.dynamicColor }
    val themeId = remember(changes) { preferences.themeId }
    val customThemes = remember(changes) { preferences.customThemes }
    val dark = when (mode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    AnlakiTheme(
        darkTheme = dark,
        amoledMode = amoled,
        dynamicColor = dynamic,
        themeId = themeId,
        customThemes = customThemes,
        content = content
    )
}
