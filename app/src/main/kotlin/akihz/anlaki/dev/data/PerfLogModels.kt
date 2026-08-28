package akihz.anlaki.dev.data

/**
 * Models for the on-device performance log.
 *
 * Every recording is a sequence of JSON-line entries; this file defines both the
 * payload shapes and the serializer that produces one line per record. Lines are
 * written to the active log as the recording progresses and rewritten in place
 * when the user stops it.
 */

/** One entry for a single OS process belonging to this app (main + any :refresh_rate_service shells). */
data class PerfProcessInfo(
    val pid: Int,
    val name: String,
    val pssKb: Long,
    val rssKb: Long?,
    val cpuPercent: Float?,
    val threads: Int?
)

/** Periodic process metric sample written once per [PerformanceMonitor] tick. */
data class PerfSample(
    val uptimeMs: Long,
    val processCpuPercent: Float?,
    val appCpuPercent: Float?,
    val threads: Int,
    val javaHeapUsedBytes: Long,
    val javaHeapMaxBytes: Long,
    val nativeHeapBytes: Long,
    val pssTotalKb: Long,
    val currentRefreshRateHz: Float?,
    val processCount: Int = 0,
    val totalPssKb: Long = 0,
    val processes: List<PerfProcessInfo> = emptyList()
)

/** One-shot event written into the log (lifecycle, error, manual marker). */
data class PerfEvent(
    val uptimeMs: Long,
    val tag: String,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

/** Header written once at the start of a recording session. */
data class PerfSessionInfo(
    val startedAtIso: String,
    val appVersionName: String,
    val appVersionCode: Int,
    val packageName: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    val androidRelease: String,
    val androidSdk: Int,
    val abi: String,
    val totalMemoryBytes: Long,
    val availableProcessors: Int
)

/** Public state of the recorder exposed to the UI. */
sealed interface PerfRecorderState {
    data object Idle : PerfRecorderState
    data class Recording(
        val session: PerfSessionInfo,
        val startedAtElapsedMs: Long,
        val samplesWritten: Int,
        val eventsWritten: Int,
        val activeLogPath: String
    ) : PerfRecorderState
    data class Stopped(
        val session: PerfSessionInfo,
        val startedAtElapsedMs: Long,
        val stoppedAtElapsedMs: Long,
        val samplesWritten: Int,
        val eventsWritten: Int,
        val activeLogPath: String,
        val stoppedReason: String
    ) : PerfRecorderState
}

/**
 * Builds a single-line JSON representation of a perf log entry.
 *
 * Keys are emitted in a stable order so two recordings of the same event are
 * byte-for-byte comparable, which makes `diff` useful.
 */
internal object PerfLogJson {
    fun escape(value: String?): String {
        if (value == null) return "null"
        val sb = StringBuilder(value.length + 8)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000c' -> sb.append("\\f")
                else -> if (ch.code < 0x20) {
                    sb.append(String.format(java.util.Locale.US, "\\u%04x", ch.code))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun number(value: Number?): String = value?.toString() ?: "null"

    fun numberOrNull(value: Float?, decimals: Int = 2): String =
        value?.let { String.format(java.util.Locale.US, "%.${decimals}f", it) } ?: "null"

    fun session(line: PerfSessionInfo): String = buildString {
        append('{')
        append("\"type\":\"session\",")
        append("\"startedAt\":${escape(line.startedAtIso)},")
        append("\"appVersionName\":${escape(line.appVersionName)},")
        append("\"appVersionCode\":${line.appVersionCode},")
        append("\"packageName\":${escape(line.packageName)},")
        append("\"deviceModel\":${escape(line.deviceModel)},")
        append("\"deviceManufacturer\":${escape(line.deviceManufacturer)},")
        append("\"androidRelease\":${escape(line.androidRelease)},")
        append("\"androidSdk\":${line.androidSdk},")
        append("\"abi\":${escape(line.abi)},")
        append("\"totalMemoryBytes\":${line.totalMemoryBytes},")
        append("\"availableProcessors\":${line.availableProcessors}")
        append('}')
    }

    fun sample(line: PerfSample): String = buildString {
        append('{')
        append("\"type\":\"sample\",")
        append("\"uptimeMs\":${line.uptimeMs},")
        append("\"processCpuPercent\":${numberOrNull(line.processCpuPercent)},")
        append("\"appCpuPercent\":${numberOrNull(line.appCpuPercent)},")
        append("\"threads\":${line.threads},")
        append("\"javaHeapUsedBytes\":${line.javaHeapUsedBytes},")
        append("\"javaHeapMaxBytes\":${line.javaHeapMaxBytes},")
        append("\"nativeHeapBytes\":${line.nativeHeapBytes},")
        append("\"pssTotalKb\":${line.pssTotalKb},")
        append("\"processCount\":${line.processCount},")
        append("\"totalPssKb\":${line.totalPssKb},")
        append("\"processes\":[")
        line.processes.forEachIndexed { i, p ->
            if (i > 0) append(',')
            append('{')
            append("\"pid\":${p.pid},")
            append("\"name\":${escape(p.name)},")
            append("\"pssKb\":${p.pssKb},")
            append("\"rssKb\":${p.rssKb?.toString() ?: "null"},")
            append("\"cpuPercent\":${numberOrNull(p.cpuPercent)},")
            append("\"threads\":${p.threads?.toString() ?: "null"}")
            append('}')
        }
        append("],")
        append("\"currentRefreshRateHz\":${numberOrNull(line.currentRefreshRateHz)}")
        append('}')
    }

    fun event(line: PerfEvent): String = buildString {
        append('{')
        append("\"type\":\"event\",")
        append("\"uptimeMs\":${line.uptimeMs},")
        append("\"tag\":${escape(line.tag)},")
        append("\"message\":${escape(line.message)},")
        append("\"data\":{")
        line.data.entries.forEachIndexed { index, entry ->
            if (index > 0) append(',')
            append("${escape(entry.key)}:${escape(entry.value)}")
        }
        append('}')
        append('}')
    }
}
