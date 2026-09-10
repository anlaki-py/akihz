package akihz.anlaki.dev.data.fps

import java.util.Locale

/**
 * Pill color options for the floating FPS overlay.
 */
enum class OverlayPillColor {
    Black,
    Green,
    Red,
    White;

    companion object {
        /**
         * Parses a stored color name.
         *
         * @param value raw stored name
         * @return matching color, or [Black] when unknown
         */
        fun fromStoredValue(value: String?): OverlayPillColor =
            entries.firstOrNull { it.name == value } ?: Black
    }
}

/**
 * Pure styling helpers for the floating FPS overlay.
 *
 * Everything here is framework-free so unit tests cover it.
 * Opacity applies to the pill fill only. Text always stays fully opaque.
 */
object OverlayStyle {
    const val ALPHA_MIN = 40
    const val ALPHA_MAX = 100
    const val ALPHA_DEFAULT = 80

    const val PILL_RADIUS_DP = 999
    const val RECT_RADIUS_DP = 10
    const val RECT_RADIUS_MIN = 0
    const val RECT_RADIUS_MAX = 28

    private const val FILL_BLACK = 0xFF000000.toInt()
    private const val FILL_GREEN = 0xFF15803D.toInt()
    private const val FILL_RED = 0xFFB91C1C.toInt()
    private const val FILL_WHITE = 0xFFFFFFFF.toInt()

    private const val TEXT_ON_DARK = 0xFFFFFFFF.toInt()
    private const val TEXT_ON_LIGHT = 0xFF000000.toInt()

    private const val STROKE_ALPHA = 0x99

    /**
     * Converts an opacity percent to an alpha byte.
     *
     * @param percent opacity from [ALPHA_MIN] to [ALPHA_MAX]
     * @return alpha byte from 0 to 255
     */
    fun alphaByte(percent: Int): Int {
        val clamped = percent.coerceIn(ALPHA_MIN, ALPHA_MAX)
        return Math.round(255 * clamped / 100f)
    }

    /**
     * Builds the pill fill color with opacity applied.
     *
     * @param color selected pill color
     * @param percent opacity from [ALPHA_MIN] to [ALPHA_MAX]
     * @return ARGB fill color
     */
    fun pillColor(color: OverlayPillColor, percent: Int): Int {
        val base = fillBase(color)
        return (alphaByte(percent) shl 24) or (base and 0x00FFFFFF)
    }

    /**
     * Returns the pill text color, always fully opaque.
     *
     * @param color selected pill color
     * @return ARGB text color
     */
    fun textColor(color: OverlayPillColor): Int =
        if (color == OverlayPillColor.White) TEXT_ON_LIGHT else TEXT_ON_DARK

    /**
     * Builds the pill edge color from the text color at fixed 60 percent alpha.
     *
     * The edge ignores the opacity setting so it stays visible at low opacity.
     *
     * @param color selected pill color
     * @return ARGB edge color
     */
    fun strokeColor(color: OverlayPillColor): Int =
        (STROKE_ALPHA shl 24) or (textColor(color) and 0x00FFFFFF)

    /**
     * Clamps a rectangle corner radius to the supported range.
     *
     * @param value requested radius in dp
     * @return radius from [RECT_RADIUS_MIN] to [RECT_RADIUS_MAX]
     */
    fun rectRadiusDp(value: Int): Int =
        value.coerceIn(RECT_RADIUS_MIN, RECT_RADIUS_MAX)

    /**
     * Returns the pill corner radius.
     *
     * @param rectangular true for rectangle, false for pill
     * @param rectRadius corner radius in dp used when rectangular
     * @return corner radius in dp
     */
    fun pillRadiusDp(rectangular: Boolean, rectRadius: Int = RECT_RADIUS_DP): Int =
        if (rectangular) rectRadiusDp(rectRadius) else PILL_RADIUS_DP

    /**
     * Formats the pill text for a reading.
     *
     * @param fps frames per second to show
     * @param showUnit whether to append the FPS label
     * @return text for the pill, such as "60.0 FPS" or "60.0"
     */
    fun formatFps(fps: Double, showUnit: Boolean): String =
        if (showUnit) {
            String.format(Locale.US, "%.1f FPS", fps)
        } else {
            String.format(Locale.US, "%.1f", fps)
        }

    private fun fillBase(color: OverlayPillColor): Int =
        when (color) {
            OverlayPillColor.Black -> FILL_BLACK
            OverlayPillColor.Green -> FILL_GREEN
            OverlayPillColor.Red -> FILL_RED
            OverlayPillColor.White -> FILL_WHITE
        }
}
