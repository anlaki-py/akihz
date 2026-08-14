package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDebugSettingsTest {
    @Test
    fun `fake refresh rate preview is disabled by default`() {
        assertFalse(HomeDebugSettings.defaults().showFakeRefreshRates)
    }

    @Test
    fun `preview rates are sorted and retain device-specific rates`() {
        val rates = HomeDebugSettings.previewRates(listOf(59.94f, 120f))

        assertEquals(rates.sorted(), rates)
        assertTrue(59.94f in rates)
        assertTrue(360f in rates)
        assertEquals(1, rates.count { it == 120f })
    }
}
