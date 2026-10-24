package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Displays a preference row with optional icon, description, and switch.
 *
 * The row morphs its corners while pressed and gives switch rows a real toggle
 * role, so TalkBack announces the state instead of a plain button.
 *
 * @param title primary preference label
 * @param modifier layout modifier supplied by the caller
 * @param description optional supporting text
 * @param icon optional leading icon
 * @param checked switch state, or null when the row is an action
 * @param onCheckedChange invoked when a switch preference changes
 * @param onClick invoked when an action preference is selected
 */
@Composable
fun PreferenceTemplate(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val isSwitch = checked != null && onCheckedChange != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 4.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "preference corner"
    )
    val pressedColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.Transparent
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "preference press"
    )
    val actionModifier = when {
        isSwitch -> Modifier.toggleable(
            value = checked!!,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Switch,
            onValueChange = onCheckedChange!!
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick
        )
        else -> Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(pressedColor)
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = description?.let { text -> { Text(text) } },
            leadingContent = icon?.let { iconValue ->
                {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = iconValue,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            trailingContent = when {
                isSwitch -> {
                    {
                        Switch(
                            checked = checked!!,
                            onCheckedChange = null,
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                }
                onClick != null -> {
                    {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.then(actionModifier)
        )
    }
}
