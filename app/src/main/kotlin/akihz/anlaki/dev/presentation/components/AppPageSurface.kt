package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.modifiers.BlurDirection
import akihz.anlaki.dev.presentation.modifiers.progressiveBlur

/**
 * Gives every app page the same full-screen bounds and progressive edge treatment.
 *
 * New top-level and detail pages should use this surface so theme and blur behavior
 * remain consistent without duplicating shell modifiers.
 *
 * @param blurEnabled whether progressive edge blur is enabled
 * @param modifier layout modifier supplied by the app shell
 * @param content page content rendered inside the shared surface
 */
@Composable
fun AppPageSurface(
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val topHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topHeightPx = with(density) { topHeight.toPx() }
    val bottomHeightPx = with(density) { 140.dp.toPx() }
    val blurRadius = if (blurEnabled) 40f else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .progressiveBlur(blurRadius, topHeightPx * 1.15f, BlurDirection.Top)
            .progressiveBlur(blurRadius, bottomHeightPx, BlurDirection.Bottom)
    ) {
        content()
    }
}
