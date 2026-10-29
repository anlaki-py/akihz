package akihz.anlaki.dev.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.presentation.fps.FpsPillStyle
import akihz.anlaki.dev.presentation.fps.FpsMonitorService
import akihz.anlaki.dev.data.PreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val REFRESH_DEBOUNCE_MS = 200L

data class FpsMonitorUiState(
    val isRunning: Boolean = false,
    val targetPackage: String? = null,
    val targetLabel: String? = null,
    val overlayStyle: FpsPillStyle = FpsPillStyle(),
    val selectedLayer: String? = null,
    val debugLoggingEnabled: Boolean = false,
    val debugLog: String = "",
    val message: String? = null
)

@HiltViewModel
class FpsMonitorViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FpsMonitorUiState())
    val uiState: StateFlow<FpsMonitorUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            FpsMonitorService.isRunning.collectLatest { running ->
                _uiState.update { it.copy(isRunning = running) }
            }
        }
        viewModelScope.launch {
            // Reload when the tile, the service, or the overlay panel writes FPS prefs.
            PreferencesHelper.preferenceChanges.collectLatest {
                delay(REFRESH_DEBOUNCE_MS)
                refresh()
            }
        }
    }

    /** Reloads saved prefs into the UI state. */
    fun refresh() {
        PreferencesHelper.init(appContext)
        _uiState.update {
            it.copy(
                isRunning = FpsMonitorService.isRunning.value,
                targetPackage = PreferencesHelper.fpsTargetPackage,
                targetLabel = PreferencesHelper.fpsTargetLabel,
                overlayStyle = FpsPillStyle.load(),
                selectedLayer = PreferencesHelper.fpsSelectedLayer,
                debugLoggingEnabled = PreferencesHelper.fpsDebugLoggingEnabled,
                debugLog = PreferencesHelper.fpsDebugLog
            )
        }
    }

    /**
     * Starts monitoring and marks it running.
     * @param context used to start the service
     */
    fun startService(context: Context) {
        if (!FpsMonitorService.start(context)) {
            _uiState.update { it.copy(isRunning = false, message = "Could not start FPS monitor") }
            return
        }
        _uiState.update { it.copy(message = "Starting FPS monitor…") }
        viewModelScope.launch {
            delay(1000)
            if (!FpsMonitorService.isRunning.value) {
                _uiState.update { it.copy(isRunning = false, message = "FPS monitor did not start") }
            }
        }
    }

    /**
     * Stops monitoring and waits for the service to report stopped.
     * @param context used to stop the service
     */
    fun stopService(context: Context) {
        if (!FpsMonitorService.isRunning.value) return
        FpsMonitorService.stop(context)
        _uiState.update { it.copy(message = "Stopping FPS monitor…") }
        viewModelScope.launch {
            delay(700)
            if (FpsMonitorService.isRunning.value) {
                _uiState.update { it.copy(isRunning = true, message = "Could not stop FPS monitor") }
            }
        }
    }

    /**
     * Returns true when the Shizuku binder is ready.
     * @return true when ready
     */
    fun hasShizukuBinder(): Boolean = ShizukuHelper.isBinderReady()
    /**
     * Returns true when Shizuku permission is granted.
     * @return true when granted
     */
    fun hasShizukuPermission(): Boolean = ShizukuHelper.hasPermission()
    /** Asks Shizuku for permission with request code 1001. */
    fun requestShizukuPermission() = ShizukuHelper.requestPermission(1001)
    /** Returns true when the app can draw overlays. */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
    /** Returns true when post notification permission is granted. */
    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Saves a fixed target app and notifies the running service. */
    fun setTarget(packageName: String, label: String) {
        PreferencesHelper.fpsTargetPackage = packageName
        PreferencesHelper.fpsTargetLabel = label
        sendIfRunning(FpsMonitorService.ACTION_NOTE) {
            putExtra(FpsMonitorService.EXTRA_NOTE, "fixed target selected: $label ($packageName)")
        }
        refresh()
        _uiState.update { it.copy(message = "Target: $label") }
    }

    /** Clears the fixed target and restores automatic detection. */
    fun clearTarget() {
        PreferencesHelper.clearFpsTarget()
        sendIfRunning(FpsMonitorService.ACTION_NOTE) {
            putExtra(FpsMonitorService.EXTRA_NOTE, "automatic app detection selected")
        }
        refresh()
        _uiState.update { it.copy(message = "Automatic detection enabled") }
    }

    /**
     * Saves the shared pill controls and applies them only if monitoring is running.
     * @param style appearance selected in the app's FPS settings
     */
    fun setOverlayStyle(style: FpsPillStyle) {
        style.save()
        _uiState.update { it.copy(overlayStyle = FpsPillStyle.load()) }
        sendIfRunning(FpsMonitorService.ACTION_APPLY_STYLE)
    }

    /**
     * Sends an intent to the monitor service only while it runs.
     *
     * Starting with Android 8, sending an intent to a stopped service creates
     * it, and our service starts sampling in `onCreate`. Unconditional sends
     * from style edits therefore boot monitoring while the user only changes
     * overlay looks. Prefs are already saved, so a later explicit start
     * picks the new values up.
     *
     * @param action service action to send
     * @param fill adds extras to the intent
     */
    private fun sendIfRunning(action: String, fill: Intent.() -> Unit = {}) {
        if (!FpsMonitorService.isRunning.value) return
        appContext.startService(
            Intent(appContext, FpsMonitorService::class.java).apply {
                this.action = action
                fill()
            }
        )
    }

    /** Clears the picked layer and restores automatic choice. */
    fun clearSelectedLayer() {
        PreferencesHelper.fpsSelectedLayer = null
        _uiState.update { it.copy(selectedLayer = null) }
        sendIfRunning(FpsMonitorService.ACTION_SET_LAYER) {
            putExtra(FpsMonitorService.EXTRA_LAYER, null as String?)
        }
        _uiState.update { it.copy(message = "Layer: Auto") }
    }

    /** Flips debug logging and pushes the state to the service. */
    fun toggleDebugLogging() {
        val enabled = !PreferencesHelper.fpsDebugLoggingEnabled
        PreferencesHelper.fpsDebugLoggingEnabled = enabled
        sendIfRunning(FpsMonitorService.ACTION_SET_LOGGING) {
            putExtra(FpsMonitorService.EXTRA_ENABLED, enabled)
        }
        refresh()
        _uiState.update { it.copy(message = "Debug logging ${if (enabled) "enabled" else "disabled"}") }
    }

    /** Clears the one shot UI message. */
    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
