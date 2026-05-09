package akihz.anlaki.dev.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * SharedPreferences wrapper for all app configuration.
 *
 * Stores refresh rate state, watchdog settings, per-app profiles, and UI preferences.
 */
object PreferencesHelper {
    private const val PREFS_NAME = "akihz_prefs"

    // Legacy keys
    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_LAST_RATE = "last_rate"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_SERVICE_RUNNING = "service_running"

    // New configuration keys
    private const val KEY_WATCHDOG_ENABLED = "watchdog_enabled"
    private const val KEY_WATCHDOG_INTERVAL_MS = "watchdog_interval_ms"
    private const val KEY_WATCHDOG_AGGRESSIVE = "watchdog_aggressive"
    private const val KEY_APP_MONITOR_ENABLED = "app_monitor_enabled"
    private const val KEY_LOCK_MODE_ENABLED = "lock_mode_enabled"
    private const val KEY_BATTERY_SAVER_OVERRIDE = "battery_saver_override"
    private const val KEY_PER_APP_PROFILES = "per_app_profiles"
    private const val KEY_DEFAULT_RATE = "default_rate"
    private const val KEY_OEM_OVERRIDE = "oem_override"
    private const val KEY_DESIRED_RATE = "desired_rate"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Legacy accessors
    var currentIndex: Int
        get() = prefs.getInt(KEY_CURRENT_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_CURRENT_INDEX, value).apply()

    var lastRate: Float
        get() = prefs.getFloat(KEY_LAST_RATE, 60f)
        set(value) = prefs.edit().putFloat(KEY_LAST_RATE, value).apply()

    var lastUpdateTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE, value).apply()

    var isServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_RUNNING, value).apply()

    // Watchdog settings
    var watchdogEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATCHDOG_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WATCHDOG_ENABLED, value).apply()

    var watchdogIntervalMs: Long
        get() = prefs.getLong(KEY_WATCHDOG_INTERVAL_MS, 3000L)
        set(value) = prefs.edit().putLong(KEY_WATCHDOG_INTERVAL_MS, value).apply()

    var watchdogAggressive: Boolean
        get() = prefs.getBoolean(KEY_WATCHDOG_AGGRESSIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_WATCHDOG_AGGRESSIVE, value).apply()

    // App monitor settings
    var appMonitorEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_MONITOR_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_MONITOR_ENABLED, value).apply()

    // Lock mode
    var lockModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_MODE_ENABLED, value).apply()

    // Battery saver
    var batterySaverOverride: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_SAVER_OVERRIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_SAVER_OVERRIDE, value).apply()

    // Default rate
    var defaultRate: Float
        get() = prefs.getFloat(KEY_DEFAULT_RATE, 0f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_RATE, value).apply()

    // OEM override
    var oemOverride: String
        get() = prefs.getString(KEY_OEM_OVERRIDE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OEM_OVERRIDE, value).apply()

    // Desired rate (used by watchdog)
    var desiredRate: Float
        get() = prefs.getFloat(KEY_DESIRED_RATE, 0f)
        set(value) = prefs.edit().putFloat(KEY_DESIRED_RATE, value).apply()

    // Per-app profiles stored as JSON
    fun setAppProfile(packageName: String, rate: Float) {
        val json = try {
            JSONObject(prefs.getString(KEY_PER_APP_PROFILES, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        json.put(packageName, rate.toDouble())
        prefs.edit().putString(KEY_PER_APP_PROFILES, json.toString()).apply()
    }

    fun getAppProfile(packageName: String): Float? {
        val json = try {
            JSONObject(prefs.getString(KEY_PER_APP_PROFILES, "{}") ?: "{}")
        } catch (_: Exception) {
            return null
        }
        return if (json.has(packageName)) {
            json.getDouble(packageName).toFloat()
        } else null
    }

    fun removeAppProfile(packageName: String) {
        val json = try {
            JSONObject(prefs.getString(KEY_PER_APP_PROFILES, "{}") ?: "{}")
        } catch (_: Exception) {
            return
        }
        json.remove(packageName)
        prefs.edit().putString(KEY_PER_APP_PROFILES, json.toString()).apply()
    }

    fun getAllAppProfiles(): Map<String, Float> {
        val json = try {
            JSONObject(prefs.getString(KEY_PER_APP_PROFILES, "{}") ?: "{}")
        } catch (_: Exception) {
            return emptyMap()
        }
        val map = mutableMapOf<String, Float>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getDouble(key).toFloat()
        }
        return map
    }

    fun saveState(index: Int, rate: Float) {
        prefs.edit()
            .putInt(KEY_CURRENT_INDEX, index)
            .putFloat(KEY_LAST_RATE, rate)
            .putFloat(KEY_DESIRED_RATE, rate)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}