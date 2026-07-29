package akihz.anlaki.dev.data

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import akihz.anlaki.dev.BuildConfig
import rikka.shizuku.Shizuku
import timber.log.Timber
import akihz.anlaki.dev.ICommandService
import akihz.anlaki.dev.data.OemSettingsStrategy.Namespace
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.PreferencesHelper
import akihz.anlaki.dev.utils.Result

object ShizukuHelper {

    private var commandService: ICommandService? = null
    private var serviceConnection: ServiceConnection? = null
    private var userServiceArgs: Shizuku.UserServiceArgs? = null
    private val connectionOwners = ConnectionOwnership()
    private val pendingConnections = mutableListOf<PendingConnection>()

    fun isBinderReady(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Timber.v(e, "Shizuku binder not ready")
            false
        }
    }

    fun hasPermission(): Boolean {
        if (!isBinderReady()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Timber.w(e, "Failed to check Shizuku permission")
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isBinderReady() && !hasPermission()) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (e: Exception) {
                Timber.w(e, "Failed to request Shizuku permission")
            }
        }
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get Shizuku UID")
            -1
        }
    }

    fun isUserServiceBound(): Boolean = commandService != null

    /**
     * Acquires the shared Shizuku user service for [owner].
     *
     * Concurrent requests share one binding operation and each receive a result.
     */
    @Synchronized
    fun acquireUserService(
        owner: String,
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
            connectionOwners.acquire(owner)
            onConnected()
            return
        }

        if (pendingConnections.any { it.owner == owner }) return
        connectionOwners.acquire(owner)
        pendingConnections += PendingConnection(owner, onConnected, onFailed)
        if (serviceConnection != null) return

        val componentName = ComponentName(
            BuildConfig.APPLICATION_ID,
            ICommandServiceImpl::class.java.name
        )
        val args = Shizuku.UserServiceArgs(componentName)
            .daemon(false)
            .processNameSuffix("refresh_rate_service")
            .debuggable(false)
            .version(2)

        userServiceArgs = args

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val callbacks = synchronized(this@ShizukuHelper) {
                    commandService = ICommandService.Stub.asInterface(binder)
                    pendingConnections.toList().also { pendingConnections.clear() }
                }
                Timber.i("Shizuku user service connected")
                callbacks.forEach { it.onConnected() }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                synchronized(this@ShizukuHelper) {
                    commandService = null
                    serviceConnection = null
                    userServiceArgs = null
                }
                Timber.i("Shizuku user service disconnected")
            }
        }

        try {
            Shizuku.bindUserService(args, requireNotNull(serviceConnection))
        } catch (e: Exception) {
            val callbacks = synchronized(this) {
                commandService = null
                serviceConnection = null
                userServiceArgs = null
                pendingConnections.toList().also {
                    pendingConnections.clear()
                    it.forEach { request -> connectionOwners.release(request.owner) }
                }
            }
            Timber.e(e, "Failed to bind Shizuku user service")
            callbacks.forEach {
                it.onFailed(
                    ErrorType.SERVICE_BINDING_FAILED,
                    e.message ?: "Unknown binding error"
                )
            }
        }
    }

    /** Releases [owner] and disconnects only when no component still needs the service. */
    @Synchronized
    fun releaseUserService(owner: String) {
        val shouldDisconnect = connectionOwners.release(owner)
        pendingConnections.removeAll { it.owner == owner }
        if (!shouldDisconnect && connectionOwners.isActive()) return

        val args = userServiceArgs
        val conn = serviceConnection

        if (args != null && conn != null) {
            try {
                Shizuku.unbindUserService(args, conn, true)
            } catch (e: Exception) {
                Timber.w(e, "Failed to unbind Shizuku user service")
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
                Timber.w("Command failed: %s", result)
                Result.error(ErrorType.COMMAND_EXECUTION_FAILED, result)
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Timber.e(e, "Command execution exception")
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Unknown error")
        }
    }

    private fun execSettings(arguments: List<String>): Result<String> {
        val service = commandService
            ?: return Result.error(ErrorType.SERVICE_BINDING_FAILED, "Service not bound")
        return try {
            val output = service.runSettingsCommand(arguments)
            if (output.startsWith("ERROR")) {
                Result.error(ErrorType.COMMAND_EXECUTION_FAILED, output)
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.error(ErrorType.COMMAND_EXECUTION_FAILED, e.message ?: "Settings command failed")
        }
    }

    /** Lists all readable entries in one Android settings namespace. */
    fun listSettings(namespace: Namespace): Result<Map<String, String>> =
        execSettings(listOf("list", namespaceToString(namespace))).map { output ->
            output.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else {
                    line.substring(0, separator) to line.substring(separator + 1)
                }
            }.toMap()
        }

    /** Reads one setting, returning null when the key does not exist. */
    fun getSetting(namespace: Namespace, key: String): Result<String?> {
        if (!isValidKey(key)) return invalidKey()
        return execSettings(listOf("get", namespaceToString(namespace), key)).map {
            it.trim().takeUnless { value -> value == "null" || value == "OK" }
        }
    }

    /** Writes one setting without evaluating user data as shell syntax. */
    fun putSetting(namespace: Namespace, key: String, value: String): Result<Unit> {
        if (!isValidKey(key)) return invalidKey()
        if (value.contains('\u0000') || value.length > MAX_SETTING_VALUE_LENGTH) {
            return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Invalid setting value.")
        }
        return execSettings(listOf("put", namespaceToString(namespace), key, value)).map { Unit }
    }

    /** Deletes one setting. */
    fun deleteSetting(namespace: Namespace, key: String): Result<Unit> {
        if (!isValidKey(key)) return invalidKey()
        return execSettings(listOf("delete", namespaceToString(namespace), key)).map { Unit }
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
        if (CustomProfileManager.profile().enabled) {
            return CustomProfileManager.applyRate(hz)
        }
        val hzInt = hz.toInt()
        val strategy = getActiveStrategy()
        val failures = mutableListOf<String>()

        strategy.writeKeys.forEach { settingsKey ->
            val ns = namespaceToString(settingsKey.namespace)
            val value = RefreshRateSettingValue.forKey(settingsKey.key, hzInt)
            val result = exec("settings put $ns ${settingsKey.key} $value")
            if (result.isError) {
                failures += "$ns/${settingsKey.key}"
            }
        }

        return if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Failed to update: ${failures.joinToString()}"
            )
        }
    }

    /**
     * Resets refresh rate settings to defaults (adaptive mode).
     */
    fun resetRefreshRate(): Result<Unit> {
        if (CustomProfileManager.profile().enabled) {
            return CustomProfileManager.disable()
        }
        val strategy = getActiveStrategy()
        val failures = mutableListOf<String>()

        strategy.writeKeys.forEach { settingsKey ->
            val ns = namespaceToString(settingsKey.namespace)
            val result = exec("settings delete $ns ${settingsKey.key}")
            if (result.isError) {
                failures += "$ns/${settingsKey.key}"
            }
        }

        if (strategy.supportsMode && strategy.modeKey != null) {
            val ns = namespaceToString(strategy.modeKey.namespace)
            val result = exec("settings put $ns ${strategy.modeKey.key} 0")
            if (result.isError) {
                failures += "$ns/${strategy.modeKey.key}"
            }
        }

        return if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Failed to reset: ${failures.distinct().joinToString()}"
            )
        }
    }

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

    private fun isValidKey(key: String): Boolean =
        key.length in 1..MAX_SETTING_KEY_LENGTH && key.all {
            it.isLetterOrDigit() || it == '_' || it == '.' || it == '-'
        }

    private fun <T> invalidKey(): Result<T> =
        Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Invalid setting key.")

    private const val MAX_SETTING_KEY_LENGTH = 128
    private const val MAX_SETTING_VALUE_LENGTH = 512

    private data class PendingConnection(
        val owner: String,
        val onConnected: () -> Unit,
        val onFailed: (ErrorType, String) -> Unit
    )
}
