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
    private var isSwitching = false

    @Inject lateinit var refreshRateRepository: RefreshRateRepository

    override fun onStartListening() {
        super.onStartListening()
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

        if (isConnecting || isSwitching) {
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

        ShizukuHelper.acquireUserService(
            owner = SHIZUKU_OWNER,
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

        val previousIndex = currentIndex
        currentIndex = (currentIndex + 1) % supportedRates.size
        val newRate = supportedRates[currentIndex]
        isSwitching = true
        updateTileWithRate(newRate)

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(newRate)
            }

            result.onSuccess {
                PreferencesHelper.saveState(currentIndex, newRate)
                akihz.anlaki.dev.utils.RefreshRateWatchdogService.start(applicationContext)
                isSwitching = false
            }.onError { _, msg ->
                currentIndex = previousIndex
                isSwitching = false
                updateTile()
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

        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = "${rate.roundToInt()} Hz"
        tile.icon = createRefreshRateTileIcon(rate)
        tile.updateTile()
    }

    private fun updateTileUnavailable() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_UNAVAILABLE
        tile.label = getString(R.string.app_name)
        tile.subtitle = "Open app first"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_refresh_rate)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.releaseUserService(SHIZUKU_OWNER)
        scope.cancel()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val SHIZUKU_OWNER = "quick_settings_tile"
    }
}
