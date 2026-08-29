package akihz.anlaki.dev.presentation

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import akihz.anlaki.dev.R
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.domain.TileRateSelection
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
    private var tileRates: List<Float> = emptyList()
    private var displayedRate: Float? = null
    private var isConnecting = false
    private var isSwitching = false

    @Inject lateinit var refreshRateRepository: RefreshRateRepository

    override fun onStartListening() {
        super.onStartListening()
        PreferencesHelper.init(applicationContext)
        if (PreferencesHelper.keepAliveEnabled) {
            KeepAliveService.start(applicationContext)
        }
        loadSupportedRatesAndRestore()
    }

    private fun loadSupportedRatesAndRestore() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.getSupportedRates()
            }

            result.onSuccess { rates ->
                val excludedRates = TileRateSelection.recoverEmptySelection(
                    rates,
                    PreferencesHelper.excludedTileRates
                )
                tileRates = TileRateSelection.includedRates(rates, excludedRates)
                val savedRate = PreferencesHelper.lastRate
                displayedRate = tileRates.firstOrNull {
                    kotlin.math.abs(it - savedRate) < 0.01f
                }
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
        if (tileRates.isEmpty()) {
            showToast("No supported refresh rates detected")
            return
        }

        val previousRate = displayedRate
        val cycleAnchor = displayedRate ?: PreferencesHelper.lastRate
        val newRate = TileRateSelection.nextRate(tileRates, cycleAnchor) ?: return
        displayedRate = newRate
        isSwitching = true
        updateTileWithRate(newRate)

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(newRate)
            }

            result.onSuccess {
                isSwitching = false
            }.onError { _, msg ->
                displayedRate = previousRate
                isSwitching = false
                updateTile()
                showToast(msg)
            }
        }
    }

    private fun updateTile() {
        val rate = displayedRate
        if (rate != null) {
            updateTileWithRate(rate)
        } else if (tileRates.isNotEmpty()) {
            updateTileReady()
        } else {
            updateTileUnavailable()
        }
    }

    private fun updateTileReady() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = "Tap to switch"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_refresh_rate)
        tile.updateTile()
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
