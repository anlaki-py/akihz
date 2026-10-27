package akihz.anlaki.dev.presentation.navigation

import android.app.Application
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.width
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import akihz.anlaki.dev.presentation.theme.ThemePreferencesScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppNavigationDisplayTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun themeSwitchCanToggleBothWaysWithoutLeavingThePage() {
        val navigator = AppNavigator(AppDestination.ThemePreferences)
        var dynamicColor by mutableStateOf(false)
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                ThemePage(navigator, dynamicColor) { dynamicColor = it }
            }
        }

        val row = compose.onNodeWithText("Dynamic color")
        row.performScrollTo().assertIsOff().performClick().assertIsOn()
        row.performClick().assertIsOff()
        compose.runOnIdle { assertEquals(AppDestination.ThemePreferences, navigator.current) }
    }

    @Test
    fun retainedPageReceivesNewValuesAndCallbacks() {
        val navigator = AppNavigator()
        var value by mutableStateOf("60 Hz")
        var firstCalls = 0
        var replacementCalls = 0
        var callback by mutableStateOf<() -> Unit>({ firstCalls++ })
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                ValuePage(navigator, value, callback)
            }
        }
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            value = "120 Hz"
            callback = { replacementCalls++ }
        }

        compose.onNodeWithTag("value").assertTextEquals("120 Hz")
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            assertEquals(1, firstCalls)
            assertEquals(1, replacementCalls)
        }
    }

    @Test
    fun backPreviewTracksTheSwipeAndCancelKeepsButtonsWorking() {
        val navigator = AppNavigator(AppDestination.ThemePreferences)
        var clicks = 0
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                AppNavigationDisplay(navigator.backStack, onBack = { navigator.pop() }) { page ->
                    Column(Modifier.fillMaxSize().testTag(page.name)) {
                        Button(onClick = { clicks++ }) { Text("Tap ${page.name}") }
                    }
                }
            }
        }
        val detail = compose.onNodeWithTag(AppDestination.ThemePreferences.name)
        val width = detail.getUnclippedBoundsInRoot().width.value
        compose.mainClock.autoAdvance = false
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnIdle {
            dispatcher.dispatchOnBackStarted(backEvent(0f))
        }
        compose.mainClock.advanceTimeBy(32)
        listOf(0.25f, 0.5f, 0.25f).forEach { progress ->
            compose.runOnIdle { dispatcher.dispatchOnBackProgressed(backEvent(progress)) }
            compose.mainClock.advanceTimeBy(32)
            val fraction = detail.getUnclippedBoundsInRoot().left.value / width
            assertEquals(progress, fraction, 0.04f)
        }
        compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Tap ThemePreferences").performClick()
        compose.runOnIdle {
            assertEquals(AppDestination.ThemePreferences, navigator.current)
            assertEquals(1, clicks)
        }

        compose.runOnIdle { dispatcher.dispatchOnBackStarted(backEvent(0f)) }
        compose.runOnIdle { dispatcher.dispatchOnBackProgressed(backEvent(0.5f)) }
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("Tap Root").performClick()
        compose.runOnIdle {
            assertEquals(AppDestination.Root, navigator.current)
            assertEquals(2, clicks)
        }
    }

    private fun backEvent(progress: Float) = BackEventCompat(
        touchX = 100f * progress,
        touchY = 200f,
        progress = progress,
        swipeEdge = BackEventCompat.EDGE_LEFT
    )
}

// Pass values across a composable boundary, just as MainActivity does. Reading
// mutableState directly inside an entry would hide the stale captured-value bug.
@Composable
private fun ThemePage(navigator: AppNavigator, dynamicColor: Boolean, onChange: (Boolean) -> Unit) {
    AppNavigationDisplay(navigator.backStack, onBack = { navigator.pop() }) {
        ThemePreferencesScreen(
            dynamicColor = dynamicColor,
            amoledMode = false,
            themedAppIcon = true,
            onThemedAppIconChanged = {},
            onDynamicColorChanged = onChange,
            onAmoledModeChanged = {},
            onOpenColorSchemes = {},
            onBack = { navigator.pop() }
        )
    }
}

@Composable
private fun ValuePage(navigator: AppNavigator, value: String, onClick: () -> Unit) {
    AppNavigationDisplay(navigator.backStack, onBack = { navigator.pop() }) {
        Column {
            Text(value, Modifier.testTag("value"))
            Button(onClick = onClick) { Text("Apply") }
        }
    }
}
