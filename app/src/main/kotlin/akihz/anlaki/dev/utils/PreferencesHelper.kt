package akihz.anlaki.dev.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import akihz.anlaki.dev.data.HomeDebugSettings
import akihz.anlaki.dev.domain.update.UpdateChannel
import akihz.anlaki.dev.presentation.theme.AppThemeMode

/**
 * SharedPreferences wrapper for all app configuration.
 *
 * Stores refresh rate state and UI preferences.
 * All writes use [SharedPreferences.Editor.apply] for async persistence.
 */
object PreferencesHelper {
    private const val PREFS_NAME = "akihz_prefs"

    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_LAST_RATE = "last_rate"
    private const val KEY_OEM_OVERRIDE = "oem_override"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_AMOLED_MODE = "amoled_mode"
    private const val KEY_BLUR_ENABLED = "blur_enabled"
    private const val KEY_UPDATE_CHANNEL = "update_channel"
    private const val KEY_HZ_TEXT_SIZE = "debug_hz_text_size"
    private const val KEY_BUTTON_HEIGHT = "debug_button_height"
    private const val KEY_BUTTON_WIDTH = "debug_button_width"
    private const val KEY_BUTTON_SPACING = "debug_button_spacing"
    private const val KEY_RESTING_CORNER = "debug_resting_corner"
    private const val KEY_SELECTED_CORNER = "debug_selected_corner"
    private const val KEY_PRESSED_CORNER = "debug_pressed_corner"

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

    var homeDebugSettings: HomeDebugSettings
        get() {
            val defaults = HomeDebugSettings.defaults()
            return HomeDebugSettings(
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
            putFloat(KEY_HZ_TEXT_SIZE, value.hzTextSizeSp)
            putFloat(KEY_BUTTON_HEIGHT, value.buttonHeightDp)
            putFloat(KEY_BUTTON_WIDTH, value.buttonWidthPercent)
            putFloat(KEY_BUTTON_SPACING, value.buttonSpacingDp)
            putFloat(KEY_RESTING_CORNER, value.restingCornerDp)
            putFloat(KEY_SELECTED_CORNER, value.selectedCornerDp)
            putFloat(KEY_PRESSED_CORNER, value.pressedCornerDp)
        }

    fun saveState(index: Int, rate: Float) {
        prefs.edit {
            putInt(KEY_CURRENT_INDEX, index)
            putFloat(KEY_LAST_RATE, rate)
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }
}
