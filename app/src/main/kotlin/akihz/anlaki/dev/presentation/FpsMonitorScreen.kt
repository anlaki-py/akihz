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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
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
    var showColorOptions by remember { mutableStateOf(false) }
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
                    onClick = { showColorOptions = true }
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

    if (showColorOptions) {
        PillColorSheet(
            selected = state.pillColor,
            onSelected = {
                viewModel.setPillColor(it)
                showColorOptions = false
            },
            onDismiss = { showColorOptions = false }
        )
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

private fun pillColorLabel(color: OverlayPillColor): String =
    when (color) {
        OverlayPillColor.Black -> "Black, white text"
        OverlayPillColor.Green -> "Green, white text"
        OverlayPillColor.Red -> "Red, white text"
        OverlayPillColor.White -> "White, black text"
    }

/**
 * Bottom sheet for picking the FPS pill background color.
 *
 * @param selected currently active color
 * @param onSelected invoked when the user picks a color
 * @param onDismiss invoked when the sheet closes without a pick
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillColorSheet(
    selected: OverlayPillColor,
    onSelected: (OverlayPillColor) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = "Pill color",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            OverlayPillColor.entries.forEach { color ->
                ListItem(
                    headlineContent = { Text(pillColorLabel(color)) },
                    leadingContent = {
                        RadioButton(
                            selected = color == selected,
                            onClick = null
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
