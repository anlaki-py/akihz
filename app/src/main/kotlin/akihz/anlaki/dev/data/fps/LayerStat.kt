package akihz.anlaki.dev.data.fps

/**
 * Parsed statistics for one SurfaceFlinger layer.
 *
 * @param name raw layer name from TimeStats dump
 * @param stableName layer name with instance counters normalized
 * @param frames total presented frames
 * @param fps averageFPS value reported by SurfaceFlinger
 */
data class LayerStat(
    val name: String,
    val stableName: String,
    val frames: Long,
    val fps: Double
) {
    constructor(name: String, frames: Long, fps: Double) : this(
        name = name,
        stableName = name.replace(Regex("#\\d+"), "#").trim(),
        frames = frames,
        fps = fps
    )

    /** Whether this layer is a preferred rendering surface. */
    fun preferredSurface(): Boolean =
        name.contains("SurfaceView[") || name.contains("(BLAST)")

    /** Short display name without instance indices. */
    fun shortName(): String {
        var value = name.replace(Regex("#\\d+"), "").trim()
        if (value.length > 58) value = value.substring(0, 55) + "…"
        return value
    }

    override fun equals(other: Any?): Boolean =
        other is LayerStat && stableName == other.stableName

    override fun hashCode(): Int = stableName.hashCode()
}
