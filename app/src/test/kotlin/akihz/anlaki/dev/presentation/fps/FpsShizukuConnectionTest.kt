package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.utils.ErrorType
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FpsShizukuConnectionTest {
    private val connections = mutableListOf<() -> Unit>()
    private val failures = mutableListOf<(ErrorType, String) -> Unit>()
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        mockkObject(ShizukuHelper)
        every { ShizukuHelper.acquireUserService(any(), any(), any()) } answers {
            connections += secondArg<() -> Unit>()
            failures += thirdArg<(ErrorType, String) -> Unit>()
        }
        every { ShizukuHelper.releaseUserService(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(ShizukuHelper)
    }

    @Test
    fun `connection waits for readiness and retains its owner`() = runTest {
        val result = async { awaitFpsShizukuConnection("fps", messages::add) }
        runCurrent()
        assertFalse(result.isCompleted)

        connections.single().invoke()

        assertTrue(result.await())
        verify(exactly = 0) { ShizukuHelper.releaseUserService(any()) }
    }

    @Test
    fun `failed connection releases its owner and allows another attempt`() = runTest {
        val first = async { awaitFpsShizukuConnection("fps", messages::add) }
        runCurrent()
        failures.single().invoke(ErrorType.SERVICE_BINDING_FAILED, "Disconnected")
        assertFalse(first.await())
        assertEquals(listOf("Disconnected"), messages)
        verify(exactly = 1) { ShizukuHelper.releaseUserService("fps") }

        val retry = async { awaitFpsShizukuConnection("fps", messages::add) }
        runCurrent()
        connections.last().invoke()
        assertTrue(retry.await())
    }

    @Test
    fun `missing callback times out and releases the pending connection`() = runTest {
        val result = async { awaitFpsShizukuConnection("fps", messages::add) }
        runCurrent()
        advanceTimeBy(15_000)
        runCurrent()

        assertFalse(result.await())
        assertEquals(listOf("Shizuku connection timed out"), messages)
        verify(exactly = 1) { ShizukuHelper.releaseUserService("fps") }
    }

    @Test
    fun `stopping while binding releases the owner and ignores a late connection`() = runTest {
        val result = async { awaitFpsShizukuConnection("fps", messages::add) }
        runCurrent()
        result.cancel()
        runCurrent()
        connections.single().invoke()

        assertTrue(result.isCancelled)
        verify(exactly = 1) { ShizukuHelper.releaseUserService("fps") }
        verify(exactly = 1) { ShizukuHelper.acquireUserService("fps", any(), any()) }
    }
}
