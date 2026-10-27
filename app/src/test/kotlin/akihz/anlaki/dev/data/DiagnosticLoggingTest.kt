package akihz.anlaki.dev.data

import android.app.Application
import android.content.Context
import android.util.Log
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class DiagnosticLoggingTest {
    private lateinit var store: CrashLogStore

    @Before
    fun setUp() {
        store = CrashLogStore(RuntimeEnvironment.getApplication())
        store.deleteAll()
        Timber.uprootAll()
        Timber.plant(DiagnosticLogTree(store))
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        store.deleteAll()
    }

    @Test
    fun `release logger saves warnings message-only errors and exceptions`() {
        Timber.tag("Test").w("Warning without exception")
        Timber.e("Error without exception")
        Timber.i(IllegalArgumentException("Caught exception"), "Operation failed")
        Timber.d("Ordinary debug message")

        val reports = store.getAll().map(store::readContent)
        assertEquals(3, reports.size)
        assertTrue(reports.any { "Warning without exception" in it && "WARN" in it })
        assertTrue(reports.any { "Error without exception" in it && "ERROR" in it })
        assertTrue(reports.any { "IllegalArgumentException: Caught exception" in it && "Operation failed" in it })
        assertTrue(reports.all { "App:" in it && "Device:" in it && "Thread:" in it })
    }

    @Test
    fun `failed results are captured even when the caller handles them`() {
        Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Settings write failed")
        assertTrue(store.recentReport().contains("Settings write failed"))
    }

    @Test
    fun `theme events survive a new store and appear beside the crash`() {
        Timber.tag("Theme").i("Color mode: Dark -> Light")
        store.saveSync(Thread.currentThread(), IllegalStateException("Fatal failure"))
        val restored = CrashLogStore(RuntimeEnvironment.getApplication())
        assertTrue(restored.recentReport().contains("Dark -> Light"))
        assertTrue(restored.getAll().single().let(restored::readContent).contains("Fatal failure"))
    }

    @Test
    fun `concurrent reports have unique files`() {
        val pool = Executors.newFixedThreadPool(4)
        try {
            repeat(40) { index -> pool.submit { store.saveLog(Log.ERROR, "Test", "Error $index") } }
            pool.shutdown()
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS))
            assertEquals(40, store.getAll().map { it.fileName }.distinct().size)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `error rotation preserves crash reports`() {
        store.saveSync(Thread.currentThread(), IllegalStateException("Keep this crash"))
        repeat(110) { store.saveLog(Log.ERROR, "Test", "Error $it") }
        assertEquals(101, store.getAll().size)
        assertEquals(1, store.getAll().count { it.fileName.startsWith("crash-") })
    }

    @Test
    fun `clipboard is bounded while file export includes all saved reports`() {
        repeat(12) { store.saveLog(Log.ERROR, "Test", "$it " + "x".repeat(25_000)) }
        val report = store.recentReport()
        assertTrue(report.length < 181_000)
        assertTrue(report.contains("Older reports omitted"))
        val bundle = File(RuntimeEnvironment.getApplication().cacheDir, "test-report.txt")
        try {
            store.writeBundle(bundle)
            assertTrue(bundle.length() > 300_000)
        } finally {
            bundle.delete()
        }
    }

    @Test
    fun `storage failure does not recurse through Timber or throw`() {
        val context = mockk<Context>()
        val blockedDirectory = File.createTempFile("blocked", ".tmp")
        every { context.filesDir } returns blockedDirectory
        Timber.uprootAll()
        Timber.plant(DiagnosticLogTree(CrashLogStore(context)))
        try {
            Timber.e("Cannot write this report")
            Timber.tag("Theme").i("Cannot write this event")
        } finally {
            blockedDirectory.delete()
        }
    }
}
