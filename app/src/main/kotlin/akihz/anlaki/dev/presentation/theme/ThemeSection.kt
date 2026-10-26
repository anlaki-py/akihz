package akihz.anlaki.dev.presentation.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

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
        formItem(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Color mode",
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(bottom = 10.dp)
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
                            .heightIn(min = 56.dp)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            AppThemeMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                AppThemeMode.System -> Icons.Default.SettingsBrightness
                                AppThemeMode.Light -> Icons.Default.LightMode
                                AppThemeMode.Dark -> Icons.Default.DarkMode
                            },
                            contentDescription = null
                        )
                        Text(mode.label, maxLines = 1)
                    }
                }
            }
        }
        PreferenceTemplate(
            title = "Theme",
            description = "Dynamic color, color schemes, and AMOLED",
            icon = Icons.Default.Palette,
            onClick = onOpenThemePreferences
        )
    }
}
