package akihz.anlaki.dev.utils

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import timber.log.Timber

/**
 * Hosts the overlay root view in the system window.
 *
 * Owns window params, add and remove, drag listener wiring, and the
 * keep on screen clamp. Pill content and options stay with the controller
 * and collaborators.
 */
internal class FpsOverlayWindow(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    var root: LinearLayout? = null
        private set
    private var params: WindowManager.LayoutParams? = null

    /** Adds [view] to the window at the default position. False when add failed. */
    fun show(view: LinearLayout): Boolean {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(80)
        }
        root = view
        return try {
            windowManager.addView(view, params)
            Timber.i("FPS overlay added at ${params?.x},${params?.y}")
            view.post { keepOnScreen() }
            true
        } catch (e: Exception) {
            Timber.w(e, "Failed to add FPS overlay")
            root = null
            params = null
            false
        }
    }

    /** Removes the root view, if present. */
    fun hide() {
        val view = root ?: return
        Timber.i("Detaching FPS overlay")
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {}
        root = null
        params = null
    }

    /** Re-runs window layout so a shrink actually reaches the screen. */
    fun refit() {
        val view = root ?: return
        val p = params ?: return
        if (!view.isAttachedToWindow) return
        try {
            windowManager.updateViewLayout(view, p)
        } catch (_: Exception) {}
    }

    /** Clamps the root inside the screen. */
    fun keepOnScreen() {
        val view = root ?: return
        val p = params ?: return
        if (!view.isAttachedToWindow) return
        val screenWidth = appContext.resources.displayMetrics.widthPixels
        val screenHeight = appContext.resources.displayMetrics.heightPixels
        val w = if (view.width > 0) view.width else view.measuredWidth
        val h = if (view.height > 0) view.height else view.measuredHeight
        if (w == 0 || h == 0) {
            view.post { keepOnScreen() }
            return
        }
        val clampedX = FpsPillRenderer.clampPosition(p.x, w, screenWidth)
        val clampedY = FpsPillRenderer.clampPosition(p.y, h, screenHeight)
        if (clampedX != p.x || clampedY != p.y) {
            p.x = clampedX
            p.y = clampedY
            try {
                windowManager.updateViewLayout(view, p)
            } catch (_: Exception) {}
        }
    }

    /** Drag listener bound to the current params. */
    fun dragListener(onDragEnd: () -> Unit): OverlayDragListener =
        OverlayDragListener(
            touchSlop = touchSlop,
            getPosition = { params?.let { it.x to it.y } },
            setPosition = { x, y ->
                params?.let {
                    it.x = x
                    it.y = y
                }
            },
            onPositionChanged = {
                try {
                    root?.let { windowManager.updateViewLayout(it, params) }
                } catch (_: Exception) {}
            },
            onDragEnd = onDragEnd
        )

    fun dp(value: Int): Int =
        Math.round(value * appContext.resources.displayMetrics.density)
}
