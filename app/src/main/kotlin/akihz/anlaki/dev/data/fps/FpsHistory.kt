package akihz.anlaki.dev.data.fps

/** One FPS measurement at a monotonic timestamp. */
data class FpsSample(val timeMs: Long, val fps: Double)

/** Keeps at most one minute of measurements, resetting when the measured layer changes. */
class FpsHistory {
    private val samples = ArrayDeque<FpsSample>()
    private var source: String? = null

    /**
     * Records a valid sample and returns an immutable snapshot for drawing.
     * @param sourceId foreground package and layer identifier
     * @param timeMs monotonic sample time in milliseconds
     * @param fps measured frames per second, finite and non-negative
     * @return retained samples, oldest first
     */
    fun record(sourceId: String, timeMs: Long, fps: Double): List<FpsSample> {
        if (source != sourceId) {
            clear()
            source = sourceId
        }
        if (!fps.isFinite() || fps < 0) return samples.toList()
        val previous = samples.lastOrNull()
        if (previous != null && timeMs <= previous.timeMs) return samples.toList()
        samples.addLast(FpsSample(timeMs, fps))
        while (samples.size > 121 || (samples.firstOrNull()?.timeMs ?: timeMs) < timeMs - 60_000L) {
            samples.removeFirst()
        }
        return samples.toList()
    }

    /** Clears readings after a disconnect, target change, or service restart. */
    fun clear() {
        samples.clear()
        source = null
    }
}
