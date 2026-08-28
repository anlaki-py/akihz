package akihz.anlaki.dev.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import androidx.core.content.getSystemService
import java.io.File

/**
 * Collects per-process metric samples for the performance recorder.
 *
 * All Android-specific reads are isolated here so the surrounding recorder and
 * tests can reason about plain numbers. CPU% is computed from `/proc/<pid>/stat`
 * because Android does not expose a direct API; everything else is pulled from
 * platform sources that need no permissions on API 30+.
 */
class ProcessMetricsCollector(
    private val appContext: Context,
    private val myPid: Int = android.os.Process.myPid(),
    private val nowElapsed: () -> Long = SystemClock::elapsedRealtime,
    private val procStatReader: (Int) -> ProcStat? = ::readProcStat
) {

    private var lastSnapshot: ProcStatSnapshot? = null
    private val perPidSnapshots = mutableMapOf<Int, ProcStatSnapshot>()

    /** Snapshot of process state needed to compute the next CPU delta. */
    data class ProcStatSnapshot(val uptimeMs: Long, val cpuTicks: Long, val elapsedRealtimeMs: Long)

    /** Builds a complete [PerfSample] for the current moment. */
    fun sample(currentRefreshRateHz: Float?): PerfSample {
        val uptime = nowElapsed()
        val procStat = procStatReader(myPid)
        val (processCpu, appCpu) = computeCpu(procStat, uptime)
        val memoryInfo = readPss()
        val processes = collectAppProcesses(uptime)
        val totalPss = processes.sumOf { it.pssKb }
        return PerfSample(
            uptimeMs = uptime,
            processCpuPercent = processCpu,
            appCpuPercent = appCpu,
            threads = Thread.activeCount(),
            javaHeapUsedBytes = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() },
            javaHeapMaxBytes = Runtime.getRuntime().maxMemory(),
            nativeHeapBytes = Debug.getNativeHeapAllocatedSize(),
            pssTotalKb = memoryInfo?.totalPss?.toLong() ?: 0L,
            currentRefreshRateHz = currentRefreshRateHz,
            processCount = processes.size,
            totalPssKb = totalPss,
            processes = processes
        )
    }

    private fun collectAppProcesses(uptime: Long): List<PerfProcessInfo> {
        val pids = listAkihzPids()
        return pids.mapNotNull { pid ->
            val name = readProcessName(pid) ?: return@mapNotNull null
            val (pssKb, rssKb) = readPssForPid(pid)
            val threads = readThreadCount(pid)
            val cpu = computeCpuForPid(pid, readProcStatWithFallback(pid), uptime)
            PerfProcessInfo(
                pid = pid,
                name = name,
                pssKb = pssKb,
                rssKb = rssKb,
                cpuPercent = cpu,
                threads = threads
            )
        }.sortedByDescending { it.pssKb }
    }

    private fun listAkihzPids(): List<Int> {
        // Prefer shell ps (sees shell UID's refresh_rate_service) when Shizuku is bound
        if (ShizukuHelper.isUserServiceBound()) {
            val res = ShizukuHelper.runShellCommand("ps -A -o PID,ARGS")
            if (res.isSuccess) {
                val out = res.getOrNull() ?: ""
                val pids = out.lineSequence().mapNotNull { line ->
                    if (!line.contains("akihz.anlaki.dev")) return@mapNotNull null
                    line.trim().split(Regex("\\s+")).firstOrNull()?.toIntOrNull()
                }.toList()
                if (pids.isNotEmpty()) return pids
            }
        }
        val procDir = File("/proc")
        val files = procDir.listFiles() ?: return emptyList()
        return files.mapNotNull { f ->
            val pid = f.name.toIntOrNull() ?: return@mapNotNull null
            val cmdline = readFileContent(File(f, "cmdline").path) ?: runCatching { File(f, "comm").readText().trim() }.getOrNull()
            val name = cmdline?.replace('\u0000', ' ')?.trim()
            if (!name.isNullOrBlank() && name.contains("akihz.anlaki.dev")) pid else null
        }
    }

    private fun readFileContent(path: String): String? {
        // Try direct read first (works for own UID)
        runCatching { File(path).readBytes().decodeToString() } .getOrNull()?.let { return it }
        // Fallback via Shizuku shell (can read shell UID's /proc)
        if (ShizukuHelper.isUserServiceBound()) {
            val res = ShizukuHelper.runShellCommand("cat $path")
            if (res.isSuccess) return res.getOrNull()
        }
        return null
    }

    private fun readProcessName(pid: Int): String? {
        val raw = readFileContent("/proc/$pid/cmdline")?.replace('\u0000', ' ')?.trim()
        if (!raw.isNullOrBlank()) return raw.substringBefore(' ').trim().takeIf { it.isNotBlank() }
        return readFileContent("/proc/$pid/comm")?.trim()
    }

    private fun readPssForPid(pid: Int): Pair<Long, Long?> {
        // Try ActivityManager first (works for app UID, may fail for shell UID)
        runCatching {
            val am = appContext.getSystemService<ActivityManager>() ?: return@runCatching null
            val infos = am.getProcessMemoryInfo(intArrayOf(pid))
            val info = infos.firstOrNull()
            if (info != null && info.totalPss > 0) {
                return@runCatching Pair(info.totalPss.toLong(), null)
            }
        }
        // Fallback to /proc/<pid>/status VmRSS via shell if needed
        val status = readFileContent("/proc/$pid/status") ?: return 0L to null
        var rssKb: Long? = null
        var pssKb: Long? = null
        status.lineSequence().forEach { line ->
            when {
                line.startsWith("VmRSS:") -> rssKb = line.split(Regex("\\s+")).getOrNull(1)?.toLongOrNull()
                line.startsWith("RssAnon:") -> pssKb = line.split(Regex("\\s+")).getOrNull(1)?.toLongOrNull()
            }
        }
        val pss = pssKb ?: rssKb ?: 0L
        return pss to rssKb
    }

    private fun readThreadCount(pid: Int): Int? {
        val status = readFileContent("/proc/$pid/status") ?: return null
        status.lineSequence().forEach { line ->
            if (line.startsWith("Threads:")) {
                return line.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull()
            }
        }
        return null
    }

    private fun readProcStatWithFallback(pid: Int): ProcStat? {
        procStatReader(pid)?.let { return it }
        if (ShizukuHelper.isUserServiceBound()) {
            val content = ShizukuHelper.runShellCommand("cat /proc/$pid/stat").getOrNull() ?: return null
            val closeParen = content.lastIndexOf(')')
            if (closeParen <= 0 || closeParen >= content.length - 1) return null
            val after = content.substring(closeParen + 1).trimStart()
            val tokens = after.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val utime = tokens.getOrNull(11)?.toLongOrNull() ?: return null
            val stime = tokens.getOrNull(12)?.toLongOrNull() ?: 0L
            return ProcStat(totalTicks = utime + stime)
        }
        return null
    }

    private fun computeCpu(procStat: ProcStat?, uptime: Long): Pair<Float?, Float?> {
        val current = procStat ?: return null to null
        val previous = lastSnapshot
        val snapshot = ProcStatSnapshot(
            uptimeMs = uptime,
            cpuTicks = current.totalTicks,
            elapsedRealtimeMs = uptime
        )
        lastSnapshot = snapshot
        if (previous == null) return null to null
        val elapsedMs = (snapshot.elapsedRealtimeMs - previous.elapsedRealtimeMs).coerceAtLeast(1L)
        val tickDelta = (snapshot.cpuTicks - previous.cpuTicks).coerceAtLeast(0L)
        val clockTicksPerSec = 100L
        val processCpu = (tickDelta.toDouble() / clockTicksPerSec.toDouble() /
            (elapsedMs.toDouble() / 1000.0) * 100.0).toFloat().coerceIn(0f, 100f)
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val appCpu = (processCpu / cpuCores).coerceIn(0f, 100f)
        return processCpu to appCpu
    }

    private fun computeCpuForPid(pid: Int, procStat: ProcStat?, uptime: Long): Float? {
        val current = procStat ?: return null
        val previous = perPidSnapshots[pid]
        val snapshot = ProcStatSnapshot(uptime, current.totalTicks, uptime)
        perPidSnapshots[pid] = snapshot
        if (previous == null) return null
        val elapsedMs = (snapshot.elapsedRealtimeMs - previous.elapsedRealtimeMs).coerceAtLeast(1L)
        val tickDelta = (snapshot.cpuTicks - previous.cpuTicks).coerceAtLeast(0L)
        val clockTicksPerSec = 100L
        return (tickDelta.toDouble() / clockTicksPerSec.toDouble() / (elapsedMs.toDouble() / 1000.0) * 100.0)
            .toFloat().coerceIn(0f, 100f)
    }

    private fun readPss(): Debug.MemoryInfo? {
        val am = appContext.getSystemService<ActivityManager>() ?: return null
        val infos = am.getProcessMemoryInfo(intArrayOf(myPid))
        return infos.firstOrNull()
    }

    companion object {
        /** Lightweight view of `/proc/<pid>/stat` containing only the fields we need. */
        data class ProcStat(val totalTicks: Long)

        /**
         * Parses `/proc/<pid>/stat` for the running process. Returns null when the
         * file cannot be read or is malformed; callers should treat that as
         * "sample without CPU%".
         */
        fun readProcStat(pid: Int): ProcStat? {
            val file = File("/proc/$pid/stat")
            if (!file.canRead()) return null
            val content = runCatching { file.readText() }.getOrNull() ?: return null
            val closeParen = content.lastIndexOf(')')
            if (closeParen <= 0 || closeParen >= content.length - 1) return null
            val after = content.substring(closeParen + 1).trimStart()
            // After the closing paren, fields start at index 3 (utime). The kernel
            // splits on whitespace; the comm field was inside the parens.
            val tokens = after.split(Regex("\\s+")).filter { it.isNotEmpty() }
            // tokens[0] is field 3 (state), tokens[1] is field 4 (ppid), ...
            // Field 14 (utime) is at index 11, field 15 (stime) at index 12.
            val utime = tokens.getOrNull(11)?.toLongOrNull() ?: return null
            val stime = tokens.getOrNull(12)?.toLongOrNull() ?: 0L
            return ProcStat(totalTicks = utime + stime)
        }

        /** Builds a [PerfSessionInfo] describing the device and app at session start. */
        fun buildSessionInfo(context: Context): PerfSessionInfo {
            val packageInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val versionName = packageInfo?.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toInt() ?: 0
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode ?: 0
            }
            return PerfSessionInfo(
                startedAtIso = java.time.Instant.now().toString(),
                appVersionName = versionName,
                appVersionCode = versionCode,
                packageName = context.packageName,
                deviceModel = Build.MODEL ?: "unknown",
                deviceManufacturer = Build.MANUFACTURER ?: "unknown",
                androidRelease = Build.VERSION.RELEASE ?: "unknown",
                androidSdk = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                totalMemoryBytes = totalMemoryBytes(context),
                availableProcessors = Runtime.getRuntime().availableProcessors()
            )
        }

        private fun totalMemoryBytes(context: Context): Long {
            val am = context.getSystemService<ActivityManager>() ?: return 0L
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            return info.totalMem
        }
    }
}
