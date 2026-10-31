package akihz.anlaki.dev.presentation.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
// Robolectric uses Android 12 graphics for SDK 33/34, which cannot compile AGSL.
@Config(sdk = [35], application = android.app.Application::class, manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavbarGlassShaderTest {
    @Test
    fun `native AGSL compiles the glass shader and accepts every uniform`() {
        val shader = RuntimeShader(NavbarGlassShader)
        shader.setFloatUniform("dimensions", 240f, 64f)
        shader.setFloatUniform("refraction", 7f)
        shader.setFloatUniform("edgeWidth", 14f)
        shader.setFloatUniform("dispersion", 0.5f)
        shader.setFloatUniform("blurRadius", 0f)
        shader.setFloatUniform("tintOpacity", 0.25f)
        shader.setColorUniform("tint", android.graphics.Color.WHITE)
        shader.setFloatUniform("saturation", 1f)
        shader.setFloatUniform("brightness", 1f)
        shader.setFloatUniform("highlight", 0.35f)
        shader.setFloatUniform("lightDirection", -0.7f, -0.7f)
        assertNotNull(RenderEffect.createRuntimeShaderEffect(shader, "backdrop"))
    }
}
