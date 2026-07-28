package akihz.anlaki.dev.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import akihz.anlaki.dev.presentation.theme.AppThemeMode

/**
 * SharedPreferences wrapper for all app configuration.
 *
 * Stores refresh rate state, watchdog settings, and UI preferences.
 * All writes use [SharedPreferences.Editor.apply] for async persistence.
 */
object PreferencesHelper {
    private const val PREFS_NAME = "akihz_prefs"

    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_LAST_RATE = "last_rate"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_SERVICE_RUNNING = "service_running"

    private const val KEY_WATCHDOG_ENABLED = "watchdog_enabled"
    private const val KEY_WATCHDOG_INTERVAL_MS = "watchdog_interval_ms"
    private const val KEY_WATCHDOG_AGGRESSIVE = "watchdog_aggressive"
    private const val KEY_OEM_OVERRIDE = "oem_override"
    private const val KEY_DESIRED_RATE = "desired_rate"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_AMOLED_MODE = "amoled_mode"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var currentIndex: Int
        get() = prefs.getInt(KEY_CURRENT_INDEX, 0)
        set(value) = prefs.edit { putInt(KEY_CURRENT_INDEX, value) }

    var lastRate: Float
        get() = prefs.getFloat(KEY_LAST_RATE, 60f)
        set(value) = prefs.edit { putFloat(KEY_LAST_RATE, value) }

    var lastUpdateTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATE, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_UPDATE, value) }

    var isServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_RUNNING, value) }

    // Watchdog settings
    var watchdogEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATCHDOG_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_WATCHDOG_ENABLED, value) }

    var watchdogIntervalMs: Long
        get() = prefs.getLong(KEY_WATCHDOG_INTERVAL_MS, 3000L)
        set(value) = prefs.edit { putLong(KEY_WATCHDOG_INTERVAL_MS, value) }

    var watchdogAggressive: Boolean
        get() = prefs.getBoolean(KEY_WATCHDOG_AGGRESSIVE, false)
        set(value) = prefs.edit { putBoolean(KEY_WATCHDOG_AGGRESSIVE, value) }

    // OEM override
    var oemOverride: String
        get() = prefs.getString(KEY_OEM_OVERRIDE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_OEM_OVERRIDE, value) }

    // Desired rate (used by watchdog)
    var desiredRate: Float
        get() = prefs.getFloat(KEY_DESIRED_RATE, 0f)
        set(value) = prefs.edit { putFloat(KEY_DESIRED_RATE, value) }

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

    fun saveState(index: Int, rate: Float) {
        prefs.edit {
            putInt(KEY_CURRENT_INDEX, index)
            putFloat(KEY_LAST_RATE, rate)
            putFloat(KEY_DESIRED_RATE, rate)
            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }
}
