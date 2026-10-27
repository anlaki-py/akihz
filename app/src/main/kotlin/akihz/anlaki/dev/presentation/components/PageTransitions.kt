package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset

/** Creates RikkaHub's forward page transition using the app's [motionScheme]. */
internal fun pagePushTransition(motionScheme: MotionScheme): ContentTransform =
    slideInHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) { it } togetherWith
        slideOutHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) { -it / 2 } +
        scaleOut(motionScheme.defaultSpatialSpec(), targetScale = 0.7f) +
        fadeOut(motionScheme.defaultEffectsSpec())

/** Creates the shared normal and predictive back transition using [motionScheme]. */
internal fun pagePopTransition(motionScheme: MotionScheme): ContentTransform =
    slideInHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) { -it / 2 } +
        scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.7f) +
        fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
        slideOutHorizontally(motionScheme.defaultSpatialSpec<IntOffset>()) { it }
