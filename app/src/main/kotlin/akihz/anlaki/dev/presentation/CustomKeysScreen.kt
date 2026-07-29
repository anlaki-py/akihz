package akihz.anlaki.dev.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import akihz.anlaki.dev.data.CustomRefreshProfile

/**
 * Advanced workflow for discovering, testing, and enabling custom settings keys.
 *
 * @param onBack returns to the standard settings screen
 * @param onProfileChanged refreshes home and tile rate data
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomKeysScreen(
    onBack: () -> Unit,
    onProfileChanged: () -> Unit,
    viewModel: CustomKeysViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var search by remember { mutableStateOf("") }
    var showRawDialog by remember { mutableStateOf(false) }
    val editingEnabled = !state.profile.enabled

    BackHandler(onBack = onBack)
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    if (!state.warningAcknowledged) {
        CustomKeysWarning(onAccept = viewModel::acknowledgeWarning, onLeave = onBack)
    }
    state.testSeconds?.let { seconds ->
        TestCountdownDialog(seconds, { viewModel.finishTest(true) }, { viewModel.finishTest(false) })
    }
    if (showRawDialog) {
        AddRawKeyDialog(
            onDismiss = { showRawDialog = false },
            onAdd = viewModel::addKey
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom refresh-rate keys") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Experimental", color = MaterialTheme.colorScheme.error)
            Text(
                if (state.profile.enabled) "Custom keys are enabled and replace the OEM strategy."
                else "Build a local profile for this device. Suggestions are never selected automatically."
            )
            SectionTitle("1. Profile rates")
            state.detectedRates.forEach { rate ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = rate in state.profile.rates,
                        onCheckedChange = { viewModel.toggleRate(rate) },
                        enabled = editingEnabled
                    )
                    Text("${CustomRefreshProfile.rateLabel(rate)} Hz", modifier = Modifier.padding(top = 12.dp))
                }
            }
            SectionTitle("2. Guided snapshots")
            Text("Capture a baseline, change the rate in Android Settings, return, then capture that rate.")
            SnapshotActions(state, editingEnabled, viewModel)
            SectionTitle("3. Find candidate keys")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::scan, enabled = editingEnabled && !state.busy) { Text("Scan") }
                TextButton(onClick = { showRawDialog = true }, enabled = editingEnabled) { Text("Add raw key") }
                if (state.busy) CircularProgressIndicator()
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search candidates") },
                singleLine = true
            )
            state.candidates.filter {
                search.isBlank() || it.id.contains(search, true) || it.value.contains(search, true)
            }.take(30).forEach { candidate ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(candidate.id)
                        Text("${candidate.value} · ${candidate.reason}", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        onClick = { viewModel.addKey(candidate.namespace, candidate.name) },
                        enabled = editingEnabled && state.profile.keys.none { it.id == candidate.id }
                    ) { Text("Add") }
                }
            }
            SectionTitle("4. Selected keys and mappings")
            state.profile.keys.forEach { key ->
                CustomKeyEditor(
                    setting = key,
                    rates = state.profile.rates,
                    enabled = editingEnabled,
                    onRolesChanged = { read, write -> viewModel.updateRoles(key.id, read, write) },
                    onValueChanged = { rate, value -> viewModel.updateValue(key.id, rate, value) },
                    onRemove = { viewModel.removeKey(key.id) }
                )
            }
            ActivationActions(state, viewModel, onProfileChanged)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    HorizontalDivider()
    Text(text, style = MaterialTheme.typography.titleMedium)
}
