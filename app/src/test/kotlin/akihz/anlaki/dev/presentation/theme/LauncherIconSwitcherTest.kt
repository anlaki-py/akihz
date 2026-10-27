package akihz.anlaki.dev.presentation.theme

import android.content.ComponentName
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class LauncherIconSwitcherTest {
    private val packageManager = mockk<PackageManager>()
    private val components = LauncherIcon.entries.associateWith { mockk<ComponentName>(name = it.name) }
    private val states = components.values.associateWith {
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }.toMutableMap()
    private val writes = mutableListOf<Pair<ComponentName, Int>>()
    private val switcher = LauncherIconSwitcher(packageManager, components, 30)

    @Before
    fun setUp() {
        every { packageManager.getComponentEnabledSetting(any()) } answers { states.getValue(firstArg()) }
        every { packageManager.setComponentEnabledSetting(any(), any(), any()) } answers {
            val component = firstArg<ComponentName>()
            val state = secondArg<Int>()
            writes += component to state
            states[component] = state
        }
    }

    @Test
    fun `normal icon ignores the app color mode`() {
        AppThemeMode.entries.forEach { mode ->
            assertEquals(LauncherIcon.Normal, LauncherIcon.selected(false, mode))
        }
    }

    @Test
    fun `material icon follows each app color mode`() {
        assertEquals(LauncherIcon.System, LauncherIcon.selected(true, AppThemeMode.System))
        assertEquals(LauncherIcon.Light, LauncherIcon.selected(true, AppThemeMode.Light))
        assertEquals(LauncherIcon.Dark, LauncherIcon.selected(true, AppThemeMode.Dark))
    }

    @Test
    fun `fresh install already has the material system icon`() {
        switcher.apply(true, AppThemeMode.System)

        assertEquals(emptyList<Pair<ComponentName, Int>>(), writes)
    }

    @Test
    fun `replacement is enabled before the old icon is disabled on Android 11`() {
        switcher.apply(false, AppThemeMode.Dark)

        assertEquals(
            listOf(
                components.getValue(LauncherIcon.Normal) to PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                components.getValue(LauncherIcon.System) to PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            ),
            writes
        )
        assertEquals(listOf(LauncherIcon.Normal), enabledIcons())
        verify(exactly = 2) {
            packageManager.setComponentEnabledSetting(any(), any(), PackageManager.DONT_KILL_APP)
        }
    }

    @Test
    fun `switching back to material restores the selected color mode`() {
        switcher.apply(false, AppThemeMode.System)
        switcher.apply(true, AppThemeMode.Dark)

        assertEquals(listOf(LauncherIcon.Dark), enabledIcons())
    }

    @Test
    fun `repeated requests do not refresh the launcher again`() {
        switcher.apply(true, AppThemeMode.Light)
        writes.clear()

        switcher.apply(true, AppThemeMode.Light)

        assertEquals(emptyList<Pair<ComponentName, Int>>(), writes)
    }

    @Test
    fun `startup repairs multiple enabled icons after an interrupted switch`() {
        states[components.getValue(LauncherIcon.Normal)] = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        switcher.apply(true, AppThemeMode.Dark)

        assertEquals(listOf(LauncherIcon.Dark), enabledIcons())
    }

    @Test
    fun `startup recovers when all aliases were disabled`() {
        states.keys.forEach { states[it] = PackageManager.COMPONENT_ENABLED_STATE_DISABLED }

        switcher.apply(true, AppThemeMode.System)

        assertEquals(listOf(LauncherIcon.System), enabledIcons())
    }

    @Test
    fun `failed enable leaves the existing icon available and reports failure`() {
        every {
            packageManager.setComponentEnabledSetting(
                components.getValue(LauncherIcon.Normal),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                any()
            )
        } throws SecurityException("denied")

        assertThrows(SecurityException::class.java) { switcher.apply(false, AppThemeMode.System) }

        assertEquals(listOf(LauncherIcon.System), enabledIcons())
    }

    @Test
    fun `Android 13 changes aliases in one atomic call`() {
        mockkConstructor(PackageManager.ComponentEnabledSetting::class)
        try {
            every { packageManager.setComponentEnabledSettings(any()) } returns Unit

            LauncherIconSwitcher(packageManager, components, 33).apply(true, AppThemeMode.Dark)

            verify(exactly = 1) { packageManager.setComponentEnabledSettings(match { it.size == 2 }) }
            verify(exactly = 0) { packageManager.setComponentEnabledSetting(any(), any(), any()) }
        } finally {
            unmockkConstructor(PackageManager.ComponentEnabledSetting::class)
        }
    }

    private fun enabledIcons(): List<LauncherIcon> = LauncherIcon.entries.filter { icon ->
        val state = states.getValue(components.getValue(icon))
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.System)
    }
}
