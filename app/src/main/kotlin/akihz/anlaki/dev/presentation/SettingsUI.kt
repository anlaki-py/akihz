package akihz.anlaki.dev.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.data.OemSettingsStrategy
import akihz.anlaki.dev.utils.AppMonitorService
import akihz.anlaki.dev.utils.BatteryOptimizationHelper
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.RefreshRateWatchdogService

private const val WEBSITE_URL = "https://anlaki.dev"
private const val SOURCE_CODE_URL = "https://github.com/anlaki-py/akihz"
private const val DONATION_URL = "https://ko-fi.com/unluky"
private const val LATEST_RELEASE_URL = "https://github.com/anlaki-py/akihz/releases/latest"

/**
 * Settings screen with all configuration options:
 * - General: Lock mode, default rate, battery saver handling
 * - Watchdog: Enable/disable, interval, aggressive mode
 * - App Monitor: Enable/disable, per-app profiles
 * - Advanced: OEM override, debug info
 * - About: Links and version
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        GeneralSection()
        WatchdogSection()
        AppMonitorSection()
        AdvancedSection()
        AboutSection()
    }
}

@Composable
private fun GeneralSection() {
    val context = LocalContext.current
    var lockMode by remember { mutableStateOf(PreferencesHelper.lockModeEnabled) }
    var batterySaverOverride by remember { mutableStateOf(PreferencesHelper.batterySaverOverride) }
    var defaultRate by remember { mutableFloatStateOf(PreferencesHelper.defaultRate) }

    SettingsCard(title = "General") {
        ToggleRow(
            label = "Lock mode",
            description = "Set min = peak refresh rate to prevent system switching",
            checked = lockMode,
            onCheckedChange = {
                lockMode = it
                PreferencesHelper.lockModeEnabled = it
            }
        )

        ToggleRow(
            label = "Override battery saver",
            description = "Re-apply rate even when battery saver is active",
            checked = batterySaverOverride,
            onCheckedChange = {
                batterySaverOverride = it
                PreferencesHelper.batterySaverOverride = it
            }
        )

        if (defaultRate > 0) {
            Text(
                text = "Default rate: ${defaultRate.toInt()} Hz",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun WatchdogSection() {
    val context = LocalContext.current
    var watchdogEnabled by remember { mutableStateOf(PreferencesHelper.watchdogEnabled) }
    var aggressive by remember { mutableStateOf(PreferencesHelper.watchdogAggressive) }
    var intervalMs by remember { mutableFloatStateOf(PreferencesHelper.watchdogIntervalMs.toFloat()) }
    var showInfo by remember { mutableStateOf(false) }

    SettingsCard(title = "Watchdog") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable watchdog", style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Default.Info, contentDescription = "Info")
            }
        }

        Switch(
            checked = watchdogEnabled,
            onCheckedChange = {
                watchdogEnabled = it
                PreferencesHelper.watchdogEnabled = it
                RefreshRateWatchdogService.restart(context)
            }
        )

        if (watchdogEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow(
                label = "Aggressive mode",
                description = "Check every 500ms (uses more battery)",
                checked = aggressive,
                onCheckedChange = {
                    aggressive = it
                    PreferencesHelper.watchdogAggressive = it
                    RefreshRateWatchdogService.restart(context)
                }
            )

            if (!aggressive) {
                Text(
                    text = "Check interval: ${(intervalMs / 1000).toInt()}s",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Slider(
                    value = intervalMs,
                    onValueChange = { intervalMs = it },
                    onValueChangeFinished = {
                        PreferencesHelper.watchdogIntervalMs = intervalMs.toLong()
                        RefreshRateWatchdogService.restart(context)
                    },
                    valueRange = 1000f..30000f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Watchdog") },
            text = {
                Text(
                    "The watchdog service monitors your current refresh rate and re-applies " +
                    "your desired rate when the system overrides it. " +
                    "Keep this enabled if your refresh rate keeps changing automatically."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AppMonitorSection() {
    val context = LocalContext.current
    var monitorEnabled by remember { mutableStateOf(PreferencesHelper.appMonitorEnabled) }
    var showProfiles by remember { mutableStateOf(false) }

    // Re-check accessibility status reactively
    val isAccessibilityEnabled by remember(monitorEnabled) {
        mutableStateOf(AppMonitorService.isEnabled(context))
    }

    SettingsCard(title = "App Monitor") {
        Text(
            text = "Requires Accessibility Service",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        ToggleRow(
            label = "Enable app monitor",
            description = "Apply per-app refresh rate profiles",
            checked = monitorEnabled,
            onCheckedChange = {
                monitorEnabled = it
                PreferencesHelper.appMonitorEnabled = it
                if (it && !AppMonitorService.isEnabled(context)) {
                    AppMonitorService.openSettings(context)
                }
            }
        )

        if (!isAccessibilityEnabled && monitorEnabled) {
            Text(
                text = "Accessibility service not enabled. Tap to configure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = { AppMonitorService.openSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }
        }

        OutlinedButton(
            onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow background activity")
        }

        if (monitorEnabled && isAccessibilityEnabled) {
            OutlinedButton(
                onClick = { showProfiles = !showProfiles },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showProfiles) "Hide profiles" else "Manage per-app profiles")
            }
        }

        if (showProfiles) {
            PerAppProfilesSection()
        }
    }
}

@Composable
private fun PerAppProfilesSection() {
    val profiles = remember { PreferencesHelper.getAllAppProfiles().toSortedMap() }
    var newPackage by remember { mutableStateOf("") }
    var newRate by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (profiles.isNotEmpty()) {
            profiles.forEach { (pkg, rate) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$pkg: ${rate.toInt()} Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        PreferencesHelper.removeAppProfile(pkg)
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Text("Add new profile", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

        TextField(
            value = newPackage,
            onValueChange = { newPackage = it },
            label = { Text("Package name (e.g., com.android.chrome)") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = newRate,
            onValueChange = { newRate = it },
            label = { Text("Refresh rate (e.g., 120)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val rate = newRate.toFloatOrNull()
                if (newPackage.isNotBlank() && rate != null && rate > 0) {
                    PreferencesHelper.setAppProfile(newPackage, rate)
                    newPackage = ""
                    newRate = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = newPackage.isNotBlank() && newRate.toFloatOrNull() != null
        ) {
            Text("Add profile")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSection() {
    val oemNames = OemSettingsStrategy.getSupportedOemNames()
    var selectedOem by remember { mutableStateOf(PreferencesHelper.oemOverride.ifBlank { "Auto-detect" }) }
    var expanded by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }

    SettingsCard(title = "Advanced") {
        Text(
            text = "OEM Override",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedOem,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                oemNames.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedOem = name
                            PreferencesHelper.oemOverride = if (name == "Auto-detect") "" else name
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showDebug = !showDebug },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showDebug) "Hide debug info" else "Show debug info")
        }

        if (showDebug) {
            DebugInfoSection()
        }
    }
}

@Composable
private fun DebugInfoSection() {
    val context = LocalContext.current
    val strategy = OemSettingsStrategy.resolve()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Detected OEM: ${android.os.Build.MANUFACTURER}", style = MaterialTheme.typography.bodySmall)
            Text("Brand: ${android.os.Build.BRAND}", style = MaterialTheme.typography.bodySmall)
            Text("Model: ${android.os.Build.MODEL}", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Read keys:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            strategy.readKeys.forEach {
                Text("  ${it.namespace.name.lowercase()}/${it.key}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Write keys:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            strategy.writeKeys.forEach {
                Text("  ${it.namespace.name.lowercase()}/${it.key}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Lock keys:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            strategy.lockKeys.forEach {
                Text("  ${it.namespace.name.lowercase()}/${it.key}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SettingsCard(title = "About") {
        TextButton(onClick = { uriHandler.openUri(WEBSITE_URL) }) {
            Text("Made by anlaki")
        }

        OutlinedButton(
            onClick = { uriHandler.openUri(SOURCE_CODE_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Source code")
        }

        Button(
            onClick = { uriHandler.openUri(DONATION_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Donate on Ko-fi")
        }

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedButton(
            onClick = { uriHandler.openUri(LATEST_RELEASE_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check for updates")
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}