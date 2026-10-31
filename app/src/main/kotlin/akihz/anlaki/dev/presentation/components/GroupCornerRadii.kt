package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radius shared by the outer corners of a grouped list of rows. */
internal val GroupOuterCornerRadius = 20.dp

/** Corner radius shared by the inner corners of a grouped list of rows. */
internal val GroupInnerCornerRadius = 4.dp

/** Larger outer corner radius used by the home screen's rate and tile groups. */
internal val HomeGroupOuterCornerRadius = 28.dp

/** Rounded viewport where scrolling content meets a page header. Bottom content stays uncut. */
internal val PageContentShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
