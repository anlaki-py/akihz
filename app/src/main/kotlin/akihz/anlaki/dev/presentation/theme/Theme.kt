package akihz.anlaki.dev.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3B82F6),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A5F),
    secondary = androidx.compose.ui.graphics.Color(0xFF4B5563),
    background = androidx.compose.ui.graphics.Color.Black,
    surface = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3B82F6),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFDBEAFE),
    secondary = androidx.compose.ui.graphics.Color(0xFFE5E7EB),
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black
)

/**
 * App theme with dynamic color support on Android 12+.
 *
 * @param darkTheme whether to use dark theme
 * @param pitchBlackTheme whether dark surfaces should use pure black
 * @param dynamicColor whether to use Material You dynamic colors (Android 12+)
 * @param content root composable content
 */
@Composable
fun AnlakiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlackTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pitchBlackTheme) dynamicScheme.withPitchBlackSurfaces() else dynamicScheme
        }
        darkTheme && pitchBlackTheme -> DarkColorScheme.withPitchBlackSurfaces()
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AkihzShapes,
        content = content
    )
}

private fun androidx.compose.material3.ColorScheme.withPitchBlackSurfaces() = copy(
    background = androidx.compose.ui.graphics.Color.Black,
    surface = androidx.compose.ui.graphics.Color.Black,
    surfaceDim = androidx.compose.ui.graphics.Color.Black,
    surfaceBright = androidx.compose.ui.graphics.Color(0xFF101010),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color.Black,
    surfaceContainerLow = androidx.compose.ui.graphics.Color.Black,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF080808),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF101010),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF181818)
)
