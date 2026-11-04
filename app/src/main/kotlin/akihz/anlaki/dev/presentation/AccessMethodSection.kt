package akihz.anlaki.dev.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import akihz.anlaki.dev.data.RootCommandService
import akihz.anlaki.dev.data.ShizukuConnection
import akihz.anlaki.dev.presentation.components.PreferenceGroup
import akihz.anlaki.dev.presentation.components.PreferenceTemplate
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private const val ACCESS_PERMISSION_REQUEST = 2002

/** Shows the saved access method and lets the user change it after granting access. */
@Composable
internal fun AccessMethodSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSelector by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rootSelected by remember { mutableStateOf(RootCommandService.enabled) }

    fun selectShizuku() {
        RootCommandService.useShizuku()
        rootSelected = false
        showSelector = false
        context.findActivity()?.recreate()
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, result ->
            if (requestCode == ACCESS_PERMISSION_REQUEST) {
                busy = false
                if (result == PackageManager.PERMISSION_GRANTED) selectShizuku()
                else error = "Shizuku permission was denied. Your access method has not changed."
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    PreferenceGroup(heading = "Access") {
        PreferenceTemplate(
            title = "Access method",
            description = if (rootSelected) "Root" else "Shizuku",
            onClick = { error = null; showSelector = true }
        )
    }
    if (showSelector) {
        AlertDialog(
            onDismissRequest = { if (!busy) showSelector = false },
            title = { Text("Access method") },
            text = {
                Column {
                    Text("Choose how akiHz gets privileged access. Your choice is saved.")
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val granted = withContext(Dispatchers.IO) { RootCommandService.request() }
                                    if (granted) {
                                        rootSelected = true
                                        showSelector = false
                                        context.findActivity()?.recreate()
                                    } else {
                                        error = "Root was denied or unavailable. Your access method has not changed."
                                    }
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    ) { Text(if (rootSelected) "Root (selected)" else "Root") }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            error = null
                            when {
                                !ShizukuConnection.isBinderReady() ->
                                    error = "Start Shizuku, then choose it here again."
                                ShizukuConnection.hasPermission() -> selectShizuku()
                                else -> {
                                    busy = true
                                    try {
                                        Shizuku.requestPermission(ACCESS_PERMISSION_REQUEST)
                                    } catch (exception: Exception) {
                                        busy = false
                                        error = "Could not request Shizuku permission. Try again."
                                        timber.log.Timber.w(exception, "Access method permission request failed")
                                    }
                                }
                            }
                        }
                    ) { Text(if (!rootSelected) "Shizuku (selected)" else "Shizuku") }
                    if (busy) Text("Waiting for permission...")
                    error?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { showSelector = false }) { Text("Close") }
            }
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
