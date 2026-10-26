package akihz.anlaki.dev.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Displays RikkaHub's preset themes in a four-column color grid.
 *
 * @param selectedThemeId active preset theme id
 * @param onThemeSelected persists the selected preset theme
 * @param modifier layout modifier supplied by the caller
 */
@Composable
fun ThemePresetGrid(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = ThemeGridColumns
    ) {
        PresetThemes.forEach { theme ->
            key(theme.id) {
                ThemePresetButton(
                    theme = theme,
                    selected = theme.id == selectedThemeId,
                    onClick = { onThemeSelected(theme.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        repeat((ThemeGridColumns - PresetThemes.size % ThemeGridColumns) % ThemeGridColumns) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ThemePresetButton(
    theme: PresetTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = theme.colorScheme(LocalDarkMode.current)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(48.dp)
            ) {
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
                    radius = (if (selected) 12 else 8).dp.toPx(),
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = theme.name,
            style = MaterialTheme.typography.labelMedium.copy(
                color = scheme.primary,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
    }
}

private const val ThemeGridColumns = 4
