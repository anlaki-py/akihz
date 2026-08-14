package akihz.anlaki.dev.domain

import kotlin.math.abs

/** Applies the user's per-rate inclusion choices to Quick Settings tile cycling. */
object TileRateSelection {
    private const val RATE_TOLERANCE_HZ = 0.01f

    /** Returns supported rates that have not been excluded from tile cycling. */
    fun includedRates(supportedRates: List<Float>, excludedRates: Set<Float>): List<Float> =
        supportedRates.filterNot { rate -> excludedRates.any { matches(it, rate) } }

    /**
     * Drops exclusions for the active rate set when they would leave the tile with no choices.
     * Exclusions for other profiles are retained so switching profiles does not lose preferences.
     */
    fun recoverEmptySelection(
        supportedRates: List<Float>,
        excludedRates: Set<Float>
    ): Set<Float> {
        if (supportedRates.isEmpty() || includedRates(supportedRates, excludedRates).isNotEmpty()) {
            return excludedRates
        }
        return excludedRates.filterNot { excluded ->
            supportedRates.any { supported -> matches(excluded, supported) }
        }.toSet()
    }

    /** Returns whether a supported rate is currently included in the tile cycle. */
    fun isIncluded(rate: Float, excludedRates: Set<Float>): Boolean =
        excludedRates.none { matches(it, rate) }

    /**
     * Updates one rate's inclusion while preserving at least one tile choice.
     * Unsupported rates and attempts to exclude the final choice leave the set unchanged.
     */
    fun setIncluded(
        supportedRates: List<Float>,
        excludedRates: Set<Float>,
        rate: Float,
        included: Boolean
    ): Set<Float> {
        val recovered = recoverEmptySelection(supportedRates, excludedRates)
        val supportedRate = supportedRates.firstOrNull { matches(it, rate) } ?: return recovered
        val withoutRate = recovered.filterNot { matches(it, supportedRate) }.toSet()
        if (included) return withoutRate
        if (includedRates(supportedRates, recovered).size <= 1) return recovered
        return withoutRate + supportedRate
    }

    /** Returns the rate after [currentRate], wrapping to the first included rate. */
    fun nextRate(includedRates: List<Float>, currentRate: Float?): Float? {
        if (includedRates.isEmpty()) return null
        val orderedRates = includedRates.sorted()
        if (currentRate == null) return orderedRates.first()
        val currentIndex = orderedRates.indexOfFirst { matches(it, currentRate) }
        if (currentIndex >= 0) return orderedRates[(currentIndex + 1) % orderedRates.size]
        return orderedRates.firstOrNull { it > currentRate + RATE_TOLERANCE_HZ }
            ?: orderedRates.first()
    }

    private fun matches(first: Float, second: Float): Boolean =
        abs(first - second) < RATE_TOLERANCE_HZ
}
