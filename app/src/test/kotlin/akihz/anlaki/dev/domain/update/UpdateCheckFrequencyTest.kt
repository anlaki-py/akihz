package akihz.anlaki.dev.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCheckFrequencyTest {

    @Test
    fun `missing value defaults to daily`() {
        assertEquals(UpdateCheckFrequency.Daily, UpdateCheckFrequency.fromStoredValue(null))
    }

    @Test
    fun `unknown value defaults to daily`() {
        assertEquals(UpdateCheckFrequency.Daily, UpdateCheckFrequency.fromStoredValue("FutureValue"))
    }

    @Test
    fun `stored frequency is restored`() {
        assertEquals(
            UpdateCheckFrequency.EveryThreeDays,
            UpdateCheckFrequency.fromStoredValue(UpdateCheckFrequency.EveryThreeDays.name)
        )
    }
}
