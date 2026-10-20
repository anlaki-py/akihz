package akihz.anlaki.dev.presentation

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.ContextCompat
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.fps.FpsMonitorService

/**
 * Quick Settings tile for toggling Surface FPS monitoring.
 *
 * Icon is the text "FPS" rendered as a bitmap mask. Label stays static,
 * subtitle shows state.
 */
class FpsMonitorTileService : TileService() {

    private var isStarting = false
    private val handler = Handler(Looper.getMainLooper())

    /** Refreshes the tile state when the shade opens. */
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    /** Starts or stops monitoring after checks, then refreshes the tile. */
    override fun onClick() {
        super.onClick()

        if (isStarting) return

        // If not running, check prerequisites before starting.
        if (!FpsMonitorService.isRunning.value) {
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
            if (!FpsMonitorService.start(applicationContext)) {
                showToast("Could not start FPS monitor")
                updateTile()
                return
            }
            isStarting = true
            showToast("FPS monitoring starting")
            updateTile()
            handler.postDelayed({
                isStarting = false
                if (!FpsMonitorService.isRunning.value) {
                    showToast("FPS monitor did not start")
                }
                updateTile()
            }, START_CHECK_DELAY_MS)
        } else {
            FpsMonitorService.stop(applicationContext)
            showToast("FPS monitoring stopped")
            updateTile()
            handler.postDelayed({ updateTile() }, STOP_CHECK_DELAY_MS)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isRunning = FpsMonitorService.isRunning.value
        tile.label = "FPS Monitor"
        when {
            isRunning -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = "On - tap to stop"
                tile.icon = createFpsTileIcon()
            }
            isStarting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = "Starting…"
                tile.icon = createFpsTileIcon()
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = "Off - tap to start"
                tile.icon = createFpsTileIconInactive()
            }
        }
        // If prerequisites are missing before start, show the hint. Keep stop reachable while running.
        if (!isRunning && !isStarting &&
            (!ShizukuHelper.isBinderReady() || !ShizukuHelper.hasPermission())
        ) {
            tile.subtitle = "Open app first"
            tile.state = Tile.STATE_UNAVAILABLE
        }
        tile.updateTile()
    }

    /** Stops pending tile refreshes when the service goes away. */
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "akihz.intent.OPEN_FPS_MONITOR"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_OPEN_FPS_MONITOR, true)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 12, intent,
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

    private companion object {
        const val START_CHECK_DELAY_MS = 1000L
        const val STOP_CHECK_DELAY_MS = 500L
    }
}
