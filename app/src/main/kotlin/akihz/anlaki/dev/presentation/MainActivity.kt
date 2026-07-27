package akihz.anlaki.dev.presentation

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.RefreshRateWatchdogService
import rikka.shizuku.Shizuku

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private val viewModel: MainViewModel by viewModels()
    private var isServiceBound = false

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

        PreferencesHelper.init(this)
        RefreshRateWatchdogService.start(this)

        setContent {
            AnlakiTheme {
                val darkTheme = isSystemInDarkTheme()
                val backgroundColor = MaterialTheme.colorScheme.background
                SideEffect {
                    window.statusBarColor = backgroundColor.toArgb()
                    window.navigationBarColor = backgroundColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
                }
                val uiState by viewModel.uiState.collectAsState()

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
                    onErrorDismissed = { viewModel.onErrorDismissed() }
                )
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
        ShizukuHelper.unbindUserService()
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

        ShizukuHelper.bindUserService(
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
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setCancelable(cancelable)
            .show()
    }
}
