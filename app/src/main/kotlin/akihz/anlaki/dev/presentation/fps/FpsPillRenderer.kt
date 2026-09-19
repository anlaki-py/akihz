package akihz.anlaki.dev.presentation.fps

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import akihz.anlaki.dev.data.fps.OverlayGeometry
import akihz.anlaki.dev.data.fps.OverlayStyle

/**
 * Pure overlay helpers shared by the controller and window.
 *
 * Compose sizes the pill from its text, so the old measure and pin
 * workaround is gone. Only rate lookup, text format, and clamp remain.
 */
internal object FpsPillRenderer {

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
