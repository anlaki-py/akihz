package akihz.anlaki.dev.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailPageNavigationTest {

    @Test
    fun `back from color schemes returns to theme preferences`() {
        assertEquals(
            DetailPage.ThemePreferences,
            detailBackDestination(DetailPage.ThemePalette)
        )
    }

    @Test
    fun `back from theme preferences returns to settings`() {
        assertNull(detailBackDestination(DetailPage.ThemePreferences))
    }

    @Test
    fun `back with no detail page stays at the app root`() {
        assertNull(detailBackDestination(null))
    }
}
