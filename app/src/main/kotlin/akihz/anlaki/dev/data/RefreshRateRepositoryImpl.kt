package akihz.anlaki.dev.data

import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefreshRateRepositoryImpl(
    private val displayManagerDataSource: DisplayManagerDataSource,
    private val shizukuHelper: ShizukuHelper
) : RefreshRateRepository {

    /** Returns supported rates, or custom profile rates when enabled. */
    override fun getSupportedRates(): Result<List<Float>> {
        val customProfile = CustomProfileManager.profile()
        if (customProfile.enabled) return Result.success(customProfile.rates)
        return displayManagerDataSource.getSupportedRefreshRates()
    }

    /** Returns the current display refresh rate. */
    override fun getCurrentRate(): Result<Float> {
        return displayManagerDataSource.getCurrentRefreshRate()
    }

    /**
     * Sets the refresh rate and saves it on success.
     * @param hz rate in hertz.
     */
    override suspend fun setRate(hz: Float): Result<Unit> = withContext(Dispatchers.IO) {
        val result = shizukuHelper.setRefreshRate(hz)

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

    /** Resets refresh rate settings to defaults. */
    override suspend fun resetToDefaults(): Result<Unit> = withContext(Dispatchers.IO) {
        shizukuHelper.resetRefreshRate()
    }
}
