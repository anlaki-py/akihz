@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.domain.TileRateSelection
import akihz.anlaki.dev.presentation.components.GroupOuterCornerRadius
import akihz.anlaki.dev.presentation.components.RefreshRateButton
import akihz.anlaki.dev.presentation.components.TileInclusionButton
import akihz.anlaki.dev.presentation.modifiers.BlurDirection
import akihz.anlaki.dev.presentation.modifiers.progressiveBlur
import kotlin.math.abs

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
    val listFadeHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    val displayedRates = if (debugSettings.showFakeRefreshRates) {
        HomeDebugSettings.previewRates(supportedRates)
    } else {
        supportedRates
    }
    if (isLoading && displayedRates.isEmpty()) {
        HomeStateContainer(modifier, statusBarHeight) {
            LoadingState()
        }
    } else if (displayedRates.isEmpty()) {
        HomeStateContainer(modifier, statusBarHeight) {
            EmptyState()
        }
    } else {
        val includedCount = TileRateSelection.includedRates(supportedRates, excludedTileRates).size
        val rateRows = displayedRates.map { hz ->
            val isSupported = supportedRates.any { abs(hz - it) < 0.01f }
            RateRow(
                hz = hz,
                isSupported = isSupported,
                isSelected = isSupported && selectedRate?.let { abs(hz - it) < 1f } == true,
                isTileIncluded = TileRateSelection.isIncluded(hz, excludedTileRates)
            )
        }
        val rowSpacing = debugSettings.buttonSpacingDp.dp
        val tileWidth = (debugSettings.buttonHeightDp * 0.5f).coerceIn(56f, 96f).dp
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = statusBarHeight + 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeTitle()
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .progressiveBlur(
                        blurRadius = 0f,
                        height = listFadeHeightPx,
                        direction = BlurDirection.Top
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(debugSettings.buttonWidthPercent / 100f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(GroupOuterCornerRadius)),
                            verticalArrangement = Arrangement.spacedBy(rowSpacing)
                        ) {
                            rateRows.forEach { row ->
                                RefreshRateButton(
                                    hz = row.hz,
                                    isSelected = row.isSelected,
                                    onClick = { if (row.isSupported) onRateSelected(row.hz) },
                                    fontSizeSp = debugSettings.hzTextSizeSp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(debugSettings.buttonHeightDp.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .width(tileWidth)
                                .clip(RoundedCornerShape(GroupOuterCornerRadius)),
                            verticalArrangement = Arrangement.spacedBy(rowSpacing)
                        ) {
                            rateRows.forEach { row ->
                                TileInclusionButton(
                                    checked = row.isTileIncluded,
                                    enabled = row.isSupported &&
                                        (!row.isTileIncluded || includedCount > 1),
                                    contentDescription =
                                        "Include ${row.hz.toInt()} Hz in Quick Settings tile",
                                    onToggle = {
                                        onTileRateIncludedChanged(row.hz, !row.isTileIncluded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(debugSettings.buttonHeightDp.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }
}

/** One refresh-rate row shared by the rate buttons and the tile toggles. */
private data class RateRow(
    val hz: Float,
    val isSupported: Boolean,
    val isSelected: Boolean,
    val isTileIncluded: Boolean
)

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
        style = MaterialTheme.typography.displayMediumEmphasized,
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
        LoadingIndicator()
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
                modifier = Modifier.size(48.dp),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
