package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private enum class AppPage(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Default.Home),
    Settings("Settings", Icons.Default.Settings)
}

/**
 * Displays the main two-page app shell with bottom navigation.
 *
 * @param supportedRates refresh rates supported by the current display
 * @param currentRate currently active refresh rate
 * @param selectedRate refresh rate selected in the UI
 * @param onRateSelected called when the user picks a refresh rate
 */
@Composable
fun AkihzApp(
    supportedRates: List<Float>,
    currentRate: Float?,
    selectedRate: Float?,
    onRateSelected: (Float) -> Unit
) {
    var currentPage by rememberSaveable { mutableStateOf(AppPage.Home) }

    Scaffold(
        bottomBar = {
            AkihzBottomBar(
                currentPage = currentPage,
                onPageSelected = { currentPage = it }
            )
        }
    ) { padding ->
        AppPageContent(
            currentPage = currentPage,
            padding = padding,
            supportedRates = supportedRates,
            currentRate = currentRate,
            selectedRate = selectedRate,
            onRateSelected = onRateSelected
        )
    }
}

@Composable
private fun AppPageContent(
    currentPage: AppPage,
    padding: PaddingValues,
    supportedRates: List<Float>,
    currentRate: Float?,
    selectedRate: Float?,
    onRateSelected: (Float) -> Unit
) {
    when (currentPage) {
        AppPage.Home -> RefreshRateScreen(
            supportedRates = supportedRates,
            currentRate = currentRate,
            selectedRate = selectedRate,
            onRateSelected = onRateSelected,
            modifier = Modifier.padding(padding)
        )
        AppPage.Settings -> SettingsScreen(modifier = Modifier.padding(padding))
    }
}

@Composable
private fun AkihzBottomBar(
    currentPage: AppPage,
    onPageSelected: (AppPage) -> Unit
) {
    NavigationBar {
        AppPage.entries.forEach { page ->
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