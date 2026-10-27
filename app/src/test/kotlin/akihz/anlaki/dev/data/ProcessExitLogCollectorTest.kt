package akihz.anlaki.dev.data

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ProcessExitLogCollectorTest {
    @Test
    fun `imports system exits once and does not recreate deleted reports`() {
        val app = RuntimeEnvironment.getApplication<Application>()
        val context = mockk<Context>()
        val manager = mockk<ActivityManager>()
        val store = mockk<CrashLogStore>()
        val exit = mockk<ApplicationExitInfo>(relaxed = true)
        app.getSharedPreferences("diagnostic_exits", Context.MODE_PRIVATE).edit().clear().commit()
        every { context.packageName } returns app.packageName
        every { context.getSharedPreferences(any(), any()) } answers {
            app.getSharedPreferences(firstArg(), secondArg())
        }
        every { context.getSystemService(ActivityManager::class.java) } returns manager
        every { manager.getHistoricalProcessExitReasons(any(), 0, 32) } returns listOf(exit)
        every { exit.timestamp } returns 1000L
        every { exit.pid } returns 123
        every { exit.processName } returns app.packageName
        every { exit.reason } returns ApplicationExitInfo.REASON_CRASH_NATIVE
        every { store.saveExit(any(), any()) } returns File("saved.log")

        val collector = ProcessExitLogCollector(context, store)
        collector.collect()
        collector.collect()

        verify(exactly = 1) { store.saveExit(1000L, match { "Native crash" in it }) }
        verify(exactly = 0) { exit.traceInputStream }
    }

    @Test
    fun `failed write is retried instead of marking the exit imported`() {
        val app = RuntimeEnvironment.getApplication<Application>()
        val context = mockk<Context>()
        val manager = mockk<ActivityManager>()
        val store = mockk<CrashLogStore>()
        val exit = mockk<ApplicationExitInfo>(relaxed = true)
        app.getSharedPreferences("diagnostic_exits", Context.MODE_PRIVATE).edit().clear().commit()
        every { context.packageName } returns app.packageName
        every { context.getSharedPreferences(any(), any()) } answers {
            app.getSharedPreferences(firstArg(), secondArg())
        }
        every { context.getSystemService(ActivityManager::class.java) } returns manager
        every { manager.getHistoricalProcessExitReasons(any(), 0, 32) } returns listOf(exit)
        every { exit.timestamp } returns 2000L
        every { exit.processName } returns app.packageName
        every { store.saveExit(any(), any()) } returnsMany listOf(null, File("saved.log"))

        val collector = ProcessExitLogCollector(context, store)
        repeat(3) { collector.collect() }

        verify(exactly = 2) { store.saveExit(2000L, any()) }
    }
}
