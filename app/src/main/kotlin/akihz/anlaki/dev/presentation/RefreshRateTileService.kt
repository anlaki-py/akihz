package akihz.anlaki.dev.presentation

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
import akihz.anlaki.dev.utils.TileFeedbackNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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
        // Clear any leftover tile feedback notification from previous builds (user wants tile notice, not shade notification).
        TileFeedbackNotification.cancel(this)
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
        Timber.d("Tile onClick isBinderReady=${ShizukuHelper.isBinderReady()} hasPerm=${ShizukuHelper.hasPermission()} bound=${ShizukuHelper.isUserServiceBound()} connecting=$isConnecting switching=$isSwitching")

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
            Timber.d("Tile click ignored: connecting or switching")
            return
        }

        if (!ShizukuHelper.isUserServiceBound()) {
            bindAndCycleRate()
        } else {
            cycleRate()
        }
    }

    private fun bindAndCycleRate() {
        isConnecting = true
        updateTileConnecting()
        Timber.d("Tile bindAndCycleRate: requesting Shizuku user service")

        ShizukuHelper.acquireUserService(
            owner = SHIZUKU_OWNER,
            onConnected = {
                scope.launch {
                    isConnecting = false
                    Timber.d("Tile Shizuku connected, cycling rate")
                    cycleRate()
                }
            },
            onFailed = { _, message ->
                isConnecting = false
                Timber.w("Tile Shizuku connect failed: $message")
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
        updateTileSwitching(newRate)
        Timber.d("Tile cycleRate: switching to $newRate from $previousRate")

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                refreshRateRepository.setRate(newRate)
            }

            result.onSuccess {
                isSwitching = false
                updateTileWithRate(newRate)
                Timber.d("Tile cycleRate success: $newRate")
            }.onError { _, msg ->
                displayedRate = previousRate
                isSwitching = false
                updateTile()
                showToast(msg)
                Timber.w("Tile cycleRate failed: $msg")
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

    private fun updateTileConnecting() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = "Connecting..."
        tile.icon = Icon.createWithResource(this, R.drawable.ic_refresh_rate)
        tile.updateTile()
    }

    private fun updateTileSwitching(rate: Float) {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = "Switching to ${rate.roundToInt()} Hz..."
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
