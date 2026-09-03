package akihz.anlaki.dev.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.domain.update.UpdateChannel
import akihz.anlaki.dev.domain.update.UpdateCheckFrequency
import akihz.anlaki.dev.presentation.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences wrapper for all app configuration.
 *
 * Stores refresh rate state and UI preferences.
 * All writes use [SharedPreferences.Editor.apply] for async persistence.
 */
object PreferencesHelper {
    private const val PREFS_NAME = "akihz_prefs"

    private const val KEY_LAST_RATE = "last_rate"
    private const val KEY_EXCLUDED_TILE_RATES = "excluded_tile_rates"
    private const val KEY_OEM_OVERRIDE = "oem_override"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_AMOLED_MODE = "amoled_mode"
    private const val KEY_BLUR_ENABLED = "blur_enabled"
    private const val KEY_UPDATE_CHANNEL = "update_channel"
    private const val KEY_UPDATE_CHECK_FREQUENCY = "update_check_frequency"
    private const val KEY_LAST_UPDATE_CHECK_AT = "last_update_check_at"
    private const val KEY_LATEST_UPDATE_CODE = "latest_update_code"
    private const val KEY_LATEST_UPDATE_NAME = "latest_update_name"
    private const val KEY_LAST_NOTIFIED_UPDATE_CODE = "last_notified_update_code"
    private const val KEY_SHOW_FAKE_REFRESH_RATES = "debug_show_fake_refresh_rates"
    private const val KEY_HZ_TEXT_SIZE = "debug_hz_text_size"
    private const val KEY_BUTTON_HEIGHT = "debug_button_height"
    private const val KEY_BUTTON_WIDTH = "debug_button_width"
    private const val KEY_BUTTON_SPACING = "debug_button_spacing"
    private const val KEY_RESTING_CORNER = "debug_resting_corner"
    private const val KEY_SELECTED_CORNER = "debug_selected_corner"
    private const val KEY_PRESSED_CORNER = "debug_pressed_corner"
    private const val KEY_DEBUG_OPTIONS_UNLOCKED = "debug_options_unlocked"
    private const val KEY_WELCOME_NOTICE_ACCEPTED = "welcome_notice_accepted"
    private const val KEY_KEEP_ALIVE_ENABLED = "keep_alive_enabled"
    private const val KEY_FPS_TARGET_PACKAGE = "fps_target_package"
    private const val KEY_FPS_TARGET_LABEL = "fps_target_label"
    private const val KEY_FPS_OVERLAY_SCALE = "fps_overlay_scale"
    private const val KEY_FPS_SELECTED_LAYER = "fps_selected_layer"
    private const val KEY_FPS_RUNNING = "fps_running"
    private const val KEY_FPS_DEBUG_LOGGING = "fps_debug_logging"
    private const val KEY_FPS_DEBUG_LOG = "fps_debug_log"
    private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

    private lateinit var prefs: SharedPreferences
    private val _lastRateFlow = MutableStateFlow(60f)

    /** Last successfully selected rate, observed across app and tile components. */
    val lastRateFlow: StateFlow<Float> = _lastRateFlow.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _lastRateFlow.value = prefs.getFloat(KEY_LAST_RATE, 60f)
    }

    var lastRate: Float
        get() = prefs.getFloat(KEY_LAST_RATE, 60f)
        set(value) {
            prefs.edit { putFloat(KEY_LAST_RATE, value) }
            _lastRateFlow.value = value
        }

    /** Refresh rates the user has removed from Quick Settings tile cycling. */
    var excludedTileRates: Set<Float>
        get() = prefs.getStringSet(KEY_EXCLUDED_TILE_RATES, emptySet())
            .orEmpty()
            .mapNotNull(String::toFloatOrNull)
            .filter { it.isFinite() && it > 0f }
            .toSet()
        set(value) = prefs.edit {
            putStringSet(
                KEY_EXCLUDED_TILE_RATES,
                value.filter { it.isFinite() && it > 0f }.map(Float::toString).toSet()
            )
        }

    // OEM override
    var oemOverride: String
        get() = prefs.getString(KEY_OEM_OVERRIDE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_OEM_OVERRIDE, value) }

    var themeMode: AppThemeMode
        get() = runCatching {
            AppThemeMode.valueOf(
                prefs.getString(KEY_THEME_MODE, AppThemeMode.System.name)
                    ?: AppThemeMode.System.name
            )
        }.getOrDefault(AppThemeMode.System)
        set(value) = prefs.edit { putString(KEY_THEME_MODE, value.name) }

    var amoledMode: Boolean
        get() = prefs.getBoolean(KEY_AMOLED_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_AMOLED_MODE, value) }

    var blurEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLUR_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_BLUR_ENABLED, value) }

    var updateChannel: UpdateChannel
        get() = runCatching {
            UpdateChannel.valueOf(
                prefs.getString(KEY_UPDATE_CHANNEL, UpdateChannel.Stable.name)
                    ?: UpdateChannel.Stable.name
            )
        }.getOrDefault(UpdateChannel.Stable)
        set(value) = prefs.edit { putString(KEY_UPDATE_CHANNEL, value.name) }

    var updateCheckFrequency: UpdateCheckFrequency
        get() = UpdateCheckFrequency.fromStoredValue(
            prefs.getString(KEY_UPDATE_CHECK_FREQUENCY, null)
        )
        set(value) = prefs.edit { putString(KEY_UPDATE_CHECK_FREQUENCY, value.name) }

    var lastUpdateCheckAt: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_UPDATE_CHECK_AT, value) }

    var latestAvailableVersionCode: Long
        get() = prefs.getLong(KEY_LATEST_UPDATE_CODE, -1L)
        set(value) = prefs.edit { putLong(KEY_LATEST_UPDATE_CODE, value) }

    var latestAvailableVersionName: String
        get() = prefs.getString(KEY_LATEST_UPDATE_NAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_LATEST_UPDATE_NAME, value) }

    var lastNotifiedVersionCode: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED_UPDATE_CODE, -1L)
        set(value) = prefs.edit { putLong(KEY_LAST_NOTIFIED_UPDATE_CODE, value) }

    /** Clears cached information about an update that is no longer newer. */
    fun clearAvailableUpdate() {
        prefs.edit {
            remove(KEY_LATEST_UPDATE_CODE)
            remove(KEY_LATEST_UPDATE_NAME)
        }
    }

    var homeDebugSettings: HomeDebugSettings
        get() {
            val defaults = HomeDebugSettings.defaults()
            return HomeDebugSettings(
                showFakeRefreshRates = prefs.getBoolean(
                    KEY_SHOW_FAKE_REFRESH_RATES,
                    defaults.showFakeRefreshRates
                ),
                hzTextSizeSp = prefs.getFloat(KEY_HZ_TEXT_SIZE, defaults.hzTextSizeSp),
                buttonHeightDp = prefs.getFloat(KEY_BUTTON_HEIGHT, defaults.buttonHeightDp),
                buttonWidthPercent = prefs.getFloat(KEY_BUTTON_WIDTH, defaults.buttonWidthPercent),
                buttonSpacingDp = prefs.getFloat(KEY_BUTTON_SPACING, defaults.buttonSpacingDp),
                restingCornerDp = prefs.getFloat(KEY_RESTING_CORNER, defaults.restingCornerDp),
                selectedCornerDp = prefs.getFloat(KEY_SELECTED_CORNER, defaults.selectedCornerDp),
                pressedCornerDp = prefs.getFloat(KEY_PRESSED_CORNER, defaults.pressedCornerDp)
            )
        }

        set(value) = prefs.edit {
            putBoolean(KEY_SHOW_FAKE_REFRESH_RATES, value.showFakeRefreshRates)
            putFloat(KEY_HZ_TEXT_SIZE, value.hzTextSizeSp)
            putFloat(KEY_BUTTON_HEIGHT, value.buttonHeightDp)
            putFloat(KEY_BUTTON_WIDTH, value.buttonWidthPercent)
            putFloat(KEY_BUTTON_SPACING, value.buttonSpacingDp)
            putFloat(KEY_RESTING_CORNER, value.restingCornerDp)
            putFloat(KEY_SELECTED_CORNER, value.selectedCornerDp)
            putFloat(KEY_PRESSED_CORNER, value.pressedCornerDp)
        }

    var debugOptionsUnlocked: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_OPTIONS_UNLOCKED, false)
        set(value) = prefs.edit { putBoolean(KEY_DEBUG_OPTIONS_UNLOCKED, value) }

    /** Whether the user has acknowledged the first-run project notice. */
    var welcomeNoticeAccepted: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_NOTICE_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_WELCOME_NOTICE_ACCEPTED, value) }

    /** Whether the foreground keep-alive notification/service is enabled. Enabled by default. */
    var keepAliveEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ALIVE_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_ALIVE_ENABLED, value) }

    var fpsTargetPackage: String?
        get() = prefs.getString(KEY_FPS_TARGET_PACKAGE, null)
        set(value) = prefs.edit { putString(KEY_FPS_TARGET_PACKAGE, value) }

    var fpsTargetLabel: String?
        get() = prefs.getString(KEY_FPS_TARGET_LABEL, null)
        set(value) = prefs.edit { putString(KEY_FPS_TARGET_LABEL, value) }

    var fpsOverlayScale: Int
        get() = prefs.getInt(KEY_FPS_OVERLAY_SCALE, 100)
        set(value) = prefs.edit { putInt(KEY_FPS_OVERLAY_SCALE, value.coerceIn(50, 200)) }

    var fpsSelectedLayer: String?
        get() = prefs.getString(KEY_FPS_SELECTED_LAYER, null)
        set(value) = prefs.edit { putString(KEY_FPS_SELECTED_LAYER, value) }

    var fpsRunning: Boolean
        get() = prefs.getBoolean(KEY_FPS_RUNNING, false)
        set(value) = prefs.edit { putBoolean(KEY_FPS_RUNNING, value) }

    var fpsDebugLoggingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FPS_DEBUG_LOGGING, false)
        set(value) = prefs.edit { putBoolean(KEY_FPS_DEBUG_LOGGING, value) }

    var fpsDebugLog: String
        get() = prefs.getString(KEY_FPS_DEBUG_LOG, "") ?: ""
        set(value) = prefs.edit { putString(KEY_FPS_DEBUG_LOG, value.takeLast(24000)) }

    var notificationPermissionRequested: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, value) }

    fun clearFpsTarget() {
        prefs.edit {
            remove(KEY_FPS_TARGET_PACKAGE)
            remove(KEY_FPS_TARGET_LABEL)
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }
}
