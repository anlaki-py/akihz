@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.fps.LayerStat
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A selectable layer in the overlay menu.
 * @param stableName layer id, or null for automatic selection
 * @param label layer name and current FPS
 * @param selected whether the row is selected
 */
data class FpsLayerRow(
    val stableName: String?,
    val label: String,
    val selected: Boolean
)

/** Floating FPS pill and options, hosted inside the app's expressive theme. */
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
    onColorChange: (OverlayPillColor) -> Unit,
    onLayerSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FpsPill(text, style, expanded, onTapPill, onDrag, onDragEnd)
        if (expanded) {
            FpsOptionsPanel(
                style = style,
                layers = layers,
                onDismiss = onTapPill,
                onScaleChange = onScaleChange,
                onScaleDone = onScaleDone,
                onAlphaChange = onAlphaChange,
                onAlphaDone = onAlphaDone,
                onUnitChange = onUnitChange,
                onColorChange = onColorChange,
                onLayerSelect = onLayerSelect
            )
        }
    }
}

@Composable
private fun FpsPill(
    text: String,
    style: FpsPillStyle,
    expanded: Boolean,
    onTap: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val (padH, padV) = style.pillPaddingDp()
    val shape = if (style.rectangularShape) {
        RoundedCornerShape(style.pillRadiusDp().dp)
    } else {
        CircleShape
    }
    val colors = MaterialTheme.colorScheme
    val foreground = OverlayStyle.textColor(style.pillColor, colors.onPrimaryContainer.toArgb())
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .widthIn(max = screenWidth)
            .background(
                Color(OverlayStyle.pillColor(style.pillColor, style.alphaPercent, colors.primaryContainer.toArgb())),
                shape
            )
            .then(
                if (style.pillOutline) {
                    Modifier.border(1.dp, Color(OverlayStyle.strokeColor(style.pillColor, foreground)), shape)
                } else Modifier
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = if (expanded) "Options open" else "Options closed"
                onClick(label = if (expanded) "Close FPS options" else "Open FPS options") {
                    onTap()
                    true
                }
            }
            .overlayDrag(touchSlop, onDrag, onDragEnd, onTap)
            .padding(horizontal = padH.dp, vertical = padV.dp)
    ) {
        Text(
            text = text,
            color = Color(foreground),
            style = MaterialTheme.typography.titleMediumEmphasized,
            fontSize = (18f * style.textScaleFactor()).sp,
            lineHeight = (24f * style.textScaleFactor()).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = Color(foreground),
            modifier = Modifier.size((16f * style.textScaleFactor()).dp)
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
 * Deltas are physical pixels, matching window params, so the pill
 * follows the finger one to one.
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
            val trackedId = down.id
            var moved = false
            var totalX = 0f
            var totalY = 0f
            var ended = false
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == trackedId }
                        ?: event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
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
                    if (event.changes.all { !it.pressed }) break
                }
                if (moved) {
                    latestEnd()
                    ended = true
                } else {
                    latestTap()
                }
            } finally {
                if (moved && !ended) {
                    latestEnd()
                }
            }
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
