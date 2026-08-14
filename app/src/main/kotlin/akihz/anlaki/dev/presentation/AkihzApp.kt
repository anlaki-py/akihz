package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.presentation.components.FloatingBottomBar
import akihz.anlaki.dev.presentation.components.FloatingNavigationItem
import akihz.anlaki.dev.presentation.components.AppPageSurface
import akihz.anlaki.dev.presentation.components.horizontalPageTransition
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

private enum class DetailPage { CustomKeys, DebugOptions }

/**
 * Hosts the refresh-rate and settings pages with floating tab navigation.
 *
 * @param uiState current refresh-rate screen state
 * @param onRateSelected invoked when the user selects a refresh rate
 * @param onTileRateIncludedChanged updates whether a rate appears in tile cycling
 * @param onResetToDefaults restores the system refresh-rate defaults
 * @param onCustomProfileChanged refreshes rates after custom profile activation changes
 * @param themeMode currently selected appearance mode
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param blurEnabled whether progressive edge blur is enabled
 * @param homeDebugSettings current home-screen tuning values
 * @param onHomeDebugSettingsChanged persists updated home-screen tuning
 * @param debugOptionsUnlocked whether the hidden developer entry has been unlocked
 * @param onDebugOptionsUnlocked persists the developer entry after its unlock gesture
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 * @param onBlurEnabledChanged invoked when progressive blur changes
 * @param onErrorDismissed clears an error after it is shown
 * @param openUpdatesRequest changes when Settings should open from an update alert
 */
@Composable
fun AkihzApp(
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onTileRateIncludedChanged: (Float, Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onCustomProfileChanged: () -> Unit,
    themeMode: AppThemeMode,
    amoledMode: Boolean,
    blurEnabled: Boolean,
    homeDebugSettings: HomeDebugSettings,
    onHomeDebugSettingsChanged: (HomeDebugSettings) -> Unit,
    debugOptionsUnlocked: Boolean,
    onDebugOptionsUnlocked: () -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    onBlurEnabledChanged: (Boolean) -> Unit,
    openUpdatesRequest: Int = 0,
    onErrorDismissed: () -> Unit = {}
) {
    var detailPage by remember { mutableStateOf<DetailPage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { AppPage.entries.size })
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

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

    LaunchedEffect(openUpdatesRequest) {
        if (openUpdatesRequest > 0) {
            pagerState.animateScrollToPage(AppPage.Settings.ordinal)
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
            AppPageSurface(blurEnabled = blurEnabled) {
                AnimatedContent(
                    targetState = detailPage,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        horizontalPageTransition(targetState != null)
                    },
                    label = "app page transition"
                ) { activeDetailPage ->
                    when (activeDetailPage) {
                        DetailPage.CustomKeys -> CustomKeysScreen(
                            onBack = { detailPage = null },
                            onProfileChanged = onCustomProfileChanged
                        )
                        DetailPage.DebugOptions -> DebugSettingsScreen(
                            settings = homeDebugSettings,
                            onSettingsChanged = onHomeDebugSettingsChanged,
                            onBack = { detailPage = null },
                            modifier = Modifier.fillMaxSize()
                        )
                        null -> HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            when (AppPage.entries[pageIndex]) {
                                AppPage.Home -> RefreshRateScreen(
                                    supportedRates = uiState.supportedRates,
                                    currentRate = uiState.currentRate,
                                    selectedRate = uiState.selectedRate,
                                    excludedTileRates = uiState.excludedTileRates,
                                    isLoading = uiState.isLoading || !uiState.isServiceBound,
                                    debugSettings = homeDebugSettings,
                                    onRateSelected = onRateSelected,
                                    onTileRateIncludedChanged = onTileRateIncludedChanged,
                                    modifier = Modifier.fillMaxSize()
                                )
                                AppPage.Settings -> SettingsScreen(
                                    onResetToDefaults = onResetToDefaults,
                                    onOpenCustomKeys = { detailPage = DetailPage.CustomKeys },
                                    themeMode = themeMode,
                                    amoledMode = amoledMode,
                                    blurEnabled = blurEnabled,
                                    onThemeModeChanged = onThemeModeChanged,
                                    onAmoledModeChanged = onAmoledModeChanged,
                                    onBlurEnabledChanged = onBlurEnabledChanged,
                                    debugOptionsUnlocked = debugOptionsUnlocked,
                                    onDebugOptionsUnlocked = onDebugOptionsUnlocked,
                                    onOpenDebugSettings = { detailPage = DetailPage.DebugOptions },
                                    autoCheckRequest = openUpdatesRequest,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = detailPage == null,
                enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 2 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(240)) { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
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
                    modifier = Modifier.zIndex(1f)
                )
            }
        }
    }
}
