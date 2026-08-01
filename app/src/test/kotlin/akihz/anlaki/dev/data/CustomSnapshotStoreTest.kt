package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomSnapshotStoreTest {
    @Test
    fun `recapture replaces previous snapshot and moves it to newest position`() {
        val retained = retainSnapshots(
            listOf(
                snapshot("Baseline", "old"),
                snapshot("60 Hz", "60"),
                snapshot("Baseline", "new")
            )
        )

        assertEquals(listOf("60 Hz", "Baseline"), retained.map { it.label })
        assertEquals("new", retained.last().values["system/key"])
    }

    @Test
    fun `retention drops oldest snapshots beyond limit`() {
        val snapshots = (0..MAX_STORED_SNAPSHOTS).map { snapshot("$it Hz", "$it") }

        val retained = retainSnapshots(snapshots)

        assertEquals(MAX_STORED_SNAPSHOTS, retained.size)
        assertEquals("1 Hz", retained.first().label)
        assertEquals("$MAX_STORED_SNAPSHOTS Hz", retained.last().label)
    }

    private fun snapshot(label: String, value: String) =
        SettingsSnapshot(label, mapOf("system/key" to value))
}
