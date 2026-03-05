package akihz.anlaki.dev.presentation

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import akihz.anlaki.dev.R
import akihz.anlaki.dev.data.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private var isServiceBound = false
    private var currentRate: Float? = null
    private lateinit var ui: RefreshRateUI

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = RefreshRateUI(this) { hz -> onRateSelected(hz) }
        setContentView(ui.createUI())
    }

    private fun onRateSelected(hz: Float) {
        if (!isServiceBound || !ShizukuHelper.hasPermission()) return
        
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                ShizukuHelper.setRefreshRate(hz)
            }
            
            if (result.isSuccess) {
                currentRate = hz
                ui.updateCurrentRate(hz)
                ui.highlightButton(hz)
                Toast.makeText(this@MainActivity, "${hz.toInt()} Hz", Toast.LENGTH_SHORT).show()
            } else {
                showError("Failed to set refresh rate.")
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
            loadCurrentRate()
            return
        }

        ShizukuHelper.bindUserService(
            onConnected = {
                isServiceBound = true
                Handler(Looper.getMainLooper()).post { loadCurrentRate() }
            },
            onFailed = { _, message ->
                Handler(Looper.getMainLooper()).post { showError(message) }
            }
        )
    }

    private fun loadCurrentRate() {
        if (!isServiceBound) return
        
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                ShizukuHelper.getCurrentRefreshRate()
            }
            
            if (result.isSuccess) {
                val rate = result.getOrNull()!!
                currentRate = rate
                ui.updateCurrentRate(rate)
                ui.highlightButton(rate)
            } else {
                ui.currentRateText.text = "Current: -- Hz"
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
