package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private enum class BottomNavPage(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Default.Home),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun AkihzApp(
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onResetToDefaults: () -> Unit,
    onErrorDismissed: () -> Unit = {}
) {
    var bottomNavPage by rememberSaveable { mutableStateOf(BottomNavPage.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    Scaffold(
        bottomBar = {
            AkihzBottomBar(
                currentPage = bottomNavPage,
                onPageSelected = { bottomNavPage = it }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        MainPageContent(
            bottomNavPage = bottomNavPage,
            padding = padding,
            uiState = uiState,
            onRateSelected = onRateSelected,
            onResetToDefaults = onResetToDefaults
        )
    }
}

@Composable
private fun MainPageContent(
    bottomNavPage: BottomNavPage,
    padding: PaddingValues,
    uiState: MainUiState,
    onRateSelected: (Float) -> Unit,
    onResetToDefaults: () -> Unit
) {
    when (bottomNavPage) {
        BottomNavPage.Home -> RefreshRateScreen(
            supportedRates = uiState.supportedRates,
            currentRate = uiState.currentRate,
            selectedRate = uiState.selectedRate,
            onRateSelected = onRateSelected,
            modifier = Modifier.padding(padding)
        )
        BottomNavPage.Settings -> SettingsScreen(onResetToDefaults = onResetToDefaults)
    }
}

@Composable
private fun AkihzBottomBar(
    currentPage: BottomNavPage,
    onPageSelected: (BottomNavPage) -> Unit
) {
    NavigationBar {
        BottomNavPage.entries.forEach { page ->
            NavigationBarItem(
                selected = currentPage == page,
                onClick = { onPageSelected(page) },
                label = { Text(page.label) },
                icon = {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = page.label
                    )
                }
            )
        }
    }
}
