@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import akihz.anlaki.dev.data.PreferencesHelper

/** Whether the saved debug font override is active in this composition. */
val LocalCuteDinoEnabled = staticCompositionLocalOf { false }

/** Font for diagnostic text, respecting the app-wide debug override. */
val LocalDiagnosticFont = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

/** Loads the bundled title font once per asset manager. */
@Composable
fun rememberCuteDinoFont(): FontFamily {
    val assets = LocalContext.current.assets
    return remember(assets) {
        FontFamily(Font(path = "fonts/cute_dino/Cute Dino.ttf", assetManager = assets))
    }
}

/** Loads the bundled font used for descriptions and supporting text. */
@Composable
fun rememberSoftCoreFont(): FontFamily {
    val assets = LocalContext.current.assets
    return remember(assets) {
        FontFamily(Font(path = "fonts/soft_core/soft_core.ttf", assetManager = assets))
    }
}

/** Applies the title font choice and Soft Core supporting text without changing sizes or spacing. */
@Composable
fun AppFontTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { PreferencesHelper.apply { init(context) } }
    val changes by preferences.preferenceChanges.collectAsState()
    val enabled = remember(changes) { preferences.cuteDinoEverywhere }
    val font = rememberCuteDinoFont()
    val descriptionFont = rememberSoftCoreFont()
    val base = MaterialTheme.typography
    val typography = remember(base, enabled, font, descriptionFont) {
        val primary = if (enabled) base.withFontFamily(font) else base
        primary.copy(
            bodyMedium = primary.bodyMedium.copy(fontFamily = descriptionFont, fontWeight = FontWeight.Normal),
            bodySmall = primary.bodySmall.copy(fontFamily = descriptionFont, fontWeight = FontWeight.Normal),
            bodyMediumEmphasized = primary.bodyMediumEmphasized.copy(fontFamily = descriptionFont),
            bodySmallEmphasized = primary.bodySmallEmphasized.copy(fontFamily = descriptionFont)
        )
    }
    CompositionLocalProvider(
        LocalCuteDinoEnabled provides enabled,
        LocalDiagnosticFont provides if (enabled) font else FontFamily.Monospace
    ) {
        MaterialTheme(typography = typography, content = content)
    }
}

/** Replaces all regular and emphasized font families while preserving the type scale. */
internal fun Typography.withFontFamily(font: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = font),
    displayMedium = displayMedium.copy(fontFamily = font),
    displaySmall = displaySmall.copy(fontFamily = font),
    headlineLarge = headlineLarge.copy(fontFamily = font),
    headlineMedium = headlineMedium.copy(fontFamily = font),
    headlineSmall = headlineSmall.copy(fontFamily = font),
    titleLarge = titleLarge.copy(fontFamily = font),
    titleMedium = titleMedium.copy(fontFamily = font),
    titleSmall = titleSmall.copy(fontFamily = font),
    bodyLarge = bodyLarge.copy(fontFamily = font),
    bodyMedium = bodyMedium.copy(fontFamily = font),
    bodySmall = bodySmall.copy(fontFamily = font),
    labelLarge = labelLarge.copy(fontFamily = font),
    labelMedium = labelMedium.copy(fontFamily = font),
    labelSmall = labelSmall.copy(fontFamily = font),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = font),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = font),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = font),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = font),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = font),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = font),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = font),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = font),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = font),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = font),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = font),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = font),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = font),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = font),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = font)
)
