package akihz.anlaki.dev.data

import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RefreshRateRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `getSupportedRates delegates to data source`() {
        val fakeSource = object : DisplayManagerDataSource(context) {
            override fun getSupportedRefreshRates(): Result<List<Float>> {
                return Result.success(listOf(60f, 120f))
            }
        }
        val repo = RefreshRateRepository(fakeSource)

        val result = repo.getSupportedRates()

        assertTrue(result.isSuccess)
        assertEquals(listOf(60f, 120f), result.getOrNull())
    }

    @Test
    fun `getCurrentRate delegates to data source`() {
        val fakeSource = object : DisplayManagerDataSource(context) {
            override fun getCurrentRefreshRate(): Result<Float> {
                return Result.success(120f)
            }
        }
        val repo = RefreshRateRepository(fakeSource)

        val result = repo.getCurrentRate()

        assertTrue(result.isSuccess)
        assertEquals(120f, result.getOrNull())
    }

    @Test
    fun `setRate succeeds when command succeeds`() = runTest {
        val fakeSource = DisplayManagerDataSource(context)
        val fakeSetRate: suspend (Float) -> Result<Unit> = { _ ->
            Result.success(Unit)
        }
        val repo = RefreshRateRepository(fakeSource, fakeSetRate)

        val result = repo.setRate(120f)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `setRate fails when command fails`() = runTest {
        val fakeSource = DisplayManagerDataSource(context)
        val fakeSetRate: suspend (Float) -> Result<Unit> = { _ ->
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "shell error")
        }
        val repo = RefreshRateRepository(fakeSource, fakeSetRate)

        val result = repo.setRate(120f)

        assertTrue(result.isError)
        assertEquals(ErrorType.COMMAND_EXECUTION_FAILED, result.getErrorOrNull()?.errorType)
    }

    @Test
    fun `setRate returns immediately without verification`() = runTest {
        val fakeSource = object : DisplayManagerDataSource(context) {
            override fun getCurrentRefreshRate(): Result<Float> {
                return Result.success(60f) // different from requested
            }
        }
        val fakeSetRate: suspend (Float) -> Result<Unit> = { _ ->
            Result.success(Unit)
        }
        val repo = RefreshRateRepository(fakeSource, fakeSetRate)

        // Should succeed even though display reports 60f
        val result = repo.setRate(120f)
        assertTrue(result.isSuccess)
    }
}
