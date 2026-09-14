package akihz.anlaki.dev.domain.usecase

import akihz.anlaki.dev.domain.TileRates
import akihz.anlaki.dev.domain.TileRateSelection
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Loads the tile rate set: supported rates plus recovered exclusions.
 *
 * Replaces the load plus recover plus persist block that MainViewModel and
 * RefreshRateTileService each carried on their own. The caller persists
 * [TileRates.excludedRates] when it owns the stored exclusions.
 */
class GetTileRatesUseCase @Inject constructor(
    private val repository: RefreshRateRepository
) {

    /**
     * Loads supported rates and recovers exclusions that would empty the cycle.
     *
     * @param excludedRates stored exclusions
     * @return resolved [TileRates], or the repository error
     */
    suspend operator fun invoke(excludedRates: Set<Float>): Result<TileRates> =
        withContext(Dispatchers.IO) {
            repository.getSupportedRates().map { rates ->
                val recovered = TileRateSelection.recoverEmptySelection(rates, excludedRates)
                TileRates(
                    allRates = rates,
                    includedRates = TileRateSelection.includedRates(rates, recovered),
                    excludedRates = recovered
                )
            }
        }
}
