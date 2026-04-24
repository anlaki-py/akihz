package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayManagerDataSourceTest {

    private lateinit var context: Context
    private lateinit var dataSource: DisplayManagerDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = DisplayManagerDataSource(context)
    }

    @Test
    fun `getSupportedRefreshRates returns Result type`() {
        val result = dataSource.getSupportedRefreshRates()
        assertTrue(
            "Result should be either Success or Error",
            result is Result.Success || result is Result.Error
        )
    }

    @Test
    fun `getCurrentRefreshRate returns success with positive rate`() {
        val result = dataSource.getCurrentRefreshRate()

        assertTrue("Should return Success", result.isSuccess)
        val rate = result.getOrNull()
        assertTrue("Rate should be positive", rate != null && rate > 0)
    }

    @Test
    fun `getCurrentRefreshRate returns rate within expected range`() {
        val result = dataSource.getCurrentRefreshRate()

        val rate = result.getOrNull()
        assertTrue("Rate should be >= 1 Hz", rate != null && rate >= 1f)
        assertTrue("Rate should be <= 1000 Hz", rate != null && rate <= 1000f)
    }
}
