package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lists persisted crash/error logs with actions to copy, save, share, and delete.
 *
 * Intended as a developer aid: when a user hits a crash they can open Debug
 * -> Crash logs -> copy/share the file without needing adb.
 */
@Composable
internal fun CrashLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrashLogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PreferenceLayout(
        label = "Error logs",
        modifier = modifier.fillMaxSize(),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back to debug options",
        onNavigationClick = onBack
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(state = state)
            PreferenceGroup(heading = "Actions") {
                PreferenceTemplate(
                    title = "Copy debug report",
                    description = "Copy recent errors and events in one report",
                    icon = Icons.Default.ContentCopy,
                    onClick = { viewModel.copyDebugReport() }
                )
                PreferenceTemplate(
                    title = "Share all logs",
                    description = "Share all saved reports and events as a text file",
                    icon = Icons.Default.Share,
                    onClick = { viewModel.shareAllLogs() }
                )
                PreferenceTemplate(
                    title = "Refresh",
                    description = "Reload errors and Android process exit records",
                    icon = Icons.Default.Refresh,
                    onClick = { viewModel.refresh() }
                )
                PreferenceTemplate(
                    title = "Create test error",
                    description = "Check error logging without crashing the app",
                    icon = Icons.Default.BugReport,
                    onClick = { viewModel.logTestCrash() }
                )
                if (state.entries.isNotEmpty()) {
                    PreferenceTemplate(
                        title = "Delete all",
                        description = "Remove ${state.entries.size} file(s)",
                        icon = Icons.Default.Delete,
                        onClick = { viewModel.deleteAll() }
                    )
                }
            }

            if (state.isLoading) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else if (state.entries.isEmpty()) {
                Text(
                    text = "No error reports saved yet. Warnings, errors, and crashes are saved even when performance recording is off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                PreferenceGroup(heading = "Reports (${state.entries.size})") {
                    state.entries.forEach { entry ->
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(entry.timestampMillis))
                        val sizeKb = String.format(Locale.US, "%.1f KiB", entry.sizeBytes / 1024.0)
                        PreferenceTemplate(
                            title = entry.fileName,
                            description = "$date • $sizeKb",
                            icon = Icons.Default.BugReport,
                            onClick = { viewModel.openEntry(entry) }
                        )
                    }
                }
            }

            // Expanded preview for selected entry
            state.selectedContent?.let { content ->
                PreferenceGroup(heading = "Preview: ${state.selectedFileName ?: ""}") {
                    formItem {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                PreferenceGroup(heading = "Preview actions") {
                    PreferenceTemplate(
                        title = "Copy",
                        description = "Copy full text to clipboard",
                        icon = Icons.Default.ContentCopy,
                        onClick = { viewModel.copyToClipboard() }
                    )
                    PreferenceTemplate(
                        title = "Save to Downloads",
                        description = "Export to Downloads/akihz/crashes",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.saveSelectedToDownloads() }
                    )
                    PreferenceTemplate(
                        title = "Share",
                        description = "Open system share sheet",
                        icon = Icons.Default.Share,
                        onClick = { viewModel.shareSelected() }
                    )
                    PreferenceTemplate(
                        title = "Delete this report",
                        description = "Remove ${state.selectedFileName ?: ""}",
                        icon = Icons.Default.Delete,
                        onClick = {
                            state.entries.firstOrNull { it.fileName == state.selectedFileName }?.let {
                                viewModel.deleteEntry(it)
                            } ?: viewModel.clearSelection()
                        }
                    )
                    PreferenceTemplate(
                        title = "Close preview",
                        description = "Hide the expanded stack trace",
                        onClick = { viewModel.clearSelection() }
                    )
                }
            }

            // Also expose quick actions per entry when not in preview? Show global actions for first entry
            if (state.selectedContent == null && state.entries.isNotEmpty()) {
                PreferenceGroup(heading = "Quick actions (most recent)") {
                    val recent = state.entries.first()
                    PreferenceTemplate(
                        title = "Copy most recent",
                        description = recent.fileName,
                        icon = Icons.Default.ContentCopy,
                        onClick = { viewModel.copyEntry(recent) }
                    )
                    PreferenceTemplate(
                        title = "Share most recent",
                        description = "Share ${recent.fileName}",
                        icon = Icons.Default.Share,
                        onClick = { viewModel.shareEntry(recent) }
                    )
                    PreferenceTemplate(
                        title = "Save most recent to Downloads",
                        description = "Save ${recent.fileName} to Downloads/akihz/crashes",
                        onClick = { viewModel.saveToDownloads(recent) }
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
private fun Header(state: CrashLogUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${state.entries.size} reports stored",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "App warnings, errors, crashes, and Android exit records. " +
                "Keeps the latest 100 errors, 30 crashes, and 30 exits. " +
                "Copy a debug report or share all saved logs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
