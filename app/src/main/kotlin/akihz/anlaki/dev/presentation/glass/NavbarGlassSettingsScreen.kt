package akihz.anlaki.dev.presentation.glass

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.data.glass.GlassLayerSettings
import akihz.anlaki.dev.data.glass.GlassParameter
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import akihz.anlaki.dev.presentation.components.FloatingBottomBar
import akihz.anlaki.dev.presentation.components.FloatingNavigationItem
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceSlider
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import java.util.Locale
import org.json.JSONObject
import timber.log.Timber

/** Shows live navbar tuning and exports [settings]; [onSettingsChanged] persists edits. */
@Composable
internal fun NavbarGlassSettingsScreen(
    settings: NavbarGlassSettings,
    onSettingsChanged: (NavbarGlassSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val supported = Build.VERSION.SDK_INT >= 33
    val backdrop = if (supported && settings.enabled) rememberGlassBackdrop() else null
    var selection by rememberSaveable { mutableIntStateOf(0) }
    val export = remember(settings) {
        JSONObject(settings.toJson()).apply {
            put("appearance", JSONObject().apply {
                put("themeId", PreferencesHelper.themeId)
                put("themeMode", PreferencesHelper.themeMode.name)
                put("dynamicColor", PreferencesHelper.dynamicColor)
                put("amoledMode", PreferencesHelper.amoledMode)
            })
            put("androidApi", Build.VERSION.SDK_INT)
        }.toString(2)
    }
    Box(Modifier.fillMaxSize()) {
        PreferenceLayout(
            label = "Liquid glass navbar",
            modifier = if (backdrop != null) Modifier.recordGlassBackdrop(backdrop) else Modifier,
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationContentDescription = "Back to debug options",
            onNavigationClick = onBack
        ) {
            PreferenceGroup(heading = "Experiment") {
                PreferenceTemplate(
                    title = "Enable liquid glass",
                    description = if (supported) "Off by default. Tap the preview tabs and scroll to inspect the glass."
                        else "Requires Android 13 or later. This device keeps the normal navbar.",
                    checked = supported && settings.enabled,
                    onCheckedChange = { if (supported) onSettingsChanged(settings.copy(enabled = it)) }
                )
                PreferenceTemplate(
                    title = "Copy preset JSON",
                    description = "Copy both layers and appearance details to send back",
                    onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("akiHz navbar glass preset", export))
                        Toast.makeText(context, "Glass preset copied", Toast.LENGTH_SHORT).show()
                    }
                )
                PreferenceTemplate(
                    title = "Share preset JSON",
                    description = "Send or save the complete settings as text",
                    onClick = { sharePreset(context, export) }
                )
                PreferenceTemplate(
                    title = "Restore glass defaults",
                    description = "Clear outer glass, slight pill blur, experiment turned off",
                    onClick = { onSettingsChanged(NavbarGlassSettings()) }
                )
            }
            GlassLayerControls("Outer background", settings.outer) {
                onSettingsChanged(settings.copy(outer = it))
            }
            GlassLayerControls("Moving pill", settings.pill) {
                onSettingsChanged(settings.copy(pill = it))
            }
        }
        FloatingBottomBar(
            items = listOf(
                FloatingNavigationItem("Home", Icons.Default.Home) { selection = 0 },
                FloatingNavigationItem("Settings", Icons.Default.Settings) { selection = 1 }
            ),
            selectedIndex = selection,
            modifier = Modifier.align(Alignment.BottomCenter),
            glassSettings = settings,
            backdrop = backdrop
        )
    }
}

@Composable
private fun GlassLayerControls(
    title: String,
    settings: GlassLayerSettings,
    onChange: (GlassLayerSettings) -> Unit
) {
    PreferenceGroup(heading = title) {
        PreferenceTemplate(
            title = "Use theme tint",
            description = "Turn off to use the custom hue below",
            checked = settings.useThemeTint,
            onCheckedChange = { onChange(settings.copy(useThemeTint = it)) }
        )
        GlassParameter.entries.forEach { parameter ->
            formItem {
                val increment = when (parameter) {
                    GlassParameter.Tint, GlassParameter.Highlight,
                    GlassParameter.TintSaturation, GlassParameter.TintBrightness -> 0.01f
                    GlassParameter.Brightness, GlassParameter.Saturation -> 0.05f
                    GlassParameter.Hue, GlassParameter.LightAngle -> 1f
                    else -> 0.25f
                }
                PreferenceSlider(
                    title = parameter.label,
                    value = settings[parameter],
                    valueLabel = String.format(Locale.US, "%.2f %s", settings[parameter], parameter.unit),
                    description = when (parameter) {
                        GlassParameter.Blur -> "Zero keeps this layer sharp."
                        GlassParameter.Refraction -> "Bend the background near the glass edge."
                        GlassParameter.EdgeWidth -> "Control how far the curved edge reaches inward."
                        GlassParameter.Dispersion -> "Separate red and blue near the rim."
                        GlassParameter.Tint -> "Blend the tint into the background."
                        GlassParameter.Hue, GlassParameter.TintSaturation,
                        GlassParameter.TintBrightness -> "Used when theme tint is off."
                        GlassParameter.Saturation -> "Zero removes background color. One preserves it."
                        GlassParameter.Brightness -> "One preserves background brightness."
                        GlassParameter.Highlight -> "Adjust the strength of the bright rim."
                        GlassParameter.LightAngle -> "Move the light around the glass edge."
                    },
                    onValueChange = { onChange(settings.withValue(parameter, it)) },
                    onValueChangeFinished = {},
                    valueRange = parameter.range,
                    steps = 0,
                    increment = increment,
                    enabled = parameter !in setOf(GlassParameter.Hue, GlassParameter.TintSaturation,
                        GlassParameter.TintBrightness) || !settings.useThemeTint
                )
            }
        }
    }
}

private fun sharePreset(context: Context, preset: String) {
    try {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "akiHz navbar glass preset")
            putExtra(Intent.EXTRA_TEXT, preset)
        }, "Share glass preset"))
    } catch (error: ActivityNotFoundException) {
        Timber.w(error, "No app can share the navbar glass preset")
        Toast.makeText(context, "No sharing app available. Use Copy preset JSON.", Toast.LENGTH_LONG).show()
    }
}
