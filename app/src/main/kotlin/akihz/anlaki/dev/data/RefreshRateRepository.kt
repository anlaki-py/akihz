package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Single source of truth for refresh rate control.
 *
 * - **Read paths** always prefer [DisplayManagerDataSource] (hardware API, no permissions).
 * - **Write paths** delegate to [ShizukuHelper] which writes to an OEM-specific
 *   fallback chain of Secure Settings keys.
 * - **Verification** re-queries [DisplayManagerDataSource] after a short settle delay
 *   to confirm the display subsystem actually switched.
 */
class RefreshRateRepository(
    private val displayManagerDataSource: DisplayManagerDataSource,
    private val setRate: suspend (Float) -> Result<Unit> = { hz ->
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
     * Applies the requested rate via Shizuku settings calls, waits for the display
     * to settle, then verifies the change against [DisplayManagerDataSource].
     *
     * @param hz target refresh rate in Hz
     * @return [Result.Success] containing the verified active rate, or [Result.Error].
     */
    suspend fun setAndVerifyRate(hz: Float): Result<Float> = withContext(Dispatchers.IO) {
        val setResult = setRate(hz)
        if (setResult.isError) {
            val err = setResult.getErrorOrNull()
            return@withContext Result.error(
                err?.errorType ?: ErrorType.COMMAND_EXECUTION_FAILED,
                err?.message ?: "Failed to set refresh rate"
            )
        }

        // Allow the SurfaceFlinger / display pipeline to switch modes
        delay(SETTLE_DELAY_MS)

        val verified = displayManagerDataSource.getCurrentRefreshRate()
        if (verified.isSuccess) {
            val actual = verified.getOrNull()!!
            if (kotlin.math.abs(actual - hz) < VERIFICATION_TOLERANCE_HZ) {
                Result.success(actual)
            } else {
                Result.error(
                    ErrorType.COMMAND_EXECUTION_FAILED,
                    "Verification failed: expected ${hz.toInt()} Hz, but display reports ${actual.toInt()} Hz"
                )
            }
        } else {
            Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Rate was written but verification read failed"
            )
        }
    }

    private companion object {
        const val SETTLE_DELAY_MS = 350L
        const val VERIFICATION_TOLERANCE_HZ = 1.0f
    }
}
