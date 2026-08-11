package akihz.anlaki.dev.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVerificationGateTest {
    @Test
    fun `same download and digest are verified only once`() = runTest {
        val gate = UpdateVerificationGate()
        var attempts = 0

        repeat(2) {
            assertTrue(
                gate.verify(42L, "ABCD") {
                    attempts += 1
                    true
                }
            )
        }

        assertEquals(1, attempts)
    }

    @Test
    fun `new digest invalidates cached verification`() = runTest {
        val gate = UpdateVerificationGate()
        var attempts = 0

        gate.verify(42L, "first") { attempts += 1; true }
        gate.verify(42L, "second") { attempts += 1; true }

        assertEquals(2, attempts)
    }
}
