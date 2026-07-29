package akihz.anlaki.dev.presentation

import android.app.DownloadManager
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
import akihz.anlaki.dev.data.AppUpdateDownloader
import akihz.anlaki.dev.data.GitHubUpdateRepository
import akihz.anlaki.dev.domain.update.AppUpdate
import akihz.anlaki.dev.domain.update.UpdateAvailability
import akihz.anlaki.dev.domain.update.resolveUpdateAvailability
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.PreferencesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Displays update channel selection and the in-app update action. */
@Composable
fun AppUpdateSection(currentVersionCode: Long) {
    val context = LocalContext.current
    val repository = remember { GitHubUpdateRepository() }
    val downloader = remember { AppUpdateDownloader(context) }
    val scope = rememberCoroutineScope()
    var channel by remember { mutableStateOf(PreferencesHelper.updateChannel) }
    var showChannels by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadingUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var needsInstallPermission by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Check and install without opening GitHub") }
    var downloadId by remember { mutableLongStateOf(-1L) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (downloader.canInstallPackages()) {
            downloader.install(downloadId)
        } else {
            message = "Install permission was not granted."
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
            statusText = "Download complete"
            if (downloader.canInstallPackages()) downloader.install(downloadId) else {
                needsInstallPermission = true
                message = "Allow akiHz to install updates on the next screen."
            }
        },
        onError = {
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
                statusText = "Check and install without opening GitHub"
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
                    runCatching { downloader.enqueue(update) }
                        .onSuccess {
                            downloadingUpdate = update
                            downloadId = it
                            availableUpdate = null
                            statusText = "Starting download…"
                        }
                        .onFailure {
                            statusText = "Download failed"
                            message = it.message ?: "Unable to start the download"
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
            onDismissRequest = {
                message = null
                needsInstallPermission = false
            },
            title = { Text("App update") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                    if (needsInstallPermission) {
                        needsInstallPermission = false
                        permissionLauncher.launch(downloader.installPermissionIntent())
                    }
                }) { Text(if (needsInstallPermission) "Continue" else "OK") }
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
    LaunchedEffect(downloadId) {
        if (downloadId < 0 || update == null) return@LaunchedEffect
        while (true) {
            val status = downloader.status(downloadId)
                ?: return@LaunchedEffect onError("The system download was removed")
            when (status.state) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (downloader.verify(downloadId, update.sha256)) onReady()
                    else onError("The downloaded APK failed its security check")
                    return@LaunchedEffect
                }
                DownloadManager.STATUS_FAILED -> {
                    onError("Android Download Manager failed (${status.reason})")
                    return@LaunchedEffect
                }
                else -> if (status.totalBytes > 0) {
                    onProgress(
                        (status.downloadedBytes * 100.0 / status.totalBytes).roundToInt()
                    )
                }
            }
            delay(500)
        }
    }
}
