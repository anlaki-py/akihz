package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.fps.FpsSample
import akihz.anlaki.dev.data.fps.LayerStat
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * A selectable layer in the overlay menu.
 * @param stableName layer id, or null for automatic selection
 * @param label layer name and current FPS
 * @param selected whether the row is selected
 */
data class FpsLayerRow(val stableName: String?, val label: String, val selected: Boolean)

/** Floating counter, optional history, and controls inside the app theme. */
@Composable
internal fun FpsOverlayContent(
    text: String,
    samples: List<FpsSample>,
    refreshRate: Double,
    style: FpsPillStyle,
    expanded: Boolean,
    layers: List<FpsLayerRow>,
    onTapPill: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    onStylePreview: (FpsPillStyle) -> Unit,
    onStyleCommit: (FpsPillStyle) -> Unit,
    onLayerSelect: (String?) -> Unit
) {
    // Leave room for the Material surface shadow inside the overlay window.
    Column(
        Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FpsPill(text, samples, refreshRate, style, expanded, onTapPill, onDrag, onDragEnd)
        if (expanded) {
            FpsOptionsPanel(
                modifier = Modifier.weight(1f, fill = false),
                style = style,
                layers = layers,
                onDismiss = onTapPill,
                onStylePreview = onStylePreview,
                onStyleCommit = onStyleCommit,
                onLayerSelect = onLayerSelect
            )
        }
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
internal fun Modifier.overlayDrag(
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
