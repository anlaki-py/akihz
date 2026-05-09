package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for refresh rate control.
 *
 * - **Read paths** always prefer [DisplayManagerDataSource] (hardware API, no permissions).
 * - **Write paths** delegate to [ShizukuHelper] which writes to an OEM-specific
 *   fallback chain of Secure/System/Global Settings keys.
 * - **Lock mode** sets min == peak to constrain SurfaceFlinger.
 * - **Per-app profiles** allow different rates for different apps.
 */
class RefreshRateRepository(
    private val displayManagerDataSource: DisplayManagerDataSource,
    private val setRateCommand: suspend (Float) -> Result<Unit> = { hz ->
        ShizukuHelper.setRefreshRate(hz)
    },
    private val setRateLockedCommand: suspend (Float) -> Result<Unit> = { hz ->
        ShizukuHelper.setRefreshRateLocked(hz)
    }
) {

    /**
     * Returns the list of refresh rates supported by the device's default display.
     */
    fun getSupportedRates(): Result<List<Float>> {
        return displayManagerDataSource.getSupportedRefreshRates()
    }

    /**
     * Returns the currently active refresh rate reported by the display subsystem.
     * This is the primary read path and works across all OEMs.
     */
    fun getCurrentRate(): Result<Float> {
        return displayManagerDataSource.getCurrentRefreshRate()
    }

    /**
     * Applies the requested rate and returns immediately.
     * Respects lock mode setting from preferences.
     *
     * @param hz target refresh rate in Hz
     * @return [Result.Success] if the write succeeded, or [Result.Error].
     */
    suspend fun setRate(hz: Float): Result<Unit> = withContext(Dispatchers.IO) {
        val useLockMode = PreferencesHelper.lockModeEnabled
        val result = if (useLockMode) {
            setRateLockedCommand(hz)
        } else {
            setRateCommand(hz)
        }

        if (result.isSuccess) {
            PreferencesHelper.lastRate = hz
        }

        if (result.isError) {
            val err = result.getErrorOrNull()
            return@withContext Result.error(
                err?.errorType ?: ErrorType.COMMAND_EXECUTION_FAILED,
                err?.message ?: "Failed to set refresh rate"
            )
        }

        Result.success(Unit)
    }

    /**
     * Applies the requested rate for a specific app package.
     * Stores the per-app profile and applies immediately.
     *
     * @param packageName the app package name
     * @param hz target refresh rate in Hz
     */
    suspend fun setRateForApp(packageName: String, hz: Float): Result<Unit> = withContext(Dispatchers.IO) {
        PreferencesHelper.setAppProfile(packageName, hz)
        setRate(hz)
    }

    /**
     * Gets the refresh rate profile for a specific app.
     *
     * @param packageName the app package name
     * @return the preferred rate, or null if no profile exists
     */
    fun getAppProfile(packageName: String): Float? {
        return PreferencesHelper.getAppProfile(packageName)
    }

    /**
     * Removes the per-app profile for a package.
     *
     * @param packageName the app package name
     */
    fun removeAppProfile(packageName: String) {
        PreferencesHelper.removeAppProfile(packageName)
    }

    /**
     * Returns all stored per-app profiles.
     */
    fun getAllAppProfiles(): Map<String, Float> {
        return PreferencesHelper.getAllAppProfiles()
    }

    /**
     * Resets all refresh rate settings to defaults (adaptive mode).
     */
    suspend fun resetToDefaults(): Result<Unit> = withContext(Dispatchers.IO) {
        ShizukuHelper.resetRefreshRate()
    }
}