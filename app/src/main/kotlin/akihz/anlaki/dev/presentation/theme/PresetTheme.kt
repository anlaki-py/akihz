package akihz.anlaki.dev.presentation.theme

import androidx.compose.material3.ColorScheme
import akihz.anlaki.dev.presentation.theme.presets.AutumnThemePreset
import akihz.anlaki.dev.presentation.theme.presets.BlackThemePreset
import akihz.anlaki.dev.presentation.theme.presets.ClaudeThemePreset
import akihz.anlaki.dev.presentation.theme.presets.MinimalThemePreset
import akihz.anlaki.dev.presentation.theme.presets.OceanThemePreset
import akihz.anlaki.dev.presentation.theme.presets.SakuraThemePreset
import akihz.anlaki.dev.presentation.theme.presets.SpringThemePreset

/** A named light and dark color-scheme pair shown in the theme picker. */
data class PresetTheme(
    val id: String,
    val name: String,
    val standardLight: ColorScheme,
    val standardDark: ColorScheme
) {
    /** Returns the color scheme for the active display mode. */
    fun colorScheme(dark: Boolean): ColorScheme = if (dark) standardDark else standardLight
}

/** Color palettes copied from RikkaHub's preset theme picker. */
val PresetThemes: List<PresetTheme> = listOf(
    SakuraThemePreset,
    OceanThemePreset,
    SpringThemePreset,
    AutumnThemePreset,
    BlackThemePreset,
    MinimalThemePreset,
    ClaudeThemePreset
)

/** Returns a preset by id, falling back to Sakura for stale preferences. */
fun findPresetTheme(id: String): PresetTheme =
    PresetThemes.firstOrNull { it.id == id } ?: SakuraThemePreset
