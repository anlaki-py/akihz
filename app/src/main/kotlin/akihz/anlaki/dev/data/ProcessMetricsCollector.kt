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
 * tests can reason about plain numbers. CPU% is computed from `/proc/self/stat`
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

    /** Snapshot of process state needed to compute the next CPU delta. */
    data class ProcStatSnapshot(val uptimeMs: Long, val cpuTicks: Long, val elapsedRealtimeMs: Long)

    /** Builds a complete [PerfSample] for the current moment. */
    fun sample(currentRefreshRateHz: Float?): PerfSample {
        val uptime = nowElapsed()
        val procStat = procStatReader(myPid)
        val (processCpu, appCpu) = computeCpu(procStat, uptime)
        val memoryInfo = readPss()
        return PerfSample(
            uptimeMs = uptime,
            processCpuPercent = processCpu,
            appCpuPercent = appCpu,
            threads = Thread.activeCount(),
            javaHeapUsedBytes = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() },
            javaHeapMaxBytes = Runtime.getRuntime().maxMemory(),
            nativeHeapBytes = Debug.getNativeHeapAllocatedSize(),
            pssTotalKb = memoryInfo?.totalPss?.toLong() ?: 0L,
            currentRefreshRateHz = currentRefreshRateHz
        )
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
