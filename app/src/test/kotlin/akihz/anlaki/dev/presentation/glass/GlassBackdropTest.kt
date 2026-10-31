package akihz.anlaki.dev.presentation.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassBackdropTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `empty page areas record the page background instead of showing the tint underneath`() {
        val pageColor = Color(0xFF1F1E1D)
        compose.setContent {
            val backdrop = rememberGlassBackdrop()
            // Like Home, the content itself has no background. The contrasting parent
            // exposes a transparent capture if the page background is omitted.
            Box(Modifier.size(120.dp).background(Color(0xFFFF8030))) {
                Box(Modifier.fillMaxSize().recordGlassBackdrop(backdrop, pageColor).testTag("page")) {
                    Box(Modifier.size(24.dp).background(Color.White))
                }
            }
        }
        val pixels = compose.onNodeWithTag("page").captureToImage().toPixelMap()
        assertEquals(pageColor.toArgb(), pixels[pixels.width / 2, pixels.height / 2].toArgb())
        assertEquals(Color.White.toArgb(), pixels[2, 2].toArgb())
    }
}
