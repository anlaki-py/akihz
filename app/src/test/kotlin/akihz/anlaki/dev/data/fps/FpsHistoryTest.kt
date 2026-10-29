package akihz.anlaki.dev.data.fps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FpsHistoryTest {
    @Test
    fun `keeps chronological measurements with real timestamps`() {
        val history = FpsHistory()
        history.record("app/layer", 1000, 60.0)
        val samples = history.record("app/layer", 4000, 42.5)
        assertEquals(listOf(FpsSample(1000, 60.0), FpsSample(4000, 42.5)), samples)
    }

    @Test
    fun `changing source discards the previous app or layer`() {
        val history = FpsHistory()
        history.record("app/first", 1000, 60.0)
        assertEquals(listOf(FpsSample(1500, 30.0)), history.record("app/second", 1500, 30.0))
        assertEquals(listOf(FpsSample(2000, 90.0)), history.record("other/second", 2000, 90.0))
    }

    @Test
    fun `drops samples older than one minute`() {
        val history = FpsHistory()
        history.record("app/layer", 0, 60.0)
        history.record("app/layer", 500, 55.0)
        assertEquals(listOf(FpsSample(60_501, 50.0)), history.record("app/layer", 60_501, 50.0))
    }

    @Test
    fun `bounds memory even when samples arrive faster than expected`() {
        val history = FpsHistory()
        repeat(1000) { history.record("app/layer", it.toLong(), 60.0) }
        val samples = history.record("app/layer", 1000, 60.0)
        assertEquals(121, samples.size)
        assertEquals(880L, samples.first().timeMs)
    }

    @Test
    fun `rejects invalid values and duplicate or reversed timestamps`() {
        val history = FpsHistory()
        val expected = history.record("app/layer", 1000, 60.0)
        for (fps in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0)) {
            assertEquals(expected, history.record("app/layer", 1500, fps))
        }
        assertEquals(expected, history.record("app/layer", 1000, 30.0))
        assertEquals(expected, history.record("app/layer", 500, 30.0))
    }

    @Test
    fun `clear removes stale data and snapshots remain unchanged`() {
        val history = FpsHistory()
        val old = history.record("app/layer", 1000, 60.0)
        history.clear()
        assertTrue(history.record("app/layer", 1500, Double.NaN).isEmpty())
        assertEquals(listOf(FpsSample(1000, 60.0)), old)
    }
}
