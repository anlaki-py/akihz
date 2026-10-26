package akihz.anlaki.dev.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

@Composable
internal fun CustomThemeEditorSheet(
    theme: CustomTheme?,
    onDismiss: () -> Unit,
    onSave: (CustomTheme) -> Unit
) {
    var currentTheme by remember(theme) { mutableStateOf(theme ?: CustomTheme()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (theme == null) "Create theme" else "Edit theme",
                style = MaterialTheme.typography.titleLargeEmphasized
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = currentTheme.name,
                    onValueChange = { currentTheme = currentTheme.copy(name = it) },
                    label = { Text("Theme name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                ThemeColorControl(
                    label = "Primary color",
                    color = Color(currentTheme.primaryColorArgb.toInt()),
                    onColorChange = {
                        currentTheme = currentTheme.copy(primaryColorArgb = it.unsignedArgb())
                    }
                )
                ThemeColorControl(
                    label = "Secondary color",
                    color = currentTheme.secondaryColorArgb?.let { Color(it.toInt()) }
                        ?: currentTheme.generateColorScheme(false).secondary,
                    onColorChange = {
                        currentTheme = currentTheme.copy(secondaryColorArgb = it.unsignedArgb())
                    }
                )
                ThemeColorControl(
                    label = "Tertiary color",
                    color = currentTheme.tertiaryColorArgb?.let { Color(it.toInt()) }
                        ?: currentTheme.generateColorScheme(false).tertiary,
                    onColorChange = {
                        currentTheme = currentTheme.copy(tertiaryColorArgb = it.unsignedArgb())
                    }
                )
                ThemePreview(currentTheme)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(currentTheme) },
                    enabled = currentTheme.name.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ThemeColorControl(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    Text(label, style = MaterialTheme.typography.titleSmallEmphasized)
    val hsl = remember(color) { FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) } }
    var hue by remember(color) { mutableFloatStateOf(hsl[0]) }
    var saturation by remember(color) { mutableFloatStateOf(hsl[1]) }
    var lightness by remember(color) { mutableFloatStateOf(hsl[2]) }
    var hslCode by remember(color) { mutableStateOf(formatHsl(hsl)) }
    var invalidCode by remember(color) { mutableStateOf(false) }

    fun update(newHue: Float, newSaturation: Float, newLightness: Float) {
        hue = newHue
        saturation = newSaturation
        lightness = newLightness
        val updated = floatArrayOf(newHue, newSaturation, newLightness)
        hslCode = formatHsl(updated)
        invalidCode = false
        onColorChange(Color(ColorUtils.HSLToColor(updated)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) { drawCircle(color) }
            Column(modifier = Modifier.weight(1f)) {
                ColorSlider("H", hue, 0f..360f) { update(it, saturation, lightness) }
                ColorSlider("S", saturation, 0f..1f) { update(hue, it, lightness) }
                ColorSlider("L", lightness, 0f..1f) { update(hue, saturation, it) }
            }
        }
        OutlinedTextField(
            value = hslCode,
            onValueChange = { value ->
                hslCode = value
                val parsed = parseHsl(value)
                invalidCode = parsed == null
                if (parsed != null) {
                    hue = parsed[0]
                    saturation = parsed[1]
                    lightness = parsed[2]
                    onColorChange(Color(ColorUtils.HSLToColor(parsed)))
                }
            },
            label = { Text("HSL") },
            placeholder = { Text("hsl(267 36% 48%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = invalidCode,
            supportingText = if (invalidCode) ({ Text("Use hsl(267 36% 48%)") }) else null
        )
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ThemePreview(theme: CustomTheme) {
    val scheme = theme.generateColorScheme(LocalDarkMode.current)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Preview", style = MaterialTheme.typography.titleSmallEmphasized)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "P" to scheme.primary,
                "S" to scheme.secondary,
                "T" to scheme.tertiary,
                "PC" to scheme.primaryContainer,
                "SC" to scheme.secondaryContainer,
                "Sf" to scheme.surface
            ).forEach { (label, color) -> ColorSwatch(color, label) }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.size(32.dp).clip(CircleShape)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val HslNumberRegex = Regex("""[-+]?\d*\.?\d+""")

internal fun parseHsl(value: String): FloatArray? {
    val values = HslNumberRegex.findAll(value).take(3).mapNotNull { it.value.toFloatOrNull() }.toList()
    if (values.size != 3 || values.any { !it.isFinite() }) return null
    return floatArrayOf(
        values[0].coerceIn(0f, 360f),
        normalizePercent(values[1]),
        normalizePercent(values[2])
    )
}

private fun normalizePercent(value: Float): Float =
    (if (value > 1f) value / 100f else value).coerceIn(0f, 1f)

private fun formatHsl(hsl: FloatArray): String =
    "hsl(${hsl[0].roundToInt()} ${(hsl[1] * 100).roundToInt()}% ${(hsl[2] * 100).roundToInt()}%)"

private fun Color.unsignedArgb(): Long = toArgb().toLong() and 0xFFFFFFFFL
