package akihz.anlaki.dev.presentation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.changelog.BundledChangelog
import akihz.anlaki.dev.presentation.components.TileInclusionButton
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HoldHelpTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun versionHoldOpensOfflineNotesWithoutCountingAsADebugTap() {
        var taps = 0
        compose.setContent { AnlakiTheme(dynamicColor = false) { AboutSection { taps++ } } }
        compose.onNodeWithText("akiHz").performTouchInput { longClick() }
        compose.onNodeWithText("What's new").assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("Full changelog")).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, taps)
        compose.onNodeWithText("Full changelog").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("0.0.50 - 2026-10-26"))
        compose.onNodeWithText("0.0.50 - 2026-10-26").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("akiHz").performClick()
        assertEquals(1, taps)
    }

    @Test
    fun holdsExplainTileChoicesAndShortTapsStillToggle() {
        var checked by mutableStateOf(false)
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                TileInclusionButton(checked, true, "60 Hz tile choice", { checked = !checked }, Modifier.size(96.dp))
            }
        }
        compose.onNodeWithContentDescription("60 Hz tile choice").performTouchInput { longClick() }
        compose.onNodeWithText("Quick Settings tile").assertIsDisplayed()
        assertFalse(checked)
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithContentDescription("60 Hz tile choice").assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun lastIncludedRateStillOffersHelpButCannotBeToggledOff() {
        var toggles = 0
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                TileInclusionButton(true, false, "60 Hz tile choice", { toggles++ }, Modifier.size(96.dp))
            }
        }
        compose.onNodeWithContentDescription("60 Hz tile choice").performClick()
        compose.onNodeWithContentDescription("60 Hz tile choice").performTouchInput { longClick() }
        compose.onNodeWithText("Quick Settings tile").assertIsDisplayed()
        assertEquals(0, toggles)
    }

    @Test
    fun buildBundlesTheCompleteHistoryAsAnAsset() = runBlocking {
        val changelog = BundledChangelog.load(RuntimeEnvironment.getApplication())
        assertTrue(changelog.releases.any { it.version == "Unreleased" })
        assertTrue(changelog.releases.any { it.version == "0.0.50" })
        assertTrue(changelog.releases.any { it.version == "0.0.0-beta.main.0" })
        assertTrue(changelog.releases.size > 40)
    }
}
