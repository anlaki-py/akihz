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
import akihz.anlaki.dev.utils.FpsMonitorService
import akihz.anlaki.dev.utils.PreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FpsMonitorUiState(
    val isRunning: Boolean = false,
    val targetPackage: String? = null,
    val targetLabel: String? = null,
    val overlayScale: Int = 100,
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
    }

    fun refresh() {
        PreferencesHelper.init(appContext)
        _uiState.update {
            it.copy(
                isRunning = PreferencesHelper.fpsRunning,
                targetPackage = PreferencesHelper.fpsTargetPackage,
                targetLabel = PreferencesHelper.fpsTargetLabel,
                overlayScale = PreferencesHelper.fpsOverlayScale,
                selectedLayer = PreferencesHelper.fpsSelectedLayer,
                debugLoggingEnabled = PreferencesHelper.fpsDebugLoggingEnabled,
                debugLog = PreferencesHelper.fpsDebugLog
            )
        }
    }

    fun startService(context: Context) {
        PreferencesHelper.init(context)
        PreferencesHelper.fpsRunning = true
        FpsMonitorService.start(context)
        _uiState.update { it.copy(isRunning = true, message = "Monitoring started") }
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            refresh()
        }
    }

    fun stopService(context: Context) {
        PreferencesHelper.init(context)
        PreferencesHelper.fpsRunning = false
        FpsMonitorService.stop(context)
        _uiState.update { it.copy(isRunning = false, message = "Monitoring stopped") }
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            refresh()
        }
    }

    /** Legacy wrapper kept for call sites; delegates to [startService]. */
    fun startMonitoring(context: Context): Boolean {
        startService(context)
        return true
    }

    fun stopMonitoring(context: Context) {
        stopService(context)
    }

    fun hasShizukuBinder(): Boolean = ShizukuHelper.isBinderReady()
    fun hasShizukuPermission(): Boolean = ShizukuHelper.hasPermission()
    fun requestShizukuPermission() = ShizukuHelper.requestPermission(1001)
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun setTarget(packageName: String, label: String) {
        PreferencesHelper.fpsTargetPackage = packageName
        PreferencesHelper.fpsTargetLabel = label
        viewModelScope.launch {
            appContext.startService(
                Intent(appContext, FpsMonitorService::class.java).apply {
                    action = FpsMonitorService.ACTION_NOTE
                    putExtra(FpsMonitorService.EXTRA_NOTE, "fixed target selected: $label ($packageName)")
                }
            )
        }
        refresh()
        _uiState.update { it.copy(message = "Target: $label") }
    }

    fun clearTarget() {
        PreferencesHelper.clearFpsTarget()
        viewModelScope.launch {
            appContext.startService(
                Intent(appContext, FpsMonitorService::class.java).apply {
                    action = FpsMonitorService.ACTION_NOTE
                    putExtra(FpsMonitorService.EXTRA_NOTE, "automatic app detection selected")
                }
            )
        }
        refresh()
        _uiState.update { it.copy(message = "Automatic detection enabled") }
    }

    fun setOverlayScale(scale: Int) {
        val clamped = scale.coerceIn(50, 200)
        PreferencesHelper.fpsOverlayScale = clamped
        _uiState.update { it.copy(overlayScale = clamped) }
        appContext.startService(
            Intent(appContext, FpsMonitorService::class.java).apply {
                action = FpsMonitorService.ACTION_SET_SCALE
                putExtra(FpsMonitorService.EXTRA_SCALE, clamped)
            }
        )
    }

    fun clearSelectedLayer() {
        PreferencesHelper.fpsSelectedLayer = null
        _uiState.update { it.copy(selectedLayer = null) }
        appContext.startService(
            Intent(appContext, FpsMonitorService::class.java).apply {
                action = FpsMonitorService.ACTION_SET_LAYER
                putExtra(FpsMonitorService.EXTRA_LAYER, null as String?)
            }
        )
        _uiState.update { it.copy(message = "Layer: Auto") }
    }

    fun toggleDebugLogging() {
        val enabled = !PreferencesHelper.fpsDebugLoggingEnabled
        PreferencesHelper.fpsDebugLoggingEnabled = enabled
        appContext.startService(
            Intent(appContext, FpsMonitorService::class.java).apply {
                action = FpsMonitorService.ACTION_SET_LOGGING
                putExtra(FpsMonitorService.EXTRA_ENABLED, enabled)
            }
        )
        refresh()
        _uiState.update { it.copy(message = "Debug logging ${if (enabled) "enabled" else "disabled"}") }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
