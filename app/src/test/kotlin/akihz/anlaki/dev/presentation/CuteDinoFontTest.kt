@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.presentation.theme.ThemePreferencesScreen
import akihz.anlaki.dev.presentation.theme.AnlakiTheme
import akihz.anlaki.dev.presentation.theme.LocalDiagnosticFont
import akihz.anlaki.dev.presentation.theme.rememberCuteDinoFont
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CuteDinoFontTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        PreferencesHelper.init(RuntimeEnvironment.getApplication())
        PreferencesHelper.clear()
    }

    @After
    fun tearDown() {
        PreferencesHelper.clear()
    }

    @Test
    fun switchUpdatesTypographyAndLogsAndRestoresDefaults() {
        PreferencesHelper.cuteDinoEverywhere = false
        lateinit var typography: Typography
        lateinit var font: FontFamily
        lateinit var diagnosticFont: FontFamily
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                val currentTypography = MaterialTheme.typography
                val currentFont = rememberCuteDinoFont()
                val currentDiagnosticFont = LocalDiagnosticFont.current
                SideEffect {
                    typography = currentTypography
                    font = currentFont
                    diagnosticFont = currentDiagnosticFont
                }
                ThemePreferencesScreen(
                    dynamicColor = false,
                    amoledMode = false,
                    themedAppIcon = true,
                    onThemedAppIconChanged = {},
                    onDynamicColorChanged = {},
                    onAmoledModeChanged = {},
                    onOpenColorSchemes = {},
                    onBack = {}
                )
            }
        }
        compose.waitForIdle()
        val original = typography
        assertFalse(PreferencesHelper.cuteDinoEverywhere)
        compose.onNodeWithText("Cute Dino everywhere").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(PreferencesHelper.cuteDinoEverywhere)
            assertEquals(font, typography.bodyLarge.fontFamily)
            assertEquals(font, typography.titleMedium.fontFamily)
            assertEquals(font, typography.displayMediumEmphasized.fontFamily)
            assertEquals(font, typography.labelSmallEmphasized.fontFamily)
            assertEquals(font, diagnosticFont)
            assertEquals(original.bodyLarge.fontSize, typography.bodyLarge.fontSize)
        }
        compose.onNodeWithText("Cute Dino everywhere").performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(PreferencesHelper.cuteDinoEverywhere)
            assertEquals(original, typography)
            assertEquals(FontFamily.Monospace, diagnosticFont)
        }
    }

    @Test
    fun savedOptOutSurvivesPreferenceInitialization() {
        PreferencesHelper.cuteDinoEverywhere = false
        PreferencesHelper.init(RuntimeEnvironment.getApplication())
        assertFalse(PreferencesHelper.cuteDinoEverywhere)
    }

    @Test
    fun fontIsEnabledByDefaultInANewTheme() {
        assertTrue(PreferencesHelper.cuteDinoEverywhere)
        compose.setContent {
            AnlakiTheme(dynamicColor = false) {
                val typography = MaterialTheme.typography
                val font = rememberCuteDinoFont()
                SideEffect { assertEquals(font, typography.bodyMedium.fontFamily) }
            }
        }
        compose.waitForIdle()
    }
}
