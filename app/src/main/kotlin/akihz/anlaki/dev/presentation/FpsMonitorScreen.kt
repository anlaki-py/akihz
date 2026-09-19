package akihz.anlaki.dev.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import akihz.anlaki.dev.presentation.components.PreferenceLayout
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
            FpsStatusHeader(state = state)

            FpsControlsSection(
                state = state,
                onStartStop = {
                    if (state.isRunning) {
                        viewModel.stopService(context)
                    } else {
                        startWithPermissionChecks(
                            context = context,
                            viewModel = viewModel,
                            overlayLauncher = { intent ->
                                pendingStartAfterPermission = true
                                overlayLauncher.launch(intent)
                            },
                            notificationLauncher = { permission ->
                                pendingStartAfterPermission = true
                                notificationLauncher.launch(permission)
                            },
                            onRequestShizuku = { pendingStartAfterPermission = true }
                        )
                    }
                },
                onToggleDebug = { viewModel.toggleDebugLogging() },
                onViewLog = { showDebugDialog = true }
            )

            FpsOverlaySection(
                state = state,
                viewModel = viewModel,
                onPickColor = { showColorOptions = true }
            )

            FpsTargetSection(
                state = state,
                viewModel = viewModel,
                onSelectApp = {
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

            FpsMonitorFootnotes(
                message = state.message,
                onConsume = { viewModel.consumeMessage() }
            )
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
        FpsDebugLogDialog(
            logText = logText,
            onCopy = {
                copyFpsDiagnostics(context, logText)
                showDebugDialog = false
            },
            onShare = {
                shareFpsDiagnostics(context, logText)
                showDebugDialog = false
            },
            onDismiss = { showDebugDialog = false }
        )
    }
}

/**
 * Stable start flow: checks each permission in order, launching requests as needed.
 *
 * Each launcher sets its own pending flag before firing, so the result
 * handler knows to retry the start when permission arrives.
 */
private fun startWithPermissionChecks(
    context: Context,
    viewModel: FpsMonitorViewModel,
    overlayLauncher: (Intent) -> Unit,
    notificationLauncher: (String) -> Unit,
    onRequestShizuku: () -> Unit
) {
    if (!viewModel.hasShizukuBinder()) {
        viewModel.refresh()
        viewModel.startService(context)
        return
    }
    if (!viewModel.hasShizukuPermission()) {
        onRequestShizuku()
        viewModel.requestShizukuPermission()
        return
    }
    if (!viewModel.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        overlayLauncher(intent)
        return
    }
    if (!viewModel.hasNotificationPermission(context)) {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationLauncher(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        return
    }
    viewModel.startService(context)
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
