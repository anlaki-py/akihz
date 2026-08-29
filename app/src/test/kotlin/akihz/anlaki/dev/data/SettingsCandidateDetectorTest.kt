package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCandidateDetectorTest {
    @Test
    fun `snapshot changes outrank keyword-only candidates`() {
        val current = mapOf(
            "system/peak_refresh_rate" to "120",
            "secure/min_refresh_rate" to "2"
        )
        val snapshots = listOf(
            SettingsSnapshot("Baseline", current + ("secure/min_refresh_rate" to "1")),
            SettingsSnapshot("120 Hz", current)
        )

        val candidates = SettingsCandidateDetector.detect(current, snapshots)

        assertEquals("secure/min_refresh_rate", candidates.first().id)
        assertTrue(candidates.first().reason.contains("changed across snapshots"))
    }

    @Test
    fun `sparse difference snapshots mark only changed keys`() {
        val current = mapOf(
            "secure/min_refresh_rate" to "2",
            "secure/unrelated" to "plain"
        )
        val snapshots = listOf(
            SettingsSnapshot("Baseline", current + ("secure/min_refresh_rate" to "1")),
            SettingsSnapshot(
                label = "120 Hz",
                values = mapOf("secure/min_refresh_rate" to "2"),
                isDiff = true
            )
        )

        val candidates = SettingsCandidateDetector.detect(current, snapshots)

        assertEquals(listOf("secure/min_refresh_rate"), candidates.map { it.id })
        assertTrue(candidates.single().reason.contains("changed across snapshots"))
    }

    @Test
    fun `only refresh_rate keys are surfaced`() {
        val current = mapOf(
            "system/peak_refresh_rate" to "120",
            "system/vendor_mode" to "2",
            "secure/app_refresh_rate" to "60",
            "secure/unrelated" to "plain"
        )
        val candidates = SettingsCandidateDetector.detect(current, emptyList())

        assertTrue(candidates.all { it.name.contains("refresh_rate", ignoreCase = true) })
        assertEquals(setOf("system/peak_refresh_rate", "secure/app_refresh_rate"), candidates.map { it.id }.toSet())
    }
}
