package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.data.fps.OverlayGeometry
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle

/**
 * Snapshot of every persisted overlay look value.
 *
 * The controller keeps one of these instead of seven loose fields, so style
 * state copies and renders as a unit. Reads and writes prefs on load and
 * on each setter, matching the old behavior.
 */
internal data class FpsPillStyle(
    val scalePercent: Int,
    val alphaPercent: Int,
    val showUnit: Boolean,
    val pillColor: OverlayPillColor,
    val rectangularShape: Boolean,
    val cornerRadius: Int,
    val pillOutline: Boolean
) {
    companion object {
        /** Loads every value from prefs. */
        fun load(): FpsPillStyle = FpsPillStyle(
            scalePercent = PreferencesHelper.fpsOverlayScale,
            alphaPercent = PreferencesHelper.fpsOverlayAlpha,
            showUnit = PreferencesHelper.fpsShowUnit,
            pillColor = PreferencesHelper.fpsPillColor,
            rectangularShape = PreferencesHelper.fpsRectShape,
            cornerRadius = PreferencesHelper.fpsCornerRadius,
            pillOutline = PreferencesHelper.fpsPillOutline
        )
    }

    /** Text size factor derived from the scale percent. */
    fun textScaleFactor(): Float = scalePercent / 100f

    /** Pill padding in dp for the current scale. */
    fun pillPaddingDp(): Pair<Int, Int> = Pair(
        OverlayGeometry.scaleDimension(16, scalePercent),
        OverlayGeometry.scaleDimension(12, scalePercent)
    )

    /** Corner radius in dp for the current shape. */
    fun pillRadiusDp(): Int = OverlayStyle.pillRadiusDp(rectangularShape, cornerRadius)
}
