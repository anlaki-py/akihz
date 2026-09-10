package akihz.anlaki.dev.data.fps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `edge follows text color and stays visible`() {
        val edgeOnWhite = OverlayStyle.strokeColor(OverlayPillColor.White)
        val edgeOnBlack = OverlayStyle.strokeColor(OverlayPillColor.Black)

        assertEquals(0xFF000000.toInt() and 0x00FFFFFF, edgeOnWhite and 0x00FFFFFF)
        assertEquals(0xFFFFFFFF.toInt() and 0x00FFFFFF, edgeOnBlack and 0x00FFFFFF)
        assertTrue(((edgeOnWhite ushr 24) and 0xFF) > 0)
    }

    @Test
    fun `shape maps to pill or rectangle radius`() {
        assertEquals(999, OverlayStyle.pillRadiusDp(false))
        assertEquals(10, OverlayStyle.pillRadiusDp(true))
    }

    @Test
    fun `format honors unit toggle`() {
        assertEquals("60.0 FPS", OverlayStyle.formatFps(60.0, true))
        assertEquals("60.0", OverlayStyle.formatFps(60.0, false))
        assertEquals("0.0 FPS", OverlayStyle.formatFps(0.0, true))
    }

    @Test
    fun `unknown stored color falls back to black`() {
        assertEquals(OverlayPillColor.Black, OverlayPillColor.fromStoredValue(null))
        assertEquals(OverlayPillColor.Black, OverlayPillColor.fromStoredValue("Purple"))
        assertEquals(OverlayPillColor.Red, OverlayPillColor.fromStoredValue("Red"))
    }
}
