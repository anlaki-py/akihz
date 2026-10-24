package akihz.anlaki.dev.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.presentation.theme.AppThemeMode

/**
 * Displays theme mode and AMOLED appearance controls.
 *
 * @param themeMode currently selected appearance mode
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param blurEnabled whether progressive edge blur is enabled
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 * @param onBlurEnabledChanged invoked when progressive blur changes
 */
@Composable
fun ThemeSection(
    themeMode: AppThemeMode,
    amoledMode: Boolean,
    blurEnabled: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit,
    onBlurEnabledChanged: (Boolean) -> Unit
) {
    var showThemeOptions by remember { mutableStateOf(false) }

    PreferenceGroup(heading = "Appearance") {
        PreferenceTemplate(
            title = "Theme",
            description = themeMode.label,
            icon = Icons.Default.Palette,
            onClick = { showThemeOptions = true }
        )
        PreferenceTemplate(
            title = "Pitch-black AMOLED",
            description = "Use pure-black surfaces whenever dark mode is active",
            icon = Icons.Default.DarkMode,
            checked = amoledMode,
            onCheckedChange = onAmoledModeChanged
        )
        PreferenceTemplate(
            title = "Background blur",
            description = "Blur content beneath the top and bottom overlays",
            icon = Icons.Default.BlurOn,
            checked = blurEnabled,
            onCheckedChange = onBlurEnabledChanged
        )
    }

    if (showThemeOptions) {
        ModalBottomSheet(
            onDismissRequest = { showThemeOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "Choose theme",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    val selected = mode == themeMode
                    val weight by animateFloatAsState(
                        targetValue = if (selected) 1.3f else 1f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        label = "theme choice weight"
                    )
                    ToggleButton(
                        checked = selected,
                        onCheckedChange = {
                            if (!selected) onThemeModeChanged(mode)
                            showThemeOptions = false
                        },
                        modifier = Modifier
                            .weight(weight)
                            .heightIn(min = 56.dp)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            AppThemeMode.entries.lastIndex ->
                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
