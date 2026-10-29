@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceSlider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Scrollable overlay controls using the same grouped cards as app settings. */
@Composable
internal fun FpsOptionsPanel(
    style: FpsPillStyle,
    layers: List<FpsLayerRow>,
    onDismiss: () -> Unit,
    onScaleChange: (Int) -> Unit,
    onScaleDone: (Int) -> Unit,
    onAlphaChange: (Int) -> Unit,
    onAlphaDone: (Int) -> Unit,
    onUnitChange: (Boolean) -> Unit,
    onColorChange: (OverlayPillColor) -> Unit,
    onLayerSelect: (String?) -> Unit
) {
    val configuration = LocalConfiguration.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(minOf(340, configuration.screenWidthDp).dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp)
            ) {
                Text(
                    "FPS overlay",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close FPS options")
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .heightIn(max = minOf(420, configuration.screenHeightDp / 2).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                PreferenceGroup(heading = "Appearance") {
                    formItem {
                        Text("Color", style = MaterialTheme.typography.bodyLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OverlayPillColor.entries.forEach { color ->
                                FilterChip(
                                    selected = style.pillColor == color,
                                    onClick = { onColorChange(color) },
                                    label = { Text(if (color == OverlayPillColor.Theme) "App theme" else color.name) }
                                )
                            }
                        }
                    }
                    formItem {
                        PreferenceSlider(
                            title = "Size",
                            value = style.scalePercent.toFloat(),
                            valueLabel = "${style.scalePercent}%",
                            description = "Scale the floating counter",
                            onValueChange = { onScaleChange(it.toInt()) },
                            onValueChangeFinished = { onScaleDone(it.toInt()) },
                            valueRange = FpsOverlayController.SCALE_MIN.toFloat()..FpsOverlayController.SCALE_MAX.toFloat(),
                            steps = 0,
                            increment = 10f
                        )
                    }
                    formItem {
                        PreferenceSlider(
                            title = "Opacity",
                            value = style.alphaPercent.toFloat(),
                            valueLabel = "${style.alphaPercent}%",
                            description = "Fade the background, keep text solid",
                            onValueChange = { onAlphaChange(it.toInt()) },
                            onValueChangeFinished = { onAlphaDone(it.toInt()) },
                            valueRange = OverlayStyle.ALPHA_MIN.toFloat()..OverlayStyle.ALPHA_MAX.toFloat(),
                            steps = 0,
                            increment = 5f
                        )
                    }
                    item(
                        toggleState = style.showUnit,
                        onToggle = onUnitChange,
                        headlineContent = { Text("Show FPS label") },
                        trailingContent = { Switch(checked = style.showUnit, onCheckedChange = null) }
                    )
                }
                FpsLayerChoices(layers, onLayerSelect)
            }
        }
    }
}

@Composable
private fun FpsLayerChoices(layers: List<FpsLayerRow>, onLayerSelect: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.selectableGroup()) {
        Text(
            "Measured layer",
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        layers.forEach { row ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (row.selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceBright,
                contentColor = if (row.selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(row.selected, role = Role.RadioButton) { onLayerSelect(row.stableName) }
                        .padding(16.dp)
                ) {
                    RadioButton(selected = row.selected, onClick = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (row.stableName == null) "Automatic" else row.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (row.stableName == null) {
                            Text("Follow the foreground app", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
