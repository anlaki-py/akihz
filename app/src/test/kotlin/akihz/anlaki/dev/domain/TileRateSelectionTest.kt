package akihz.anlaki.dev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileRateSelectionTest {
    private val supportedRates = listOf(60f, 90f, 120f)

    @Test
    fun `rates are included by default`() {
        assertEquals(supportedRates, TileRateSelection.includedRates(supportedRates, emptySet()))
    }

    @Test
    fun `excluded rates are removed from tile cycle`() {
        val excluded = TileRateSelection.setIncluded(supportedRates, emptySet(), 120f, false)

        assertEquals(listOf(60f, 90f), TileRateSelection.includedRates(supportedRates, excluded))
        assertFalse(TileRateSelection.isIncluded(120f, excluded))
    }

    @Test
    fun `final included rate cannot be excluded`() {
        val excluded = setOf(60f, 90f)

        assertEquals(
            excluded,
            TileRateSelection.setIncluded(supportedRates, excluded, 120f, false)
        )
    }

    @Test
    fun `empty selection recovers without losing exclusions for another profile`() {
        val recovered = TileRateSelection.recoverEmptySelection(
            listOf(60f, 90f),
            setOf(60f, 90f, 120f)
        )

        assertEquals(setOf(120f), recovered)
        assertEquals(listOf(60f, 90f), TileRateSelection.includedRates(listOf(60f, 90f), recovered))
    }

    @Test
    fun `cycle skips excluded current rate and wraps`() {
        val included = listOf(60f, 90f)

        assertEquals(60f, TileRateSelection.nextRate(included, 120f))
        assertEquals(90f, TileRateSelection.nextRate(included, 60f))
        assertEquals(60f, TileRateSelection.nextRate(included, 90f))
    }

    @Test
    fun `minor float differences match persisted exclusions`() {
        assertTrue(TileRateSelection.includedRates(listOf(59.9401f), setOf(59.94f)).isEmpty())
    }
}
