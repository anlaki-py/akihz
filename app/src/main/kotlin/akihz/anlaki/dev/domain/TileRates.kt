package akihz.anlaki.dev.domain

/**
 * Refresh rates resolved for Quick Settings tile cycling.
 *
 * @param allRates every rate the device reports
 * @param includedRates rates the tile cycles through
 * @param excludedRates exclusions after empty-selection recovery
 */
data class TileRates(
    val allRates: List<Float>,
    val includedRates: List<Float>,
    val excludedRates: Set<Float>
)
