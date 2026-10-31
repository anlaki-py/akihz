package akihz.anlaki.dev.presentation.components

import android.os.Build
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import akihz.anlaki.dev.presentation.glass.GlassBackdrop
import akihz.anlaki.dev.presentation.glass.GlassSurface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val ItemWidth = 52.dp
private val LabelWidth = 80.dp
private val ItemSpacing = 8.dp

internal data class FloatingNavigationItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
/** Shows the floating bottom navigation bar. */
internal fun FloatingBottomBar(
    items: List<FloatingNavigationItem>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    glassSettings: NavbarGlassSettings = NavbarGlassSettings(),
    backdrop: GlassBackdrop? = null
) {
    val glassEnabled = glassSettings.enabled && backdrop != null && Build.VERSION.SDK_INT >= 33
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val selectionPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "navigation_selection"
    )
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = RoundedCornerShape(36.dp),
        color = if (glassEnabled) Color.Transparent else MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = if (glassEnabled) 0.dp else 8.dp
    ) {
        Box {
            if (glassEnabled && Build.VERSION.SDK_INT >= 33) {
                GlassSurface(backdrop, glassSettings.outer, MaterialTheme.colorScheme.primary,
                    Modifier.matchParentSize())
            }
            Box(Modifier.padding(6.dp)) {
                // Animate one pill across the bar. Its overshoot never changes touch bounds.
                Box(
                    Modifier
                        .size(width = ItemWidth + LabelWidth, height = 52.dp)
                        .graphicsLayer {
                            translationX = (ItemWidth + ItemSpacing).toPx() * selectionPosition * direction
                        }
                        .testTag("navigation_indicator")
                        .then(if (glassEnabled) Modifier else Modifier.background(MaterialTheme.colorScheme.surface, CircleShape))
                ) {
                    if (glassEnabled && Build.VERSION.SDK_INT >= 33) {
                        GlassSurface(backdrop, glassSettings.pill, MaterialTheme.colorScheme.surface,
                            Modifier.fillMaxSize(), selectionPosition)
                    }
                }
                Row {
                    items.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        val boundedPosition = selectionPosition.coerceIn(0f, items.lastIndex.toFloat())
                        val labelProgress = (1f - abs(boundedPosition - index)).coerceIn(0f, 1f)
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onPrimary,
                            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                            label = "navigation_color_$index"
                        )
                        Surface(
                            selected = selected,
                            onClick = item.onClick,
                            modifier = Modifier.height(52.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            contentColor = contentColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .width(LabelWidth * labelProgress)
                                        .clipToBounds()
                                        .wrapContentWidth(Alignment.Start, unbounded = true)
                                        .width(LabelWidth)
                                        .padding(start = 8.dp)
                                        .graphicsLayer {
                                            alpha = labelProgress
                                        }
                                        .clearAndSetSemantics {}
                                )
                            }
                        }
                        if (index < items.lastIndex) {
                            Spacer(modifier = Modifier.width(ItemSpacing))
                        }
                    }
                }
            }
        }
    }
}
