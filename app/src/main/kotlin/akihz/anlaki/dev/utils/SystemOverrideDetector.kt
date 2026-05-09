package akihz.anlaki.dev.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Detects system conditions that may override refresh rate settings.
 *
 * Monitors:
 * - Battery saver state (forces 60Hz on most devices)
 * - Device thermal status (may throttle refresh rate)
 * - Screen on/off state
 * - Power save mode changes
 */
class SystemOverrideDetector(private val context: Context) {

    private val _events = Channel<SystemEvent>(Channel.BUFFERED)
    val events: Flow<SystemEvent> = _events.receiveAsFlow()

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

                    // Thermal throttling heuristic: battery temp > 420 (42.0C)
                    if (temperature > 420) {
                        _events.trySend(SystemEvent.ThermalThrottling(temperature / 10f))
                    }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    _events.trySend(SystemEvent.PowerSaveChanged(isPowerSaveMode()))
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> _events.trySend(SystemEvent.ScreenOn)
                Intent.ACTION_SCREEN_OFF -> _events.trySend(SystemEvent.ScreenOff)
            }
        }
    }

    /**
     * Registers all receivers. Call from a component with appropriate lifecycle.
     */
    fun register() {
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(batteryReceiver, batteryFilter)

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, screenFilter)
    }

    /**
     * Unregisters all receivers.
     */
    fun unregister() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
            // Receiver may not be registered
        }
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
            // Receiver may not be registered
        }
    }

    /**
     * Checks if battery saver / power save mode is currently active.
     */
    fun isPowerSaveMode(): Boolean = powerManager.isPowerSaveMode

    /**
     * Checks if the device is currently in an interactive state (screen on and user present).
     */
    fun isInteractive(): Boolean = powerManager.isInteractive

    sealed class SystemEvent {
        data class PowerSaveChanged(val enabled: Boolean) : SystemEvent()
        data class ThermalThrottling(val temperatureCelsius: Float) : SystemEvent()
        data object ScreenOn : SystemEvent()
        data object ScreenOff : SystemEvent()
    }
}