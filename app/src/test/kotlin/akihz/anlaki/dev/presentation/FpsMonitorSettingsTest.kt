package akihz.anlaki.dev.presentation

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import akihz.anlaki.dev.data.PreferencesHelper
import akihz.anlaki.dev.presentation.fps.FpsMonitorService
import akihz.anlaki.dev.presentation.fps.FpsPillStyle
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FpsMonitorSettingsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val running = MutableStateFlow(false)
    private lateinit var app: Application
    private lateinit var viewModel: FpsMonitorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("akihz_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        PreferencesHelper.init(app)
        FpsPillStyle().save()
        mockkObject(FpsMonitorService.Companion)
        every { FpsMonitorService.isRunning } returns running
        viewModel = FpsMonitorViewModel(app)
        store.put("fps", viewModel)
        dispatcher.scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        unmockkObject(FpsMonitorService.Companion)
        Dispatchers.resetMain()
    }

    @Test
    fun `app controls persist all new options without starting a stopped monitor`() {
        val chosen = FpsPillStyle(
            graphEnabled = true, graphSeconds = 60, graphWidth = 240, graphHeight = 64,
            defaultFont = true, compact = true, decimals = false, raised = false,
            rectangularShape = true, cornerRadius = 18, scalePercent = 120, alphaPercent = 90
        )
        viewModel.setOverlayStyle(chosen)
        assertEquals(chosen, FpsPillStyle.load())
        assertEquals(chosen, viewModel.uiState.value.overlayStyle)
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test
    fun `pill menu changes refresh the app controls through preference notifications`() {
        val chosen = FpsPillStyle().copy(graphEnabled = true, defaultFont = true, graphSeconds = 10)
        chosen.save()
        shadowOf(Looper.getMainLooper()).idle()
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(201)
        dispatcher.scheduler.runCurrent()
        assertEquals(chosen, viewModel.uiState.value.overlayStyle)
    }

    @Test
    fun `app changes notify a running overlay and show clamped saved values`() {
        running.value = true
        viewModel.setOverlayStyle(FpsPillStyle(graphWidth = 999, graphSeconds = 0, scalePercent = 999))
        val saved = viewModel.uiState.value.overlayStyle
        assertEquals(280, saved.graphWidth)
        assertEquals(10, saved.graphSeconds)
        assertEquals(200, saved.scalePercent)
        assertEquals(FpsMonitorService.ACTION_APPLY_STYLE, shadowOf(app).nextStartedService?.action)
    }
}
