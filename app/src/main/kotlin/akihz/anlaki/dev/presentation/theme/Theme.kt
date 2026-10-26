@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Whether the active theme is dark, available to color helpers outside MaterialTheme. */
val LocalDarkMode = compositionLocalOf { false }

/**
 * Surface roles shared by every screen.
 *
 * Page containers stay distinct from row cards: rows use [ListItemColors] on
 * `surfaceBright`, while the page sits on `surfaceContainer` in light mode and
 * `surface` in dark mode. This keeps cards visible in AMOLED black.
 */
object AkihzColors {
    /** Background behind page content and the top app bar. */
    val pageContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalDarkMode.current) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }

    /** Top app bar colors matching the page container. */
    val topBarColors: TopAppBarColors
        @Composable
        get() = if (LocalDarkMode.current) {
            TopAppBarDefaults.topAppBarColors()
        } else {
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        }

    /** Row card colors, raised above the page container in every mode. */
    val listItemColors: ListItemColors
        @Composable
        get() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright)
}

/**
 * App theme with dynamic color support on Android 12+.
 *
 * Uses the Material 3 Expressive theme so every screen shares the same motion
 * scheme and emphasized type scale. AMOLED mode turns only the base background
 * and surface black, so raised cards remain visible.
 *
 * @param darkTheme whether to use dark theme
 * @param amoledMode whether dark base surfaces should use pure black
 * @param dynamicColor whether to use Material You dynamic colors (Android 12+)
 * @param themeId selected preset or custom theme id
 * @param customThemes saved custom themes available to [themeId]
 * @param content root composable content
 */
@Composable
fun AnlakiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    dynamicColor: Boolean = true,
    themeId: String = "sakura",
    customThemes: List<CustomTheme> = emptyList(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledMode) dynamicScheme.withAmoledSurfaces() else dynamicScheme
        }
        else -> {
            val selectedScheme = customThemes.firstOrNull { it.id == themeId }
                ?.generateColorScheme(darkTheme)
                ?: findPresetTheme(themeId).colorScheme(darkTheme)
            if (darkTheme && amoledMode) selectedScheme.withAmoledSurfaces() else selectedScheme
        }
    }

    CompositionLocalProvider(LocalDarkMode provides darkTheme) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            shapes = AkihzShapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

private fun ColorScheme.withAmoledSurfaces() = copy(
    background = Color.Black,
    surface = Color.Black
)
