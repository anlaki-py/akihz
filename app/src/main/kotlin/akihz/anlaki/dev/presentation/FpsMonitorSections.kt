package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceGroup
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
                "Show FPS, updated every 500 ms"
            },
            icon = if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            onClick = onStartStop
        )
        PreferenceTemplate(
            title = if (state.debugLoggingEnabled) "Turn off debug logging" else "Turn on debug logging",
            description = "Record FPS diagnostics",
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
            description = "Monitor a specific app",
            icon = Icons.Default.Tune,
            onClick = onSelectApp
        )
        if (state.targetPackage != null) {
            PreferenceTemplate(
                title = "Use automatic detection",
                description = "Follow the foreground app",
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
            text = "Method is OEM-dependent. Xiaomi 12T / HyperOS 2 validated. Static screens show 0.0 FPS. Drag the pill to move it, tap to change its appearance, graph, or measured layer.",
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
