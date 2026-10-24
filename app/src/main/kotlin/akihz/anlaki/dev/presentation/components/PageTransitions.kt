package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/** Creates the shared directional slide-and-fade transition used between app pages. */
@Composable
internal fun horizontalPageTransition(movingForward: Boolean): ContentTransform {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val enter = slideInHorizontally(spatialSpec) { width ->
        if (movingForward) width else -width
    } + fadeIn(effectsSpec)
    val exit = slideOutHorizontally(spatialSpec) { width ->
        if (movingForward) -width else width
    } + fadeOut(effectsSpec)
    return enter togetherWith exit
}
