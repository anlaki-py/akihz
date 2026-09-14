package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceSlider
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/** Header showing whether monitoring runs. */
@Composable
internal fun FpsStatusHeader(state: FpsMonitorUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (state.isRunning) "Monitoring is running" else "Monitoring is stopped",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = if (state.isRunning) {
                "Overlay visible. Open a game or app to see FPS. Tap pill for options."
            } else {
                "Press Start to show the overlay."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Start, stop, and debug logging controls.
 *
 * @param state current monitor state
 * @param onStartStop invoked for the start/stop row
 * @param onToggleDebug invoked for the debug logging row
 * @param onViewLog invoked for the view log row
 */
@Composable
internal fun FpsControlsSection(
    state: FpsMonitorUiState,
    onStartStop: () -> Unit,
    onToggleDebug: () -> Unit,
    onViewLog: () -> Unit
) {
    PreferenceGroup(heading = "Controls") {
        PreferenceTemplate(
            title = if (state.isRunning) "Stop monitoring" else "Start monitoring",
            description = if (state.isRunning) {
                "Hide overlay and stop sampling"
            } else {
                "Show floating FPS pill and sample every 500 ms"
            },
            icon = if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            onClick = onStartStop
        )
        PreferenceTemplate(
            title = if (state.debugLoggingEnabled) "Turn off debug logging" else "Turn on debug logging",
            description = "Log Shizuku state, focus lines, and layer stats",
            icon = Icons.Default.BugReport,
            onClick = onToggleDebug
        )
        PreferenceTemplate(
            title = "View debug log",
            description = if (state.debugLoggingEnabled) "Copy or share diagnostics" else "Enable logging first",
            icon = Icons.Default.Api,
            onClick = onViewLog
        )
    }
}

/**
 * Overlay look controls bound to the view model.
 *
 * @param state current monitor state
 * @param viewModel receiver of every change
 * @param onPickColor invoked when the color row opens the picker
 */
@Composable
internal fun FpsOverlaySection(
    state: FpsMonitorUiState,
    viewModel: FpsMonitorViewModel,
    onPickColor: () -> Unit
) {
    PreferenceGroup(heading = "Overlay") {
        PreferenceSlider(
            title = "Overlay size",
            value = state.overlayScale.toFloat(),
            valueLabel = "${state.overlayScale}%",
            description = "Scales the floating FPS pill",
            onValueChange = { viewModel.setOverlayScale(it.toInt()) },
            onValueChangeFinished = { viewModel.setOverlayScale(it.toInt()) },
            valueRange = 50f..200f,
            steps = 0,
            increment = 10f
        )
        PreferenceSlider(
            title = "Overlay opacity",
            value = state.overlayAlpha.toFloat(),
            valueLabel = "${state.overlayAlpha}%",
            description = "Fades the pill background only, text stays solid",
            onValueChange = { viewModel.setOverlayAlpha(it.toInt()) },
            onValueChangeFinished = { viewModel.setOverlayAlpha(it.toInt()) },
            valueRange = 40f..100f,
            steps = 0,
            increment = 5f
        )
        PreferenceTemplate(
            title = "Show FPS label",
            description = if (state.showUnit) "Pill shows 60.0 FPS" else "Pill shows 60.0",
            icon = Icons.Default.TextFields,
            checked = state.showUnit,
            onCheckedChange = { viewModel.setShowUnit(it) }
        )
        PreferenceTemplate(
            title = "Pill color",
            description = pillColorLabel(state.pillColor),
            icon = Icons.Default.Palette,
            onClick = onPickColor
        )
        PreferenceTemplate(
            title = "Rectangular shape",
            description = if (state.rectShape) "Rectangle pill" else "Round pill",
            icon = Icons.Default.CropSquare,
            checked = state.rectShape,
            onCheckedChange = { viewModel.setRectShape(it) }
        )
        PreferenceSlider(
            title = "Corner radius",
            value = state.cornerRadius.toFloat(),
            valueLabel = "${state.cornerRadius} dp",
            description = if (state.rectShape) {
                "Round the rectangle corners, 0 is sharp"
            } else {
                "Turn on Rectangular shape to use this"
            },
            onValueChange = { viewModel.setCornerRadius(it.toInt()) },
            onValueChangeFinished = { viewModel.setCornerRadius(it.toInt()) },
            valueRange = OverlayStyle.RECT_RADIUS_MIN.toFloat()..OverlayStyle.RECT_RADIUS_MAX.toFloat(),
            steps = OverlayStyle.RECT_RADIUS_MAX - OverlayStyle.RECT_RADIUS_MIN - 1,
            increment = 1f,
            enabled = state.rectShape
        )
        PreferenceTemplate(
            title = "Pill outline",
            description = if (state.pillOutline) "Edge is visible" else "Flat pill, no edge",
            icon = Icons.Default.Layers,
            checked = state.pillOutline,
            onCheckedChange = { viewModel.setPillOutline(it) }
        )
    }
}

/**
 * Monitor target picker.
 *
 * @param state current monitor state
 * @param viewModel receiver of the clear action
 * @param onSelectApp invoked for the select app row
 */
@Composable
internal fun FpsTargetSection(
    state: FpsMonitorUiState,
    viewModel: FpsMonitorViewModel,
    onSelectApp: () -> Unit
) {
    PreferenceGroup(heading = "Target") {
        val targetText = if (state.targetPackage == null) {
            "Automatic foreground app"
        } else {
            val label = state.targetLabel ?: state.targetPackage
            "$label\n${state.targetPackage}"
        }
        PreferenceTemplate(
            title = "Current target",
            description = targetText,
            icon = Icons.Default.AutoAwesome,
            onClick = null
        )
        PreferenceTemplate(
            title = "Select app",
            description = "Pick a launcher activity to pin monitoring to that package",
            icon = Icons.Default.Tune,
            onClick = onSelectApp
        )
        if (state.targetPackage != null) {
            PreferenceTemplate(
                title = "Use automatic detection",
                description = "Follow the foreground window again",
                icon = Icons.Default.AutoAwesome,
                onClick = { viewModel.clearTarget() }
            )
        }
    }
}

/**
 * Method notes plus the one-shot status message.
 *
 * @param message pending message, if any
 * @param onConsume called once per message to clear it
 */
@Composable
internal fun FpsMonitorFootnotes(
    message: String?,
    onConsume: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(
            text = "Method is OEM-dependent. Xiaomi 12T / HyperOS 2 validated. Static screens show 0.0 FPS. Drag the pill to move it, tap to show layer, size, and opacity options.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "Requires Shizuku, overlay permission, and notification permission.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    message?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        LaunchedEffect(it) { onConsume() }
    }
}
