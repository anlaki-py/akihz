package akihz.anlaki.dev.data

import akihz.anlaki.dev.data.OemSettingsStrategy.Namespace
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result

/**
 * Facade over the Shizuku collaborators.
 *
 * Connection state lives in [ShizukuConnection], settings transport in
 * [ShizukuSettingsGateway], rate policy in [ShizukuRefreshRates], and shell
 * inspection in [ShizukuProcessInspector]. This object keeps the public API
 * stable so existing callers do not change.
 */
object ShizukuHelper {

    /** Returns true when the Shizuku binder answers. */
    fun isBinderReady(): Boolean = ShizukuConnection.isBinderReady()

    /** Returns true when this app holds Shizuku permission. */
    fun hasPermission(): Boolean = ShizukuConnection.hasPermission()

    /**
     * Asks Shizuku for permission.
     * @param requestCode identifies the permission result.
     */
    fun requestPermission(requestCode: Int) = ShizukuConnection.requestPermission(requestCode)

    /** Returns the Shizuku UID, or -1 when unavailable. */
    fun getUid(): Int = ShizukuConnection.getUid()

    /** Returns true when the user service is bound. */
    fun isUserServiceBound(): Boolean = ShizukuConnection.isBound()

    /**
     * Acquires the shared Shizuku user service for [owner].
     *
     * Concurrent requests share one binding operation and each receive a result.
     */
    fun acquireUserService(
        owner: String,
        onConnected: () -> Unit,
        onFailed: (ErrorType, String) -> Unit = { _, _ -> }
    ) {
        ShizukuConnection.acquire(
            owner = owner,
            onConnected = onConnected,
            onFailed = onFailed,
            onServiceReady = {
                // Best-effort cleanup of stale refresh_rate_service shells left by old version/bug.
                // Runs after a short delay so the new service is fully up.
                try {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        runCatching { killStaleRefreshServices() }
                    }, 2000)
                } catch (_: Exception) {}
            }
        )
    }

    /** Releases [owner] and disconnects only when no component still needs the service. */
    fun releaseUserService(owner: String) = ShizukuConnection.release(owner)

    private fun exec(command: String): Result<String> =
        ShizukuSettingsGateway.exec(ShizukuConnection.service(), command)

    /** Lists all readable entries in one Android settings namespace. */
    fun listSettings(namespace: Namespace): Result<Map<String, String>> =
        ShizukuSettingsGateway.listSettings(ShizukuConnection.service(), namespace)

    /** Reads one setting, returning null when the key does not exist. */
    fun getSetting(namespace: Namespace, key: String): Result<String?> =
        ShizukuSettingsGateway.getSetting(ShizukuConnection.service(), namespace, key)

    /** Writes one setting without evaluating user data as shell syntax. */
    fun putSetting(namespace: Namespace, key: String, value: String): Result<Unit> =
        ShizukuSettingsGateway.putSetting(ShizukuConnection.service(), namespace, key, value)

    /** Deletes one setting. */
    fun deleteSetting(namespace: Namespace, key: String): Result<Unit> =
        ShizukuSettingsGateway.deleteSetting(ShizukuConnection.service(), namespace, key)

    /**
     * Reads the current refresh rate from OEM-specific settings keys.
     */
    fun getCurrentRefreshRate(): Result<Float> =
        ShizukuRefreshRates.getCurrentRefreshRate(::exec)

    /**
     * Sets the refresh rate using standard write keys.
     */
    fun setRefreshRate(hz: Float): Result<Unit> =
        ShizukuRefreshRates.setRefreshRate(::exec, hz)

    /**
     * Resets refresh rate settings to defaults (adaptive mode).
     */
    fun resetRefreshRate(): Result<Unit> =
        ShizukuRefreshRates.resetRefreshRate(::exec)

    /** Returns the user service pid, or null when unknown. */
    fun getServicePid(): Int? = ShizukuConnection.servicePid()

    /**
     * Kills stale `refresh_rate_service` shell processes left over from previous
     * app launches / version 2 daemon bug. Keeps the current service PID alive.
     * Returns a human-readable summary; safe to call even when not bound.
     */
    fun killStaleRefreshServices(): Result<String> =
        ShizukuProcessInspector.killStaleRefreshServices(ShizukuConnection.service())

    /** Runs an arbitrary shell command as `shell` (via Shizuku) when bound, else as app. */
    fun runShellCommand(command: String): Result<String> =
        ShizukuProcessInspector.runShellCommand(ShizukuConnection.service(), command)

    /** Lists all `akihz` PIDs with their PSS/RSS for debugging. */
    fun listAppProcesses(): Result<String> =
        ShizukuProcessInspector.listAppProcesses(::runShellCommand)

    /** Collects per-process PSS/CPU for perf log via shell when possible. */
    fun collectAppProcessesForPerf(): List<PerfProcessInfo> =
        ShizukuProcessInspector.collectAppProcessesForPerf(::runShellCommand)
}
