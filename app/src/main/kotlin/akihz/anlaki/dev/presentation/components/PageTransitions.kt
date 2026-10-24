package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset

/**
 * Creates the shared directional slide-and-fade transition used between app pages.
 *
 * The motion scheme is passed in because `AnimatedContent` builds its transition
 * spec outside composition.
 *
 * @param movingForward whether the destination sits to the right of the current page
 * @param motionScheme motion specs shared with the rest of the app
 */
internal fun horizontalPageTransition(
    movingForward: Boolean,
    motionScheme: MotionScheme
): ContentTransform {
    val spatialSpec = motionScheme.defaultSpatialSpec<IntOffset>()
    val effectsSpec = motionScheme.defaultEffectsSpec<Float>()
    val enter = slideInHorizontally(spatialSpec) { width ->
        if (movingForward) width else -width
    } + fadeIn(effectsSpec)
    val exit = slideOutHorizontally(spatialSpec) { width ->
        if (movingForward) -width else width
    } + fadeOut(effectsSpec)
    return enter togetherWith exit
}
