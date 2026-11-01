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
    fun `wide navbar choice survives export and old presets stay compact`() {
        val wide = NavbarGlassSettings(wide = true)
        assertTrue(NavbarGlassSettings.fromJson(wide.toJson()).wide)
        val oldPreset = JSONObject(wide.toJson()).apply { remove("wide") }
        assertFalse(NavbarGlassSettings.fromJson(oldPreset.toString()).wide)
    }

    @Test
    fun `defaults match the selected preset with glass enabled`() {
        val settings = NavbarGlassSettings()
        val expected = NavbarGlassSettings.fromJson("""{
            "format":"akihz-navbar-glass", "version":1, "enabled":true,
            "outer": {"useThemeTint":true, "Refraction":9.595243453979492,
                "EdgeWidth":4.5190205574035645, "Dispersion":0.5, "Blur":7.667381286621094,
                "Tint":0.1033502146601677, "Hue":0, "TintSaturation":0.6499999761581421,
                "TintBrightness":0.8500000238418579, "Saturation":2, "Brightness":1,
                "Highlight":0.3499999940395355, "LightAngle":180.02334594726562},
            "pill": {"useThemeTint":true, "Refraction":7.19543981552124,
                "EdgeWidth":8.105807304382324, "Dispersion":0.5, "Blur":0,
                "Tint":0.0961742028594017, "Hue":0, "TintSaturation":0.6499999761581421,
                "TintBrightness":0.10000000149011612, "Saturation":1.0009421110153198,
                "Brightness":0.9005719423294067, "Highlight":0.3499999940395355, "LightAngle":360}
        }""")
        assertTrue(settings.enabled)
        assertTrue(settings.outer.useThemeTint)
        assertTrue(settings.pill.useThemeTint)
        GlassParameter.entries.forEach {
            assertEquals(expected.outer[it], settings.outer[it], 0f)
            assertEquals(expected.pill[it], settings.pill[it], 0f)
        }
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
        assertEquals(7.667381286621094f, restored.outer[GlassParameter.Blur], 0f)
    }

    @Test
    fun `out of range and nonfinite controls cannot reach the shader`() {
        val defaults = NavbarGlassSettings()
        assertEquals(24f, defaults.outer.withValue(GlassParameter.Refraction, 1000f)[GlassParameter.Refraction], 0f)
        assertEquals(0f, defaults.outer.withValue(GlassParameter.Blur, -1f)[GlassParameter.Blur], 0f)
        assertEquals(0f, defaults.pill.withValue(GlassParameter.Blur, Float.NaN)[GlassParameter.Blur], 0f)
        val json = JSONObject(defaults.toJson())
        json.getJSONObject("pill").put("Blur", 99999)
        assertEquals(12f, NavbarGlassSettings.fromJson(json.toString()).pill[GlassParameter.Blur], 0f)
    }

    @Test
    fun `partial preset fills missing fields from the correct layer defaults`() {
        val restored = NavbarGlassSettings.fromJson("""{"format":"akihz-navbar-glass","version":1}""")
        assertTrue(restored.enabled)
        assertEquals(7.667381286621094f, restored.outer[GlassParameter.Blur], 0f)
        assertEquals(0f, restored.pill[GlassParameter.Blur], 0f)
    }

    @Test
    fun `saved opt out and custom blur survive the new defaults`() {
        val saved = NavbarGlassSettings(enabled = false)
        val custom = saved.copy(outer = saved.outer.withValue(GlassParameter.Blur, 3f))
        val restored = NavbarGlassSettings.fromJson(custom.toJson())
        assertFalse(restored.enabled)
        assertEquals(3f, restored.outer[GlassParameter.Blur], 0f)
    }

    @Test
    fun `old presets gain neutral glass and adaptive ink without losing optical controls`() {
        val restored = NavbarGlassSettings.fromJson("""{
            "format":"akihz-navbar-glass", "version":1,
            "outer":{"Refraction":5, "Blur":3}
        }""")
        assertTrue(restored.monochrome)
        assertTrue(restored.automaticContrast)
        assertEquals(0.35f, restored.contentRefraction, 0f)
        assertEquals(5f, restored.outer[GlassParameter.Refraction], 0f)
        assertEquals(3f, restored.outer[GlassParameter.Blur], 0f)
    }

    @Test
    fun `content controls survive export and unsafe refraction is bounded`() {
        val settings = NavbarGlassSettings(monochrome = false, automaticContrast = false, contentRefraction = 0.6f)
        val restored = NavbarGlassSettings.fromJson(settings.toJson())
        assertFalse(restored.monochrome)
        assertFalse(restored.automaticContrast)
        assertEquals(0.6f, restored.contentRefraction, 0f)
        val json = JSONObject(settings.toJson()).put("contentRefraction", 20)
        assertEquals(1f, NavbarGlassSettings.fromJson(json.toString()).contentRefraction, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `future preset versions are rejected`() {
        NavbarGlassSettings.fromJson("""{"format":"akihz-navbar-glass","version":2}""")
    }
}
