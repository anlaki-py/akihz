package akihz.anlaki.dev.presentation

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.RefreshRateWatchdogService
import rikka.shizuku.Shizuku
import kotlinx.coroutines.flow.MutableStateFlow

private data class AppDialog(
    val title: String,
    val message: String,
    val cancelable: Boolean
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
        private const val SHIZUKU_OWNER = "main_activity"
    }

    private val viewModel: MainViewModel by viewModels()
    private var isServiceBound = false
    private val dialogState = MutableStateFlow<AppDialog?>(null)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isServiceBound = false
        showError("Shizuku service died. Please restart Shizuku.")
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU && grantResult == PackageManager.PERMISSION_GRANTED) {
            bindUserServiceAndLoad()
        } else if (requestCode == REQUEST_CODE_SHIZUKU) {
            showError("Shizuku permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        PreferencesHelper.init(this)
        RefreshRateWatchdogService.start(this)

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(PreferencesHelper.themeMode) }
            var amoledMode by rememberSaveable { mutableStateOf(PreferencesHelper.amoledMode) }
            var blurEnabled by rememberSaveable { mutableStateOf(PreferencesHelper.blurEnabled) }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.System -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            AnlakiTheme(
                darkTheme = darkTheme,
                pitchBlackTheme = amoledMode
            ) {
                val backgroundColor = MaterialTheme.colorScheme.background
                SideEffect {
                    window.statusBarColor = backgroundColor.toArgb()
                    window.navigationBarColor = backgroundColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                val uiState by viewModel.uiState.collectAsState()
                val appDialog by dialogState.collectAsState()

                AkihzApp(
                    uiState = uiState,
                    onRateSelected = {
                        viewModel.selectRate(it) {
                            RefreshRateWatchdogService.start(this)
                        }
                    },
                    onResetToDefaults = {
                        viewModel.resetToDefaults {
                            RefreshRateWatchdogService.stop(this)
                        }
                    },
                    onCustomProfileChanged = viewModel::onCustomProfileChanged,
                    themeMode = themeMode,
                    amoledMode = amoledMode,
                    blurEnabled = blurEnabled,
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        PreferencesHelper.themeMode = mode
                    },
                    onAmoledModeChanged = { enabled ->
                        amoledMode = enabled
                        PreferencesHelper.amoledMode = enabled
                    },
                    onBlurEnabledChanged = { enabled ->
                        blurEnabled = enabled
                        PreferencesHelper.blurEnabled = enabled
                    },
                    onErrorDismissed = { viewModel.onErrorDismissed() }
                )
                appDialog?.let { dialog ->
                    AlertDialog(
                        onDismissRequest = {
                            if (dialog.cancelable) dialogState.value = null
                        },
                        title = { Text(dialog.title) },
                        text = { Text(dialog.message) },
                        confirmButton = {
                            TextButton(onClick = { dialogState.value = null }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        checkShizukuPermission()
    }

    override fun onPause() {
        super.onPause()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.releaseUserService(SHIZUKU_OWNER)
    }

    private fun checkShizukuPermission() {
        if (!ShizukuHelper.isBinderReady()) {
            showShizukuNotInstalledDialog()
            return
        }

        if (ShizukuHelper.hasPermission()) {
            bindUserServiceAndLoad()
        } else {
            ShizukuHelper.requestPermission(REQUEST_CODE_SHIZUKU)
        }
    }

    private fun bindUserServiceAndLoad() {
        if (isServiceBound) return

        ShizukuHelper.acquireUserService(
            owner = SHIZUKU_OWNER,
            onConnected = {
                isServiceBound = true
                viewModel.onShizukuBound()
            },
            onFailed = { _, message ->
                showError(message)
            }
        )
    }

    private fun showShizukuNotInstalledDialog() {
        showDialog("Shizuku Required", "This app requires Shizuku to function.\n\n1. Install Shizuku\n2. Open Shizuku and follow setup\n3. Return to this app", false)
    }

    private fun showError(message: String) = showDialog("Error", message)

    private fun showDialog(title: String, message: String, cancelable: Boolean = true) {
        dialogState.value = AppDialog(
            title = title,
            message = message,
            cancelable = cancelable
        )
    }
}
