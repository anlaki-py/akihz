package akihz.anlaki.dev.presentation.theme

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import java.util.UUID

/**
 * Shows RikkaHub's preset and custom color-scheme editor page.
 *
 * @param dynamicColor whether wallpaper colors currently override saved themes
 * @param selectedThemeId active preset or custom theme id
 * @param customThemes saved custom themes
 * @param onThemeSelected persists the active theme id
 * @param onCustomThemesChanged persists the custom theme collection
 * @param onBack returns to theme preferences
 * @param modifier page modifier
 */
@Composable
fun ThemePaletteScreen(
    dynamicColor: Boolean,
    selectedThemeId: String,
    customThemes: List<CustomTheme>,
    onThemeSelected: (String) -> Unit,
    onCustomThemesChanged: (List<CustomTheme>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingTheme by remember { mutableStateOf<CustomTheme?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var deletingTheme by remember { mutableStateOf<CustomTheme?>(null) }

    PreferenceLayout(
        label = "Color schemes",
        modifier = modifier,
        navigationIcon = Icons.Default.ArrowBack,
        navigationContentDescription = "Back",
        onNavigationClick = onBack
    ) {
        if (dynamicColor) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Dynamic color is enabled. Disable it in Theme preferences to use custom themes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Preset themes",
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceBright
            ) {
                ThemePresetGrid(
                    selectedThemeId = selectedThemeId,
                    onThemeSelected = onThemeSelected
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Custom themes",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showImport = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Text("Import")
                    }
                    FilledTonalButton(
                        onClick = {
                            editingTheme = null
                            showEditor = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add")
                    }
                }
            }

            if (customThemes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom themes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customThemes.forEach { theme ->
                        CustomThemeItem(
                            theme = theme,
                            selected = selectedThemeId == theme.id,
                            onSelect = { onThemeSelected(theme.id) },
                            onExport = { exportTheme(context, theme) },
                            onEdit = {
                                editingTheme = theme
                                showEditor = true
                            },
                            onDelete = { deletingTheme = theme }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        CustomThemeEditorSheet(
            theme = editingTheme,
            onDismiss = { showEditor = false },
            onSave = { saved ->
                val themes = if (editingTheme == null) {
                    customThemes + saved
                } else {
                    customThemes.map { if (it.id == saved.id) saved else it }
                }
                onCustomThemesChanged(themes)
                onThemeSelected(saved.id)
                showEditor = false
            }
        )
    }
    if (showImport) {
        ImportThemeDialog(
            onDismiss = { showImport = false },
            onImport = { imported ->
                val saved = imported.copy(id = UUID.randomUUID().toString())
                onCustomThemesChanged(customThemes + saved)
                onThemeSelected(saved.id)
                showImport = false
                Toast.makeText(context, "Theme imported", Toast.LENGTH_SHORT).show()
            }
        )
    }
    deletingTheme?.let { theme ->
        DeleteThemeDialog(
            onDismiss = { deletingTheme = null },
            onConfirm = {
                onCustomThemesChanged(customThemes.filterNot { it.id == theme.id })
                if (selectedThemeId == theme.id) onThemeSelected("sakura")
                deletingTheme = null
            }
        )
    }
}

private fun exportTheme(context: Context, theme: CustomTheme) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("theme", theme.toJson()))
    Toast.makeText(context, "Theme copied", Toast.LENGTH_SHORT).show()
}
