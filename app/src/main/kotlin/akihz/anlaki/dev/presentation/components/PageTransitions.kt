package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/** Creates the shared directional slide-and-fade transition used between app pages. */
internal fun horizontalPageTransition(movingForward: Boolean): ContentTransform {
    val enter = slideInHorizontally(tween(320)) { width ->
        if (movingForward) width else -width
    } + fadeIn(tween(220))
    val exit = slideOutHorizontally(tween(320)) { width ->
        if (movingForward) -width else width
    } + fadeOut(tween(180))
    return enter togetherWith exit
}
