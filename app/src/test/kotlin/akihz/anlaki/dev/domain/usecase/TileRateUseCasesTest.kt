package akihz.anlaki.dev.domain.usecase

import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRateRepository(
    var supported: Result<List<Float>> = Result.success(listOf(60f, 90f, 120f)),
    var setResult: Result<Unit> = Result.success(Unit)
) : RefreshRateRepository {
    val applied = mutableListOf<Float>()
    override fun getSupportedRates(): Result<List<Float>> = supported
    override fun getCurrentRate(): Result<Float> = Result.success(60f)
    override suspend fun setRate(hz: Float): Result<Unit> {
        applied += hz
        return setResult
    }
    override suspend fun resetToDefaults(): Result<Unit> = Result.success(Unit)
}

class GetTileRatesUseCaseTest {

    @Test
    fun `resolves included rates from stored exclusions`() = runTest {
        val useCase = GetTileRatesUseCase(FakeRateRepository())

        val result = useCase(setOf(120f))

        assertTrue(result.isSuccess)
        val rates = result.getOrNull()!!
        assertEquals(listOf(60f, 90f, 120f), rates.allRates)
        assertEquals(listOf(60f, 90f), rates.includedRates)
        assertEquals(setOf(120f), rates.excludedRates)
    }

    @Test
    fun `recovers exclusions that would empty the cycle`() = runTest {
        val useCase = GetTileRatesUseCase(FakeRateRepository())

        val result = useCase(setOf(60f, 90f, 120f))

        assertTrue(result.isSuccess)
        val rates = result.getOrNull()!!
        assertEquals(emptySet<Float>(), rates.excludedRates)
        assertEquals(listOf(60f, 90f, 120f), rates.includedRates)
    }

    @Test
    fun `passes repository errors through`() = runTest {
        val failure = Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "nope")
        val useCase = GetTileRatesUseCase(FakeRateRepository(supported = failure))

        val result = useCase(emptySet())

        assertTrue(result.isError)
        assertEquals("nope", result.getErrorOrNull()?.message)
    }
}

class CycleTileRateUseCaseTest {

    @Test
    fun `applies the rate after the anchor`() = runTest {
        val repo = FakeRateRepository()
        val useCase = CycleTileRateUseCase(repo)

        val result = useCase(listOf(60f, 90f, 120f), 90f)

        assertTrue(result.isSuccess)
        assertEquals(120f, result.getOrNull())
        assertEquals(listOf(120f), repo.applied)
    }

    @Test
    fun `empty cycle returns an error without touching the repository`() = runTest {
        val repo = FakeRateRepository()
        val useCase = CycleTileRateUseCase(repo)

        val result = useCase(emptyList(), 60f)

        assertTrue(result.isError)
        assertTrue(repo.applied.isEmpty())
    }

    @Test
    fun `failed write returns the repository error`() = runTest {
        val repo = FakeRateRepository(setResult = Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "denied"))
        val useCase = CycleTileRateUseCase(repo)

        val result = useCase(listOf(60f, 90f), 60f)

        assertTrue(result.isError)
        assertEquals("denied", result.getErrorOrNull()?.message)
    }
}
