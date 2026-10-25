package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a large refresh-rate selection button.
 *
 * The button belongs to a grouped list, so it keeps the 4dp inner corners and
 * rounds to 20dp while pressed. The group container supplies the outer corners.
 *
 * @param hz refresh rate represented by this button
 * @param isSelected whether this rate is currently selected
 * @param onClick invoked when the button is selected
 * @param fontSizeSp size of the rate label in sp
 * @param modifier layout modifier supplied by the parent, which also sets the height
 */
@Composable
fun RefreshRateButton(
    hz: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) GroupOuterCornerRadius else GroupInnerCornerRadius,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "refresh rate button shape"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "refresh rate button container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "refresh rate button content"
    )

    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.semantics {
            role = Role.RadioButton
            this.selected = isSelected
        },
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = "${hz.toInt()} Hz",
            style = if (isSelected) {
                MaterialTheme.typography.displaySmallEmphasized
            } else {
                MaterialTheme.typography.displaySmall
            },
            fontSize = fontSizeSp.sp
        )
    }
}
