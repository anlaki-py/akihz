package akihz.anlaki.dev.presentation.navigation

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun `back from color schemes reveals theme preferences then the root tabs`() {
        val navigator = AppNavigator()
        navigator.open(AppDestination.ThemePreferences)
        navigator.open(AppDestination.ThemePalette)

        navigator.pop()
        assertEquals(AppDestination.ThemePreferences, navigator.current)
        navigator.pop()
        assertEquals(listOf(AppDestination.Root), navigator.backStack)
    }

    @Test
    fun `every debug deep link includes the categories as its back destination`() {
        listOf(AppDestination.DebugHome, AppDestination.Performance, AppDestination.CrashLogs).forEach { page ->
            val navigator = AppNavigator()
            navigator.open(page)

            assertEquals(listOf(AppDestination.Root, AppDestination.DebugOptions, page), navigator.backStack)
            navigator.pop()
            assertEquals(AppDestination.DebugOptions, navigator.current)
            navigator.pop()
            assertEquals(AppDestination.Root, navigator.current)
        }
    }

    @Test
    fun `custom keys and FPS monitor both return to the root tabs`() {
        listOf(AppDestination.CustomKeys, AppDestination.FpsMonitor).forEach { page ->
            val navigator = AppNavigator(page)

            navigator.pop()

            assertEquals(listOf(AppDestination.Root), navigator.backStack)
        }
    }

    @Test
    fun `root Back is left to the system and never empties the stack`() {
        val navigator = AppNavigator()

        repeat(3) { navigator.pop() }

        assertEquals(listOf(AppDestination.Root), navigator.backStack)
    }

    @Test
    fun `repeated external requests do not duplicate destinations`() {
        val navigator = AppNavigator()

        repeat(3) { navigator.open(AppDestination.CrashLogs) }

        assertEquals(
            listOf(AppDestination.Root, AppDestination.DebugOptions, AppDestination.CrashLogs),
            navigator.backStack
        )
    }

    @Test
    fun `external FPS request replaces unrelated pages with the correct return path`() {
        val navigator = AppNavigator(AppDestination.ThemePalette)

        navigator.open(AppDestination.FpsMonitor)
        navigator.pop()

        assertEquals(listOf(AppDestination.Root), navigator.backStack)
    }

    @Test
    fun `delayed draft-save completion cannot pop a different page`() {
        val navigator = AppNavigator(AppDestination.CustomKeys)
        val leavingPage = navigator.current
        navigator.open(AppDestination.FpsMonitor)

        navigator.pop(leavingPage)

        assertEquals(AppDestination.FpsMonitor, navigator.current)
    }

    @Test
    fun `opening an existing parent keeps its entry and removes only its descendants`() {
        val navigator = AppNavigator(AppDestination.ThemePalette)

        navigator.open(AppDestination.ThemePreferences)

        assertEquals(listOf(AppDestination.Root, AppDestination.ThemePreferences), navigator.backStack)
    }

    @Test
    fun `all destinations restore their complete back path after recreation`() {
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        AppDestination.entries.forEach { page ->
            val navigator = AppNavigator(page)
            val saved = with(AppNavigator.StateSaver) { scope.save(navigator) }
            assertNotNull(saved)

            val restored = AppNavigator.StateSaver.restore(checkNotNull(saved))

            assertEquals(navigator.backStack.toList(), checkNotNull(restored).backStack.toList())
        }
    }

    @Test
    fun `unknown saved destination safely returns to the root`() {
        val restored = AppNavigator.StateSaver.restore("RemovedPage")

        assertEquals(listOf(AppDestination.Root), checkNotNull(restored).backStack)
    }

    @Test
    fun `existing CLI aliases still open the same debug pages`() {
        mapOf(
            AppDestination.Performance to listOf("performance", "perf", "PERF"),
            AppDestination.CrashLogs to listOf("crash", "crashes", "crash_logs", "crashlogs"),
            AppDestination.DebugHome to listOf("home", "home_screen", "homescreen")
        ).forEach { (page, aliases) ->
            aliases.forEach { assertEquals(page, AppDestination.fromDebugPage(it)) }
        }
        assertEquals(AppDestination.DebugOptions, AppDestination.fromDebugPage(null))
        assertEquals(AppDestination.DebugOptions, AppDestination.fromDebugPage("unknown"))
    }
}
