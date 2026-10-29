package akihz.anlaki.dev.data.fps

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayStyleTest {
    @Test
    fun `alpha byte matches opacity percent`() {
        assertEquals(255, OverlayStyle.alphaByte(100))
        assertEquals(204, OverlayStyle.alphaByte(80))
        assertEquals(102, OverlayStyle.alphaByte(40))
    }

    @Test
    fun `alpha byte clamps to supported range`() {
        assertEquals(255, OverlayStyle.alphaByte(120))
        assertEquals(102, OverlayStyle.alphaByte(0))
    }

    @Test
    fun `pill fill keeps base color and applies opacity`() {
        val green80 = OverlayStyle.pillColor(OverlayPillColor.Green, 80)

        assertEquals(204, (green80 ushr 24) and 0xFF)
        assertEquals(0x15803D, green80 and 0x00FFFFFF)
    }

    @Test
    fun `white pill inverts text to black`() {
        assertEquals(0xFF000000.toInt(), OverlayStyle.textColor(OverlayPillColor.White))
        assertEquals(0xFFFFFFFF.toInt(), OverlayStyle.textColor(OverlayPillColor.Black))
        assertEquals(0xFFFFFFFF.toInt(), OverlayStyle.textColor(OverlayPillColor.Green))
        assertEquals(0xFFFFFFFF.toInt(), OverlayStyle.textColor(OverlayPillColor.Red))
    }

    @Test
    fun `text color stays fully opaque`() {
        for (color in OverlayPillColor.entries) {
            assertEquals(255, (OverlayStyle.textColor(color) ushr 24) and 0xFF)
        }
    }

    @Test
    fun `shape maps to pill or rectangle radius`() {
        assertEquals(999, OverlayStyle.pillRadiusDp(false))
        assertEquals(10, OverlayStyle.pillRadiusDp(true))
    }

    @Test
    fun `rectangle uses custom corner radius`() {
        assertEquals(0, OverlayStyle.pillRadiusDp(true, 0))
        assertEquals(20, OverlayStyle.pillRadiusDp(true, 20))
        assertEquals(999, OverlayStyle.pillRadiusDp(false, 0))
    }

    @Test
    fun `corner radius clamps to supported range`() {
        assertEquals(0, OverlayStyle.rectRadiusDp(-5))
        assertEquals(28, OverlayStyle.rectRadiusDp(99))
        assertEquals(0, OverlayStyle.pillRadiusDp(true, -5))
        assertEquals(28, OverlayStyle.pillRadiusDp(true, 99))
    }

    @Test
    fun `format honors unit toggle`() {
        assertEquals("60.0 FPS", OverlayStyle.formatFps(60.0, true))
        assertEquals("60.0", OverlayStyle.formatFps(60.0, false))
        assertEquals("0.0 FPS", OverlayStyle.formatFps(0.0, true))
    }

    @Test
    fun `missing or unknown stored color follows app theme`() {
        assertEquals(OverlayPillColor.Theme, OverlayPillColor.fromStoredValue(null))
        assertEquals(OverlayPillColor.Theme, OverlayPillColor.fromStoredValue("Purple"))
        assertEquals(OverlayPillColor.Red, OverlayPillColor.fromStoredValue("Red"))
    }

    @Test
    fun `saved explicit colors survive the theme default change`() {
        for (color in OverlayPillColor.entries) {
            assertEquals(color, OverlayPillColor.fromStoredValue(color.name))
        }
    }

    @Test
    fun `theme pill uses supplied palette and keeps text opaque`() {
        val background = 0xFFEADDFF.toInt()
        val foreground = 0x0021005D
        val fill = OverlayStyle.pillColor(OverlayPillColor.Theme, 80, background)
        assertEquals(0xCCEADDFF.toInt(), fill)
        assertEquals(0xFF21005D.toInt(), OverlayStyle.textColor(OverlayPillColor.Theme, foreground))
    }

    @Test
    fun `explicit color ignores theme palette`() {
        assertEquals(
            OverlayStyle.pillColor(OverlayPillColor.Green, 80),
            OverlayStyle.pillColor(OverlayPillColor.Green, 80, 0xFFEADDFF.toInt())
        )
        assertEquals(0xFF000000.toInt(), OverlayStyle.textColor(OverlayPillColor.White, 0xFFFFFFFF.toInt()))
    }
}
