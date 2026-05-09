package akihz.anlaki.dev.presentation

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.data.OemSettingsStrategy
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceLayout
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.RefreshRateWatchdogService

private const val WEBSITE_URL = "https://anlaki.dev"
private const val SOURCE_CODE_URL = "https://github.com/anlaki-py/akihz"
private const val DONATION_URL = "https://ko-fi.com/unluky"
private const val LATEST_RELEASE_URL = "https://github.com/anlaki-py/akihz/releases/latest"

/**
 * Settings screen with clean, simple layout.
 */
@Composable
fun SettingsScreen() {
    PreferenceLayout(label = "Settings") {
        WatchdogSection()
        AdvancedSection()
        AboutSection()
    }
}



@Composable
private fun WatchdogSection() {
    val context = LocalContext.current
    var watchdogEnabled by remember { mutableStateOf(PreferencesHelper.watchdogEnabled) }
    var aggressive by remember { mutableStateOf(PreferencesHelper.watchdogAggressive) }
    var intervalMs by remember { mutableFloatStateOf(PreferencesHelper.watchdogIntervalMs.toFloat()) }
    var showInfo by remember { mutableStateOf(false) }

    PreferenceGroup(heading = "Watchdog") {
        PreferenceTemplate(
            title = "Enable watchdog",
            description = "Monitor and re-apply refresh rate automatically",
            checked = watchdogEnabled,
            onCheckedChange = {
                watchdogEnabled = it
                PreferencesHelper.watchdogEnabled = it
                RefreshRateWatchdogService.restart(context)
            }
        )

        if (watchdogEnabled) {
            PreferenceTemplate(
                title = "Aggressive mode",
                description = "Check every 500ms (uses more battery)",
                checked = aggressive,
                onCheckedChange = {
                    aggressive = it
                    PreferencesHelper.watchdogAggressive = it
                    RefreshRateWatchdogService.restart(context)
                }
            )

            if (!aggressive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Check interval: ${(intervalMs / 1000).toInt()}s",
                    style = MaterialTheme.typography.bodyMedium
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
                    "your desired rate when the system overrides it."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSection() {
    val oemNames = OemSettingsStrategy.getSupportedOemNames()
    var selectedOem by remember { mutableStateOf(PreferencesHelper.oemOverride.ifBlank { "Auto-detect" }) }
    var showDebug by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    PreferenceGroup(heading = "Advanced") {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "OEM Override",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
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
        }

        PreferenceTemplate(
            title = if (showDebug) "Hide debug info" else "Show debug info",
            onClick = { showDebug = !showDebug }
        )

        if (showDebug) {
            DebugInfoSection()
        }
    }
}

@Composable
private fun DebugInfoSection() {
    val strategy = OemSettingsStrategy.resolve()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val pm = context.packageManager
    val packageInfo = remember {
        try {
            pm.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: BuildConfig.VERSION_NAME

    PreferenceGroup(heading = "About") {
        Text(
            text = "akihz",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        PreferenceTemplate(
            title = "Website",
            description = "anlaki.dev",
            onClick = { uriHandler.openUri(WEBSITE_URL) }
        )

        PreferenceTemplate(
            title = "Source code",
            description = "github.com/anlaki-py/akihz",
            onClick = { uriHandler.openUri(SOURCE_CODE_URL) }
        )

        PreferenceTemplate(
            title = "Donate",
            description = "ko-fi.com/unluky",
            onClick = { uriHandler.openUri(DONATION_URL) }
        )

        PreferenceTemplate(
            title = "Check for updates",
            onClick = { uriHandler.openUri(LATEST_RELEASE_URL) }
        )
    }
}
