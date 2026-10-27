package akihz.anlaki.dev.presentation.theme

import timber.log.Timber

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/** Manifest aliases for the normal icon and each Material color mode. */
internal enum class LauncherIcon {
    System, Light, Dark, Normal;

    val className: String
        get() = "akihz.anlaki.dev.presentation.launcher.${name}Launcher"

    companion object {
        /** Returns the alias matching [themed] and the app's [mode]. */
        fun selected(themed: Boolean, mode: AppThemeMode): LauncherIcon = when {
            !themed -> Normal
            mode == AppThemeMode.Light -> Light
            mode == AppThemeMode.Dark -> Dark
            else -> System
        }
    }
}

/** Changes launcher aliases. Call only after the app leaves the foreground. */
internal class LauncherIconSwitcher(
    private val packageManager: PackageManager,
    private val components: Map<LauncherIcon, ComponentName>,
    private val sdkInt: Int
) {
    /** Creates a switcher for [context]'s installed package, including the debug suffix. */
    constructor(context: Context) : this(
        context.packageManager,
        LauncherIcon.entries.associateWith { ComponentName(context.packageName, it.className) },
        Build.VERSION.SDK_INT
    )

    /**
     * Enables exactly one icon matching [themed] and [mode]. Repeated calls do no work.
     * Package-manager failures propagate so callers can keep the previous preference.
     */
    fun apply(themed: Boolean, mode: AppThemeMode) {
        val selected = LauncherIcon.selected(themed, mode)
        val changes = LauncherIcon.entries
            .sortedBy { it != selected }
            .mapNotNull { icon ->
                val component = components.getValue(icon)
                val state = packageManager.getComponentEnabledSetting(component)
                val enabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.System)
                val shouldEnable = icon == selected
                if (enabled == shouldEnable) null else component to if (shouldEnable) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            }
        if (changes.isEmpty()) return

        Timber.tag("Theme").i("Applying launcher icon: %s", selected)

        if (supportsAtomicChanges()) {
            packageManager.setComponentEnabledSettings(changes.map { (component, state) ->
                PackageManager.ComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            })
        } else {
            // Enable the replacement first so an interrupted switch never hides the app.
            changes.forEach { (component, state) ->
                packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    private fun supportsAtomicChanges(): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU
}
