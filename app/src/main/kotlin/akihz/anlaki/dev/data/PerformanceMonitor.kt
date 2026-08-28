package akihz.anlaki.dev.data

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground-only recorder that appends process metric samples and ad-hoc
 * events to a JSON-line log file in the app's cache directory.
 *
 * Recording is always explicit and scoped to the lifetime of the app process:
 * if the process is killed the log file remains on disk but no final "stop"
 * entry is appended, which is easy to detect on later analysis.
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val displayManagerDataSource: DisplayManagerDataSource
) {
    private val mutex = Mutex()
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val collector = ProcessMetricsCollector(appContext)

    private val _state = MutableStateFlow<PerfRecorderState>(PerfRecorderState.Idle)
    val state: StateFlow<PerfRecorderState> = _state.asStateFlow()

    private var samplingJob: Job? = null

    /** Begins a new recording session. Returns the active log file when started. */
    suspend fun start(sampleIntervalMs: Long = 1_000L): File? = mutex.withLock {
        val current = _state.value
        if (current is PerfRecorderState.Recording) {
            return@withLock File(current.activeLogPath)
        }
        val session = ProcessMetricsCollector.buildSessionInfo(appContext)
        val activeFile = activeLogFile().apply {
            try {
                writeText(PerfLogJson.session(session) + "\n")
            } catch (e: IOException) {
                Timber.w(e, "Failed to write perf log header")
                return@withLock null
            }
        }
        val startedAt = SystemClock.elapsedRealtime()
        _state.value = PerfRecorderState.Recording(
            session = session,
            startedAtElapsedMs = startedAt,
            samplesWritten = 0,
            eventsWritten = 1,
            activeLogPath = activeFile.absolutePath
        )
        launchSampling(sampleIntervalMs)
        activeFile
    }

    /** Stops the active recording and tags the file with a stop reason. */
    suspend fun stop(reason: String = "user_stopped"): File? = mutex.withLock {
        val current = _state.value as? PerfRecorderState.Recording ?: return@withLock null
        samplingJob?.cancel()
        samplingJob = null
        val stoppedAt = SystemClock.elapsedRealtime()
        val stopped = PerfRecorderState.Stopped(
            session = current.session,
            startedAtElapsedMs = current.startedAtElapsedMs,
            stoppedAtElapsedMs = stoppedAt,
            samplesWritten = current.samplesWritten,
            eventsWritten = current.eventsWritten + 1,
            activeLogPath = current.activeLogPath,
            stoppedReason = reason
        )
        _state.value = stopped
        runCatching {
            File(current.activeLogPath).appendText(
                PerfLogJson.event(
                    PerfEvent(
                        uptimeMs = stoppedAt,
                        tag = "recording_stopped",
                        message = reason
                    )
                ) + "\n"
            )
        }.onFailure { Timber.w(it, "Failed to append perf stop event") }
        File(current.activeLogPath)
    }

    /**
     * Appends a manual event to the active recording. No-op when not recording.
     */
    suspend fun log(tag: String, message: String, data: Map<String, String> = emptyMap()): Boolean =
        mutex.withLock {
            val current = _state.value as? PerfRecorderState.Recording ?: return@withLock false
            val uptime = SystemClock.elapsedRealtime()
            val event = PerfEvent(uptimeMs = uptime, tag = tag, message = message, data = data)
            val appended = runCatching {
                File(current.activeLogPath).appendText(PerfLogJson.event(event) + "\n")
            }.isSuccess
            if (appended) {
                _state.value = current.copy(eventsWritten = current.eventsWritten + 1)
            }
            appended
        }

    /**
     * Returns a snapshot of the current process metrics without writing a
     * sample line. Useful for the live readouts on the debug page.
     */
    fun currentSample(currentRefreshRateHz: Float? = null): PerfSample =
        collector.sample(currentRefreshRateHz)

    /** Resets back to [PerfRecorderState.Idle] and removes the cached log file. */
    suspend fun discardActive(): Boolean = mutex.withLock {
        val current = _state.value
        val path = when (current) {
            is PerfRecorderState.Recording -> current.activeLogPath
            is PerfRecorderState.Stopped -> current.activeLogPath
            PerfRecorderState.Idle -> null
        }
        samplingJob?.cancel()
        samplingJob = null
        _state.value = PerfRecorderState.Idle
        if (path != null) {
            runCatching { File(path).delete() }.isSuccess
        } else {
            false
        }
    }

    /** Returns the human-readable duration of the most recent recording session. */
    fun lastSessionDurationMs(): Long? {
        return when (val current = _state.value) {
            is PerfRecorderState.Recording -> SystemClock.elapsedRealtime() - current.startedAtElapsedMs
            is PerfRecorderState.Stopped -> current.stoppedAtElapsedMs - current.startedAtElapsedMs
            PerfRecorderState.Idle -> null
        }
    }

    private fun launchSampling(intervalMs: Long) {
        samplingJob?.cancel()
        samplingJob = recorderScope.launch {
            while (isActive) {
                delay(intervalMs)
                val state = _state.value as? PerfRecorderState.Recording ?: return@launch
                val refreshRate = readCurrentRefreshRate()
                val sample = try {
                    collector.sample(currentRefreshRateHz = refreshRate)
                } catch (e: Throwable) {
                    Timber.w(e, "Failed to read perf sample")
                    continue
                }
                val written = runCatching {
                    File(state.activeLogPath).appendText(PerfLogJson.sample(sample) + "\n")
                }.isSuccess
                if (written) {
                    _state.value = state.copy(samplesWritten = state.samplesWritten + 1)
                }
            }
        }
    }

    private fun readCurrentRefreshRate(): Float? {
        return when (val result = displayManagerDataSource.getCurrentRefreshRate()) {
            is akihz.anlaki.dev.utils.Result.Success -> result.data
            is akihz.anlaki.dev.utils.Result.Error -> null
        }
    }

    private fun activeLogFile(): File {
        val dir = File(appContext.cacheDir, "perf")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "perf-active.log")
    }

    companion object {
        /** Filename used when copying the active log out for sharing or save. */
        fun exportFilename(timestamp: Date = Date()): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(timestamp)
            return "akihz-perf-$stamp.log"
        }
    }
}
