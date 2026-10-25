package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Large toggle button that controls whether a refresh rate joins the Quick
 * Settings tile cycle.
 *
 * The button belongs to a grouped list, so it keeps the 4dp inner corners and
 * rounds to 20dp while pressed. The group container supplies the outer corners.
 *
 * @param checked whether the rate is included in the tile cycle
 * @param enabled whether the toggle accepts input
 * @param contentDescription TalkBack label naming the rate
 * @param onToggle invoked when the button is tapped
 * @param modifier layout modifier supplied by the parent, which also sets the size
 */
@Composable
fun TileInclusionButton(
    checked: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) GroupOuterCornerRadius else GroupInnerCornerRadius,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "tile toggle shape"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "tile toggle container"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            checked -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "tile toggle content"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
            )
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tile",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
