package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.CustomRefreshProfile

/** Snapshot capture controls for baseline and selected profile rates. */
@Composable
fun SnapshotActions(
    state: CustomKeysUiState,
    editingEnabled: Boolean,
    viewModel: CustomKeysViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { viewModel.capture("Baseline") },
                enabled = editingEnabled && !state.busy
            ) { Text("Capture baseline") }
            TextButton(
                onClick = viewModel::clearSnapshots,
                enabled = editingEnabled && state.snapshots.isNotEmpty()
            ) { Text("Clear") }
        }
        state.profile.rates.forEach { rate ->
            val label = "${CustomRefreshProfile.rateLabel(rate)} Hz"
            TextButton(
                onClick = { viewModel.capture(label) },
                enabled = editingEnabled && !state.busy
            ) {
                Text(if (state.snapshots.any { it.label == label }) "Recapture $label" else "Capture $label")
            }
        }
        if (state.snapshots.isNotEmpty()) {
            Text(
                "Saved: ${state.snapshots.joinToString { it.label }}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Test and activation controls for the custom profile. */
@Composable
fun ActivationActions(
    state: CustomKeysUiState,
    viewModel: CustomKeysViewModel,
    onProfileChanged: () -> Unit
) {
    var testRate by remember { mutableStateOf<Float?>(null) }
    val validationError = state.profile.validationError()
    SectionHeading("5. Test and enable")
    validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (!state.profile.enabled) {
        Text("Test rate")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.profile.rates.forEach { rate ->
                TextButton(
                    onClick = { testRate = rate },
                    enabled = validationError == null && !state.busy
                ) { Text("${CustomRefreshProfile.rateLabel(rate)} Hz") }
            }
        }
        Button(
            onClick = { viewModel.setEnabled(true, onProfileChanged) },
            enabled = state.profile.tested && validationError == null && !state.busy
        ) { Text(if (state.profile.tested) "Enable custom profile" else "Test required") }
    } else {
        Button(
            onClick = { viewModel.setEnabled(false, onProfileChanged) },
            enabled = !state.busy
        ) { Text("Disable and restore original values") }
    }
    testRate?.let { rate ->
        AlertDialog(
            onDismissRequest = { testRate = null },
            title = { Text("Temporarily write custom keys?") },
            text = {
                Text(
                    "This experimental test will write ${CustomRefreshProfile.rateLabel(rate)} Hz " +
                        "mappings. Confirm within 15 seconds or the captured values will be restored."
                )
            },
            confirmButton = {
                TextButton(onClick = { testRate = null; viewModel.beginTest(rate) }) { Text("Start test") }
            },
            dismissButton = { TextButton(onClick = { testRate = null }) { Text("Cancel") } }
        )
    }
}

/** Confirmation countdown displayed while temporary custom values are active. */
@Composable
fun TestCountdownDialog(seconds: Int, onConfirm: () -> Unit, onReject: () -> Unit) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text("Keep this test result?") },
        text = {
            Text(
                "Is the display working at the expected rate? Original values will be restored in $seconds seconds."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("It worked") } },
        dismissButton = { TextButton(onClick = onReject) { Text("Restore now") } }
    )
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
}
