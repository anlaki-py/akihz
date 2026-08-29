package akihz.anlaki.dev

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import akihz.anlaki.dev.data.CrashLogStore
import akihz.anlaki.dev.data.PerformanceMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * CLI entry for debugging without UI navigation.
 *
 * Trigger via:
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd list_crashes`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd cat_crash --es arg crash-...log`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd create_test_crash`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd clear_crashes`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd perf_status`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd perf_start`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd perf_stop`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd perf_cat`
 * `adb shell am broadcast -a akihz.anlaki.dev.DEBUG_CLI --es cmd trigger_crash --es arg "test message"`
 *
 * Results are written to `filesDir/debug_cli_output.txt` and also logged via Timber/logcat.
 * Read with:
 * `adb shell run-as akihz.anlaki.dev.debug cat /data/data/akihz.anlaki.dev.debug/files/debug_cli_output.txt`
 * For release: `adb shell run-as akihz.anlaki.dev cat ...`
 */
@AndroidEntryPoint
class DebugCliReceiver : BroadcastReceiver() {

    @Inject lateinit var crashLogStore: CrashLogStore
    @Inject lateinit var performanceMonitor: PerformanceMonitor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val cmd = intent.getStringExtra(EXTRA_CMD) ?: run {
            writeOutput(context, "ERROR: missing --es cmd <value>")
            return
        }
        val arg = intent.getStringExtra(EXTRA_ARG)
        // Use goAsync to allow coroutine work without ANR.
        val pending = goAsync()
        scope.launch {
            try {
                val result = handleCommand(context, cmd, arg)
                writeOutput(context, result)
                Timber.i("DebugCli cmd=%s arg=%s -> %s", cmd, arg, result.take(400))
            } catch (e: Exception) {
                val err = "ERROR handling $cmd: ${e.message}\n${e.stackTraceToString().take(2000)}"
                writeOutput(context, err)
                Timber.e(e, "DebugCli failed")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleCommand(context: Context, cmd: String, arg: String?): String = when (cmd) {
        "list_crashes" -> listCrashes()
        "cat_crash" -> catCrash(arg)
        "create_test_crash" -> createTestCrash()
        "clear_crashes" -> clearCrashes()
        "perf_status" -> perfStatus()
        "perf_start" -> perfStart()
        "perf_stop" -> perfStop()
        "perf_cat" -> perfCat(context)
        "trigger_crash" -> triggerCrash(arg)
        "list_processes", "ps", "list_ps" -> listProcesses()
        "kill_stale", "cleanup" -> killStale()
        "test_tile" -> testTile(context, arg)
        "help" -> helpText()
        else -> "Unknown cmd: $cmd\n${helpText()}"
    }

    private fun listCrashes(): String {
        val entries = crashLogStore.getAll()
        if (entries.isEmpty()) return "No crashes stored (filesDir/crash_logs is empty)"
        return buildString {
            appendLine("Crashes: ${entries.size}")
            entries.forEach { e ->
                appendLine("${e.fileName} | ${e.sizeBytes} bytes | ${java.util.Date(e.timestampMillis)} | ${e.file.absolutePath}")
            }
        }
    }

    private fun catCrash(arg: String?): String {
        if (arg.isNullOrBlank()) return "Usage: --es cmd cat_crash --es arg <filename>\n${listCrashes()}"
        val entries = crashLogStore.getAll()
        val entry = entries.firstOrNull { it.fileName == arg } ?: entries.firstOrNull { it.fileName.contains(arg) }
            ?: return "Not found: $arg\n${listCrashes()}"
        val content = crashLogStore.readContent(entry)
        return "File: ${entry.fileName}\nSize: ${entry.sizeBytes}\n---\n$content"
    }

    private fun createTestCrash(): String {
        val file = crashLogStore.saveCaught("cli_test_crash", RuntimeException("Test crash via DebugCliReceiver"))
            ?: return "Failed to create test crash"
        return "Created test crash: ${file.name} (${file.length()} bytes) at ${file.absolutePath}\n${file.readText().take(2000)}"
    }

    private fun clearCrashes(): String {
        val count = crashLogStore.deleteAll()
        return "Deleted $count crash file(s)"
    }

    private fun perfStatus(): String {
        val state = performanceMonitor.state.value
        return "Perf state: $state\nLast duration: ${performanceMonitor.lastSessionDurationMs()} ms"
    }

    private suspend fun perfStart(): String {
        val file = performanceMonitor.start()
        return if (file != null) "Perf started: ${file.absolutePath}\nState: ${performanceMonitor.state.value}"
        else "Failed to start perf (already recording?)\nState: ${performanceMonitor.state.value}"
    }

    private suspend fun perfStop(): String {
        val file = performanceMonitor.stop()
        return if (file != null) "Perf stopped: ${file.absolutePath} (${file.length()} bytes)\nState: ${performanceMonitor.state.value}"
        else "No active recording to stop\nState: ${performanceMonitor.state.value}"
    }

    private fun perfCat(context: Context): String {
        val state = performanceMonitor.state.value
        val path = when (state) {
            is akihz.anlaki.dev.data.PerfRecorderState.Recording -> state.activeLogPath
            is akihz.anlaki.dev.data.PerfRecorderState.Stopped -> state.activeLogPath
            else -> return "No active perf log (state=Idle). Start with perf_start first."
        }
        val file = File(path)
        if (!file.exists()) return "Perf file not found at $path"
        val content = runCatching { file.readText() }.getOrElse { "Failed to read: ${it.message}" }
        return "Perf log: $path (${file.length()} bytes)\n---\n${content.take(8000)}${if (content.length > 8000) "\n...truncated" else ""}"
    }

    private fun triggerCrash(arg: String?): String {
        // Create a crash file without actually crashing the process; useful for testing save/copy/share.
        val msg = arg ?: "CLI triggered crash"
        val file = crashLogStore.saveSync(Thread.currentThread(), RuntimeException(msg))
            ?: return "Failed to trigger crash"
        // Note: we deliberately do NOT throw, just persist. To actually crash, use `throw RuntimeException(msg)`.
        return "Triggered crash log: ${file.name}\n${file.readText().take(2000)}"
    }

    private fun listProcesses(): String {
        // First try ShizukuHelper's detailed list (needs service, but we also have fallback)
        val shizukuRes = runCatching { akihz.anlaki.dev.data.ShizukuHelper.listAppProcesses() }.getOrNull()
        if (shizukuRes != null && shizukuRes.isSuccess) {
            return "Via ShizukuHelper:\n${shizukuRes.getOrNull()}\n---\n" + fallbackPsList()
        }
        return fallbackPsList()
    }

    private fun fallbackPsList(): String {
        val procDir = File("/proc")
        val files = procDir.listFiles() ?: return "No /proc"
        val sb = StringBuilder()
        sb.appendLine("PID | PPID | RSS | Threads | CMD")
        var found = 0
        files.forEach { f ->
            val pid = f.name.toIntOrNull() ?: return@forEach
            val cmdline = runCatching { File(f, "cmdline").readBytes().decodeToString().replace('\u0000', ' ').trim() }.getOrNull() ?: return@forEach
            if (!cmdline.contains("akihz.anlaki.dev")) return@forEach
            val status = runCatching { File("/proc/$pid/status").readText() }.getOrNull() ?: ""
            val rss = status.lineSequence().firstOrNull { it.startsWith("VmRSS:") }?.replace(Regex("\\s+"), " ")?.trim() ?: "VmRSS: n/a"
            val threads = status.lineSequence().firstOrNull { it.startsWith("Threads:") }?.trim() ?: ""
            val stat = runCatching { File("/proc/$pid/stat").readText() }.getOrNull() ?: ""
            val ppid = stat.split(Regex("\\s+")).getOrNull(3) ?: "?"
            sb.appendLine("$pid | ppid $ppid | $rss | $threads | $cmdline")
            found++
        }
        if (found == 0) sb.appendLine("No akihz processes found via /proc scan")
        // Also include top-like summary via ActivityManager if available
        sb.appendLine("--- summary: $found akihz processes ---")
        // Try to get total PSS via collector's last sample if available (not here)
        return sb.toString()
    }

    private fun killStale(): String {
        val res = akihz.anlaki.dev.data.ShizukuHelper.killStaleRefreshServices()
        return when {
            res.isSuccess -> res.getOrNull() ?: "Killed"
            else -> "Failed: ${res.getErrorOrNull()?.message ?: "unknown"} (is Shizuku bound? ${akihz.anlaki.dev.data.ShizukuHelper.isUserServiceBound()})"
        }
    }

    private fun testTile(context: Context, arg: String?): String {
        val rate = arg?.toFloatOrNull() ?: 90f
        // Test both connecting and switching notifications
        akihz.anlaki.dev.utils.TileFeedbackNotification.showConnecting(context)
        // Small delay then switching to simulate real flow
        Thread.sleep(400)
        akihz.anlaki.dev.utils.TileFeedbackNotification.showSwitching(context, rate)
        Thread.sleep(400)
        akihz.anlaki.dev.utils.TileFeedbackNotification.showSwitched(context, rate)
        return "Posted tile feedback notifications: Connecting -> Switching to ${rate}Hz -> Switched. Check shade or toast fallback if blocked.\nChannel importance=${context.getSystemService(android.app.NotificationManager::class.java).getNotificationChannel("akihz_tile_feedback")?.importance} areNotificationsEnabled=${androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()}"
    }

    private fun helpText(): String = """
        Available cmds:
          list_crashes          - list all crash files
          cat_crash <name>      - cat specific crash file (partial match)
          create_test_crash     - write a fake crash for testing
          clear_crashes         - delete all crash files
          trigger_crash [msg]   - write uncaught-style crash log
          perf_status           - show perf recorder state
          perf_start            - start perf recording
          perf_stop             - stop perf recording
          perf_cat              - cat active perf log (includes per-process pss/cpu)
          list_processes|ps     - list all akihz PIDs with RSS/threads (via /proc)
          kill_stale|cleanup    - kill stale refresh_rate_service shells (frees RAM)
        Results also written to filesDir/debug_cli_output.txt
        Read with: adb shell run-as <pkg> cat /data/data/<pkg>/files/debug_cli_output.txt
    """.trimIndent()

    private fun writeOutput(context: Context, text: String) {
        runCatching {
            val out = File(context.filesDir, OUTPUT_FILE)
            out.writeText(text + "\n")
        }
    }

    companion object {
        const val ACTION = "akihz.anlaki.dev.DEBUG_CLI"
        const val EXTRA_CMD = "cmd"
        const val EXTRA_ARG = "arg"
        const val OUTPUT_FILE = "debug_cli_output.txt"
    }
}
