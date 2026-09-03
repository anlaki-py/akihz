package akihz.anlaki.dev.data.fps

import android.content.Context
import akihz.anlaki.dev.utils.PreferencesHelper
import java.util.Date
import java.util.Locale

/**
 * Bounded debug logger for FPS sampling.
 *
 * Persists at most 24k chars to [PreferencesHelper.fpsDebugLog] when enabled.
 */
class FpsDebugLogger(private val appContext: Context) {

    private val buffer = StringBuilder()
    private var lastPersistMs = 0L
    private var enabled: Boolean = false

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        if (!value) {
            appendInternal("User action: debug logging disabled")
            persistInternal()
            enabled = false
            buffer.setLength(0)
            PreferencesHelper.fpsDebugLog = ""
        } else {
            enabled = true
            appendInternal("User action: debug logging enabled")
            persistInternal()
        }
        PreferencesHelper.fpsDebugLoggingEnabled = enabled
    }

    fun init(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            val existing = PreferencesHelper.fpsDebugLog
            if (existing.isNotBlank()) {
                buffer.append(existing)
                if (buffer.length > MAX_LENGTH) buffer.delete(0, buffer.length - MAX_LENGTH)
            }
        }
    }

    fun isEnabled(): Boolean = enabled

    @Synchronized
    fun append(message: String) {
        if (!enabled) return
        appendInternal(message)
        val now = System.currentTimeMillis()
        if (now - lastPersistMs >= DEBOUNCE_MS || message.startsWith("ERROR")) {
            persistInternal()
            lastPersistMs = now
        }
    }

    @Synchronized
    fun persist() {
        if (!enabled) return
        persistInternal()
    }

    private fun appendInternal(message: String) {
        val timestamp = String.format(Locale.US, "%1\$tF %1\$tT.%1\$tL", Date())
        buffer.append("\n=== ").append(timestamp).append(" ===\n").append(message).append('\n')
        if (buffer.length > MAX_LENGTH) {
            buffer.delete(0, buffer.length - MAX_LENGTH)
        }
    }

    private fun persistInternal() {
        if (!enabled) return
        PreferencesHelper.fpsDebugLog = buffer.toString().trim()
    }

    companion object {
        private const val MAX_LENGTH = 24000
        private const val DEBOUNCE_MS = 1000L
    }
}
