package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OemSettingsStrategyTest {

    @Test
    fun `every displayed override resolves to its expected primary key`() {
        val expectedKeys = mapOf(
            "Xiaomi / Redmi" to "miui_refresh_rate",
            "Samsung" to "refresh_rate_mode",
            "OnePlus" to "peak_refresh_rate",
            "OPPO" to "peak_refresh_rate",
            "vivo / iQOO" to "active",
            "realme" to "user_refresh_rate",
            "ASUS / ROG" to "peak_refresh_rate",
            "Motorola" to "peak_refresh_rate",
            "Sony" to "peak_refresh_rate",
            "Google Pixel" to "peak_refresh_rate",
            "AOSP / Stock" to "user_refresh_rate"
        )

        expectedKeys.forEach { (label, expectedKey) ->
            val actualKey = OemSettingsStrategy.resolveByName(label).readKeys.first().key
            assertEquals("Unexpected strategy for $label", expectedKey, actualKey)
        }
    }
}
