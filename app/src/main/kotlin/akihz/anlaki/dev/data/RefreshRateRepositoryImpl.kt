package akihz.anlaki.dev.data

import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefreshRateRepositoryImpl(
    private val displayManagerDataSource: DisplayManagerDataSource,
    private val shizukuHelper: ShizukuHelper
) : RefreshRateRepository {

    override fun getSupportedRates(): Result<List<Float>> {
        return displayManagerDataSource.getSupportedRefreshRates()
    }

    override fun getCurrentRate(): Result<Float> {
        return displayManagerDataSource.getCurrentRefreshRate()
    }

    override suspend fun setRate(hz: Float): Result<Unit> = withContext(Dispatchers.IO) {
        val result = shizukuHelper.setRefreshRate(hz)

        if (result.isSuccess) {
            PreferencesHelper.lastRate = hz
            PreferencesHelper.desiredRate = hz
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

    override suspend fun resetToDefaults(): Result<Unit> = withContext(Dispatchers.IO) {
        shizukuHelper.resetRefreshRate()
    }
}
