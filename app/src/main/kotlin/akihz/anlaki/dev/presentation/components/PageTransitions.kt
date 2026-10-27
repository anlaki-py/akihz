package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset

/** Slides forward using [motionScheme] with a restrained preview of the previous page. */
internal fun pagePushTransition(motionScheme: MotionScheme): ContentTransform =
    slideInHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) { it } togetherWith
        slideOutHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) { -it / 8 } +
        scaleOut(motionScheme.defaultSpatialSpec(), targetScale = 0.96f) +
        fadeOut(motionScheme.defaultEffectsSpec())

/** Returns a back transition, with linear seeking when [predictive] follows the user's finger. */
internal fun pagePopTransition(predictive: Boolean = false): ContentTransform {
    // Seeking a spring spends most of the gesture in its settling tail. Equal-duration,
    // linear tracks keep the preview proportional to the swipe and reversible on cancel.
    val easing = if (predictive) LinearEasing else FastOutSlowInEasing
    return slideInHorizontally(tween(300, easing = easing)) { -it / 8 } +
        scaleIn(tween(300, easing = easing), initialScale = 0.96f) +
        fadeIn(tween(300, easing = easing), initialAlpha = 0.9f) togetherWith
        slideOutHorizontally(tween(300, easing = easing)) { it }
}
