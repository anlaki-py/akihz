package akihz.anlaki.dev.presentation

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.view.WindowCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import akihz.anlaki.dev.data.DisplayManagerDataSource
import akihz.anlaki.dev.data.RefreshRateRepository
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private lateinit var refreshRateRepository: RefreshRateRepository
    private var isServiceBound = false
    private var currentRate: Float? = null
    private var selectedRate by mutableStateOf<Float?>(null)
    private var supportedRates by mutableStateOf<List<Float>>(emptyList())

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isServiceBound = false
        showError("Shizuku service died. Please restart Shizuku.")
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindUserServiceAndLoad()
            } else {
                showError("Shizuku permission denied.")
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshRateRepository = RefreshRateRepository(DisplayManagerDataSource(this))
        setContent {
            AnlakiTheme {
                val darkTheme = isSystemInDarkTheme()
                val backgroundColor = MaterialTheme.colorScheme.background
                SideEffect {
                    window.statusBarColor = backgroundColor.toArgb()
                    window.navigationBarColor = backgroundColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    RefreshRateScreen(
                        supportedRates = supportedRates,
                        currentRate = currentRate,
                        selectedRate = selectedRate,
                        onRateSelected = { hz -> onRateSelected(hz) }
                    )
                }
            }
        }
    }

    private fun onRateSelected(hz: Float) {
        if (!isServiceBound || !ShizukuHelper.hasPermission()) return

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(hz)
            }

            result.onSuccess {
                currentRate = hz
                selectedRate = hz
                Toast.makeText(this@MainActivity, "${hz.toInt()} Hz", Toast.LENGTH_SHORT).show()
            }.onError { _, message ->
                showError(message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ShizukuHelper.isBinderReady()) {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
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
        scope.cancel()
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
        if (isServiceBound) {
            loadSupportedRates()
            loadCurrentRate()
            return
        }

        ShizukuHelper.bindUserService(
            onConnected = {
                isServiceBound = true
                loadSupportedRates()
                loadCurrentRate()
            },
            onFailed = { _, message ->
                showError(message)
            }
        )
    }

    private fun loadSupportedRates() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.getSupportedRates()
            }
            result.onSuccess { rates ->
                supportedRates = rates
            }.onError { _, message ->
                showError("Failed to detect supported rates: $message")
            }
        }
    }

    private fun loadCurrentRate() {
        if (!isServiceBound) return

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.getCurrentRate()
            }

            result.onSuccess { rate ->
                currentRate = rate
                selectedRate = rate
            }
        }
    }

    private fun showShizukuNotInstalledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Shizuku Required")
            .setMessage("This app requires Shizuku to function.\n\n1. Install Shizuku\n2. Open Shizuku and follow setup\n3. Return to this app")
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
