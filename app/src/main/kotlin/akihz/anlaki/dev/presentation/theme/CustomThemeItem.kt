package akihz.anlaki.dev.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
internal fun CustomThemeItem(
    theme: CustomTheme,
    selected: Boolean,
    onSelect: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = theme.generateColorScheme(LocalDarkMode.current)
    var menuExpanded by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        headlineContent = { Text(theme.name.ifBlank { "Unnamed" }) },
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(40.dp).clip(CircleShape)) {
                    drawRect(color = scheme.primaryContainer, size = size)
                    drawRect(
                        color = scheme.secondaryContainer,
                        size = size,
                        topLeft = Offset(size.width / 2, 0f)
                    )
                    drawRect(
                        color = scheme.tertiaryContainer,
                        size = size,
                        topLeft = Offset(size.width / 2, size.height / 2)
                    )
                    drawCircle(
                        color = scheme.primary,
                        radius = (if (selected) 10 else 6).dp.toPx(),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Theme actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    ThemeMenuItem("Edit", Icons.Default.Edit) { menuExpanded = false; onEdit() }
                    ThemeMenuItem("Export", Icons.Default.Share) { menuExpanded = false; onExport() }
                    ThemeMenuItem("Delete", Icons.Default.Delete) { menuExpanded = false; onDelete() }
                }
            }
        },
        colors = AkihzColors.listItemColors
    )
}

@Composable
private fun ThemeMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

@Composable
internal fun ImportThemeDialog(
    onDismiss: () -> Unit,
    onImport: (CustomTheme) -> Unit
) {
    var json by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it; invalid = false },
                    label = { Text("JSON") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    isError = invalid,
                    supportingText = if (invalid) ({ Text("This theme JSON is not valid") }) else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val theme = CustomTheme.fromJson(json)
                    invalid = theme == null
                    if (theme != null) onImport(theme)
                },
                enabled = json.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun DeleteThemeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete theme?") },
        text = { Text("This custom theme will be removed permanently.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
