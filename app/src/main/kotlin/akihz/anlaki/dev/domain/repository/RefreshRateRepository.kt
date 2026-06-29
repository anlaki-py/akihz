package akihz.anlaki.dev.domain.repository

import akihz.anlaki.dev.utils.Result

interface RefreshRateRepository {

    fun getSupportedRates(): Result<List<Float>>

    fun getCurrentRate(): Result<Float>

    suspend fun setRate(hz: Float): Result<Unit>

    suspend fun resetToDefaults(): Result<Unit>
}
