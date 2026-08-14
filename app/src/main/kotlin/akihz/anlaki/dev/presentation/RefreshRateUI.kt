package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.domain.TileRateSelection
import akihz.anlaki.dev.presentation.components.RefreshRateButton

/**
 * Shows detected refresh rates and lets the user select one.
 *
 * @param supportedRates refresh rates supported by the current display
 * @param currentRate currently active refresh rate
 * @param selectedRate refresh rate selected in the UI
 * @param excludedTileRates rates excluded from Quick Settings tile cycling
 * @param isLoading whether Shizuku or refresh-rate data is still initializing
 * @param onRateSelected called when the user picks a refresh rate
 * @param onTileRateIncludedChanged called when a rate's tile inclusion changes
 * @param debugSettings adjustable home-screen presentation values
 * @param modifier layout modifier supplied by the parent screen
 */
@Composable
fun RefreshRateScreen(
    supportedRates: List<Float>,
    currentRate: Float?,
    selectedRate: Float?,
    excludedTileRates: Set<Float>,
    isLoading: Boolean,
    onRateSelected: (Float) -> Unit,
    onTileRateIncludedChanged: (Float, Boolean) -> Unit,
    debugSettings: HomeDebugSettings,
    modifier: Modifier = Modifier
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    if (isLoading && supportedRates.isEmpty()) {
        HomeStateContainer(modifier, statusBarHeight) {
            LoadingState()
        }
    } else if (supportedRates.isEmpty()) {
        HomeStateContainer(modifier, statusBarHeight) {
            EmptyState()
        }
    } else {
        val includedCount = TileRateSelection.includedRates(supportedRates, excludedTileRates).size
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = statusBarHeight + 16.dp,
                end = 16.dp,
                bottom = 150.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(debugSettings.buttonSpacingDp.dp)
        ) {
            item(key = "home-title") {
                HomeTitle()
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(supportedRates, key = { it }) { hz ->
                val isTileIncluded = TileRateSelection.isIncluded(hz, excludedTileRates)
                Row(
                    modifier = Modifier.fillMaxWidth(debugSettings.buttonWidthPercent / 100f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RefreshRateButton(
                        hz = hz,
                        isSelected = selectedRate?.let { kotlin.math.abs(hz - it) < 1f } == true,
                        onClick = { onRateSelected(hz) },
                        debugSettings = debugSettings,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tile",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = isTileIncluded,
                            enabled = !isTileIncluded || includedCount > 1,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "Include ${hz.toInt()} Hz in Quick Settings tile"
                            },
                            onCheckedChange = { included ->
                                onTileRateIncludedChanged(hz, included)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStateContainer(
    modifier: Modifier,
    statusBarHeight: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = statusBarHeight, bottom = 134.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeTitle()
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun HomeTitle() {
    Text(
        text = "akiHz",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
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
