package akihz.anlaki.dev.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    private const val PREFS_NAME = "akihz_prefs"
    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_LAST_RATE = "last_rate"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_SERVICE_RUNNING = "service_running"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

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

    fun saveState(index: Int, rate: Float) {
        prefs.edit()
            .putInt(KEY_CURRENT_INDEX, index)
            .putFloat(KEY_LAST_RATE, rate)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
