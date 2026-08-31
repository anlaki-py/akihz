package akihz.anlaki.dev.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.OemSettingsStrategy
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/**
 * Diagnostic visibility controls.
 */
@Composable
fun DiagnosticsSection() {
    var showDebug by remember { mutableStateOf(false) }

    PreferenceGroup(heading = "Diagnostics") {
        PreferenceTemplate(
            title = if (showDebug) "Hide debug info" else "Show debug info",
            icon = Icons.Default.BugReport,
            onClick = { showDebug = !showDebug }
        )
        AnimatedVisibility(
            visible = showDebug,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            DebugInfo()
        }
    }
}

@Composable
private fun DebugInfo() {
    val strategy = OemSettingsStrategy.resolve()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "Detected OEM: ${android.os.Build.MANUFACTURER}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Brand: ${android.os.Build.BRAND}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Model: ${android.os.Build.MODEL}",
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Read keys:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        strategy.readKeys.forEach {
            Text(
                "  ${it.namespace.name.lowercase()}/${it.key}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Write keys:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        strategy.writeKeys.forEach {
            Text(
                "  ${it.namespace.name.lowercase()}/${it.key}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
