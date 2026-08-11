package akihz.anlaki.dev.presentation

import akihz.anlaki.dev.data.CustomRefreshProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomDraftSaverTest {
    @Test
    fun `rapid edits persist only the latest profile after debounce`() = runTest {
        val saved = mutableListOf<CustomRefreshProfile>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val saver = CustomDraftSaver(this, { saved += it }, dispatcher, delayMillis = 400L)
        val first = CustomRefreshProfile(rates = listOf(60f))
        val latest = CustomRefreshProfile(rates = listOf(60f, 120f))

        saver.schedule(first)
        saver.schedule(latest)
        advanceTimeBy(399L)
        runCurrent()
        assertTrue(saved.isEmpty())

        advanceTimeBy(1L)
        runCurrent()
        assertEquals(listOf(latest), saved)
    }

    @Test
    fun `flush persists latest profile without waiting for debounce`() = runTest {
        val saved = mutableListOf<CustomRefreshProfile>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val saver = CustomDraftSaver(this, { saved += it }, dispatcher, delayMillis = 400L)
        val latest = CustomRefreshProfile(rates = listOf(90f))

        saver.schedule(latest)
        saver.flush()

        assertEquals(listOf(latest), saved)
    }
}
