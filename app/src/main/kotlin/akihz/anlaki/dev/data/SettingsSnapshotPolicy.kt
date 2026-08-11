package akihz.anlaki.dev.data

internal object SettingsSnapshotPolicy {
    const val BASELINE_LABEL = "Baseline"
    private const val MAX_RATE_SNAPSHOTS = 7
    private const val MAX_TOTAL_SNAPSHOTS = MAX_RATE_SNAPSHOTS + 1

    fun compact(existing: List<SettingsSnapshot>): List<SettingsSnapshot> {
        val baselineIndex = existing.indexOfLast { it.label == BASELINE_LABEL }
        if (baselineIndex < 0) {
            return existing.uniqueByLatestLabel().takeLast(MAX_TOTAL_SNAPSHOTS)
        }
        val baseline = existing[baselineIndex]
        val rates = existing.asSequence()
            .drop(baselineIndex + 1)
            .map { it.asDiffFrom(baseline) }
            .toList()
            .uniqueByLatestLabel()
            .takeLast(MAX_RATE_SNAPSHOTS)
        return listOf(baseline.copy(isDiff = false)) + rates
    }

    fun merge(
        existing: List<SettingsSnapshot>,
        label: String,
        current: Map<String, String>
    ): SnapshotMerge? {
        val compacted = compact(existing)
        val captured = SettingsSnapshot(label, current)
        if (label == BASELINE_LABEL) {
            return SnapshotMerge(captured, listOf(captured))
        }

        val baseline = compacted.firstOrNull { it.label == BASELINE_LABEL } ?: return null
        val normalizedRates = compacted.asSequence()
            .filter { it.label != BASELINE_LABEL && it.label != label }
            .map { it.asDiffFrom(baseline) }
            .toList()
        val rateSnapshot = captured.asDiffFrom(baseline)
        val retainedRates = (normalizedRates + rateSnapshot).takeLast(MAX_RATE_SNAPSHOTS)
        return SnapshotMerge(rateSnapshot, listOf(baseline.copy(isDiff = false)) + retainedRates)
    }

    fun inferredValue(
        snapshots: List<SettingsSnapshot>,
        label: String,
        id: String
    ): String? {
        val snapshot = snapshots.lastOrNull { it.label == label } ?: return null
        return snapshot.values[id] ?: if (snapshot.isDiff) {
            snapshots.firstOrNull { it.label == BASELINE_LABEL }?.values?.get(id)
        } else {
            null
        }
    }

    private fun SettingsSnapshot.asDiffFrom(baseline: SettingsSnapshot): SettingsSnapshot {
        if (isDiff) return this
        return copy(
            values = values.filter { (id, value) -> baseline.values[id] != value },
            isDiff = true
        )
    }

    private fun List<SettingsSnapshot>.uniqueByLatestLabel(): List<SettingsSnapshot> =
        asReversed().distinctBy { it.label }.asReversed()
}

internal data class SnapshotMerge(
    val captured: SettingsSnapshot,
    val retained: List<SettingsSnapshot>
)
