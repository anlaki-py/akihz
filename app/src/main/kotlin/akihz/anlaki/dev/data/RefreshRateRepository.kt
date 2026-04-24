package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for refresh rate control.
 *
 * - **Read paths** always prefer [DisplayManagerDataSource] (hardware API, no permissions).
 * - **Write paths** delegate to [ShizukuHelper] which writes to an OEM-specific
 *   fallback chain of Secure Settings keys.
 */
class RefreshRateRepository(
    private val displayManagerDataSource: DisplayManagerDataSource,
    private val setRateCommand: suspend (Float) -> Result<Unit> = { hz ->
        ShizukuHelper.setRefreshRate(hz)
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
     * Applies the requested rate via Shizuku settings calls and returns immediately.
     * The caller may verify the switch independently via [getCurrentRate].
     *
     * @param hz target refresh rate in Hz
     * @return [Result.Success] if the write succeeded, or [Result.Error].
     */
    suspend fun setRate(hz: Float): Result<Unit> = withContext(Dispatchers.IO) {
        val setResult = setRateCommand(hz)
        if (setResult.isError) {
            val err = setResult.getErrorOrNull()
            return@withContext Result.error(
                err?.errorType ?: ErrorType.COMMAND_EXECUTION_FAILED,
                err?.message ?: "Failed to set refresh rate"
            )
        }

        Result.success(Unit)
    }
}
