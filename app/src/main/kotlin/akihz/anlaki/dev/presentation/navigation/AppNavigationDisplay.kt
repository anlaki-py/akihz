package akihz.anlaki.dev.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import akihz.anlaki.dev.presentation.components.pagePopTransition
import akihz.anlaki.dev.presentation.components.pagePushTransition

/**
 * Displays [backStack] with live screen state and gesture-driven back previews.
 *
 * @param onBack handles a completed back action
 * @param content renders each destination with the latest state and callbacks
 */
@Composable
internal fun AppNavigationDisplay(
    backStack: List<AppDestination>,
    onBack: () -> Unit,
    content: @Composable (AppDestination) -> Unit
) {
    // NavDisplay caches entries until the stack changes. Read the latest content
    // inside each entry so theme controls, rate selections, and callbacks stay live.
    val currentContent by rememberUpdatedState(content)
    val motionScheme = MaterialTheme.motionScheme
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = onBack,
        transitionSpec = { pagePushTransition(motionScheme) },
        popTransitionSpec = { pagePopTransition() },
        predictivePopTransitionSpec = { pagePopTransition(predictive = true) },
        entryProvider = { destination ->
            NavEntry(destination) { currentContent(destination) }
        }
    )
}
