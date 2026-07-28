package akihz.anlaki.dev.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.presentation.theme.AppThemeMode

/**
 * Displays theme mode and AMOLED appearance controls.
 *
 * @param themeMode currently selected appearance mode
 * @param amoledMode whether pure-black dark surfaces are enabled
 * @param onThemeModeChanged invoked when the appearance mode changes
 * @param onAmoledModeChanged invoked when AMOLED mode changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSection(
    themeMode: AppThemeMode,
    amoledMode: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAmoledModeChanged: (Boolean) -> Unit
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
    }

    if (showThemeOptions) {
        ModalBottomSheet(
            onDismissRequest = { showThemeOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "Choose theme",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                AppThemeMode.entries.forEach { mode ->
                    ListItem(
                        headlineContent = { Text(mode.label) },
                        leadingContent = {
                            RadioButton(
                                selected = mode == themeMode,
                                onClick = null
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeModeChanged(mode)
                                showThemeOptions = false
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
