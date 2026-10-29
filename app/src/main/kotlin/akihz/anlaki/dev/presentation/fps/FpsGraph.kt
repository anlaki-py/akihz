package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.fps.FpsSample
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** Draws timestamped FPS samples; pauses leave gaps instead of invented measurements. */
@Composable
internal fun FpsGraph(
    samples: List<FpsSample>,
    refreshRate: Double,
    style: FpsPillStyle,
    color: Color,
    labelStyle: TextStyle,
    maxHeightDp: Float
) {
    val end = samples.lastOrNull()?.timeMs ?: 0L
    val duration = style.graphSeconds * 1000L
    val visible = samples.filter { it.timeMs >= end - duration }
    val ceiling = maxOf(1.0, refreshRate, visible.maxOfOrNull { it.fps } ?: 0.0)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width((style.graphWidth * style.textScaleFactor()).dp)
            .height(minOf(style.graphHeight * style.textScaleFactor(), maxHeightDp).dp)
            .semantics { contentDescription = "FPS history, last ${style.graphSeconds} seconds" }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val inset = 3.dp.toPx()
            val chartWidth = (size.width - 2 * inset).coerceAtLeast(0f)
            val chartHeight = (size.height - 2 * inset).coerceAtLeast(0f)
            fun point(sample: FpsSample) = Offset(
                inset + ((sample.timeMs - end + duration).toFloat() / duration).coerceIn(0f, 1f) * chartWidth,
                inset + (1f - (sample.fps / ceiling).toFloat().coerceIn(0f, 1f)) * chartHeight
            )
            for (fraction in listOf(0f, 0.5f, 1f)) {
                val y = inset + fraction * chartHeight
                drawLine(color.copy(alpha = 0.1f), Offset(inset, y), Offset(inset + chartWidth, y), 1.dp.toPx())
            }
            // A long pause or drag must not look like a measured straight line.
            val segments = mutableListOf<MutableList<FpsSample>>()
            visible.forEach { sample ->
                val segment = segments.lastOrNull()
                if (segment == null || sample.timeMs - segment.last().timeMs > 1500L) {
                    segments.add(mutableListOf(sample))
                } else segment.add(sample)
            }
            segments.forEach { segment ->
                val first = point(segment.first())
                val last = point(segment.last())
                val line = Path().apply {
                    moveTo(first.x, first.y)
                    segment.drop(1).forEach { sample -> point(sample).let { lineTo(it.x, it.y) } }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(last.x, inset + chartHeight)
                    lineTo(first.x, inset + chartHeight)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)))
                drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            visible.lastOrNull()?.let { drawCircle(color, 2.5.dp.toPx(), point(it)) }
        }
        if (visible.isEmpty()) {
            Text("Waiting for samples", style = labelStyle, color = color.copy(alpha = 0.65f))
        }
    }
}
