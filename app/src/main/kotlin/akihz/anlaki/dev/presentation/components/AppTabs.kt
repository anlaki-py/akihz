package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

/** Root tabs that share one navigation entry and keep their selected page during back previews. */
internal enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home), Settings("Settings", Icons.Default.Settings)
}

/**
 * Displays the root [pagerState] and floating navigation as one previewable page.
 *
 * @param content renders each tab beneath the navigation bar
 */
@Composable
internal fun AppTabs(pagerState: PagerState, content: @Composable (AppTab) -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pagerState) {
        var firstPage = true
        snapshotFlow { pagerState.currentPage }.collect {
            if (firstPage) firstPage = false
            else haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            content(AppTab.entries[index])
        }
        FloatingBottomBar(
            items = AppTab.entries.mapIndexed { index, tab ->
                FloatingNavigationItem(
                    label = tab.label,
                    icon = tab.icon,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                )
            },
            selectedIndex = pagerState.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
