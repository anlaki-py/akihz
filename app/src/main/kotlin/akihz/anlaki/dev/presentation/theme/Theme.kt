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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0B2A4A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDBEAFE),
    inversePrimary = Color(0xFF1D4ED8),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF164E63),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF0B0D12),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceTint = Color(0xFF60A5FA),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    scrim = Color.Black,
    surfaceBright = Color(0xFF37393F),
    surfaceDim = Color(0xFF0B0D12),
    surfaceContainerLowest = Color(0xFF08090C),
    surfaceContainerLow = Color(0xFF12151B),
    surfaceContainer = Color(0xFF181B22),
    surfaceContainerHigh = Color(0xFF22262E),
    surfaceContainerHighest = Color(0xFF2C313A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0B2A4A),
    inversePrimary = Color(0xFF93C5FD),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF0E7490),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF083344),
    background = Color.White,
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = Color(0xFF1D4ED8),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE2E8F0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1)
)

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
 * @param content root composable content
 */
@Composable
fun AnlakiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledMode) dynamicScheme.withAmoledSurfaces() else dynamicScheme
        }
        darkTheme && amoledMode -> DarkColorScheme.withAmoledSurfaces()
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
