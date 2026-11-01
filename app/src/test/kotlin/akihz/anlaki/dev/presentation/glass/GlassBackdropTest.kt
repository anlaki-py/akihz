package akihz.anlaki.dev.presentation.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import akihz.anlaki.dev.presentation.components.AppTab
import akihz.anlaki.dev.presentation.components.FloatingBottomBar
import akihz.anlaki.dev.presentation.components.FloatingNavigationItem
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    @Test
    fun `glass navbar updates its ink when the page changes and keeps tabs clickable`() {
        var pageColor by mutableStateOf(Color.White)
        var selected by mutableIntStateOf(0)
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                val backdrop = rememberGlassBackdrop()
                Box(Modifier.size(320.dp, 110.dp)) {
                    Box(Modifier.fillMaxSize().recordGlassBackdrop(backdrop, pageColor))
                    FloatingBottomBar(
                        items = AppTab.entries.mapIndexed { index, tab ->
                            FloatingNavigationItem(tab.label, tab.icon) { selected = index }
                        },
                        selectedIndex = selected,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        // Contrast reads the rendered glass on frames, so let the sampler settle first.
        compose.mainClock.advanceTimeBy(1_000)
        val bright = compose.onNodeWithContentDescription("Home", useUnmergedTree = true).captureToImage().toPixelMap()
        var darkPixels = 0
        for (y in 0 until bright.height) for (x in 0 until bright.width) {
            if (bright[x, y].red < 0.2f) darkPixels++
        }
        assertTrue("Bright glass needs dark ink", darkPixels > bright.width * bright.height / 10)
        println("NAV_TEST bright dark=$darkPixels total=${bright.width * bright.height}")
        compose.runOnIdle { pageColor = Color.Black }
        compose.mainClock.advanceTimeBy(1_000)
        val dark = compose.onNodeWithContentDescription("Home", useUnmergedTree = true).captureToImage().toPixelMap()
        var lightPixels = 0
        for (y in 0 until dark.height) for (x in 0 until dark.width) {
            if (dark[x, y].red > 0.8f) lightPixels++
        }
        println("NAV_TEST dark light=$lightPixels total=${dark.width * dark.height} corner=${dark[0, 0]} center=${dark[dark.width / 2, dark.height / 2]}")
        assertTrue("Dark glass needs light ink", lightPixels > dark.width * dark.height / 10)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.runOnIdle { assertEquals(1, selected) }
    }

}
