package akihz.anlaki.dev.data.fps

/**
 * Parses SurfaceFlinger TimeStats and WindowManager dumps.
 *
 * Ported from the standalone Surface FPS Monitor utility.
 */
object TimeStatsParser {

    private val focusMarkers = arrayOf(
        "mCurrentFocus", "mFocusedWindow", "mFocusedApp", "topResumedActivity",
        "mResumedActivity", "ResumedActivity", "topActivity"
    )

    /** Extracts the foreground package from a window or activity dump. */
    fun foregroundPackage(dump: String?): String? {
        if (dump == null) return null
        for (marker in focusMarkers) {
            var searchFrom = 0
            while (searchFrom < dump.length) {
                val markerAt = dump.indexOf(marker, searchFrom)
                if (markerAt < 0) break
                var lineEnd = dump.indexOf('\n', markerAt)
                if (lineEnd < 0) lineEnd = dump.length
                val packageName = componentPackage(dump, markerAt + marker.length, lineEnd)
                if (packageName != null) return packageName
                searchFrom = lineEnd + 1
            }
        }
        return null
    }

    /**
     * Corrects quantized FPS caused by millisecond binning in TimeStats.
     *
     * For example, 8 ms presents as 125 FPS even when the display is 120 Hz.
     */
    fun displayFps(measuredFps: Double, displayRefreshRate: Double): Double {
        if (!(measuredFps > 0.0) || !(displayRefreshRate > 0.0) || measuredFps <= displayRefreshRate) {
            return measuredFps
        }
        val refreshIntervalMs = 1000.0 / displayRefreshRate
        val integerIntervalMs = Math.floor(refreshIntervalMs)
        if (integerIntervalMs < 1.0) return measuredFps
        val quantizedCeiling = 1000.0 / integerIntervalMs
        return if (measuredFps <= quantizedCeiling * 1.01) displayRefreshRate else measuredFps
    }

    private fun componentPackage(text: String, start: Int, end: Int): String? {
        var slash = text.indexOf('/', start)
        while (slash >= 0 && slash < end) {
            var packageEnd = slash
            while (packageEnd > start && text[packageEnd - 1].isWhitespace()) {
                packageEnd--
            }
            var packageStart = packageEnd
            while (packageStart > start && isPackageCharacter(text[packageStart - 1])) {
                packageStart--
            }
            if (isPackageName(text, packageStart, packageEnd)) {
                return text.substring(packageStart, packageEnd)
            }
            slash = text.indexOf('/', slash + 1)
        }
        var position = start
        var candidate: String? = null
        while (position < end) {
            while (position < end && !isPackageCharacter(text[position])) position++
            val tokenStart = position
            while (position < end && isPackageCharacter(text[position])) position++
            if (isPackageName(text, tokenStart, position)) {
                candidate = text.substring(tokenStart, position)
            }
        }
        return candidate
    }

    private fun isPackageCharacter(value: Char): Boolean =
        value == '.' || value == '_' || value.isLetterOrDigit()

    private fun isPackageName(text: String, start: Int, end: Int): Boolean {
        if (start >= end || !text[start].isLetter() || text[end - 1] == '.') return false
        var dot = false
        for (index in start until end) {
            val value = text[index]
            if (value == '.') {
                if (index == start || text[index - 1] == '.') return false
                dot = true
            } else if (value != '_' && !value.isLetterOrDigit()) {
                return false
            }
        }
        return dot
    }

    /** Returns focus-related diagnostic lines, limited to [limit]. */
    fun diagnosticLines(dump: String?, limit: Int): String {
        if (dump.isNullOrEmpty()) return "<empty output>"
        val result = StringBuilder()
        var count = 0
        for (raw in dump.split(Regex("\\R"))) {
            val lower = raw.lowercase()
            if (lower.contains("focus") || lower.contains("resum") || lower.contains("topactivity")) {
                result.append(raw.trim()).append('\n')
                if (++count >= limit) break
            }
        }
        return if (count == 0) "<no focus/resumed lines found>" else result.toString().trim()
    }

    /** Counts layer blocks in the dump. */
    fun layerBlockCount(dump: String?): Int {
        if (dump == null) return 0
        var count = 0
        for (line in dump.split(Regex("\\R"))) {
            if (line.trim().startsWith("layerName =")) count++
        }
        return count
    }

    /** Returns layer names, limited to [limit]. */
    fun layerNames(dump: String?, limit: Int): String {
        if (dump.isNullOrEmpty()) return "<empty output>"
        val result = StringBuilder()
        var count = 0
        for (raw in dump.split(Regex("\\R"))) {
            val line = raw.trim()
            if (!line.startsWith("layerName =")) continue
            result.append(line.substring("layerName =".length).trim()).append('\n')
            if (++count >= limit) break
        }
        return if (count == 0) "<no layerName fields found>" else result.toString().trim()
    }

    /** Parses TimeStats layers that match [packageName]. */
    fun layers(dump: String?, packageName: String?): List<LayerStat> {
        val result = mutableListOf<LayerStat>()
        var name: String? = null
        var frames: Long = -1
        var fps: Double = Double.NaN
        var waitingForName = false

        if (dump == null) return result

        for (raw in dump.split(Regex("\\R"))) {
            val line = raw.trim()
            when {
                line.startsWith("layerName =") -> {
                    add(result, packageName, name, frames, fps)
                    name = line.substring("layerName =".length).trim()
                    waitingForName = name.isEmpty()
                    frames = -1
                    fps = Double.NaN
                }
                waitingForName && line.isNotEmpty() -> {
                    name = line
                    waitingForName = false
                }
                name != null && line.startsWith("totalFrames =") -> {
                    frames = line.substringAfter('=').trim().toLongOrNull() ?: frames
                }
                name != null && line.startsWith("averageFPS =") -> {
                    fps = line.substringAfter('=').trim().toDoubleOrNull() ?: fps
                }
            }
        }
        add(result, packageName, name, frames, fps)
        result.sortWith(
            compareByDescending<LayerStat> { it.preferredSurface() }
                .thenByDescending { it.frames }
        )
        return result
    }

    private fun add(
        result: MutableList<LayerStat>,
        packageName: String?,
        name: String?,
        frames: Long,
        fps: Double
    ) {
        if (name != null && packageName != null && name.contains(packageName) &&
            frames >= 2 && !fps.isNaN()
        ) {
            result.add(LayerStat(name, frames, fps))
        }
    }
}
