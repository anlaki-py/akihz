package akihz.anlaki.dev.presentation

import androidx.lifecycle.ViewModelStore
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.data.ShizukuHelper
import akihz.anlaki.dev.domain.TileRates
import akihz.anlaki.dev.domain.repository.RefreshRateRepository
import akihz.anlaki.dev.domain.usecase.GetTileRatesUseCase
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelConnectionTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<RefreshRateRepository>()
    private val getTileRates = mockk<GetTileRatesUseCase>()
    private val connections = mutableListOf<() -> Unit>()
    private val failures = mutableListOf<(ErrorType, String) -> Unit>()
    private val store = ViewModelStore()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(ShizukuHelper, PreferencesHelper)
        every { PreferencesHelper.lastRateFlow } returns MutableStateFlow(60f)
        every { PreferencesHelper.excludedTileRates } returns emptySet()
        every { ShizukuHelper.isUserServiceBound() } returns false
        every { ShizukuHelper.releaseUserService(any()) } returns Unit
        every { ShizukuHelper.acquireUserService(any(), any(), any()) } answers {
            connections += secondArg<() -> Unit>()
            failures += thirdArg<(ErrorType, String) -> Unit>()
        }
        val rates = listOf(60f, 90f, 120f)
        coEvery { getTileRates(any()) } returns Result.success(TileRates(rates, rates, emptySet()))
        coEvery { repository.setRate(any()) } returns Result.success(Unit)
        coEvery { repository.resetToDefaults() } returns Result.success(Unit)
        every { repository.getCurrentRate() } returns Result.success(60f)
        viewModel = MainViewModel(repository, getTileRates)
        store.put("home", viewModel)
        viewModel.loadSupportedRates()
        dispatcher.scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        unmockkObject(ShizukuHelper, PreferencesHelper)
        Dispatchers.resetMain()
    }

    @Test
    fun `rate switch reacquires even when the UI still reports bound`() = runTest(dispatcher) {
        viewModel.selectRate(60f)
        runCurrent()
        connections.single().invoke()
        viewModel.uiState.first { !it.isLoading }
        assertTrue(viewModel.uiState.value.isServiceBound)

        viewModel.selectRate(90f)
        runCurrent()
        viewModel.selectRate(120f)
        viewModel.resetToDefaults()
        runCurrent()

        assertEquals(2, connections.size)
        coVerify(exactly = 0) { repository.setRate(90f) }
        coVerify(exactly = 0) { repository.setRate(120f) }
        coVerify(exactly = 0) { repository.resetToDefaults() }
        connections.last().invoke()
        viewModel.uiState.first { !it.isLoading }
        coVerify(exactly = 1) { repository.setRate(90f) }
        assertEquals(90f, viewModel.uiState.value.currentRate)
    }

    @Test
    fun `reset waits for the user service before writing`() = runTest(dispatcher) {
        viewModel.resetToDefaults()
        runCurrent()
        coVerify(exactly = 0) { repository.resetToDefaults() }

        connections.single().invoke()
        viewModel.uiState.first { !it.isLoading }
        coVerify(exactly = 1) { repository.resetToDefaults() }
    }

    @Test
    fun `connection failure reports the cause and allows another attempt`() = runTest(dispatcher) {
        viewModel.selectRate(90f)
        runCurrent()
        failures.single().invoke(ErrorType.PERMISSION_DENIED, "Shizuku permission denied")
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isServiceBound)
        assertEquals("Shizuku permission denied", viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.setRate(any()) }

        viewModel.selectRate(90f)
        runCurrent()
        connections.last().invoke()
        viewModel.uiState.first { !it.isLoading }
        coVerify(exactly = 1) { repository.setRate(90f) }
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `connection timeout releases the request and ignores a late callback`() = runTest(dispatcher) {
        viewModel.selectRate(90f)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.error.orEmpty().contains("Timed out"))
        verify(exactly = 1) { ShizukuHelper.releaseUserService(any()) }
        connections.single().invoke()
        runCurrent()
        coVerify(exactly = 0) { repository.setRate(any()) }

        viewModel.selectRate(120f)
        runCurrent()
        connections.last().invoke()
        viewModel.uiState.first { !it.isLoading }
        coVerify(exactly = 1) { repository.setRate(120f) }
    }

    @Test
    fun `settings write failures remain visible after connecting`() = runTest(dispatcher) {
        coEvery { repository.setRate(90f) } returns Result.error(
            ErrorType.COMMAND_EXECUTION_FAILED,
            "Settings write rejected"
        )
        every { ShizukuHelper.acquireUserService(any(), any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }

        viewModel.selectRate(90f)
        viewModel.uiState.first { !it.isLoading && it.currentRate == 60f }

        assertEquals("Settings write rejected", viewModel.uiState.value.error)
        coVerify(exactly = 1) { repository.setRate(90f) }
    }

    @Test
    fun `clearing the screen cancels a pending switch`() = runTest(dispatcher) {
        viewModel.selectRate(90f)
        runCurrent()
        store.clear()
        connections.single().invoke()
        runCurrent()

        verify(atLeast = 1) { ShizukuHelper.releaseUserService(any()) }
        coVerify(exactly = 0) { repository.setRate(any()) }
    }
}
