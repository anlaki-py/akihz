package akihz.anlaki.dev.data

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import akihz.anlaki.dev.BuildConfig
import akihz.anlaki.dev.ICommandService
import akihz.anlaki.dev.utils.ErrorType
import rikka.shizuku.Shizuku
import timber.log.Timber

/**
 * Owns the Shizuku binder state and the shared user service binding.
 *
 * Permission checks, reference counted acquire and release, and service
 * access live here. Command logic lives in the gateway and inspectors.
 */
internal object ShizukuConnection {

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

    fun isBound(): Boolean = commandService != null

    fun service(): ICommandService? = commandService

    fun servicePid(): Int? = commandService?.let { runCatching { it.getPid() }.getOrNull() }

    /**
     * Acquires the shared user service for [owner].
     *
     * Concurrent requests share one binding operation and each receive a result.
     * [onServiceReady] runs after connect, before queued callbacks fire.
     */
    @Synchronized
    fun acquire(
        owner: String,
        onConnected: () -> Unit,
        onFailed: (ErrorType, String) -> Unit = { _, _ -> },
        onServiceReady: () -> Unit = {}
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
            .daemon(true)
            .processNameSuffix("refresh_rate_service")
            .debuggable(false)
            .version(3)

        userServiceArgs = args

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val callbacks = synchronized(this@ShizukuConnection) {
                    commandService = ICommandService.Stub.asInterface(binder)
                    pendingConnections.toList().also { pendingConnections.clear() }
                }
                Timber.i("Shizuku user service connected")
                runCatching { onServiceReady() }
                callbacks.forEach { it.onConnected() }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                synchronized(this@ShizukuConnection) {
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
    fun release(owner: String) {
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

    private data class PendingConnection(
        val owner: String,
        val onConnected: () -> Unit,
        val onFailed: (ErrorType, String) -> Unit
    )
}
