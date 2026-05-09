package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.RefreshRateButton
import akihz.anlaki.dev.utils.PreferencesHelper

/**
 * Shows detected refresh rates and lets the user select one.
 * Displays lock mode indicator if enabled.
 *
 * @param supportedRates refresh rates supported by the current display
 * @param currentRate currently active refresh rate
 * @param selectedRate refresh rate selected in the UI
 * @param onRateSelected called when the user picks a refresh rate
 * @param modifier layout modifier supplied by the parent screen
 */
@Composable
fun RefreshRateScreen(
    supportedRates: List<Float>,
    currentRate: Float?,
    selectedRate: Float?,
    onRateSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title
        Text(
            text = "akihz",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Rate selection buttons
        if (supportedRates.isEmpty()) {
            EmptyState()
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                supportedRates.forEach { hz ->
                    RefreshRateButton(
                        hz = hz,
                        isSelected = selectedRate != null && kotlin.math.abs(hz - selectedRate) < 1f,
                        onClick = { onRateSelected(hz) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.height(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "No supported refresh rates detected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Make sure Shizuku is running and granted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}
