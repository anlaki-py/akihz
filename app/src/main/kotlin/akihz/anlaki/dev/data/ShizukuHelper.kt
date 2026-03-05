package akihz.anlaki.dev.data

import akihz.anlaki.dev.ICommandService
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import akihz.anlaki.dev.utils.ErrorType
import akihz.anlaki.dev.utils.Result
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private var commandService: ICommandService? = null
    private var serviceConnection: ServiceConnection? = null
    private var userServiceArgs: Shizuku.UserServiceArgs? = null

    fun isBinderReady(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        if (!isBinderReady()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isBinderReady() && !hasPermission()) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (e: Exception) {
            }
        }
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Exception) {
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
            } catch (e: Exception) {
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

    fun getCurrentRefreshRate(): Result<Float> {
        val userRateResult = exec("settings get secure user_refresh_rate")
        if (userRateResult.isSuccess) {
            val userRate = userRateResult.getOrNull()
            if (userRate != null && userRate != "null" && userRate.isNotBlank()) {
                userRate.trim().toFloatOrNull()?.let {
                    return Result.success(it)
                }
            }
        }

        val miuiRateResult = exec("settings get secure miui_refresh_rate")
        if (miuiRateResult.isSuccess) {
            val miuiRate = miuiRateResult.getOrNull()
            if (miuiRate != null && miuiRate != "null" && miuiRate.isNotBlank()) {
                miuiRate.trim().toFloatOrNull()?.let {
                    return Result.success(it)
                }
            }
        }

        return Result.error(ErrorType.COMMAND_EXECUTION_FAILED, "Could not retrieve refresh rate")
    }

    fun setRefreshRate(hz: Float): Result<Unit> {
        val hzInt = hz.toInt()

        val result1 = exec("settings put secure user_refresh_rate $hzInt")
        val result2 = exec("settings put secure miui_refresh_rate $hzInt")

        return when {
            result1.isSuccess || result2.isSuccess -> Result.success(Unit)
            else -> Result.error(
                ErrorType.COMMAND_EXECUTION_FAILED,
                "Failed to set refresh rate"
            )
        }
    }

    fun resetRefreshRate(): Result<Unit> {
        exec("settings delete secure user_refresh_rate")
        exec("settings delete secure miui_refresh_rate")
        return Result.success(Unit)
    }
}
