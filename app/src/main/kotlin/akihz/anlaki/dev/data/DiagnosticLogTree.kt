package akihz.anlaki.dev.data

import android.util.Log
import timber.log.Timber

/** Persists app warnings, errors, and logged exceptions in both release and debug builds. */
class DiagnosticLogTree(private val store: CrashLogStore) : Timber.DebugTree() {
    /** Saves [message] with [tag] and [priority]; Timber already includes [t]'s stack trace. */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when {
            priority >= Log.WARN || t != null -> store.saveLog(
                if (t != null) maxOf(priority, Log.ERROR) else priority, tag, message
            )
            tag == "Theme" || tag == "AppLifecycle" -> store.recordEvent(tag, message)
        }
    }
}
