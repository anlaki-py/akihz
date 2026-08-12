package akihz.anlaki.dev.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import akihz.anlaki.dev.data.UpdateCheckScheduler
import akihz.anlaki.dev.domain.update.UpdateCheckFrequency
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.UpdateAvailableNotification

/** Displays and applies the automatic update-check interval preference. */
@Composable
internal fun AutomaticUpdateCheckPreference() {
    val context = LocalContext.current
    var frequency by remember { mutableStateOf(PreferencesHelper.updateCheckFrequency) }
    var showChoices by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            message = "Automatic checks will continue, but Android will not show update alerts."
        }
    }

    fun applyFrequency(selected: UpdateCheckFrequency) {
        frequency = selected
        PreferencesHelper.updateCheckFrequency = selected
        UpdateCheckScheduler.schedule(context, selected)
        showChoices = false
        if (selected == UpdateCheckFrequency.Never) return

        val runtimePermissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        when {
            runtimePermissionMissing -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !UpdateAvailableNotification.canNotify(context) -> {
                message = "Automatic checks are enabled, but notifications are disabled in Android settings."
            }
        }
    }

    val notificationNote = if (
        frequency != UpdateCheckFrequency.Never &&
        !UpdateAvailableNotification.canNotify(context)
    ) {
        " · notifications disabled"
    } else {
        ""
    }
    PreferenceTemplate(
        title = "Automatic checks",
        description = frequency.label + notificationNote,
        icon = Icons.Default.Schedule,
        onClick = { showChoices = true }
    )
    if (showChoices) {
        UpdateFrequencyDialog(
            selected = frequency,
            onSelect = ::applyFrequency,
            onDismiss = { showChoices = false }
        )
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Update notifications") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("OK") }
            }
        )
    }
}
