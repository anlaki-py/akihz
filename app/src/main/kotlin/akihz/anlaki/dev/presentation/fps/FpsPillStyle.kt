package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.data.fps.OverlayGeometry
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle

/** Persisted appearance of the floating FPS counter and optional graph. */
data class FpsPillStyle(
    val scalePercent: Int = 100,
    val alphaPercent: Int = OverlayStyle.ALPHA_DEFAULT,
    val showUnit: Boolean = true,
    val pillColor: OverlayPillColor = OverlayPillColor.Theme,
    val rectangularShape: Boolean = false,
    val cornerRadius: Int = OverlayStyle.RECT_RADIUS_DP,
    val pillOutline: Boolean = true,
    val graphEnabled: Boolean = false,
    val graphSeconds: Int = 30,
    val graphWidth: Int = 180,
    val graphHeight: Int = 40,
    val defaultFont: Boolean = false,
    val compact: Boolean = false,
    val decimals: Boolean = true,
    val raised: Boolean = true
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
            pillOutline = PreferencesHelper.fpsPillOutline,
            graphEnabled = PreferencesHelper.fpsGraphEnabled,
            graphSeconds = PreferencesHelper.fpsGraphSeconds,
            graphWidth = PreferencesHelper.fpsGraphWidth,
            graphHeight = PreferencesHelper.fpsGraphHeight,
            defaultFont = PreferencesHelper.fpsDefaultFont,
            compact = PreferencesHelper.fpsCompact,
            decimals = PreferencesHelper.fpsDecimals,
            raised = PreferencesHelper.fpsRaised
        )
    }

    /** Saves changed fields only, so menu edits do not rewrite unrelated preferences. */
    fun save() {
        if (PreferencesHelper.fpsOverlayScale != scalePercent) PreferencesHelper.fpsOverlayScale = scalePercent
        if (PreferencesHelper.fpsOverlayAlpha != alphaPercent) PreferencesHelper.fpsOverlayAlpha = alphaPercent
        if (PreferencesHelper.fpsShowUnit != showUnit) PreferencesHelper.fpsShowUnit = showUnit
        if (PreferencesHelper.fpsPillColor != pillColor) PreferencesHelper.fpsPillColor = pillColor
        if (PreferencesHelper.fpsRectShape != rectangularShape) PreferencesHelper.fpsRectShape = rectangularShape
        if (PreferencesHelper.fpsCornerRadius != cornerRadius) PreferencesHelper.fpsCornerRadius = cornerRadius
        if (PreferencesHelper.fpsPillOutline != pillOutline) PreferencesHelper.fpsPillOutline = pillOutline
        if (PreferencesHelper.fpsGraphEnabled != graphEnabled) PreferencesHelper.fpsGraphEnabled = graphEnabled
        if (PreferencesHelper.fpsGraphSeconds != graphSeconds) PreferencesHelper.fpsGraphSeconds = graphSeconds
        if (PreferencesHelper.fpsGraphWidth != graphWidth) PreferencesHelper.fpsGraphWidth = graphWidth
        if (PreferencesHelper.fpsGraphHeight != graphHeight) PreferencesHelper.fpsGraphHeight = graphHeight
        if (PreferencesHelper.fpsDefaultFont != defaultFont) PreferencesHelper.fpsDefaultFont = defaultFont
        if (PreferencesHelper.fpsCompact != compact) PreferencesHelper.fpsCompact = compact
        if (PreferencesHelper.fpsDecimals != decimals) PreferencesHelper.fpsDecimals = decimals
        if (PreferencesHelper.fpsRaised != raised) PreferencesHelper.fpsRaised = raised
    }

    /** Text size factor derived from the scale percent. */
    fun textScaleFactor(): Float = scalePercent / 100f

    /** Pill padding in dp for the current scale. */
    fun pillPaddingDp(): Pair<Int, Int> = Pair(
        OverlayGeometry.scaleDimension(if (compact) 12 else 16, scalePercent),
        OverlayGeometry.scaleDimension(if (compact) 6 else 10, scalePercent)
    )

    /** Corner radius in dp for the current shape. */
    fun pillRadiusDp(): Int = OverlayStyle.pillRadiusDp(rectangularShape, cornerRadius)
}
