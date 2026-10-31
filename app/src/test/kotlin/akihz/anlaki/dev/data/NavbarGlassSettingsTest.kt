package akihz.anlaki.dev.data

import akihz.anlaki.dev.data.glass.GlassParameter
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class, manifest = Config.NONE)
class NavbarGlassSettingsTest {
    @Test
    fun `experiment defaults to off with clear outer glass and lightly blurred pill`() {
        val settings = NavbarGlassSettings()
        assertFalse(settings.enabled)
        assertEquals(0f, settings.outer[GlassParameter.Blur], 0f)
        assertEquals(2f, settings.pill[GlassParameter.Blur], 0f)
    }

    @Test
    fun `export round trip retains both layers and every control`() {
        val defaults = NavbarGlassSettings()
        val original = defaults.copy(
            enabled = true,
            outer = defaults.outer.copy(useThemeTint = false).withValue(GlassParameter.Hue, 180f),
            pill = defaults.pill.withValue(GlassParameter.Blur, 4f)
        )
        val restored = NavbarGlassSettings.fromJson(original.toJson())
        assertTrue(restored.enabled)
        assertFalse(restored.outer.useThemeTint)
        GlassParameter.entries.forEach {
            assertEquals(original.outer[it], restored.outer[it], 0f)
            assertEquals(original.pill[it], restored.pill[it], 0f)
        }
        assertEquals(0f, restored.outer[GlassParameter.Blur], 0f)
    }

    @Test
    fun `out of range and nonfinite controls cannot reach the shader`() {
        val defaults = NavbarGlassSettings()
        assertEquals(24f, defaults.outer.withValue(GlassParameter.Refraction, 1000f)[GlassParameter.Refraction], 0f)
        assertEquals(0f, defaults.outer.withValue(GlassParameter.Blur, -1f)[GlassParameter.Blur], 0f)
        assertEquals(2f, defaults.pill.withValue(GlassParameter.Blur, Float.NaN)[GlassParameter.Blur], 0f)
        val json = JSONObject(defaults.toJson())
        json.getJSONObject("pill").put("Blur", 99999)
        assertEquals(12f, NavbarGlassSettings.fromJson(json.toString()).pill[GlassParameter.Blur], 0f)
    }

    @Test
    fun `partial preset fills missing fields from the correct layer defaults`() {
        val restored = NavbarGlassSettings.fromJson("""{"format":"akihz-navbar-glass","version":1}""")
        assertFalse(restored.enabled)
        assertEquals(0f, restored.outer[GlassParameter.Blur], 0f)
        assertEquals(2f, restored.pill[GlassParameter.Blur], 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `future preset versions are rejected`() {
        NavbarGlassSettings.fromJson("""{"format":"akihz-navbar-glass","version":2}""")
    }
}
