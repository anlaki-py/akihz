package akihz.anlaki.dev.presentation

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import akihz.anlaki.dev.R
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.KeepAliveService
import akihz.anlaki.dev.utils.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import javax.inject.Inject

@AndroidEntryPoint
class RefreshRateTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var supportedRates: List<Float> = emptyList()
    private var currentIndex = 0
    private var isConnecting = false

    @Inject lateinit var refreshRateRepository: RefreshRateRepository

    override fun onStartListening() {
        super.onStartListening()
        KeepAliveService.start(applicationContext)
        PreferencesHelper.init(applicationContext)
        loadSupportedRatesAndRestore()
    }

    private fun loadSupportedRatesAndRestore() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.getSupportedRates()
            }

            result.onSuccess { rates ->
                supportedRates = rates
                val savedRate = PreferencesHelper.lastRate
                currentIndex = rates.indexOfFirst { kotlin.math.abs(it - savedRate) < 1f }
                    .takeIf { it >= 0 } ?: 0
                updateTile()
            }.onError { _, _ ->
                updateTileUnavailable()
            }
        }
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

        ShizukuHelper.bindUserService(
            onConnected = {
                scope.launch {
                    cycleRate()
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

    private fun cycleRate() {
        if (supportedRates.isEmpty()) {
            showToast("No supported refresh rates detected")
            return
        }

        currentIndex = (currentIndex + 1) % supportedRates.size
        val newRate = supportedRates[currentIndex]

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(newRate)
            }

            result.onSuccess {
                PreferencesHelper.saveState(currentIndex, newRate)
                updateTileWithRate(newRate)
            }.onError { _, msg ->
                showToast(msg)
            }
        }
    }

    private fun updateTile() {
        val rate = supportedRates.getOrNull(currentIndex)
        if (rate != null) {
            updateTileWithRate(rate)
        } else {
            updateTileUnavailable()
        }
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

    private fun getIconRes(rate: Float): Int = when (rate.roundToInt()) {
        45 -> R.drawable.ic_rate_45
        60 -> R.drawable.ic_rate_60
        90 -> R.drawable.ic_rate_90
        120 -> R.drawable.ic_rate_120
        144 -> R.drawable.ic_rate_144
        165 -> R.drawable.ic_rate_165
        240 -> R.drawable.ic_rate_240
        else -> R.drawable.ic_refresh_rate
    }
}
