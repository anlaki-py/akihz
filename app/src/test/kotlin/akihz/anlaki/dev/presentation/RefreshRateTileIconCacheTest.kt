package akihz.anlaki.dev.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class RefreshRateTileIconCacheTest {
    @Test
    fun `rates with the same displayed label reuse one icon`() {
        var allocations = 0
        val cache = RefreshRateTileIconCache<Any> {
            allocations++
            Any()
        }

        val first = cache.get(59.94f)
        val second = cache.get(60f)

        assertSame(first, second)
        assertEquals(1, allocations)
    }

    @Test
    fun `clearing cache releases icon for replacement`() {
        val cache = RefreshRateTileIconCache<Any> { Any() }
        val first = cache.get(120f)

        cache.clear()

        assertNotSame(first, cache.get(120f))
    }
}
