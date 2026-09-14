package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result

/**
 * Reads and writes the refresh rate through OEM settings keys.
 *
 * Stateless. The caller supplies the bound service and the raw exec
 * function, so this file owns only rate policy.
 */
internal object ShizukuRefreshRates {

    /** Returns the override strategy, or the auto-detected one. */
    internal fun activeStrategy(): OemSettingsStrategy.KeySet {
        val override = PreferencesHelper.oemOverride
        return if (override.isNotBlank() && override != "Auto-detect") {
            OemSettingsStrategy.resolveByName(override)
        } else {
            OemSettingsStrategy.resolve()
        }
    }

    /**
     * Reads the current refresh rate from OEM-specific settings keys.
     */
    internal fun getCurrentRefreshRate(
        exec: (String) -> Result<String>
    ): Result<Float> {
        val strategy = activeStrategy()
        val keys = strategy.readKeys

        for (settingsKey in keys) {
            val ns = ShizukuSettingsGateway.namespaceToString(settingsKey.namespace)
            val result = exec("settings get $ns ${settingsKey.key}")
            if (result.isSuccess) {
                result.getOrNull()?.let { raw ->
                    if (raw.isNotBlank() && raw != "null") {
                        raw.trim().toFloatOrNull()?.let { rate ->
                            return Result.success(rate)
                        }
                    }
                }
            }
        }
        return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Could not retrieve refresh rate")
    }

    /**
     * Sets the refresh rate using standard write keys.
     */
    internal fun setRefreshRate(
        exec: (String) -> Result<String>,
        hz: Float
    ): Result<Unit> {
        if (CustomProfileManager.profile().enabled) {
            return CustomProfileManager.applyRate(hz)
        }
        val hzInt = hz.toInt()
        val strategy = activeStrategy()
        val failures = mutableListOf<String>()

        strategy.writeKeys.forEach { settingsKey ->
            val ns = ShizukuSettingsGateway.namespaceToString(settingsKey.namespace)
            val value = RefreshRateSettingValue.forKey(settingsKey.key, hzInt)
            val result = exec("settings put $ns ${settingsKey.key} $value")
            if (result.isError) {
                failures += "$ns/${settingsKey.key}"
            }
        }

        return if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Failed to update: ${failures.joinToString()}"
            )
        }
    }

    /**
     * Resets refresh rate settings to defaults (adaptive mode).
     */
    internal fun resetRefreshRate(
        exec: (String) -> Result<String>
    ): Result<Unit> {
        if (CustomProfileManager.profile().enabled) {
            return CustomProfileManager.disable()
        }
        val strategy = activeStrategy()
        val failures = mutableListOf<String>()

        strategy.writeKeys.forEach { settingsKey ->
            val ns = ShizukuSettingsGateway.namespaceToString(settingsKey.namespace)
            val result = exec("settings delete $ns ${settingsKey.key}")
            if (result.isError) {
                failures += "$ns/${settingsKey.key}"
            }
        }

        if (strategy.supportsMode && strategy.modeKey != null) {
            val ns = ShizukuSettingsGateway.namespaceToString(strategy.modeKey.namespace)
            val result = exec("settings put $ns ${strategy.modeKey.key} 0")
            if (result.isError) {
                failures += "$ns/${strategy.modeKey.key}"
            }
        }

        return if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Failed to reset: ${failures.distinct().joinToString()}"
            )
        }
    }
}
