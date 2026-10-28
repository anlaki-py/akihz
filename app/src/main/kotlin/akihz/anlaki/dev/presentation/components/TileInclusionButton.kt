package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

/**
 * Large toggle button that controls whether a refresh rate joins the Quick
 * Settings tile cycle.
 *
 * The button belongs to a grouped list, so it keeps the 4dp inner corners and
 * rounds to 20dp while pressed. The group container supplies the outer corners.
 *
 * @param checked whether the rate is included in the tile cycle
 * @param enabled whether tapping changes the choice; holding always opens help
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
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) HomeGroupOuterCornerRadius else GroupInnerCornerRadius,
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
            .combinedClickable(
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (enabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
                },
                onLongClick = { showHelp = true },
                onLongClickLabel = "Explain Quick Settings tile choices"
            )
            .semantics {
                this.contentDescription = contentDescription
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (!enabled) stateDescription = "${if (checked) "On" else "Off"}. Cannot change this rate. Hold for help."
            },
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
                modifier = Modifier.size(28.dp)
            )
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Quick Settings tile") },
            text = {
                Text(
                    "These buttons choose which refresh rates the akiHz tile cycles through. " +
                        "Turn a rate on to include it or off to skip it. At least one rate must stay on.\n\n" +
                        "Add akiHz from your phone's Quick Settings editor. Each tap on the tile " +
                        "switches to the next included rate.\n\n" +
                        "To change your refresh rate now, tap the large rate button."
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } }
        )
    }
}
