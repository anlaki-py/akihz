package akihz.anlaki.dev.presentation

import akihz.anlaki.dev.presentation.theme.PresetThemes
import akihz.anlaki.dev.presentation.theme.parseHsl
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeColorInputTest {

    @Test
    fun `parses percent HSL input`() {
        assertArrayEquals(floatArrayOf(267f, 0.36f, 0.48f), parseHsl("hsl(267 36% 48%)"), 0.001f)
    }

    @Test
    fun `parses fractional HSL input and clamps its bounds`() {
        assertArrayEquals(floatArrayOf(360f, 0.5f, 0f), parseHsl("400, 0.5, -1"), 0.001f)
    }

    @Test
    fun `rejects incomplete HSL input`() {
        assertNull(parseHsl("hsl(120 50%)"))
    }

    @Test
    fun `ships the same preset order as RikkaHub`() {
        assertEquals(
            listOf("sakura", "ocean", "spring", "autumn", "black", "minimal", "claude"),
            PresetThemes.map { it.id }
        )
    }
}
