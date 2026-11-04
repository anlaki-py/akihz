package akihz.anlaki.dev.data

import android.app.Application
import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class ProcessExitLogCollectorTest {
    @Test
    fun android10SkipsSystemExitHistoryAndStorage() {
        val context = mockk<Context>()
        val store = mockk<CrashLogStore>()

        ProcessExitLogCollector(context, store).collect()

        verify { context wasNot Called }
        verify { store wasNot Called }
    }
}
