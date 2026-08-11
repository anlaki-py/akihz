package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSnapshotPolicyTest {
    private val baselineValues = mapOf("system/rate" to "60", "system/static" to "1")

    @Test
    fun `rate capture requires a baseline`() {
        assertNull(SettingsSnapshotPolicy.merge(emptyList(), "120 Hz", baselineValues))
    }

    @Test
    fun `rate captures retain only baseline differences`() {
        val baseline = SettingsSnapshot("Baseline", baselineValues)

        val merge = requireNotNull(
            SettingsSnapshotPolicy.merge(
                listOf(baseline),
                "120 Hz",
                baselineValues + ("system/rate" to "120")
            )
        )

        assertTrue(merge.captured.isDiff)
        assertEquals(mapOf("system/rate" to "120"), merge.captured.values)
        assertEquals(2, merge.retained.size)
    }

    @Test
    fun `recapturing baseline invalidates old rate differences`() {
        val oldBaseline = SettingsSnapshot("Baseline", baselineValues)
        val oldRate = SettingsSnapshot("120 Hz", mapOf("system/rate" to "120"), isDiff = true)

        val merge = requireNotNull(
            SettingsSnapshotPolicy.merge(
                listOf(oldBaseline, oldRate),
                "Baseline",
                baselineValues + ("system/new" to "value")
            )
        )

        assertEquals(1, merge.retained.size)
        assertFalse(merge.retained.single().isDiff)
    }

    @Test
    fun `rate snapshots replace labels and remain bounded`() {
        var snapshots = listOf(SettingsSnapshot("Baseline", baselineValues))
        repeat(10) { index ->
            snapshots = requireNotNull(
                SettingsSnapshotPolicy.merge(
                    snapshots,
                    "$index Hz",
                    baselineValues + ("system/rate" to index.toString())
                )
            ).retained
        }

        assertEquals(8, snapshots.size)
        assertEquals("3 Hz", snapshots[1].label)
        assertEquals("9 Hz", snapshots.last().label)
    }

    @Test
    fun `legacy full snapshots are compacted into differences`() {
        val snapshots = listOf(
            SettingsSnapshot("Baseline", baselineValues),
            SettingsSnapshot("120 Hz", baselineValues + ("system/rate" to "120"))
        )

        val compacted = SettingsSnapshotPolicy.compact(snapshots)

        assertEquals(mapOf("system/rate" to "120"), compacted.last().values)
        assertTrue(compacted.last().isDiff)
    }

    @Test
    fun `unchanged values are inferred from baseline`() {
        val snapshots = listOf(
            SettingsSnapshot("Baseline", baselineValues),
            SettingsSnapshot("120 Hz", mapOf("system/rate" to "120"), isDiff = true)
        )

        assertEquals(
            "1",
            SettingsSnapshotPolicy.inferredValue(snapshots, "120 Hz", "system/static")
        )
    }
}
