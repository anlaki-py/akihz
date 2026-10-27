package akihz.anlaki.dev.presentation.theme

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import akihz.anlaki.dev.data.PreferencesHelper
import timber.log.Timber

/** Defers launcher changes until every app activity is in the background. */
internal class LauncherIconLifecycleObserver(
    private val applySavedIcon: () -> Unit
) : DefaultLifecycleObserver {
    /** Reads the latest preferences when [context]'s app leaves the foreground. */
    constructor(context: Context) : this({
        LauncherIconSwitcher(context).apply(PreferencesHelper.themedAppIcon, PreferencesHelper.themeMode)
    })

    /** Applies only the final choice after rapid theme changes, retrying on the next stop if needed. */
    override fun onStop(owner: LifecycleOwner) {
        // DONT_KILL_APP protects the process, but launchers can still remove a task whose
        // alias was disabled. ProcessLifecycleOwner also skips configuration-change stops.
        try {
            applySavedIcon()
        } catch (error: RuntimeException) {
            Timber.e(error, "Could not update launcher icon in the background")
        }
    }
}
