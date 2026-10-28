package akihz.anlaki.dev.presentation.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Moon02
import me.rerere.hugeicons.stroke.SmartPhone01
import me.rerere.hugeicons.stroke.Sun01

/**
 * Shows RikkaHub's color-mode selector and the entry to theme preferences.
 *
 * @param themeMode active System, Light, or Dark mode
 * @param onThemeModeChanged persists a newly selected color mode
 * @param onOpenThemePreferences opens dynamic color and palette settings
 */
@Composable
fun ThemeSection(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenThemePreferences: () -> Unit
) {
    PreferenceGroup(heading = "Appearance") {
        formItem {
            Text(
                text = "Color mode",
                style = MaterialTheme.typography.titleMediumEmphasized
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    val selected = mode == themeMode
                    val weight by animateFloatAsState(
                        targetValue = if (selected) 1.3f else 1f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        label = "color mode weight"
                    )
                    ToggleButton(
                        checked = selected,
                        onCheckedChange = { if (!selected) onThemeModeChanged(mode) },
                        modifier = Modifier
                            .weight(weight)
                            .heightIn(min = 48.dp)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            AppThemeMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        ConnectedChoiceLabel(
                            text = mode.label,
                            selected = selected,
                            icon = mode.icon()
                        )
                    }
                }
            }
        }
        PreferenceTemplate(
            title = "Theme",
            description = "Colors, fonts, and app icon",
            icon = Icons.Default.Palette,
            onClick = onOpenThemePreferences
        )
    }
}

@Composable
private fun ConnectedChoiceLabel(
    text: String,
    selected: Boolean,
    icon: ImageVector
) {
    AnimatedVisibility(
        visible = selected,
        enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
        exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
    ) {
        Row {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ToggleButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
        }
    }
    Text(
        text = text,
        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}

private fun AppThemeMode.icon(): ImageVector = when (this) {
    AppThemeMode.System -> HugeIcons.SmartPhone01
    AppThemeMode.Light -> HugeIcons.Sun01
    AppThemeMode.Dark -> HugeIcons.Moon02
}
