package akihz.anlaki.dev.presentation.glass

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationContentShaderTest {
    @Test
    fun `ink becomes black over bright glass and white over dark glass`() {
        val bright = render(Color.WHITE, 0f)
        val dark = render(Color.BLACK, 0f)
        assertEquals(Color.BLACK, bright.getPixel(32, 32))
        assertEquals(Color.WHITE, dark.getPixel(32, 32))
        assertEquals(Color.WHITE, bright.getPixel(100, 32))
        assertEquals(Color.BLACK, dark.getPixel(100, 32))
    }

    @Test
    fun `moving lens bends an opaque glyph without turning the glyph into glass`() {
        val straight = render(Color.WHITE, 0f)
        val bent = render(Color.WHITE, 1f)
        // The lens's left edge pulls a stripe at x=28 outward to x=23.
        assertEquals(Color.WHITE, straight.getPixel(23, 32))
        assertTrue(Color.red(bent.getPixel(23, 32)) < 64)
        // The tab's untouched backdrop remains unchanged.
        assertEquals(Color.WHITE, bent.getPixel(180, 32))
    }

    private fun render(backgroundColor: Int, strength: Float): Bitmap {
        val input = Bitmap.createBitmap(240, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(input)
        val paint = Paint().apply { color = backgroundColor }
        canvas.drawRect(0f, 0f, 240f, 64f, paint)
        paint.color = Color.WHITE
        canvas.drawRect(28f, 84f, 36f, 108f, paint)
        val shader = RuntimeShader(navigationContentShader(2))
        shader.setInputShader("atlas", BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        shader.setFloatUniform("dimensions", 240f, 64f)
        shader.setFloatUniform("pillBounds", 20f, 6f, 132f, 52f)
        shader.setFloatUniform("outerLens", 0f, 4f)
        shader.setFloatUniform("pillLens", 8f, 12f)
        shader.setFloatUniform("strength", strength)
        shader.setFloatUniform("automaticContrast", 1f)
        shader.setFloatUniform("fixedInk", 1f)
        shader.setFloatUniform("outlineRadius", 0.65f)
        shader.setFloatUniform("regions", 66f, 132f, 176f, 52f)
        return Bitmap.createBitmap(240, 64, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawPaint(Paint().apply { this.shader = shader })
        }
    }
}
