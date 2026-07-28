package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Displays a large refresh-rate selection button.
 *
 * @param hz refresh rate represented by this button
 * @param isSelected whether this rate is currently selected
 * @param onClick invoked when the button is selected
 * @param modifier layout modifier supplied by the parent
 */
@Composable
fun RefreshRateButton(
    hz: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val selectRate = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }

    if (isSelected) {
        Button(
            onClick = selectRate,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "${hz.toInt()} Hz",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        FilledTonalButton(
            onClick = selectRate,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        ) {
            Text(
                text = "${hz.toInt()} Hz",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
