package akihz.anlaki.dev.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshRateSettingValueTest {

    @Test
    fun `rate keys retain literal hz value`() {
        assertEquals(120, RefreshRateSettingValue.forKey("peak_refresh_rate", 120))
    }

    @Test
    fun `mode key uses discrete standard and high values`() {
        assertEquals(2, RefreshRateSettingValue.forKey("refresh_rate_mode", 60))
        assertEquals(3, RefreshRateSettingValue.forKey("refresh_rate_mode", 120))
    }

    @Test
    fun `feature keys use enabled value`() {
        assertEquals(1, RefreshRateSettingValue.forKey("active", 120))
        assertEquals(1, RefreshRateSettingValue.forKey("smooth_display_entry_point", 120))
    }
}
