package akihz.anlaki.dev.presentation

import android.Manifest
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import akihz.anlaki.dev.data.AppUpdateDownloader
import akihz.anlaki.dev.data.GitHubUpdateRepository
import akihz.anlaki.dev.data.UpdateDownloadStore
import akihz.anlaki.dev.domain.update.AppUpdate
import akihz.anlaki.dev.domain.update.UpdateAvailability
import akihz.anlaki.dev.domain.update.resolveUpdateAvailability
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.UpdateNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Displays update channel selection and the in-app update action. */
@Composable
fun AppUpdateSection(currentVersionCode: Long) {
    val context = LocalContext.current
    val repository = remember { GitHubUpdateRepository() }
    val downloader = remember { AppUpdateDownloader(context) }
    val downloadStore = remember { UpdateDownloadStore(context) }
    val restoredDownload = remember { downloadStore.load() }
    val scope = rememberCoroutineScope()
    var channel by remember { mutableStateOf(PreferencesHelper.updateChannel) }
    var showChannels by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadingUpdate by remember {
        mutableStateOf(restoredDownload?.update)
    }
    var pendingDownloadRequest by remember { mutableStateOf<AppUpdate?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var statusText by remember {
        mutableStateOf(restoredDownload?.let { "Restoring update download…" })
    }
    var downloadId by remember {
        mutableLongStateOf(restoredDownload?.downloadId ?: -1L)
    }

    fun enqueueUpdate(update: AppUpdate) {
        runCatching { downloader.enqueue(update) }
            .onSuccess {
                downloadingUpdate = update
                downloadId = it
                availableUpdate = null
                pendingDownloadRequest = null
                statusText = "Starting download…"
            }
            .onFailure {
                statusText = "Download failed"
                message = it.message ?: "Unable to start the download"
            }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val update = pendingDownloadRequest
        if (granted && update != null) {
            enqueueUpdate(update)
        } else {
            pendingDownloadRequest = null
            message = "Notification permission is required to alert you when the update is ready."
        }
    }

    fun checkForUpdate() {
        statusText = "Checking for updates…"
        scope.launch {
            runCatching { repository.findLatest(channel) }
                .onSuccess { update ->
                    when (
                        resolveUpdateAvailability(
                            currentVersionCode = currentVersionCode,
                            latestVersionCode = update.versionCode,
                            channel = channel
                        )
                    ) {
                        UpdateAvailability.Available -> {
                            availableUpdate = update
                            statusText = "Version ${update.versionName} is available"
                        }
                        UpdateAvailability.AheadOfStable -> {
                            statusText = "You’re ahead of stable; the next stable will install normally"
                        }
                        UpdateAvailability.UpToDate -> {
                            statusText = "You’re up to date"
                        }
                    }
                }
                .onFailure {
                    statusText = "Update check failed"
                    message = it.message ?: "Unable to check for updates"
                }
        }
    }

    PreferenceTemplate(
        title = "Update channel",
        description = channel.label,
        icon = Icons.Default.NewReleases,
        onClick = { showChannels = true }
    )
    PreferenceTemplate(
        title = "Check for updates",
        description = statusText,
        icon = Icons.Default.SystemUpdate,
        onClick = ::checkForUpdate
    )

    UpdateDownloadMonitor(
        downloadId = downloadId,
        update = downloadingUpdate,
        downloader = downloader,
        onProgress = { statusText = "Downloading… $it%" },
        onReady = {
            statusText = "Download complete; tap the notification to install"
            downloadingUpdate?.let {
                UpdateNotification.showReady(context, downloadId, it.versionName)
            }
        },
        onError = {
            downloadStore.clear()
            statusText = "Download failed"
            message = it
            downloadId = -1L
        }
    )

    if (showChannels) {
        UpdateChannelDialog(
            selected = channel,
            onSelect = {
                channel = it
                PreferencesHelper.updateChannel = it
                statusText = null
                showChannels = false
            },
            onDismiss = { showChannels = false }
        )
    }
    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = { Text("Update available") },
            text = { Text("Download and install akiHz ${update.versionName}?") },
            confirmButton = {
                TextButton(onClick = {
                    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        enqueueUpdate(update)
                    } else {
                        pendingDownloadRequest = update
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { availableUpdate = null }) { Text("Not now") }
            }
        )
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("App update") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun UpdateDownloadMonitor(
    downloadId: Long,
    update: AppUpdate?,
    downloader: AppUpdateDownloader,
    onProgress: (Int) -> Unit,
    onReady: () -> Unit,
    onError: (String) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(downloadId, update, lifecycle) {
        if (downloadId < 0 || update == null) return@LaunchedEffect
        var terminal = false
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (!terminal) {
                val status = withContext(Dispatchers.IO) {
                    downloader.status(downloadId)
                }
                if (status == null) {
                    terminal = true
                    onError("The system download was removed")
                    continue
                }

                when (status.state) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        terminal = true
                        val verified = withContext(Dispatchers.IO) {
                            downloader.verify(downloadId, update.sha256)
                        }
                        if (verified) onReady()
                        else onError("The downloaded APK failed its security check")
                    }
                    DownloadManager.STATUS_FAILED -> {
                        terminal = true
                        onError("Android Download Manager failed (${status.reason})")
                    }
                    else -> if (status.totalBytes > 0) {
                        onProgress(
                            (status.downloadedBytes * 100.0 / status.totalBytes).roundToInt()
                        )
                    }
                }
                if (!terminal) delay(500)
            }
        }
    }
}
