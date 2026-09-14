package akihz.anlaki.dev.presentation.fps

import android.content.Context
import android.graphics.PixelFormat
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import timber.log.Timber

/**
 * Hosts the overlay root view in the system window.
 *
 * Owns window params, add and remove, drag position, and the keep on
 * screen clamp. Content is a ComposeView, so this file holds no UI code.
 */
internal class FpsOverlayWindow(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    var root: View? = null
        private set
    private var params: WindowManager.LayoutParams? = null
    private var moveFrameScheduled = false

    /** Adds [view] to the window at the default position. False when add failed. */
    fun show(view: View): Boolean {
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

    /** Current window position in pixels, null when not shown. */
    fun position(): Pair<Int, Int>? = params?.let { it.x to it.y }

    /**
     * Moves the window to [x] and [y] in pixels.
     *
     * Touch events arrive faster than the screen refreshes, and every
     * layout call crosses into the window manager. Params update at once,
     * but the layout call runs at most once per frame with the latest
     * position. That keeps drags light.
     */
    fun moveTo(x: Int, y: Int) {
        val p = params ?: return
        if (root == null) return
        p.x = x
        p.y = y
        if (moveFrameScheduled) return
        moveFrameScheduled = true
        Choreographer.getInstance().postFrameCallback {
            moveFrameScheduled = false
            val view = root ?: return@postFrameCallback
            val current = params ?: return@postFrameCallback
            try {
                windowManager.updateViewLayout(view, current)
            } catch (_: Exception) {}
        }
    }

    /** Re-runs window layout so a size change actually reaches the screen. */
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

    /**
     * Converts dp to pixels for the current screen.
     * @param value size in dp
     */
    fun dp(value: Int): Int =
        Math.round(value * appContext.resources.displayMetrics.density)
}
