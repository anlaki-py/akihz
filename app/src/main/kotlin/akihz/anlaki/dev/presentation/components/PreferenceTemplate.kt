package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Adds a preference row with optional icon, description, and switch.
 *
 * Rows with a switch toggle when tapped anywhere, and TalkBack reports the
 * switch role. Rows with a click action show a trailing chevron.
 *
 * @param title primary preference label
 * @param modifier layout modifier supplied by the caller
 * @param description optional supporting text
 * @param icon optional leading icon
 * @param checked switch state, or null when the row is an action
 * @param onCheckedChange invoked when a switch preference changes
 * @param onClick invoked when an action preference is selected
 */
fun PreferenceGroupScope.PreferenceTemplate(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val switchEnabled = checked != null && onCheckedChange != null

    item(
        modifier = modifier,
        onClick = if (switchEnabled) null else onClick,
        toggleState = if (switchEnabled) checked else null,
        onToggle = if (switchEnabled) onCheckedChange else null,
        headlineContent = { Text(title) },
        supportingContent = description?.let { text -> { Text(text) } },
        leadingContent = icon?.let { iconValue ->
            { PreferenceIcon(iconValue) }
        },
        trailingContent = when {
            checked != null -> {
                {
                    Switch(
                        checked = checked,
                        onCheckedChange = null,
                        enabled = switchEnabled,
                        modifier = if (switchEnabled) {
                            Modifier.clearAndSetSemantics {}
                        } else {
                            Modifier
                        }
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
        }
    )
}

@Composable
private fun PreferenceIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(8.dp)
        )
    }
}
