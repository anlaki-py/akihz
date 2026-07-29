package akihz.anlaki.dev.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomRefreshProfileTest {
    @Test
    fun `complete writable mappings validate`() {
        val profile = CustomRefreshProfile(
            rates = listOf(60f, 120f),
            keys = listOf(
                CustomSettingsKey(
                    OemSettingsStrategy.Namespace.SYSTEM,
                    "peak_refresh_rate",
                    values = mapOf("60.0" to "60", "120.0" to "120")
                )
            )
        )

        assertNull(profile.validationError())
    }

    @Test
    fun `missing writable mapping blocks activation`() {
        val profile = CustomRefreshProfile(
            rates = listOf(60f, 120f),
            keys = listOf(
                CustomSettingsKey(
                    OemSettingsStrategy.Namespace.SECURE,
                    "refresh_rate_mode",
                    values = mapOf("60.0" to "2")
                )
            )
        )

        assertNotNull(profile.validationError())
    }
}
