package akihz.anlaki.dev.domain.repository

import akihz.anlaki.dev.utils.Result

interface RefreshRateRepository {

    /**
     * Returns supported refresh rates.
     * @return result with list of rates in Hz.
     */
    fun getSupportedRates(): Result<List<Float>>

    /**
     * Returns the current refresh rate.
     * @return result with rate in Hz.
     */
    fun getCurrentRate(): Result<Float>

    /**
     * Sets the display refresh rate.
     * @param hz target rate in Hz. @return result with unit on success.
     */
    suspend fun setRate(hz: Float): Result<Unit>

    /** Restores the default refresh rate settings. */
    suspend fun resetToDefaults(): Result<Unit>
}
