package akihz.anlaki.dev.data

import android.app.Application
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AccessMethodTest {
    @Before
    fun setUp() {
        RootCommandService.init(RuntimeEnvironment.getApplication())
        RootCommandService.useShizuku()
        mockkObject(CommandProcess)
    }

    @After
    fun tearDown() {
        RootCommandService.useShizuku()
        unmockkObject(CommandProcess)
    }

    @Test
    fun grantedRootIsRememberedAndCanSwitchBack() {
        every { CommandProcess.execute(any()) } returns "0"
        assertTrue(RootCommandService.request())
        RootCommandService.init(RuntimeEnvironment.getApplication())
        assertTrue(RootCommandService.enabled)
        RootCommandService.useShizuku()
        assertFalse(RootCommandService.enabled)
    }

    @Test
    fun deniedRootKeepsShizukuSelected() {
        every { CommandProcess.execute(any()) } returns "ERROR: denied"
        assertFalse(RootCommandService.request())
        assertFalse(RootCommandService.enabled)
    }

    @Test
    fun failedRootRecheckDoesNotSilentlySwitchBackend() {
        every { CommandProcess.execute(any()) } returns "0"
        RootCommandService.request()
        every { CommandProcess.execute(any()) } returns "ERROR: denied"
        assertFalse(RootCommandService.request())
        assertTrue(RootCommandService.enabled)
    }
}
