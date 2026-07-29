package akihz.anlaki.dev.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateAvailabilityTest {

    @Test
    fun newerStableReleaseIsAvailable() {
        val result = resolveUpdateAvailability(100, 101, UpdateChannel.Stable)

        assertEquals(UpdateAvailability.Available, result)
    }

    @Test
    fun prereleaseAheadOfStableWaitsForNextStable() {
        val result = resolveUpdateAvailability(102, 101, UpdateChannel.Stable)

        assertEquals(UpdateAvailability.AheadOfStable, result)
    }

    @Test
    fun equalStableReleaseIsUpToDate() {
        val result = resolveUpdateAvailability(101, 101, UpdateChannel.Stable)

        assertEquals(UpdateAvailability.UpToDate, result)
    }

    @Test
    fun prereleaseChannelDoesNotOfferDowngrade() {
        val result = resolveUpdateAvailability(102, 101, UpdateChannel.Prerelease)

        assertEquals(UpdateAvailability.UpToDate, result)
    }
}
