package akihz.anlaki.dev.presentation.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FloatingBottomBarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectionPillSlidesPastItsDestinationThenSettles() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                FloatingBottomBar(
                    items = AppTab.entries.mapIndexed { index, tab ->
                        FloatingNavigationItem(tab.label, tab.icon) { selected = index }
                    },
                    selectedIndex = selected
                )
            }
        }
        val indicator = compose.onNodeWithTag("navigation_indicator", useUnmergedTree = true)
        val start = indicator.getUnclippedBoundsInRoot().left.value
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Settings").performClick()
        var furthest = start
        repeat(30) {
            compose.mainClock.advanceTimeByFrame()
            furthest = maxOf(furthest, indicator.getUnclippedBoundsInRoot().left.value)
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val settled = indicator.getUnclippedBoundsInRoot().left.value
        assertEquals(60f, settled - start, 1f)
        assertTrue("The pill should visibly overshoot", furthest > settled + 1f)
        assertTrue("The bounce should stay inside the bar padding", furthest < settled + 6f)
    }

    @Test
    fun tabsRemainClickableDuringReversalsAndBarWidthStaysFixed() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                FloatingBottomBar(
                    items = AppTab.entries.mapIndexed { index, tab ->
                        FloatingNavigationItem(tab.label, tab.icon) { selected = index }
                    },
                    selectedIndex = selected,
                    modifier = Modifier.testTag("navbar")
                )
            }
        }
        val width = compose.onNodeWithTag("navbar").getUnclippedBoundsInRoot().width.value
        compose.mainClock.autoAdvance = false
        repeat(3) {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.mainClock.advanceTimeBy(96)
            compose.onNodeWithContentDescription("Settings").assertIsSelected()
            assertEquals(width, compose.onNodeWithTag("navbar").getUnclippedBoundsInRoot().width.value, 1f)
            compose.onNodeWithContentDescription("Home").performClick()
            compose.mainClock.advanceTimeBy(96)
            compose.onNodeWithContentDescription("Home").assertIsSelected()
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(width, compose.onNodeWithTag("navbar").getUnclippedBoundsInRoot().width.value, 1f)
    }
}
