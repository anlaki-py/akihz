package akihz.anlaki.dev.presentation.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver

/** Pages sharing the app's predictive back stack. Home and Settings remain root tabs. */
internal enum class AppDestination {
    Root, CustomKeys, FpsMonitor, ThemePreferences, ThemePalette,
    DebugOptions, DebugHome, DebugGlass, Performance, CrashLogs;

    val parent: AppDestination?
        get() = when (this) {
            Root -> null
            ThemePalette -> ThemePreferences
            DebugHome, DebugGlass, Performance, CrashLogs -> DebugOptions
            else -> Root
        }

    companion object {
        /** Maps a CLI debug-page [value] to its destination, defaulting to the categories. */
        fun fromDebugPage(value: String?): AppDestination = when (value?.lowercase()) {
            "performance", "perf" -> Performance
            "crash", "crashes", "crash_logs", "crashlogs" -> CrashLogs
            "glass", "navbar" -> DebugGlass
            "home", "home_screen", "homescreen" -> DebugHome
            else -> DebugOptions
        }
    }
}

/** Owns the canonical page path so buttons, gestures, and external requests agree. */
internal class AppNavigator(initialDestination: AppDestination = AppDestination.Root) {
    val backStack = mutableStateListOf<AppDestination>().apply { addAll(pathTo(initialDestination)) }
    val current: AppDestination get() = backStack.last()

    /** Opens [destination], retaining its existing parent entries and avoiding duplicates. */
    fun open(destination: AppDestination) {
        val path = pathTo(destination)
        val commonSize = backStack.zip(path).takeWhile { (existing, next) -> existing == next }.size
        while (backStack.size > commonSize) backStack.removeAt(backStack.lastIndex)
        backStack.addAll(path.drop(commonSize))
    }

    /** Pops [expected] only if it is still current. The root stays for system Back handling. */
    fun pop(expected: AppDestination = current) {
        if (current == expected && backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        // These enum-only paths need no serialization plugin or polymorphic route registry.
        val StateSaver = Saver<AppNavigator, String>(
            save = { it.current.name },
            restore = { name ->
                AppNavigator(AppDestination.entries.firstOrNull { it.name == name } ?: AppDestination.Root)
            }
        )

        private fun pathTo(destination: AppDestination): List<AppDestination> =
            generateSequence(destination) { it.parent }.toList().asReversed()
    }
}
