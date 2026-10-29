@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.fps.FpsSample
import akihz.anlaki.dev.data.fps.OverlayStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Material counter surface with an optional history graph and pill-only typography. */
@Composable
internal fun FpsPill(
    text: String,
    samples: List<FpsSample>,
    refreshRate: Double,
    style: FpsPillStyle,
    expanded: Boolean,
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val foreground = Color(OverlayStyle.textColor(style.pillColor, colors.onPrimaryContainer.toArgb()))
    val shape = when {
        style.rectangularShape -> RoundedCornerShape(style.pillRadiusDp().dp)
        style.graphEnabled -> MaterialTheme.shapes.large
        else -> CircleShape
    }
    val scale = style.textScaleFactor()
    val (padH, padV) = style.pillPaddingDp()
    val number = text.removeSuffix(" FPS").toDoubleOrNull()?.takeIf { it.isFinite() }
    val value = number?.let { String.format(Locale.US, if (style.decimals) "%.1f" else "%.0f", it) } ?: text
    val typography = MaterialTheme.typography
    val numberStyle = typography.headlineSmallEmphasized.copy(
        fontFamily = if (style.defaultFont) FontFamily.Default else typography.headlineSmallEmphasized.fontFamily,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
        fontSize = ((if (style.compact) 20f else 26f) * scale).sp,
        lineHeight = ((if (style.compact) 24f else 32f) * scale).sp
    )
    val labelStyle = typography.labelMedium.copy(
        fontFamily = if (style.defaultFont) FontFamily.Default else typography.labelMedium.fontFamily,
        fontSize = (11f * scale).sp,
        lineHeight = (16f * scale).sp
    )
    Surface(
        shape = shape,
        color = Color(OverlayStyle.pillColor(style.pillColor, style.alphaPercent, colors.primaryContainer.toArgb())),
        contentColor = foreground,
        shadowElevation = if (style.raised) 6.dp else 0.dp,
        border = if (style.pillOutline) BorderStroke(1.dp, foreground.copy(alpha = 0.18f)) else null,
        modifier = Modifier
            .widthIn(max = (LocalConfiguration.current.screenWidthDp - 12).coerceAtLeast(1).dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = if (expanded) "Options open" else "Options closed"
                onClick(label = if (expanded) "Close FPS options" else "Open FPS options") {
                    onTap()
                    true
                }
            }
            .overlayDrag(LocalViewConfiguration.current.touchSlop, onDrag, onDragEnd, onTap)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy((6f * scale).dp),
            modifier = Modifier.padding(horizontal = padH.dp, vertical = padV.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)) {
                Text(
                    value,
                    style = numberStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline()
                )
                if (number != null && style.showUnit) {
                    Text("FPS", style = labelStyle, color = foreground.copy(alpha = 0.75f), modifier = Modifier.alignByBaseline())
                }
            }
            if (style.graphEnabled) {
                FpsGraph(
                    samples, refreshRate, style, foreground, labelStyle,
                    maxHeightDp = LocalConfiguration.current.screenHeightDp * if (expanded) 0.2f else 0.4f
                )
            }
        }
    }
}
