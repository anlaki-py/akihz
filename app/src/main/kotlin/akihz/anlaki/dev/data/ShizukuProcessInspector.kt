package akihz.anlaki.dev.data

import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result

/**
 * Shell and process inspection behind [ShizukuHelper].
 *
 * Owns raw shell fallback, stale service cleanup, and perf process
 * collection. Stateless. The caller supplies the bound service.
 */
internal object ShizukuProcessInspector {

    /** Runs an arbitrary shell command as `shell` when bound, else as app. */
    internal fun runShellCommand(service: ICommandService?, command: String): Result<String> {
        if (service != null) {
            return ShizukuSettingsGateway.exec(service, command)
        }
        // Fallback: try as app (may be filtered by hidepid)
        return runCatching {
            val proc = ProcessBuilder("/system/bin/sh", "-c", command).redirectErrorStream(true).start()
            val out = proc.inputStream.bufferedReader().readText().trim()
            if (proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) && proc.exitValue() == 0) Result.success(out)
            else Result.error(ErrorType.COMMAND_EXECUTION_FAILED, out.ifBlank { "Exit ${proc.exitValue()}" })
        }.getOrElse { Result.error(ErrorType.COMMAND_EXECUTION_FAILED, it.message ?: "exec failed") }
    }

    /**
     * Kills stale `refresh_rate_service` shell processes left over from previous
     * app launches / version 2 daemon bug. Keeps the current service PID alive.
     * Returns a human-readable summary; safe to call even when not bound.
     */
    internal fun killStaleRefreshServices(service: ICommandService?): Result<String> {
        if (service == null) {
            return Result.error(ErrorType.SERVICE_BINDING_FAILED, "Service not bound")
        }
        val myServicePid = runCatching { service.getPid() }.getOrNull()
        // Use ps to list all matching PIDs (works as shell)
        val psOutput = runCatching { service.runCommand("ps -A -o PID,ARGS") }.getOrNull()
            ?: return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "ps failed")
        val stalePids = psOutput.lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (!trimmed.contains("refresh_rate_service")) return@mapNotNull null
                if (!trimmed.contains(BuildConfig.APPLICATION_ID)) return@mapNotNull null
                trimmed.split(Regex("\\s+")).firstOrNull()?.toIntOrNull()
            }
            .filter { it != myServicePid }
            .toList()
        if (stalePids.isEmpty()) return Result.success("No stale refresh_rate_service processes to kill")
        var killed = 0
        var failed = 0
        stalePids.forEach { pid ->
            val res = runCatching { service.runCommand("kill -9 $pid") }.getOrNull() ?: "ERROR"
            if (res == "OK" || res.isEmpty() || !res.startsWith("ERROR")) killed++ else failed++
        }
        // Fallback: also try via /proc scan + kill as app (may fail for shell UID but best-effort)
        return Result.success("Killed $killed, failed $failed of ${stalePids.size} stale PIDs: ${stalePids.joinToString()}")
    }

    /** Lists all `akihz` PIDs with their PSS/RSS for debugging. */
    internal fun listAppProcesses(runShell: (String) -> Result<String>): Result<String> {
        // Prefer shell ps (sees all UIDs) when Shizuku is bound
        val shellRes = runShell("ps -A -o PID,ARGS")
        if (shellRes.isSuccess) {
            val out = shellRes.getOrNull() ?: ""
            val sb = StringBuilder()
            sb.appendLine("PID | ARGS (via shell ps)")
            out.lineSequence().forEach { line ->
                if (line.contains("akihz.anlaki.dev")) sb.appendLine(line.trim())
            }
            // Also try to get detailed status via shell for each PID
            val pids = out.lineSequence().mapNotNull { line ->
                if (!line.contains("akihz.anlaki.dev")) return@mapNotNull null
                line.trim().split(Regex("\\s+")).firstOrNull()?.toIntOrNull()
            }.toList()
            if (pids.isNotEmpty()) {
                sb.appendLine("--- details via shell cat /proc/<pid>/status ---")
                pids.take(10).forEach { pid ->
                    val status = runShell("cat /proc/$pid/status").getOrNull() ?: ""
                    val rss = status.lineSequence().firstOrNull { it.startsWith("VmRSS:") }?.trim() ?: "VmRSS: n/a"
                    val threads = status.lineSequence().firstOrNull { it.startsWith("Threads:") }?.trim() ?: ""
                    val cmdline = runShell("cat /proc/$pid/cmdline").getOrNull()?.replace('\u0000', ' ')?.trim() ?: ""
                    sb.appendLine("pid $pid $rss $threads $cmdline")
                }
                if (pids.size > 10) sb.appendLine("... and ${pids.size - 10} more")
            }
            return Result.success(sb.toString())
        }
        // Fallback: app's /proc scan (hidepid may hide shell PIDs)
        val procDir = java.io.File("/proc")
        val files = procDir.listFiles() ?: return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "no /proc")
        val sb = StringBuilder()
        sb.appendLine("PID | RSS | Threads | ARGS (fallback, may be filtered)")
        files.forEach { f ->
            val pid = f.name.toIntOrNull() ?: return@forEach
            val cmdline = runCatching { java.io.File(f, "cmdline").readBytes().decodeToString().replace('\u0000', ' ').trim() }.getOrNull() ?: return@forEach
            if (!cmdline.contains("akihz.anlaki.dev")) return@forEach
            val status = runCatching { java.io.File("/proc/$pid/status").readText() }.getOrNull() ?: ""
            val rss = status.lineSequence().firstOrNull { it.startsWith("VmRSS:") }?.trim() ?: "VmRSS: n/a"
            val threads = status.lineSequence().firstOrNull { it.startsWith("Threads:") }?.trim() ?: ""
            sb.appendLine("$pid | $rss | $threads | $cmdline")
        }
        return Result.success(sb.toString())
    }

    /** Collects per-process PSS/CPU for perf log via shell when possible. */
    internal fun collectAppProcessesForPerf(runShell: (String) -> Result<String>): List<PerfProcessInfo> {
        val pids = runShell("ps -A -o PID,ARGS").getOrNull()
            ?.lineSequence()
            ?.mapNotNull { line ->
                if (!line.contains("akihz.anlaki.dev")) return@mapNotNull null
                line.trim().split(Regex("\\s+")).firstOrNull()?.toIntOrNull()?.let { it to line.trim() }
            }?.toList() ?: emptyList()
        if (pids.isEmpty()) return emptyList()
        // For each PID, try to get status via shell for RSS/threads and stat for CPU
        return pids.mapNotNull { (pid, line) ->
            val name = line.substringAfter(" ").trim().takeIf { it.isNotBlank() } ?: "akihz.anlaki.dev:refresh_rate_service"
            // Use shell to cat status
            val status = runShell("cat /proc/$pid/status").getOrNull() ?: ""
            val rssKb = status.lineSequence().firstOrNull { it.startsWith("VmRSS:") }
                ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull()
            val pssKb = rssKb ?: 0L // fallback to RSS if PSS not available (shell's RssAnon may be PSS)
            // Try to get actual PSS via dumpsys meminfo if needed, but fallback to RSS
            val threads = status.lineSequence().firstOrNull { it.startsWith("Threads:") }
                ?.split(Regex("\\s+"))?.getOrNull(1)?.toIntOrNull()
            // CPU delta is owned by ProcessMetricsCollector, which keeps tick history.
            // The old parseStatForCpu stub always returned null, so report null directly.
            PerfProcessInfo(pid = pid, name = name, pssKb = pssKb, rssKb = rssKb, cpuPercent = null, threads = threads)
        }.sortedByDescending { it.pssKb }
    }
}
