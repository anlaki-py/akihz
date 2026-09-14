package akihz.anlaki.dev.presentation.fps

import android.view.MotionEvent
import android.view.View

/**
 * Drag to move, tap to click behavior for the overlay pill.
 *
 * Stateless apart from the gesture start point. Position reads and writes
 * go through lambdas so window params stay with the controller.
 */
internal class OverlayDragListener(
    private val touchSlop: Int,
    private val getPosition: () -> Pair<Int, Int>?,
    private val setPosition: (Int, Int) -> Unit,
    private val onPositionChanged: () -> Unit,
    private val onDragEnd: () -> Unit
) : View.OnTouchListener {

    private var startX = 0
    private var startY = 0
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    /** Moves the overlay on drag and clicks the pill on tap. */
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val (x, y) = getPosition() ?: return false
                startX = x
                startY = y
                downX = event.rawX
                downY = event.rawY
                moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (getPosition() == null) return false
                val deltaX = event.rawX - downX
                val deltaY = event.rawY - downY
                if (kotlin.math.hypot(deltaX.toDouble(), deltaY.toDouble()) > touchSlop) moved = true
                setPosition(
                    startX + Math.round(event.rawX - downX),
                    startY + Math.round(event.rawY - downY)
                )
                onPositionChanged()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (moved) {
                    onDragEnd()
                } else {
                    view.performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return false
    }
}
