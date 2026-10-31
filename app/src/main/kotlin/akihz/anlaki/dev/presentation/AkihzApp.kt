package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import akihz.anlaki.dev.presentation.glass.NavbarGlassSettingsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.presentation.components.AppTab
import akihz.anlaki.dev.presentation.components.AppTabs
import akihz.anlaki.dev.presentation.navigation.AppDestination
import akihz.anlaki.dev.presentation.navigation.AppNavigationDisplay
import akihz.anlaki.dev.presentation.navigation.AppNavigator
import akihz.anlaki.dev.presentation.theme.AkihzColors
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import akihz.anlaki.dev.presentation.theme.CustomTheme
import akihz.anlaki.dev.presentation.theme.ThemePaletteScreen
import akihz.anlaki.dev.presentation.theme.ThemePreferencesScreen

/**
 * Hosts root tabs and detail pages on the same predictive back stack.
 *
 * @param uiState current refresh-rate screen state
 * @param onRateSelected invoked when the user selects a refresh rate
 * @param onTileRateIncludedChanged updates whether a rate appears in tile cycling
 * @param onResetToDefaults restores the system refresh-rate defaults
 * @param onCustomProfileChanged refreshes rates after custom profile activation changes
 * @param themeMode currently selected appearance mode
 * @param dynamicColor whether wallpaper colors override the selected color scheme
 * @param themeId selected preset or custom theme id
 * @param customThemes user-created color themes
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param themedAppIcon whether the launcher icon uses Material colors
 * @param onThemedAppIconChanged updates the launcher icon preference
 * @param homeDebugSettings current home-screen tuning values
 * @param onHomeDebugSettingsChanged persists updated home-screen tuning
 * @param debugOptionsUnlocked whether the hidden developer entry has been unlocked
 * @param onDebugOptionsUnlocked persists the developer entry after its unlock gesture
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onDynamicColorChanged invoked when dynamic color changes
 * @param onThemeIdChanged invoked when the selected color scheme changes
 * @param onCustomThemesChanged invoked when custom themes change
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 * @param keepAliveEnabled whether the keep-alive notification is enabled
 * @param onKeepAliveEnabledChanged invoked when the keep-alive toggle changes
 * @param onErrorDismissed clears an error after it is shown
 * @param openUpdatesRequest changes when Settings should open from an update alert
 * @param openDebugRequest changes when a CLI request opens a debug page
 * @param openFpsMonitorRequest changes when a tile request opens FPS Monitor
 * @param debugPage CLI debug-page name associated with the latest request
 */
@Composable
fun AkihzApp(
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onTileRateIncludedChanged: (Float, Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onCustomProfileChanged: () -> Unit,
    themeMode: AppThemeMode,
    dynamicColor: Boolean,
    themeId: String,
    customThemes: List<CustomTheme>,
    amoledMode: Boolean,
    themedAppIcon: Boolean,
    onThemedAppIconChanged: (Boolean) -> Unit,
    homeDebugSettings: HomeDebugSettings,
    onHomeDebugSettingsChanged: (HomeDebugSettings) -> Unit,
    debugOptionsUnlocked: Boolean,
    onDebugOptionsUnlocked: () -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeIdChanged: (String) -> Unit,
    onCustomThemesChanged: (List<CustomTheme>) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    keepAliveEnabled: Boolean = true,
    onKeepAliveEnabledChanged: (Boolean) -> Unit = {},
    openUpdatesRequest: Int = 0,
    openDebugRequest: Int = 0,
    openFpsMonitorRequest: Int = 0,
    debugPage: String? = null,
    onErrorDismissed: () -> Unit = {}
) {
    var glassSettings by remember { mutableStateOf(PreferencesHelper.navbarGlassSettings) }
    val onGlassSettingsChanged: (NavbarGlassSettings) -> Unit = {
        glassSettings = it
        PreferencesHelper.navbarGlassSettings = it
    }
    val navigator = rememberSaveable(saver = AppNavigator.StateSaver) { AppNavigator() }
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { AppTab.entries.size })
    // Keep guarded refresh-rate tests on the activity's existing ViewModel lifetime.
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val customKeysViewModel = if (navigator.current == AppDestination.CustomKeys) {
        hiltViewModel<CustomKeysViewModel>(activityOwner)
    } else null
    val navigateBack: (AppDestination) -> Unit = { destination ->
        if (navigator.current == destination) {
            if (destination == AppDestination.CustomKeys) {
                customKeysViewModel?.flushDraft { navigator.pop(destination) }
            } else {
                navigator.pop(destination)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    LaunchedEffect(openUpdatesRequest) {
        if (openUpdatesRequest > 0) {
            navigator.open(AppDestination.Root)
            pagerState.requestScrollToPage(AppTab.Settings.ordinal)
        }
    }

    LaunchedEffect(openDebugRequest) {
        if (openDebugRequest > 0) {
            // The root pager may be off-screen. Do not wait for its layout before navigating.
            pagerState.requestScrollToPage(AppTab.Settings.ordinal)
            navigator.open(AppDestination.fromDebugPage(debugPage))
            if (!debugOptionsUnlocked) onDebugOptionsUnlocked()
        }
    }

    LaunchedEffect(openFpsMonitorRequest) {
        if (openFpsMonitorRequest > 0) {
            pagerState.requestScrollToPage(AppTab.Settings.ordinal)
            navigator.open(AppDestination.FpsMonitor)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = AkihzColors.pageContainer,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 84.dp)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AppNavigationDisplay(
                backStack = navigator.backStack,
                onBack = { navigateBack(navigator.current) }
            ) { destination ->
                val onBack = { navigateBack(destination) }
                when (destination) {
                    AppDestination.CustomKeys -> CustomKeysScreen(
                        onBack = onBack,
                        onProfileChanged = onCustomProfileChanged,
                        viewModel = hiltViewModel(activityOwner)
                    )
                    AppDestination.DebugOptions -> DebugSettingsScreen(
                        onBack = onBack,
                        onOpenGlass = { navigator.open(AppDestination.DebugGlass) },
                        onOpenHomeScreen = { navigator.open(AppDestination.DebugHome) },
                        onOpenPerformance = { navigator.open(AppDestination.Performance) },
                        onOpenCrashLogs = { navigator.open(AppDestination.CrashLogs) }
                    )
                    AppDestination.DebugGlass -> NavbarGlassSettingsScreen(
                        settings = glassSettings,
                        onSettingsChanged = onGlassSettingsChanged,
                        onBack = onBack
                    )
                    AppDestination.DebugHome -> HomeDebugSettingsScreen(
                        settings = homeDebugSettings,
                        onSettingsChanged = onHomeDebugSettingsChanged,
                        onBack = onBack
                    )
                    AppDestination.Performance -> PerformanceMonitorScreen(onBack = onBack)
                    AppDestination.CrashLogs -> CrashLogScreen(onBack = onBack)
                    AppDestination.FpsMonitor -> FpsMonitorScreen(onBack = onBack)
                    AppDestination.ThemePreferences -> ThemePreferencesScreen(
                        themedAppIcon = themedAppIcon,
                        onThemedAppIconChanged = onThemedAppIconChanged,
                        dynamicColor = dynamicColor,
                        amoledMode = amoledMode,
                        onDynamicColorChanged = onDynamicColorChanged,
                        onAmoledModeChanged = onAmoledModeChanged,
                        onOpenColorSchemes = { navigator.open(AppDestination.ThemePalette) },
                        onBack = onBack
                    )
                    AppDestination.ThemePalette -> ThemePaletteScreen(
                        dynamicColor = dynamicColor,
                        selectedThemeId = themeId,
                        customThemes = customThemes,
                        onThemeSelected = onThemeIdChanged,
                        onCustomThemesChanged = onCustomThemesChanged,
                        onBack = onBack
                    )
                    AppDestination.Root -> AppTabs(pagerState, glassSettings) { tab ->
                        when (tab) {
                            AppTab.Home -> RefreshRateScreen(
                                supportedRates = uiState.supportedRates,
                                currentRate = uiState.currentRate,
                                excludedTileRates = uiState.excludedTileRates,
                                isLoading = uiState.isLoading,
                                debugSettings = homeDebugSettings,
                                onRateSelected = onRateSelected,
                                onTileRateIncludedChanged = onTileRateIncludedChanged,
                                modifier = Modifier.fillMaxSize()
                            )
                            AppTab.Settings -> SettingsScreen(
                                onResetToDefaults = onResetToDefaults,
                                onOpenCustomKeys = { navigator.open(AppDestination.CustomKeys) },
                                themeMode = themeMode,
                                onThemeModeChanged = onThemeModeChanged,
                                onOpenThemePreferences = {
                                    navigator.open(AppDestination.ThemePreferences)
                                },
                                debugOptionsUnlocked = debugOptionsUnlocked,
                                onDebugOptionsUnlocked = onDebugOptionsUnlocked,
                                onOpenDebugSettings = { navigator.open(AppDestination.DebugOptions) },
                                onOpenFpsMonitor = { navigator.open(AppDestination.FpsMonitor) },
                                autoCheckRequest = openUpdatesRequest,
                                keepAliveEnabled = keepAliveEnabled,
                                onKeepAliveEnabledChanged = onKeepAliveEnabledChanged,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
