package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.RefreshRateButton
import akihz.anlaki.dev.utils.Constants

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshRateScreen(
    currentRate: Float?,
    selectedRate: Float?,
    onRateSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (currentRate != null) "Current: ${currentRate.toInt()} Hz" else "RefreshRate Manager",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Constants.REFRESH_RATES.forEach { hz ->
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
