package akihz.anlaki.dev.presentation.components

import android.os.Build
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import akihz.anlaki.dev.presentation.glass.GlassBackdrop
import akihz.anlaki.dev.presentation.glass.GlassSurface
import akihz.anlaki.dev.presentation.glass.rememberGlassBackdrop
import akihz.anlaki.dev.presentation.glass.recordGlassBackdrop
import akihz.anlaki.dev.presentation.glass.refractedNavigationContent
import akihz.anlaki.dev.presentation.theme.AkihzColors
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal val NavigationItemWidth = 52.dp
internal val NavigationLabelWidth = 80.dp
internal val NavigationItemSpacing = 8.dp
internal val NavigationPadding = 6.dp
internal val NavigationHeight = 52.dp

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
    val navigationBackground = if (glassEnabled) rememberGlassBackdrop() else null
    val pageColor = AkihzColors.pageContainer
    val outerColor = if (glassSettings.monochrome) Color(0xFFD0D0D0) else MaterialTheme.colorScheme.primary
    val pillColor = if (glassSettings.monochrome) Color(0xFF303030) else MaterialTheme.colorScheme.surface
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val selectionPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "navigation_selection"
    )
    val boundedPosition = selectionPosition.coerceIn(0f, items.lastIndex.toFloat())
    val labelProgresses = items.indices.map { (1f - abs(boundedPosition - it)).coerceIn(0f, 1f) }
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = RoundedCornerShape(36.dp),
        color = if (glassEnabled) Color.Transparent else outerColor,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = if (glassEnabled) 0.dp else 8.dp
    ) {
        Box {
            Box(Modifier.matchParentSize().then(
                if (navigationBackground != null) Modifier.recordGlassBackdrop(navigationBackground, pageColor)
                else Modifier
            )) {
                if (glassEnabled && Build.VERSION.SDK_INT >= 33) {
                    GlassSurface(backdrop, glassSettings.outer, MaterialTheme.colorScheme.primary,
                        Modifier.matchParentSize(), monochrome = glassSettings.monochrome)
                }
                Box(Modifier.fillMaxSize().padding(NavigationPadding)) {
                    // The moving lens changes only drawing. Hit targets keep their layout bounds.
                    Box(
                        Modifier
                            .size(width = NavigationItemWidth + NavigationLabelWidth, height = NavigationHeight)
                            .graphicsLayer {
                                translationX = (NavigationItemWidth + NavigationItemSpacing).toPx() * selectionPosition * direction
                            }
                            .testTag("navigation_indicator")
                            .then(if (glassEnabled) Modifier else Modifier.background(pillColor, CircleShape))
                    ) {
                        if (glassEnabled && Build.VERSION.SDK_INT >= 33) {
                            GlassSurface(backdrop, glassSettings.pill, MaterialTheme.colorScheme.surface,
                                Modifier.fillMaxSize(), selectionPosition, glassSettings.monochrome)
                        }
                    }
                }
            }
            val contentLens = if (navigationBackground != null && Build.VERSION.SDK_INT >= 33) {
                Modifier.refractedNavigationContent(navigationBackground, glassSettings,
                    selectionPosition, labelProgresses)
            } else Modifier
            Box(contentLens.padding(NavigationPadding)) {
                Row {
                    items.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        val labelProgress = labelProgresses[index]
                        // White becomes an alpha mask for the lens. The fallback also uses
                        // neutral ink, chosen against its actual solid surface color.
                        val baseColor = if (selected) pillColor else outerColor
                        val contentColor = if (glassEnabled) Color.White
                            else if (baseColor.luminance() > 0.179f) Color.Black else Color.White
                        Surface(
                            selected = selected,
                            onClick = item.onClick,
                            modifier = Modifier.height(NavigationHeight),
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
                                        .width(NavigationLabelWidth * labelProgress)
                                        .clipToBounds()
                                        .wrapContentWidth(Alignment.Start, unbounded = true)
                                        .width(NavigationLabelWidth)
                                        .padding(start = 8.dp)
                                        .graphicsLayer {
                                            alpha = labelProgress
                                        }
                                        .clearAndSetSemantics {}
                                )
                            }
                        }
                        if (index < items.lastIndex) {
                            Spacer(modifier = Modifier.width(NavigationItemSpacing))
                        }
                    }
                }
            }
        }
    }
}
