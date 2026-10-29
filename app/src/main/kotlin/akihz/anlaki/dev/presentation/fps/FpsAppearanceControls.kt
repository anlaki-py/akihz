package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceGroupScope
import akihz.anlaki.dev.presentation.components.PreferenceSlider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Pill-only display, shape, and history controls; slider previews save on release. */
@Composable
internal fun FpsAppearanceControls(
    style: FpsPillStyle,
    onPreview: (FpsPillStyle) -> Unit,
    onCommit: (FpsPillStyle) -> Unit
) {
    PreferenceGroup(heading = "Counter") {
        toggle("Default font", "Use the system font for this pill only", style.defaultFont) {
            onCommit(style.copy(defaultFont = it))
        }
        toggle("Show FPS label", "Unit beside the reading", style.showUnit) { onCommit(style.copy(showUnit = it)) }
        toggle("Show decimals", "One decimal place", style.decimals) { onCommit(style.copy(decimals = it)) }
        toggle("Compact spacing", "Smaller digits and less padding", style.compact) { onCommit(style.copy(compact = it)) }
        formItem {
            PillSlider("Size", style.scalePercent, "%", "Scale the counter and graph", 50f..200f, 10f,
                { onPreview(style.copy(scalePercent = it)) }, { onCommit(style.copy(scalePercent = it)) })
        }
    }
    PreferenceGroup(heading = "Graph") {
        toggle("FPS graph", "Recent measurements below the counter", style.graphEnabled) {
            onCommit(style.copy(graphEnabled = it))
        }
        if (style.graphEnabled) {
            formItem {
                Text("History", style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 30, 60).forEach { seconds ->
                        FilterChip(
                            selected = style.graphSeconds == seconds,
                            onClick = { onCommit(style.copy(graphSeconds = seconds)) },
                            label = { Text("${seconds}s") }
                        )
                    }
                }
            }
            formItem {
                PillSlider("Width", style.graphWidth, " dp", "Before the size multiplier", 120f..280f, 10f,
                    { onPreview(style.copy(graphWidth = it)) }, { onCommit(style.copy(graphWidth = it)) })
            }
            formItem {
                PillSlider("Height", style.graphHeight, " dp", "Before the size multiplier", 24f..80f, 4f,
                    { onPreview(style.copy(graphHeight = it)) }, { onCommit(style.copy(graphHeight = it)) })
            }
        }
    }
    PreferenceGroup(heading = "Surface") {
        formItem {
            Text("Color", style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OverlayPillColor.entries.forEach { color ->
                    FilterChip(
                        selected = style.pillColor == color,
                        onClick = { onCommit(style.copy(pillColor = color)) },
                        label = { Text(if (color == OverlayPillColor.Theme) "App theme" else color.name) }
                    )
                }
            }
        }
        formItem {
            PillSlider("Opacity", style.alphaPercent, "%", "Keep the text and graph solid", 40f..100f, 5f,
                { onPreview(style.copy(alphaPercent = it)) }, { onCommit(style.copy(alphaPercent = it)) })
        }
        toggle("Raised surface", "Soft shadow around the pill", style.raised) { onCommit(style.copy(raised = it)) }
        toggle("Outline", "Subtle edge around the surface", style.pillOutline) { onCommit(style.copy(pillOutline = it)) }
        toggle("Custom corners", "Choose the corner radius", style.rectangularShape) {
            onCommit(style.copy(rectangularShape = it))
        }
        if (style.rectangularShape) {
            formItem {
                PillSlider("Corners", style.cornerRadius, " dp", "Zero makes square corners",
                    OverlayStyle.RECT_RADIUS_MIN.toFloat()..OverlayStyle.RECT_RADIUS_MAX.toFloat(), 2f,
                    { onPreview(style.copy(cornerRadius = it)) }, { onCommit(style.copy(cornerRadius = it)) })
            }
        }
    }
}

@Composable
private fun PreferenceGroupScope.toggle(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    item(
        toggleState = checked,
        onToggle = onChange,
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) }
    )
}

@Composable
private fun PillSlider(
    title: String,
    value: Int,
    suffix: String,
    description: String,
    range: ClosedFloatingPointRange<Float>,
    increment: Float,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit
) {
    PreferenceSlider(
        title = title,
        value = value.toFloat(),
        valueLabel = "$value$suffix",
        description = description,
        onValueChange = { onPreview(it.toInt()) },
        onValueChangeFinished = { onCommit(it.toInt()) },
        valueRange = range,
        steps = 0,
        increment = increment
    )
}
