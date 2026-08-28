package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import akihz.anlaki.dev.data.PerfRecorderState
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import java.util.concurrent.TimeUnit

/**
 * Diagnostic page that records app performance metrics to a log file.
 *
 * The page is reached from Debug options. Start begins sampling the current
 * process at 1 Hz; Stop freezes the recording and unlocks Save / Share so the
 * user can take the JSON-line log off-device for analysis.
 */
@Composable
internal fun PerformanceMonitorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerformanceMonitorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) {
            if (state.recorder is PerfRecorderState.Recording) {
                viewModel.refreshSample()
            }
            kotlinx.coroutines.delay(1_000L)
        }
    }

    PreferenceLayout(
        label = "Performance monitoring",
        modifier = modifier.fillMaxSize(),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back to debug options",
        onNavigationClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RecordingStatus(state = state)
            LiveReadout(state = state)
            PreferenceGroup(heading = "Controls") {
                PreferenceTemplate(
                    title = if (state.recorder is PerfRecorderState.Recording) {
                        "Stop recording"
                    } else {
                        "Start recording"
                    },
                    description = if (state.recorder is PerfRecorderState.Recording) {
                        "Finish the current session and unlock Save / Share"
                    } else {
                        "Begin sampling CPU, memory, threads, and refresh rate"
                    },
                    onClick = {
                        if (state.recorder is PerfRecorderState.Recording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    }
                )
                if (state.recorder is PerfRecorderState.Stopped) {
                    PreferenceTemplate(
                        title = "Save to Downloads",
                        description = state.savedLocation
                            ?: "Copy the log to Downloads/akihz/perf",
                        onClick = { viewModel.saveToDownloads() }
                    )
                    PreferenceTemplate(
                        title = "Share log",
                        description = "Open the system share sheet for the log",
                        onClick = { viewModel.shareLog() }
                    )
                    PreferenceTemplate(
                        title = "Discard",
                        description = "Delete the recording and return to idle",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.discard() }
                    )
                }
            }
            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                LaunchedEffect(message) { viewModel.consumeMessage() }
            }
        }
    }
}

@Composable
private fun RecordingStatus(state: PerformanceMonitorUiState) {
    val (title, detail) = when (val recorder = state.recorder) {
        is PerfRecorderState.Recording -> {
            val durationMs = android.os.SystemClock.elapsedRealtime() - recorder.startedAtElapsedMs
            "Recording" to "Duration ${formatDuration(durationMs)} • " +
                "${recorder.samplesWritten} samples • ${recorder.eventsWritten} events"
        }
        is PerfRecorderState.Stopped -> {
            val durationMs = recorder.stoppedAtElapsedMs - recorder.startedAtElapsedMs
            "Stopped (${recorder.stoppedReason})" to "Duration ${formatDuration(durationMs)} • " +
                "${recorder.samplesWritten} samples • ${recorder.eventsWritten} events"
        }
        PerfRecorderState.Idle -> "Idle" to "Press start to begin a new session"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun LiveReadout(state: PerformanceMonitorUiState) {
    val sample = state.lastSample
    val rows: List<Pair<String, String>> = if (sample == null) {
        listOf("Status" to "No samples yet \u2014 start a recording to see live values.")
    } else {
        listOf(
            "Process CPU" to (sample.processCpuPercent?.let { "%.1f %%".format(it) } ?: "n/a"),
            "App CPU" to (sample.appCpuPercent?.let { "%.1f %%".format(it) } ?: "n/a"),
            "Threads" to sample.threads.toString(),
            "Java heap" to "${formatBytes(sample.javaHeapUsedBytes)} / ${formatBytes(sample.javaHeapMaxBytes)}",
            "Native heap" to formatBytes(sample.nativeHeapBytes),
            "PSS total" to "${sample.pssTotalKb / 1024} MiB",
            "Refresh rate" to (sample.currentRefreshRateHz?.let { "%.1f Hz".format(it) } ?: "n/a")
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Live readings", style = MaterialTheme.typography.titleMedium)
        Text(
            text = rows.joinToString("\n") { pair -> "${pair.first}: ${pair.second}" },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kib = bytes / 1024.0
    if (kib < 1024) return "%.1fKiB".format(kib)
    val mib = kib / 1024.0
    return "%.1fMiB".format(mib)
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
