package akihz.anlaki.dev.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceSlider
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import rikka.shizuku.Shizuku

/**
 * Surface FPS Monitor page.
 *
 * Shows controls for overlay monitoring, target app selection, and diagnostics.
 * Uses M3 Preference components to match akiHz design system.
 */
@Composable
fun FpsMonitorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FpsMonitorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDebugDialog by remember { mutableStateOf(false) }
    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    // Keep isRunning in sync with the service even when prefs are changed externally.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            kotlinx.coroutines.delay(1000)
        }
    }

    val pickAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val component = data.component ?: return@rememberLauncherForActivityResult
        val packageName = component.packageName
        var label = packageName
        try {
            val info = context.packageManager.getActivityInfo(component, 0)
            info.loadLabel(context.packageManager)?.let { label = it.toString() }
        } catch (_: Exception) {}
        viewModel.setTarget(packageName, label)
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            if (pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                attemptStart(context, viewModel)
            } else {
                viewModel.refresh()
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            attemptStart(context, viewModel)
        } else {
            viewModel.refresh()
        }
    }

    // Listen for Shizuku permission result to auto-retry start.
    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED && pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                attemptStart(context, viewModel)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    PreferenceLayout(
        label = "FPS Monitor",
        modifier = modifier.fillMaxSize(),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back",
        onNavigationClick = onBack
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            StatusHeader(state = state)

            PreferenceGroup(heading = "Controls") {
                PreferenceTemplate(
                    title = if (state.isRunning) "Stop monitoring" else "Start monitoring",
                    description = if (state.isRunning) {
                        "Hide overlay and stop sampling"
                    } else {
                        "Show floating FPS pill and sample every 500 ms"
                    },
                    icon = if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    onClick = {
                        if (state.isRunning) {
                            viewModel.stopService(context)
                        } else {
                            // Stable start flow: check each permission sequentially, launch as needed.
                            if (!viewModel.hasShizukuBinder()) {
                                viewModel.refresh()
                                // Show message via state; ViewModel will set message.
                                // Use direct status update.
                                viewModel.startService(context)
                                return@PreferenceTemplate
                            }
                            if (!viewModel.hasShizukuPermission()) {
                                pendingStartAfterPermission = true
                                viewModel.requestShizukuPermission()
                                return@PreferenceTemplate
                            }
                            if (!viewModel.canDrawOverlays(context)) {
                                pendingStartAfterPermission = true
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayLauncher.launch(intent)
                                return@PreferenceTemplate
                            }
                            if (!viewModel.hasNotificationPermission(context)) {
                                pendingStartAfterPermission = true
                                if (Build.VERSION.SDK_INT >= 33) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                return@PreferenceTemplate
                            }
                            viewModel.startService(context)
                        }
                    }
                )
                PreferenceTemplate(
                    title = if (state.debugLoggingEnabled) "Turn off debug logging" else "Turn on debug logging",
                    description = "Log Shizuku state, focus lines, and layer stats",
                    icon = Icons.Default.BugReport,
                    onClick = { viewModel.toggleDebugLogging() }
                )
                PreferenceTemplate(
                    title = "View debug log",
                    description = if (state.debugLoggingEnabled) "Copy or share diagnostics" else "Enable logging first",
                    icon = Icons.Default.Api,
                    onClick = { showDebugDialog = true }
                )
            }

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
            }

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
                    onClick = {
                        val launcher = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                        val picker = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
                            putExtra(Intent.EXTRA_INTENT, launcher)
                        }
                        try {
                            pickAppLauncher.launch(picker)
                        } catch (_: Exception) {
                            viewModel.refresh()
                        }
                    }
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

            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = "Method is OEM-dependent. Xiaomi 12T / HyperOS 2 validated. Static screens may show Idle / no data. Drag the pill to move it, tap to show layer and size options.",
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

            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                LaunchedEffect(message) { viewModel.consumeMessage() }
            }
        }
    }

    if (showDebugDialog) {
        val logText = if (state.debugLoggingEnabled) {
            state.debugLog.ifBlank { "No diagnostics yet. Start the monitor first." }
        } else {
            "Debug logging is turned off."
        }
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("FPS Monitor diagnostics") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    copyToClipboard(context, logText)
                    showDebugDialog = false
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = {
                    shareText(context, logText)
                    showDebugDialog = false
                }) { Text("Share") }
            }
        )
    }
}

private fun attemptStart(context: Context, viewModel: FpsMonitorViewModel) {
    if (!viewModel.hasShizukuBinder()) {
        viewModel.startService(context)
        return
    }
    if (!viewModel.hasShizukuPermission()) {
        viewModel.requestShizukuPermission()
        return
    }
    if (!viewModel.canDrawOverlays(context)) return
    if (!viewModel.hasNotificationPermission(context)) return
    viewModel.startService(context)
}

@Composable
private fun StatusHeader(state: FpsMonitorUiState) {
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

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FPS Monitor diagnostics", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "FPS Monitor diagnostics")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share diagnostics"))
}
