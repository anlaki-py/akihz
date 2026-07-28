package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import akihz.anlaki.dev.presentation.components.FloatingBottomBar
import akihz.anlaki.dev.presentation.components.FloatingNavigationItem

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
 * @param onErrorDismissed clears an error after it is shown
 */
@Composable
fun AkihzApp(
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onResetToDefaults: () -> Unit,
    onErrorDismissed: () -> Unit = {}
) {
    var currentPage by rememberSaveable { mutableStateOf(AppPage.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MainPageContent(
                currentPage = currentPage,
                uiState = uiState,
                onRateSelected = onRateSelected,
                onResetToDefaults = onResetToDefaults
            )
            FloatingBottomBar(
                items = AppPage.entries.map { page ->
                    FloatingNavigationItem(
                        label = page.label,
                        icon = page.icon,
                        onClick = { currentPage = page }
                    )
                },
                selectedIndex = currentPage.ordinal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
            )
        }
    }
}

@Composable
private fun MainPageContent(
    currentPage: AppPage,
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onResetToDefaults: () -> Unit
) {
    val contentModifier = Modifier.padding(bottom = 112.dp)
    when (currentPage) {
        AppPage.Home -> RefreshRateScreen(
            supportedRates = uiState.supportedRates,
            currentRate = uiState.currentRate,
            selectedRate = uiState.selectedRate,
            onRateSelected = onRateSelected,
            modifier = contentModifier
        )
        AppPage.Settings -> SettingsScreen(
            onResetToDefaults = onResetToDefaults,
            modifier = contentModifier
        )
    }
}
