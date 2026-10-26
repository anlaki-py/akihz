@file:Suppress("RestrictedApi")

package akihz.anlaki.dev.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.DynamicScheme

/** Converts Material Color Utilities output to a Compose color scheme. */
internal fun DynamicScheme.toComposeColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(surfaceTint),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        primaryFixed = Color(primaryFixed),
        primaryFixedDim = Color(primaryFixedDim),
        onPrimaryFixed = Color(onPrimaryFixed),
        onPrimaryFixedVariant = Color(onPrimaryFixedVariant),
        secondaryFixed = Color(secondaryFixed),
        secondaryFixedDim = Color(secondaryFixedDim),
        onSecondaryFixed = Color(onSecondaryFixed),
        onSecondaryFixedVariant = Color(onSecondaryFixedVariant),
        tertiaryFixed = Color(tertiaryFixed),
        tertiaryFixedDim = Color(tertiaryFixedDim),
        onTertiaryFixed = Color(onTertiaryFixed),
        onTertiaryFixedVariant = Color(onTertiaryFixedVariant)
    )
}
