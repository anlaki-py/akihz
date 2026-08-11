package akihz.anlaki.dev.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.presentation.components.horizontalPageTransition

private enum class DebugPage { Categories, HomeScreen }

/** Displays organized debug categories and their detail pages. */
@Composable
internal fun DebugSettingsScreen(
    settings: HomeDebugSettings,
    onSettingsChanged: (HomeDebugSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(DebugPage.Categories) }
    val navigateBack = {
        if (page == DebugPage.Categories) onBack() else page = DebugPage.Categories
    }
    BackHandler(onBack = navigateBack)

    AnimatedContent(
        targetState = page,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            horizontalPageTransition(targetState.ordinal > initialState.ordinal)
        },
        label = "debug page transition"
    ) { currentPage ->
        when (currentPage) {
            DebugPage.Categories -> DebugCategories(
                onBack = navigateBack,
                onOpenHomeScreen = { page = DebugPage.HomeScreen },
                modifier = Modifier.fillMaxSize()
            )
            DebugPage.HomeScreen -> HomeDebugSettingsScreen(
                settings = settings,
                onSettingsChanged = onSettingsChanged,
                onBack = navigateBack,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DebugCategories(
    onBack: () -> Unit,
    onOpenHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceLayout(
        label = "Debug options",
        modifier = modifier,
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationContentDescription = "Back to settings",
        onNavigationClick = onBack
    ) {
        PreferenceGroup(heading = "Interface") {
            PreferenceTemplate(
                title = "Home screen",
                description = "Refresh-rate text, button sizing, spacing, and expressive shapes",
                icon = Icons.Default.Home,
                onClick = onOpenHomeScreen
            )
        }
    }
}
