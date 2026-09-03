package akihz.anlaki.dev.presentation

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.ContextCompat
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.utils.FpsMonitorService
import akihz.anlaki.dev.utils.PreferencesHelper

/**
 * Quick Settings tile for toggling Surface FPS monitoring.
 *
 * Icon is the text "FPS" rendered as a bitmap mask. Label stays static,
 * subtitle shows state.
 */
class FpsMonitorTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        PreferencesHelper.init(applicationContext)
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        PreferencesHelper.init(applicationContext)

        // If not running, check prerequisites before starting.
        if (!PreferencesHelper.fpsRunning) {
            if (!ShizukuHelper.isBinderReady()) {
                showToast("Shizuku not running")
                updateTile()
                launchApp()
                return
            }
            if (!ShizukuHelper.hasPermission()) {
                showToast("Grant Shizuku permission in app")
                updateTile()
                launchApp()
                return
            }
            if (!Settings.canDrawOverlays(this)) {
                showToast("Grant overlay permission")
                updateTile()
                launchApp()
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                showToast("Grant notification permission")
                updateTile()
                launchApp()
                return
            }
            // Start monitoring
            FpsMonitorService.start(applicationContext)
            PreferencesHelper.fpsRunning = true
            showToast("FPS monitoring started")
        } else {
            FpsMonitorService.stop(applicationContext)
            PreferencesHelper.fpsRunning = false
            showToast("FPS monitoring stopped")
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isRunning = try {
            PreferencesHelper.init(applicationContext)
            PreferencesHelper.fpsRunning
        } catch (_: Exception) {
            false
        }
        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "FPS Monitor"
            tile.subtitle = "On - tap to stop"
            tile.icon = createFpsTileIcon()
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "FPS Monitor"
            tile.subtitle = "Off - tap to start"
            tile.icon = createFpsTileIconInactive()
        }
        // If prerequisites missing, show unavailable hint
        if (!ShizukuHelper.isBinderReady() || !ShizukuHelper.hasPermission()) {
            tile.subtitle = "Open app first"
            tile.state = Tile.STATE_UNAVAILABLE
        }
        tile.updateTile()
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_OPEN_FPS_MONITOR, true)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: Exception) {
            try {
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
