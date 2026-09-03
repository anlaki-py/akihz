package akihz.anlaki.dev.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon

private const val ICON_SIZE_PX = 96
private const val MAX_TEXT_WIDTH_RATIO = 0.82f
private const val INITIAL_TEXT_SIZE_PX = 44f

/**
 * Creates a monochrome Quick Settings icon containing "FPS".
 *
 * Android uses the bitmap's alpha channel as the tile icon mask.
 */
fun createFpsTileIcon(): Icon {
    val text = "FPS"
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

/**
 * Creates an icon for the FPS tile when monitoring is off.
 *
 * Uses the same "FPS" glyph but at lower weight to hint inactive state.
 */
fun createFpsTileIconInactive(): Icon {
    val text = "FPS"
    val bitmap = Bitmap.createBitmap(
        ICON_SIZE_PX,
        ICON_SIZE_PX,
        Bitmap.Config.ARGB_8888
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
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
