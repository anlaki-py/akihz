package akihz.anlaki.dev.presentation.fps

import akihz.anlaki.dev.data.ShizukuHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Acquires the FPS connection and waits up to 15 seconds for its callback.
 * @param owner identifies the monitor instance that owns the connection.
 * @param onFailed records a failed binding or timeout.
 * @return true when the connection is ready, false when it failed or timed out.
 */
internal suspend fun awaitFpsShizukuConnection(owner: String, onFailed: (String) -> Unit): Boolean {
    var connected = false
    try {
        connected = withTimeoutOrNull(15_000) {
            val result = CompletableDeferred<Boolean>()
            ShizukuHelper.acquireUserService(
                owner = owner,
                onConnected = { result.complete(true) },
                onFailed = { _, message ->
                    onFailed(message)
                    result.complete(false)
                }
            )
            result.await()
        } ?: run {
            onFailed("Shizuku connection timed out")
            false
        }
        return connected
    } finally {
        // Remove pending callbacks on cancellation so a stopped monitor cannot reconnect later.
        if (!connected) ShizukuHelper.releaseUserService(owner)
    }
}
