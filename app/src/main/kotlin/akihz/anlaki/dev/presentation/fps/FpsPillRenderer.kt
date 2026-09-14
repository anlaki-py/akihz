package akihz.anlaki.dev.presentation.fps

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import akihz.anlaki.dev.data.fps.OverlayGeometry
import akihz.anlaki.dev.data.fps.OverlayStyle

/**
 * Renders the FPS pill view from an [FpsPillStyle].
 *
 * Stateless. The controller owns views and style state, this object only
 * applies them. Density comes from the passed context.
 */
internal object FpsPillRenderer {

    /** Applies scale to text size and padding. */
    fun applyScale(context: Context, view: TextView, style: FpsPillStyle) {
        view.textSize = 14f * style.textScaleFactor()
        val (padH, padV) = style.pillPaddingDp()
        val density = context.resources.displayMetrics.density
        view.setPadding(
            Math.round(padH * density),
            Math.round(padV * density),
            Math.round(padH * density),
            Math.round(padV * density)
        )
    }

    /** Applies color, radius, and outline to the pill background. */
    fun applyStyle(context: Context, view: TextView, background: GradientDrawable, style: FpsPillStyle) {
        val density = context.resources.displayMetrics.density
        background.setColor(OverlayStyle.pillColor(style.pillColor, style.alphaPercent))
        background.cornerRadius = Math.round(style.pillRadiusDp() * density).toFloat()
        if (style.pillOutline) {
            background.setStroke(Math.round(1 * density), OverlayStyle.strokeColor(style.pillColor))
        } else {
            background.setStroke(0, OverlayStyle.strokeColor(style.pillColor))
        }
        view.setTextColor(OverlayStyle.textColor(style.pillColor))
    }

    /**
     * Forces the pill background to fit its text.
     *
     * The overlay window can keep a width measured for an earlier, longer
     * text such as "Connecting...", leaving a stretched pill behind short
     * readings like "0.0". Measuring the pill unconstrained and pinning the
     * exact width makes the background track the text on every update.
     *
     * @return true when the width changed and the window needs a re-layout
     */
    fun refitPillToText(view: TextView): Boolean {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val fitted = view.measuredWidth.coerceAtLeast(1)
        val lp = view.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        if (lp.width != fitted) {
            lp.width = fitted
            view.layoutParams = lp
            return true
        }
        return false
    }

    /** Current display refresh rate in Hz, or 0 when unknown. */
    fun displayRefreshRate(context: Context): Double {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = manager?.getDisplay(Display.DEFAULT_DISPLAY)
        return display?.refreshRate?.toDouble() ?: 0.0
    }

    /** Formats an FPS value with or without the unit label. */
    fun formatFps(fps: Double, showUnit: Boolean): String =
        OverlayStyle.formatFps(fps, showUnit)

    /** Clamps a window position inside the screen, mirroring overlay geometry rules. */
    fun clampPosition(pos: Int, size: Int, screen: Int): Int =
        OverlayGeometry.clampPosition(pos, size, screen)
}
