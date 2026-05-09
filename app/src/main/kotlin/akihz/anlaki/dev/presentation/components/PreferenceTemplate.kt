package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Reusable preference row template with title, description, and optional start/end widgets.
 *
 * @param title The main label composable
 * @param description Optional secondary text composable
 * @param startWidget Icon or widget displayed at the start
 * @param endWidget Widget displayed at the end (e.g. switch, arrow)
 * @param onClick Called when the row is clicked
 * @param verticalPadding Internal vertical padding
 * @param modifier Additional modifier
 */
@Composable
fun PreferenceTemplate(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: @Composable (() -> Unit)? = null,
    startWidget: @Composable (() -> Unit)? = null,
    endWidget: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    verticalPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(clickableModifier),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (startWidget != null) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    startWidget()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                title()
                if (description != null) {
                    description()
                }
            }

            if (endWidget != null) {
                endWidget()
            }
        }
    }
}
