package akihz.anlaki.dev.presentation.theme

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ThemeModeSwitchingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun repeatedLightDarkAndSystemSelectionsKeepTheScreenWorking() {
        var mode by mutableStateOf(AppThemeMode.System)
        compose.setContent {
            AnlakiTheme(darkTheme = mode == AppThemeMode.Dark, dynamicColor = false) {
                ThemeSection(mode, onThemeModeChanged = { mode = it }, onOpenThemePreferences = {})
            }
        }
        repeat(10) {
            listOf(AppThemeMode.Light, AppThemeMode.Dark, AppThemeMode.System).forEach { selected ->
                compose.onNodeWithText(selected.label).performClick().assertIsOn()
                compose.runOnIdle { assertEquals(selected, mode) }
            }
        }
        assertFalse(compose.activity.isFinishing)
    }
}
