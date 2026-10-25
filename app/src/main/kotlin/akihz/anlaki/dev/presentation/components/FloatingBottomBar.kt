package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp
    ) {
        Row(modifier = Modifier.padding(6.dp)) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val labelWidth by animateDpAsState(
                    targetValue = if (selected) 80.dp else 0.dp,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "navigation_label_width_$index"
                )
                Surface(
                    selected = selected,
                    onClick = item.onClick,
                    modifier = Modifier.height(52.dp),
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
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
                        // Keep the label composed and measured at full width. Clamp its
                        // reveal width so spring overshoot cannot resize the whole bar.
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            maxLines = 1,
                            modifier = Modifier
                                .width(labelWidth.coerceIn(0.dp, 80.dp))
                                .clipToBounds()
                                .wrapContentWidth(Alignment.Start, unbounded = true)
                                .width(80.dp)
                                .padding(start = 8.dp)
                                .graphicsLayer {
                                    alpha = (labelWidth / 80.dp).coerceIn(0f, 1f)
                                }
                                .clearAndSetSemantics {}
                        )
                    }
                }
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
