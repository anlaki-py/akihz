package akihz.anlaki.dev.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionOwnershipTest {

    @Test
    fun firstOwnerStartsConnection() {
        assertTrue(ConnectionOwnership().acquire("activity"))
    }

    @Test
    fun additionalAndDuplicateOwnersDoNotStartAnotherConnection() {
        val ownership = ConnectionOwnership()
        ownership.acquire("activity")

        assertFalse(ownership.acquire("tile"))
        assertFalse(ownership.acquire("activity"))
    }

    @Test
    fun onlyFinalOwnerDisconnects() {
        val ownership = ConnectionOwnership()
        ownership.acquire("activity")
        ownership.acquire("tile")

        assertFalse(ownership.release("activity"))
        assertTrue(ownership.release("tile"))
    }

    @Test
    fun unknownOwnerDoesNotDisconnect() {
        val ownership = ConnectionOwnership()
        ownership.acquire("activity")

        assertFalse(ownership.release("unknown"))
        assertTrue(ownership.isActive())
    }
}
