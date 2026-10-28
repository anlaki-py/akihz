package akihz.anlaki.dev.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Icon
import kotlin.math.roundToInt

private const val ICON_SIZE_PX = 96
private const val MAX_TEXT_WIDTH_RATIO = 0.82f
private const val INITIAL_TEXT_SIZE_PX = 52f

/**
 * Creates a monochrome Quick Settings icon containing the supplied refresh rate.
 *
 * Android uses the bitmap's alpha channel as the tile icon mask and applies the
 * appropriate color for the tile's current state.
 *
 * @param rate refresh rate to display
 * @param typeface font used for the rate digits
 * @return an icon sized for use by a Quick Settings tile
 */
fun createRefreshRateTileIcon(
    rate: Float,
    typeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
): Icon {
    val text = rate.roundToInt().toString()
    val bitmap = Bitmap.createBitmap(
        ICON_SIZE_PX,
        ICON_SIZE_PX,
        Bitmap.Config.ARGB_8888
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        this.typeface = typeface
        textSize = INITIAL_TEXT_SIZE_PX
    }

    val maximumTextWidth = ICON_SIZE_PX * MAX_TEXT_WIDTH_RATIO
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    if (bounds.width() > maximumTextWidth) {
        paint.textSize *= maximumTextWidth / bounds.width()
        paint.getTextBounds(text, 0, text.length, bounds)
    }

    // Center the visible digits, excluding the font's side bearings and line spacing.
    val x = ICON_SIZE_PX / 2f - bounds.exactCenterX()
    val baseline = ICON_SIZE_PX / 2f - bounds.exactCenterY()
    Canvas(bitmap).drawText(text, x, baseline, paint)
    return Icon.createWithBitmap(bitmap)
}
