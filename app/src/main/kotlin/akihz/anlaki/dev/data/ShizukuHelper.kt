package akihz.anlaki.dev.data

import akihz.anlaki.dev.ICommandService
import akihz.anlaki.dev.data.OemSettingsStrategy.Namespace
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.Result
import rikka.shizuku.Shizuku

/**
 * Helper for binding to Shizuku user service and executing settings commands.
 *
 * All command execution goes through [ICommandService] running in a Shizuku-owned
 * process with elevated privileges.
 */
object ShizukuHelper {

    private var commandService: ICommandService? = null
    private var serviceConnection: ServiceConnection? = null
    private var userServiceArgs: Shizuku.UserServiceArgs? = null

    fun isBinderReady(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        if (!isBinderReady()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isBinderReady() && !hasPermission()) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (_: Exception) {
                // ignored
            }
        }
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (_: Exception) {
            -1
        }
    }

    fun isUserServiceBound(): Boolean = commandService != null

    fun bindUserService(
        onConnected: () -> Unit,
        onFailed: (ErrorType, String) -> Unit = { _, _ -> }
    ) {
        if (!isBinderReady()) {
            onFailed(ErrorType.SHIZUKU_NOT_RUNNING, ErrorType.SHIZUKU_NOT_RUNNING.getUserMessage())
            return
        }

        if (!hasPermission()) {
            onFailed(ErrorType.PERMISSION_DENIED, ErrorType.PERMISSION_DENIED.getUserMessage())
            return
        }

        if (commandService != null) {
            onConnected()
            return
        }

        val componentName = ComponentName("akihz.anlaki.dev", "akihz.anlaki.dev.data.ICommandServiceImpl")
        val args = Shizuku.UserServiceArgs(componentName)
            .daemon(false)
            .processNameSuffix("refresh_rate_service")
            .debuggable(false)
            .version(1)

        userServiceArgs = args

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                commandService = ICommandService.Stub.asInterface(binder)
                onConnected()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                commandService = null
            }
        }

        try {
            Shizuku.bindUserService(args, serviceConnection!!)
        } catch (e: Exception) {
            commandService = null
            onFailed(ErrorType.SERVICE_BINDING_FAILED, e.message ?: "Unknown binding error")
        }
    }

    fun unbindUserService() {
        val args = userServiceArgs
        val conn = serviceConnection

        if (args != null && conn != null) {
            try {
                Shizuku.unbindUserService(args, conn, true)
            } catch (_: Exception) {
                // ignored
            }
        }

        commandService = null
        serviceConnection = null
        userServiceArgs = null
    }

    private fun exec(command: String): Result<String> {
        val service = commandService
        if (service == null) {
            return Result.error(ErrorType.SERVICE_BINDING_FAILED, "Service not bound")
        }

        return try {
            val result = service.runCommand(command)
            if (result.startsWith("ERROR")) {
                Result.error(ErrorType.COMMAND_EXECUTION_FAILED, result)
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Unknown error")
        }
    }

    /**
     * Reads the current refresh rate from OEM-specific settings keys.
     */
    fun getCurrentRefreshRate(): Result<Float> {
        val strategy = getActiveStrategy()
        val keys = strategy.readKeys

        for (settingsKey in keys) {
            val ns = namespaceToString(settingsKey.namespace)
            val result = exec("settings get $ns ${settingsKey.key}")
            if (result.isSuccess) {
                result.getOrNull()?.let { raw ->
                    if (raw.isNotBlank() && raw != "null") {
                        raw.trim().toFloatOrNull()?.let { rate ->
                            return Result.success(rate)
                        }
                    }
                }
            }
        }
        return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Could not retrieve refresh rate")
    }

    /**
     * Sets the refresh rate using standard write keys.
     */
    fun setRefreshRate(hz: Float): Result<Unit> {
        val hzInt = hz.toInt()
        val strategy = getActiveStrategy()
        var anySuccess = false

        strategy.writeKeys.forEach { settingsKey ->
            val ns = namespaceToString(settingsKey.namespace)
            val result = exec("settings put $ns ${settingsKey.key} $hzInt")
            if (result.isSuccess) {
                anySuccess = true
            }
        }

        return if (anySuccess) {
            Result.success(Unit)
        } else {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Failed to set refresh rate")
        }
    }

    /**
     * Sets refresh rate in lock mode: writes both min and peak to the same value.
     * This constrains SurfaceFlinger's selection range to a single rate.
     */
    fun setRefreshRateLocked(hz: Float): Result<Unit> {
        val hzInt = hz.toInt()
        val strategy = getActiveStrategy()
        var anySuccess = false

        strategy.lockKeys.forEach { settingsKey ->
            val ns = namespaceToString(settingsKey.namespace)
            val result = exec("settings put $ns ${settingsKey.key} $hzInt")
            if (result.isSuccess) {
                anySuccess = true
            }
        }

        // Also set mode to "high" (3) if supported, to prevent adaptive switching
        if (strategy.supportsMode && strategy.modeKey != null) {
            val ns = namespaceToString(strategy.modeKey.namespace)
            exec("settings put $ns ${strategy.modeKey.key} 3")
        }

        return if (anySuccess) {
            Result.success(Unit)
        } else {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Failed to set locked refresh rate")
        }
    }

    /**
     * Resets refresh rate settings to defaults (adaptive mode).
     */
    fun resetRefreshRate(): Result<Unit> {
        val strategy = getActiveStrategy()

        strategy.writeKeys.forEach { settingsKey ->
            val ns = namespaceToString(settingsKey.namespace)
            exec("settings delete $ns ${settingsKey.key}")
        }

        // Reset mode to adaptive (0) if supported
        if (strategy.supportsMode && strategy.modeKey != null) {
            val ns = namespaceToString(strategy.modeKey.namespace)
            exec("settings put $ns ${strategy.modeKey.key} 0")
        }

        return Result.success(Unit)
    }

    /**
     * Gets the active strategy, respecting manual OEM override if set.
     */
    private fun getActiveStrategy(): OemSettingsStrategy.KeySet {
        val override = PreferencesHelper.oemOverride
        return if (override.isNotBlank() && override != "Auto-detect") {
            OemSettingsStrategy.resolveByName(override)
        } else {
            OemSettingsStrategy.resolve()
        }
    }

    private fun namespaceToString(namespace: Namespace): String {
        return when (namespace) {
            Namespace.SECURE -> "secure"
            Namespace.SYSTEM -> "system"
            Namespace.GLOBAL -> "global"
        }
    }
}