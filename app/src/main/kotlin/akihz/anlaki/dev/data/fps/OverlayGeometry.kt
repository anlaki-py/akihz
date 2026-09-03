package akihz.anlaki.dev.data.fps

/** Pure geometry helpers for the floating FPS overlay. */
object OverlayGeometry {

    /** Clamps preferred panel size to available screen space. */
    fun fitPanelSize(preferredSize: Int, screenSize: Int, reservedSpace: Int): Int =
        maxOf(1, minOf(preferredSize, maxOf(1, screenSize - reservedSpace)))

    /** Keeps the overlay fully visible on screen. */
    fun clampPosition(position: Int, overlaySize: Int, screenSize: Int): Int =
        maxOf(0, minOf(position, maxOf(0, screenSize - overlaySize)))

    /** Scales a base dimension by a percentage. */
    fun scaleDimension(baseSize: Int, percent: Int): Int =
        maxOf(1, Math.round(baseSize * percent / 100f))
}
