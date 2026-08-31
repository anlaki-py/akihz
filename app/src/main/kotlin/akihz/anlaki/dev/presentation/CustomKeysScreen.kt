package akihz.anlaki.dev.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import akihz.anlaki.dev.data.CustomRefreshProfile
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate

/**
 * Simplified manual workflow for refresh_rate keys.
 * Only keys containing "refresh_rate" are surfaced and editable.
 *
 * @param onBack returns to the standard settings screen
 * @param onProfileChanged refreshes home and tile rate data
 */
@Composable
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
    val leaveScreen: () -> Unit = { viewModel.flushDraft(onBack) }

    BackHandler(onBack = leaveScreen)
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    if (!state.warningAcknowledged) {
        CustomKeysWarning(onAccept = viewModel::acknowledgeWarning, onLeave = leaveScreen)
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

    Box(modifier = Modifier.fillMaxSize()) {
        PreferenceLayout(
            label = "Custom keys",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationContentDescription = "Back",
            onNavigationClick = leaveScreen,
            modifier = Modifier.fillMaxSize()
        ) {
            PreferenceGroup(heading = "Info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Experimental, only refresh_rate keys",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        if (state.profile.enabled) "Custom keys are enabled and replace the OEM strategy."
                        else "Manually select keys containing \"refresh_rate\" that should change when switching refresh rates. Values are never selected automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PreferenceGroup(heading = "Profile rates") {
                if (state.detectedRates.isEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "No refresh rates detected. Ensure Shizuku is running.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    state.detectedRates.forEach { rate ->
                        PreferenceTemplate(
                            title = "${CustomRefreshProfile.rateLabel(rate)} Hz",
                            description = if (rate in state.profile.rates) "Included in profile" else "Tap to include",
                            icon = Icons.Default.Speed,
                            checked = rate in state.profile.rates,
                            onCheckedChange = if (editingEnabled) {
                                { viewModel.toggleRate(rate) }
                            } else null
                        )
                    }
                }
            }

            PreferenceGroup(heading = "Available refresh_rate keys") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = viewModel::scan,
                            enabled = editingEnabled && !state.busy
                        ) { Text("Scan") }
                        TextButton(
                            onClick = { showRawDialog = true },
                            enabled = editingEnabled
                        ) { Text("Add key") }
                        if (state.busy) CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        "Scans all settings namespaces and shows only keys containing \"refresh_rate\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("Search refresh_rate keys") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                val filtered = state.candidates.filter {
                    (search.isBlank() || it.id.contains(search, true) || it.value.contains(search, true)) &&
                        CustomRefreshProfile.isRefreshRateKey(it.name)
                }.take(30)
                if (filtered.isEmpty() && !state.busy) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (state.candidates.isEmpty()) "Tap Scan to discover refresh_rate keys." else "No matching refresh_rate keys.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    filtered.forEach { candidate ->
                        val alreadyAdded = state.profile.keys.any { it.id == candidate.id }
                        PreferenceTemplate(
                            title = candidate.id,
                            description = "${candidate.value} · ${candidate.reason}",
                            icon = Icons.Default.Tune,
                            onClick = if (editingEnabled && !alreadyAdded) {
                                { viewModel.addKey(candidate.namespace, candidate.name) }
                            } else null
                        )
                    }
                }
            }

            val selectedRefreshKeys = state.profile.keys.filter { CustomRefreshProfile.isRefreshRateKey(it.name) }
            if (selectedRefreshKeys.isNotEmpty()) {
                selectedRefreshKeys.forEach { key ->
                    CustomKeyEditor(
                        setting = key,
                        rates = state.profile.rates,
                        enabled = editingEnabled,
                        onRolesChanged = { read, write -> viewModel.updateRoles(key.id, read, write) },
                        onValueChanged = { rate, value -> viewModel.updateValue(key.id, rate, value) },
                        onRemove = { viewModel.removeKey(key.id) }
                    )
                }
            } else {
                PreferenceGroup(heading = "Selected keys") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "No refresh_rate keys selected. Add from available keys above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            PreferenceGroup(heading = "Activation") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val validationError = state.profile.validationError()
                    validationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!state.profile.enabled) {
                        Text("Test a rate before enabling.", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.profile.rates.forEach { rate ->
                                TextButton(
                                    onClick = { viewModel.beginTest(rate) },
                                    enabled = validationError == null && !state.busy
                                ) { Text("${CustomRefreshProfile.rateLabel(rate)} Hz") }
                            }
                        }
                        Button(
                            onClick = { viewModel.setEnabled(true, onProfileChanged) },
                            enabled = state.profile.tested && validationError == null && !state.busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (state.profile.tested) "Enable custom profile" else "Test required before enable") }
                    } else {
                        Text("Custom profile is active and replaces OEM handling.", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { viewModel.setEnabled(false, onProfileChanged) },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Disable and restore originals") }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 84.dp)
        )
    }
}
