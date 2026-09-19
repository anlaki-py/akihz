package akihz.anlaki.dev.domain.usecase

import akihz.anlaki.dev.domain.TileRateSelection
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Advances the tile to the next included rate and applies it.
 *
 * Owns the anchor plus next plus apply policy so the tile service keeps
 * only its state machine and tile rendering.
 */
class CycleTileRateUseCase @Inject constructor(
    private val repository: RefreshRateRepository
) {

    /**
     * Applies the rate after [anchor], wrapping to the first included rate.
     *
     * @param includedRates rates the tile cycles through, in any order
     * @param anchor rate to advance from, null starts at the first rate
     * @return the applied rate, or an error when the cycle is empty or the write fails
     */
    suspend operator fun invoke(includedRates: List<Float>, anchor: Float?): Result<Float> =
        withContext(Dispatchers.IO) {
            val next = TileRateSelection.nextRate(includedRates, anchor)
                ?: return@withContext Result.error(
                    ErrorType.COMMAND_EXECUTION_FAILED,
                    "No supported refresh rates detected"
                )
            repository.setRate(next).map { next }
        }
}
