package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PreferenceSlider(
    title: String,
    value: Float,
    valueLabel: String,
    description: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    increment: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    val newValue = (value - increment).coerceIn(valueRange)
                    onValueChange(newValue)
                    onValueChangeFinished(newValue)
                }
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease $title",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = { onValueChangeFinished(value) },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val newValue = (value + increment).coerceIn(valueRange)
                    onValueChange(newValue)
                    onValueChangeFinished(newValue)
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase $title",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
