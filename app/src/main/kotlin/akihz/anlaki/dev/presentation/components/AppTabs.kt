package akihz.anlaki.dev.presentation.components

import android.os.Build
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import akihz.anlaki.dev.presentation.glass.rememberGlassBackdrop
import akihz.anlaki.dev.presentation.glass.recordGlassBackdrop
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
 * @param glassSettings opt-in navbar appearance
 * @param content renders each tab beneath the navigation bar
 */
@Composable
internal fun AppTabs(
    pagerState: PagerState,
    glassSettings: NavbarGlassSettings = NavbarGlassSettings(),
    content: @Composable (AppTab) -> Unit
) {
    val backdrop = if (glassSettings.enabled && Build.VERSION.SDK_INT >= 33) rememberGlassBackdrop() else null
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
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().then(
            if (backdrop != null) Modifier.recordGlassBackdrop(backdrop) else Modifier
        )) { index ->
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
            glassSettings = glassSettings,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
