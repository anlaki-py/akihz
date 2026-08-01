package akihz.anlaki.dev.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import kotlin.math.roundToInt

private const val ICON_SIZE_PX = 96
private const val MAX_TEXT_WIDTH_RATIO = 0.82f
private const val INITIAL_TEXT_SIZE_PX = 52f

/** Reuses tile icons that render the same rounded refresh-rate label. */
internal class RefreshRateTileIconCache<T>(private val create: (Int) -> T) {
    private val icons = mutableMapOf<Int, T>()

    fun get(rate: Float): T {
        val label = rate.roundToInt()
        return icons.getOrPut(label) { create(label) }
    }

    fun clear() = icons.clear()
}

/**
 * Creates a monochrome Quick Settings icon containing the supplied refresh rate.
 *
 * Android uses the bitmap's alpha channel as the tile icon mask and applies the
 * appropriate color for the tile's current state.
 *
 * @param rate refresh rate to display
 * @return an icon sized for use by a Quick Settings tile
 */
fun createRefreshRateTileIcon(rate: Int): Icon {
    val text = rate.toString()
    val bitmap = Bitmap.createBitmap(
        ICON_SIZE_PX,
        ICON_SIZE_PX,
        Bitmap.Config.ARGB_8888
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = INITIAL_TEXT_SIZE_PX
    }

    val maximumTextWidth = ICON_SIZE_PX * MAX_TEXT_WIDTH_RATIO
    val measuredWidth = paint.measureText(text)
    if (measuredWidth > maximumTextWidth) {
        paint.textSize *= maximumTextWidth / measuredWidth
    }

    val fontMetrics = paint.fontMetrics
    val baseline = ICON_SIZE_PX / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
    Canvas(bitmap).drawText(text, ICON_SIZE_PX / 2f, baseline, paint)
    return Icon.createWithBitmap(bitmap)
}
