package akihz.anlaki.dev.data

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Imports system exit records that never reach a JVM uncaught-exception handler. */
@Singleton
class ProcessExitLogCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: CrashLogStore
) {
    /** Saves previously unseen exits on a worker thread; deleting logs does not reimport them. */
    @Synchronized
    fun collect() {
        try {
            val preferences = context.getSharedPreferences("diagnostic_exits", Context.MODE_PRIVATE)
            val seen = preferences.getStringSet("seen", emptySet()).orEmpty().toMutableSet()
            val manager = context.getSystemService(ActivityManager::class.java)
            val exits = manager.getHistoricalProcessExitReasons(context.packageName, 0, 32)
            for (exit in exits.sortedBy { it.timestamp }) {
                val id = "${exit.timestamp}:${exit.pid}:${exit.processName}"
                if (id in seen) continue
                if (store.saveExit(exit.timestamp, describe(exit)) != null) seen += id
            }
            // Keep ids beyond Android's current buffer so clearing reports stays effective.
            check(preferences.edit().putStringSet("seen", seen.sorted().takeLast(128).toSet()).commit()) {
                "Could not save imported exit ids"
            }
        } catch (error: Exception) {
            Timber.e(error, "Could not collect Android process exit history")
        }
    }

    private fun describe(exit: ApplicationExitInfo): String = buildString {
        appendLine("Source: Android process history. App version above is the collecting version.")
        appendLine("Process: ${exit.processName} pid=${exit.pid}")
        appendLine("Reason: ${reasonName(exit.reason)} (${exit.reason})")
        appendLine("Status: ${exit.status}; importance=${exit.importance}")
        appendLine("Memory: pss=${exit.pss} KiB; rss=${exit.rss} KiB")
        appendLine("Description: ${exit.description ?: "Not supplied by Android"}")
        // Native tombstones are protobuf, not readable text. Preserve the reason and metadata.
        if (exit.reason == ApplicationExitInfo.REASON_ANR) {
            try {
                val trace = exit.traceInputStream?.bufferedReader()?.use { reader ->
                    val buffer = CharArray(24_000)
                    var count = 0
                    while (count < buffer.size) {
                        val read = reader.read(buffer, count, buffer.size - count)
                        if (read < 0) break
                        count += read
                    }
                    String(buffer, 0, count)
                }
                appendLine("--- ANR trace ---")
                appendLine(trace ?: "No trace retained by Android")
            } catch (error: Exception) {
                appendLine("Trace unavailable: ${error.javaClass.simpleName}")
            }
        }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "Java/Kotlin crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
        ApplicationExitInfo.REASON_ANR -> "App not responding"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "Low memory"
        ApplicationExitInfo.REASON_SIGNALED -> "Process signal"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "User or system requested stop"
        ApplicationExitInfo.REASON_EXIT_SELF -> "Process exited"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Startup failure"
        ApplicationExitInfo.REASON_OTHER -> "Other system stop"
        else -> "System exit"
    }
}
