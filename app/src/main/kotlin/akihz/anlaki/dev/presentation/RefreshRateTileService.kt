package akihz.anlaki.dev.presentation

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import akihz.anlaki.dev.R
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.utils.Constants
import akihz.anlaki.dev.utils.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RefreshRateTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentIndex = 0
    private var isConnecting = false

    override fun onStartListening() {
        super.onStartListening()
        PreferencesHelper.init(applicationContext)
        currentIndex = PreferencesHelper.currentIndex
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        if (!ShizukuHelper.isBinderReady()) {
            showToast("Shizuku not running")
            updateTileUnavailable()
            return
        }

        if (!ShizukuHelper.hasPermission()) {
            showToast("Grant Shizuku permission in app")
            updateTileUnavailable()
            return
        }

        if (isConnecting) {
            return
        }

        if (!ShizukuHelper.isUserServiceBound()) {
            showToast("Connecting...")
            bindAndCycleRate()
        } else {
            cycleRate()
        }
    }

    private fun bindAndCycleRate() {
        isConnecting = true
        
        scope.launch {
            var bindSuccess = false
            
            ShizukuHelper.bindUserService(
                onConnected = {
                    bindSuccess = true
                    scope.launch {
                        if (bindSuccess) {
                            cycleRate()
                        }
                        isConnecting = false
                    }
                },
                onFailed = { _, message ->
                    isConnecting = false
                    showToast(message)
                    updateTileUnavailable()
                }
            )
        }
    }

    private fun cycleRate() {
        currentIndex = (currentIndex + 1) % Constants.REFRESH_RATES.size
        val newRate = Constants.REFRESH_RATES[currentIndex]

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                ShizukuHelper.setRefreshRate(newRate)
            }

            if (result.isSuccess) {
                PreferencesHelper.saveState(currentIndex, newRate)
                updateTileWithRate(newRate)
            } else {
                showToast("Failed to change rate")
            }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val currentRate = Constants.REFRESH_RATES[currentIndex]
        val iconRes = getIconRes(currentRate)

        tile.state = Tile.STATE_ACTIVE
        tile.label = "akihz"
        tile.subtitle = "${currentRate.toInt()} Hz"
        tile.icon = Icon.createWithResource(this, iconRes)
        tile.updateTile()
    }

    private fun updateTileWithRate(rate: Float) {
        val tile = qsTile ?: return
        val iconRes = getIconRes(rate)

        tile.state = Tile.STATE_ACTIVE
        tile.label = "akihz"
        tile.subtitle = "${rate.toInt()} Hz"
        tile.icon = Icon.createWithResource(this, iconRes)
        tile.updateTile()
    }

    private fun updateTileUnavailable() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_UNAVAILABLE
        tile.label = "akihz"
        tile.subtitle = "Open app first"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_refresh_rate)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun getIconRes(rate: Float): Int = when (rate.toInt()) {
        60 -> R.drawable.ic_rate_60
        90 -> R.drawable.ic_rate_90
        120 -> R.drawable.ic_rate_120
        else -> R.drawable.ic_refresh_rate
    }
}
