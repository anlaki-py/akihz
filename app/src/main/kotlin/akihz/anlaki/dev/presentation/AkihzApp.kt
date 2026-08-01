package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import akihz.anlaki.dev.presentation.components.FloatingBottomBar
import akihz.anlaki.dev.presentation.components.FloatingNavigationItem
import akihz.anlaki.dev.presentation.modifiers.progressiveBlur
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class AppPage(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Default.Home),
    Settings("Settings", Icons.Default.Settings)
}

/**
 * Hosts the refresh-rate and settings pages with floating tab navigation.
 *
 * @param uiState current refresh-rate screen state
 * @param onRateSelected invoked when the user selects a refresh rate
 * @param onResetToDefaults restores the system refresh-rate defaults
 * @param onCustomProfileChanged refreshes rates after custom profile activation changes
 * @param themeMode currently selected appearance mode
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param blurEnabled whether progressive edge blur is enabled
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 * @param onBlurEnabledChanged invoked when progressive blur changes
 * @param onErrorDismissed clears an error after it is shown
 */
@Composable
fun AkihzApp(
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onResetToDefaults: () -> Unit,
    onCustomProfileChanged: () -> Unit,
    themeMode: AppThemeMode,
    amoledMode: Boolean,
    blurEnabled: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    onBlurEnabledChanged: (Boolean) -> Unit,
    onErrorDismissed: () -> Unit = {}
) {
    var showCustomKeys by remember { mutableStateOf(false) }
    if (showCustomKeys) {
        CustomKeysScreen(
            onBack = { showCustomKeys = false },
            onProfileChanged = onCustomProfileChanged
        )
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { AppPage.entries.size })
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarHeightPx = with(density) { statusBarHeight.toPx() }
    val bottomBlurHeightPx = with(density) { 140.dp.toPx() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    LaunchedEffect(pagerState) {
        var firstPage = true
        snapshotFlow { pagerState.currentPage }.collect {
            if (firstPage) {
                firstPage = false
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 84.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .progressiveBlur(
                        blurRadius = if (blurEnabled) 40f else 0f,
                        topHeight = statusBarHeightPx * 1.15f,
                        bottomHeight = bottomBlurHeightPx
                    )
            ) { pageIndex ->
                when (AppPage.entries[pageIndex]) {
                    AppPage.Home -> RefreshRateScreen(
                        supportedRates = uiState.supportedRates,
                        currentRate = uiState.currentRate,
                        selectedRate = uiState.selectedRate,
                        isLoading = uiState.isLoading || !uiState.isServiceBound,
                        onRateSelected = onRateSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                    AppPage.Settings -> SettingsScreen(
                        onResetToDefaults = onResetToDefaults,
                        onOpenCustomKeys = { showCustomKeys = true },
                        themeMode = themeMode,
                        amoledMode = amoledMode,
                        blurEnabled = blurEnabled,
                        onThemeModeChanged = onThemeModeChanged,
                        onAmoledModeChanged = onAmoledModeChanged,
                        onBlurEnabledChanged = onBlurEnabledChanged,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            FloatingBottomBar(
                items = AppPage.entries.mapIndexed { index, page ->
                    FloatingNavigationItem(
                        label = page.label,
                        icon = page.icon,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                },
                selectedIndex = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
            )
        }
    }
}
