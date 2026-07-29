package akihz.anlaki.dev.domain.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateChannelTest {

    @Test
    fun stableChannelRejectsPrereleases() {
        assertFalse(UpdateChannel.Stable.acceptsPrereleases)
    }

    @Test
    fun prereleaseChannelAcceptsPrereleases() {
        assertTrue(UpdateChannel.Prerelease.acceptsPrereleases)
    }
}
