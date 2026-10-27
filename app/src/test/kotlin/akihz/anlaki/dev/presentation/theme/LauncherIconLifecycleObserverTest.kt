package akihz.anlaki.dev.presentation.theme

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconLifecycleObserverTest {
    @Test
    fun `rapid theme changes never switch launcher while app is visible`() {
        val owner = mockk<LifecycleOwner>()
        val lifecycle = LifecycleRegistry.createUnsafe(owner)
        var savedMode = AppThemeMode.System
        val applied = mutableListOf<AppThemeMode>()
        lifecycle.addObserver(LauncherIconLifecycleObserver { applied += savedMode })
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        repeat(30) {
            savedMode = AppThemeMode.Light
            savedMode = AppThemeMode.Dark
            savedMode = AppThemeMode.System
        }
        savedMode = AppThemeMode.Light
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertEquals(emptyList<AppThemeMode>(), applied)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertEquals(listOf(AppThemeMode.Light), applied)
    }

    @Test
    fun `failed background switch is caught and retried after next foreground session`() {
        val owner = mockk<LifecycleOwner>()
        var attempts = 0
        val observer = LauncherIconLifecycleObserver {
            attempts++
            if (attempts == 1) throw SecurityException("Package manager refused")
        }
        observer.onStop(owner)
        observer.onStart(owner)
        observer.onStop(owner)
        assertEquals(2, attempts)
    }
}
