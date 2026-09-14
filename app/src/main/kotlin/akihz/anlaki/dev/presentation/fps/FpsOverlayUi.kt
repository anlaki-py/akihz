package akihz.anlaki.dev.presentation.fps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.OverlayStyle

/**
 * One layer row in the overlay options panel.
 *
 * @param stableName stable id used for selection, null for the Auto row
 * @param label text shown in the row, including the live FPS value
 * @param selected whether this row is checked
 */
data class FpsLayerRow(
    val stableName: String?,
    val label: String,
    val selected: Boolean
)

/**
 * Root of the floating overlay: pill plus expandable options panel.
 *
 * Runs inside a ComposeView hosted by the overlay window. All colors are
 * explicit because the service overlay has no Material theme.
 */
@Composable
internal fun FpsOverlayContent(
    text: String,
    style: FpsPillStyle,
    expanded: Boolean,
    layers: List<FpsLayerRow>,
    onTapPill: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    onScaleChange: (Int) -> Unit,
    onScaleDone: (Int) -> Unit,
    onAlphaChange: (Int) -> Unit,
    onAlphaDone: (Int) -> Unit,
    onUnitChange: (Boolean) -> Unit,
    onLayerSelect: (String?) -> Unit
) {
    Column {
        FpsPill(
            text = text,
            style = style,
            onTap = onTapPill,
            onDrag = onDrag,
            onDragEnd = onDragEnd
        )
        if (expanded) {
            FpsOptionsPanel(
                style = style,
                layers = layers,
                onScaleChange = onScaleChange,
                onScaleDone = onScaleDone,
                onAlphaChange = onAlphaChange,
                onAlphaDone = onAlphaDone,
                onUnitChange = onUnitChange,
                onLayerSelect = onLayerSelect
            )
        }
    }
}

/** Floating pill showing the current FPS. Drag moves it, tap opens options. */
@Composable
private fun FpsPill(
    text: String,
    style: FpsPillStyle,
    onTap: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current.density
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val (padH, padV) = style.pillPaddingDp()
    val shape = if (style.rectangularShape) {
        RoundedCornerShape(style.pillRadiusDp().dp)
    } else {
        CircleShape
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color(OverlayStyle.pillColor(style.pillColor, style.alphaPercent)), shape)
            .then(
                if (style.pillOutline) {
                    Modifier.border(1.dp, Color(OverlayStyle.strokeColor(style.pillColor)), shape)
                } else {
                    Modifier
                }
            )
            .overlayDrag(touchSlop, onDrag, onDragEnd, onTap)
            .padding(horizontal = padH.dp, vertical = padV.dp)
    ) {
        Text(
            text = text,
            color = Color(OverlayStyle.textColor(style.pillColor)),
            fontSize = (14f * style.textScaleFactor()).sp,
            maxLines = 1
        )
    }
}

/**
 * Drag when the pointer moves past touch slop, tap otherwise.
 *
 * Mirrors the old View touch listener so a tap still opens the panel
 * while a drag moves the window without clicking. Callbacks stay fresh
 * through rememberUpdatedState with a fixed key, so the FPS text ticking
 * every 500 ms never restarts a drag in progress. That restart was the
 * lag: each recomposition reset the gesture and the pill stuttered.
 */
private fun Modifier.overlayDrag(
    touchSlopPx: Float,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit
): Modifier = composed {
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onDragEnd)
    val latestTap by rememberUpdatedState(onTap)
    pointerInput(touchSlopPx) {
        val slop = touchSlopPx
        awaitEachGesture {
            val down = awaitFirstDown()
            var moved = false
            var totalX = 0f
            var totalY = 0f
            var done = false
            while (!done) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) {
                    done = true
                    break
                }
                val dx = change.position.x - change.previousPosition.x
                val dy = change.position.y - change.previousPosition.y
                totalX += dx
                totalY += dy
                if (!moved && kotlin.math.hypot(totalX.toDouble(), totalY.toDouble()) > slop) {
                    moved = true
                }
                if (moved) {
                    latestDrag(dx, dy)
                    change.consume()
                }
                if (event.changes.all { !it.pressed }) done = true
            }
            if (moved) {
                latestEnd()
            } else {
                down.consume()
                latestTap()
            }
        }
    }
}

/** Expandable panel with layer list, size, opacity, and label options. */
@Composable
private fun FpsOptionsPanel(
    style: FpsPillStyle,
    layers: List<FpsLayerRow>,
    onScaleChange: (Int) -> Unit,
    onScaleDone: (Int) -> Unit,
    onAlphaChange: (Int) -> Unit,
    onAlphaDone: (Int) -> Unit,
    onUnitChange: (Boolean) -> Unit,
    onLayerSelect: (String?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .widthIn(max = 320.dp)
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .background(Color(0xE6000000), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        layers.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLayerSelect(row.stableName) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = row.selected, onClick = null)
                Text(text = row.label, color = Color.White)
            }
        }
        Text(text = "Overlay size: ${style.scalePercent}%", color = Color.White)
        Slider(
            value = style.scalePercent.toFloat(),
            onValueChange = { onScaleChange(it.toInt()) },
            onValueChangeFinished = { onScaleDone(style.scalePercent) },
            valueRange = FpsOverlayController.SCALE_MIN.toFloat()..FpsOverlayController.SCALE_MAX.toFloat()
        )
        Text(text = "Overlay opacity: ${style.alphaPercent}%", color = Color.White)
        Slider(
            value = style.alphaPercent.toFloat(),
            onValueChange = { onAlphaChange(it.toInt()) },
            onValueChangeFinished = { onAlphaDone(style.alphaPercent) },
            valueRange = OverlayStyle.ALPHA_MIN.toFloat()..OverlayStyle.ALPHA_MAX.toFloat()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = style.showUnit, onCheckedChange = onUnitChange)
            Text(text = "Show FPS label", color = Color.White)
        }
    }
}

/** Picks the layer to show: user choice when present, else the first. */
internal fun chooseOverlayLayer(layers: List<LayerStat>, selectedLayer: String?): LayerStat {
    val sel = selectedLayer ?: akihz.anlaki.dev.data.PreferencesHelper.fpsSelectedLayer
    if (sel != null) {
        for (layer in layers) {
            if (layer.stableName == sel) return layer
        }
    }
    return layers.first()
}
