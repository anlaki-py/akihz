package akihz.anlaki.dev.presentation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import akihz.anlaki.dev.data.PerfRecorderState
import akihz.anlaki.dev.data.PerfSessionInfo
import akihz.anlaki.dev.data.PerformanceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** UI-facing state for the performance monitor page. */
data class PerformanceMonitorUiState(
    val recorder: PerfRecorderState = PerfRecorderState.Idle,
    val lastSample: akihz.anlaki.dev.data.PerfSample? = null,
    val savedLocation: String? = null,
    val message: String? = null
)

/**
 * ViewModel for the performance monitor screen.
 *
 * Combines the [PerformanceMonitor]'s recorder state with the latest live
 * sample so the UI can show both the persistent state (recording/stopped/idle)
 * and the most recent instantaneous readings.
 */
@HiltViewModel
class PerformanceMonitorViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val performanceMonitor: PerformanceMonitor
) : ViewModel() {

    private val lastSample = MutableStateFlow<akihz.anlaki.dev.data.PerfSample?>(null)
    private val savedLocation = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PerformanceMonitorUiState> = combine(
        performanceMonitor.state,
        lastSample,
        savedLocation,
        message
    ) { recorder, sample, saved, msg ->
        PerformanceMonitorUiState(
            recorder = recorder,
            lastSample = sample,
            savedLocation = saved,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PerformanceMonitorUiState())

    /** Begins a recording session and immediately captures a baseline sample. */
    fun startRecording() {
        viewModelScope.launch {
            val started = performanceMonitor.start()
            if (started == null) {
                message.value = "Could not start recording"
                return@launch
            }
            lastSample.value = performanceMonitor.currentSample()
        }
    }

    /** Stops the active recording. */
    fun stopRecording() {
        viewModelScope.launch {
            val file = performanceMonitor.stop()
            if (file == null) {
                message.value = "No active recording"
            }
        }
    }

    /** Refreshes the instantaneous sample shown on the page. */
    fun refreshSample() {
        lastSample.value = performanceMonitor.currentSample()
    }

    /** Cancels the active recording and removes the log file from disk. */
    fun discard() {
        viewModelScope.launch {
            performanceMonitor.discardActive()
            savedLocation.value = null
            message.value = "Recording discarded"
        }
    }

    /**
     * Copies the active log into the public Downloads/akihz/perf directory via
     * MediaStore. The resulting path is exposed via [savedLocation] so the UI
     * can show "Saved to ..." and a confirmation.
     */
    fun saveToDownloads() {
        viewModelScope.launch {
            val current = uiState.value.recorder as? PerfRecorderState.Stopped
                ?: run {
                    message.value = "Stop the recording before saving"
                    return@launch
                }
            val saved = runCatching { writeToDownloads(current.session) }
                .getOrElse {
                    Timber.w(it, "Failed to save perf log to Downloads")
                    message.value = "Could not save log"
                    return@launch
                }
            savedLocation.value = saved
            message.value = "Saved"
        }
    }

    /**
     * Exposes a shareable URI through the system share sheet so the user can
     * send the log anywhere (clipboard, drive, email, etc.).
     */
    fun shareLog() {
        viewModelScope.launch {
            val current = uiState.value.recorder as? PerfRecorderState.Stopped
                ?: run {
                    message.value = "Stop the recording before sharing"
                    return@launch
                }
            val uri = shareableUriFor(current.session)
            if (uri == null) {
                message.value = "Could not prepare log for sharing"
                return@launch
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share performance log").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(chooser)
            message.value = "Choose where to share the log"
        }
    }

    /** Clears the transient toast/status message after the UI consumes it. */
    fun consumeMessage() {
        message.value = null
    }

    private suspend fun writeToDownloads(session: PerfSessionInfo): String =
        withContext(Dispatchers.IO) {
            val source = File(activeLogPath())
            val name = exportFilename(session)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appContext.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/akihz/perf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, values)
                    ?: error("MediaStore.insert returned null")
                resolver.openOutputStream(uri).use { output ->
                    source.inputStream().use { input ->
                        checkNotNull(output) { "Output stream was null for $uri" }.also {
                            input.copyTo(it)
                        }
                    }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "${Environment.DIRECTORY_DOWNLOADS}/akihz/perf/$name"
            } else {
                @Suppress("DEPRECATION")
                val downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val targetDir = File(downloads, "akihz/perf").apply { mkdirs() }
                val target = File(targetDir, name)
                source.copyTo(target, overwrite = true)
                target.absolutePath
            }
        }

    private suspend fun shareableUriFor(session: PerfSessionInfo): Uri? = withContext(Dispatchers.IO) {
        val source = File(activeLogPath())
        val cached = File(appContext.cacheDir, "share").apply { mkdirs() }
        val target = File(cached, exportFilename(session))
        source.copyTo(target, overwrite = true)
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", target)
    }

    private fun activeLogPath(): String = when (val current = performanceMonitor.state.value) {
        is PerfRecorderState.Recording -> current.activeLogPath
        is PerfRecorderState.Stopped -> current.activeLogPath
        PerfRecorderState.Idle -> error("No active log file")
    }

    private fun exportFilename(session: PerfSessionInfo): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val safeVersion = session.appVersionName.replace(Regex("[^A-Za-z0-9._-]"), "-")
        return "akihz-perf-v$safeVersion-$stamp.log"
    }
}
