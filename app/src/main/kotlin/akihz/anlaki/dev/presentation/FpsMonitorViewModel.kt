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
import akihz.anlaki.dev.data.fps.OverlayPillColor
import akihz.anlaki.dev.data.fps.OverlayStyle
import akihz.anlaki.dev.presentation.fps.FpsMonitorService
import akihz.anlaki.dev.data.PreferencesHelper
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
    val overlayAlpha: Int = OverlayStyle.ALPHA_DEFAULT,
    val showUnit: Boolean = true,
    val pillColor: OverlayPillColor = OverlayPillColor.Black,
    val rectShape: Boolean = false,
    val cornerRadius: Int = OverlayStyle.RECT_RADIUS_DP,
    val pillOutline: Boolean = true,
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
                overlayAlpha = PreferencesHelper.fpsOverlayAlpha,
                showUnit = PreferencesHelper.fpsShowUnit,
                pillColor = PreferencesHelper.fpsPillColor,
                rectShape = PreferencesHelper.fpsRectShape,
                cornerRadius = PreferencesHelper.fpsCornerRadius,
                pillOutline = PreferencesHelper.fpsPillOutline,
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
        sendIfRunning(FpsMonitorService.ACTION_NOTE) {
            putExtra(FpsMonitorService.EXTRA_NOTE, "fixed target selected: $label ($packageName)")
        }
        refresh()
        _uiState.update { it.copy(message = "Target: $label") }
    }

    fun clearTarget() {
        PreferencesHelper.clearFpsTarget()
        sendIfRunning(FpsMonitorService.ACTION_NOTE) {
            putExtra(FpsMonitorService.EXTRA_NOTE, "automatic app detection selected")
        }
        refresh()
        _uiState.update { it.copy(message = "Automatic detection enabled") }
    }

    fun setOverlayScale(scale: Int) {
        val clamped = scale.coerceIn(50, 200)
        PreferencesHelper.fpsOverlayScale = clamped
        _uiState.update { it.copy(overlayScale = clamped) }
        sendIfRunning(FpsMonitorService.ACTION_SET_SCALE) {
            putExtra(FpsMonitorService.EXTRA_SCALE, clamped)
        }
    }

    /**
     * Updates the pill fill opacity and pushes it to the running overlay.
     *
     * @param alpha opacity from 40 to 100
     */
    fun setOverlayAlpha(alpha: Int) {
        val clamped = alpha.coerceIn(OverlayStyle.ALPHA_MIN, OverlayStyle.ALPHA_MAX)
        PreferencesHelper.fpsOverlayAlpha = clamped
        _uiState.update { it.copy(overlayAlpha = clamped) }
        sendApplyStyle()
    }

    /**
     * Shows or hides the FPS label next to the value.
     *
     * @param enabled true for "60.0 FPS", false for "60.0"
     */
    fun setShowUnit(enabled: Boolean) {
        PreferencesHelper.fpsShowUnit = enabled
        _uiState.update { it.copy(showUnit = enabled) }
        sendApplyStyle()
    }

    /**
     * Changes the pill background color.
     *
     * @param color new pill color
     */
    fun setPillColor(color: OverlayPillColor) {
        PreferencesHelper.fpsPillColor = color
        _uiState.update { it.copy(pillColor = color) }
        sendApplyStyle()
    }

    /**
     * Switches the pill between pill and rectangle shape.
     *
     * @param rectangular true for rectangle, false for pill
     */
    fun setRectShape(rectangular: Boolean) {
        PreferencesHelper.fpsRectShape = rectangular
        _uiState.update { it.copy(rectShape = rectangular) }
        sendApplyStyle()
    }

    /**
     * Changes the rectangle corner radius.
     *
     * Takes effect on the pill only when the rectangle shape is on.
     * Stored always so the value survives shape toggles and restarts.
     *
     * @param radius corner radius in dp from [OverlayStyle.RECT_RADIUS_MIN] to [OverlayStyle.RECT_RADIUS_MAX]
     */
    fun setCornerRadius(radius: Int) {
        val clamped = radius.coerceIn(OverlayStyle.RECT_RADIUS_MIN, OverlayStyle.RECT_RADIUS_MAX)
        PreferencesHelper.fpsCornerRadius = clamped
        _uiState.update { it.copy(cornerRadius = clamped) }
        sendApplyStyle()
    }

    /**
     * Shows or hides the pill outline.
     *
     * @param enabled true to draw the edge, false for a flat pill
     */
    fun setPillOutline(enabled: Boolean) {
        PreferencesHelper.fpsPillOutline = enabled
        _uiState.update { it.copy(pillOutline = enabled) }
        sendApplyStyle()
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
        if (!_uiState.value.isRunning && !PreferencesHelper.fpsRunning) return
        appContext.startService(
            Intent(appContext, FpsMonitorService::class.java).apply {
                this.action = action
                fill()
            }
        )
    }

    private fun sendApplyStyle() {
        sendIfRunning(FpsMonitorService.ACTION_APPLY_STYLE)
    }

    fun clearSelectedLayer() {
        PreferencesHelper.fpsSelectedLayer = null
        _uiState.update { it.copy(selectedLayer = null) }
        sendIfRunning(FpsMonitorService.ACTION_SET_LAYER) {
            putExtra(FpsMonitorService.EXTRA_LAYER, null as String?)
        }
        _uiState.update { it.copy(message = "Layer: Auto") }
    }

    fun toggleDebugLogging() {
        val enabled = !PreferencesHelper.fpsDebugLoggingEnabled
        PreferencesHelper.fpsDebugLoggingEnabled = enabled
        sendIfRunning(FpsMonitorService.ACTION_SET_LOGGING) {
            putExtra(FpsMonitorService.EXTRA_ENABLED, enabled)
        }
        refresh()
        _uiState.update { it.copy(message = "Debug logging ${if (enabled) "enabled" else "disabled"}") }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
