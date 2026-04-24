package akihz.anlaki.dev.data

import android.os.Build
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class OemSettingsStrategyTest {

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "")
    }

    @Test
    fun `realme brand returns realme keyset with system write keys`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "realme")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "realme")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(
            listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate"),
            keySet.readKeys
        )
        assertEquals(
            listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate"),
            keySet.writeKeys
        )
        assertEquals(listOf("peak_refresh_rate"), keySet.systemWriteKeys)
    }

    @Test
    fun `realme manufacturer with different brand still returns realme keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "realme")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(listOf("peak_refresh_rate"), keySet.systemWriteKeys)
    }

    @Test
    fun `xiaomi manufacturer returns xiaomi keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Xiaomi")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(listOf("miui_refresh_rate", "user_refresh_rate"), keySet.readKeys)
        assertEquals(listOf("miui_refresh_rate", "user_refresh_rate"), keySet.writeKeys)
    }

    @Test
    fun `redmi manufacturer returns xiaomi keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Redmi")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Redmi")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(listOf("miui_refresh_rate", "user_refresh_rate"), keySet.readKeys)
    }

    @Test
    fun `xiaomi brand with different manufacturer returns xiaomi keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "SomeOEM")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "xiaomi")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(listOf("miui_refresh_rate", "user_refresh_rate"), keySet.readKeys)
    }

    @Test
    fun `samsung manufacturer returns samsung keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(
            listOf("peak_refresh_rate", "min_refresh_rate", "user_refresh_rate"),
            keySet.readKeys
        )
        assertEquals(
            listOf("peak_refresh_rate", "min_refresh_rate", "user_refresh_rate"),
            keySet.writeKeys
        )
    }

    @Test
    fun `unknown manufacturer returns generic keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Google")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "google")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(
            listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate"),
            keySet.readKeys
        )
        assertEquals(
            listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate"),
            keySet.writeKeys
        )
    }

    @Test
    fun `empty manufacturer returns generic keyset`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "")

        val keySet = OemSettingsStrategy.resolve()

        assertEquals(
            listOf("user_refresh_rate", "peak_refresh_rate", "min_refresh_rate"),
            keySet.readKeys
        )
    }
}
