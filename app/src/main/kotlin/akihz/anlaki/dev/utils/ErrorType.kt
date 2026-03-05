package akihz.anlaki.dev.utils

enum class ErrorType {
    SHIZUKU_NOT_RUNNING,
    PERMISSION_DENIED,
    SERVICE_BINDING_FAILED,
    COMMAND_EXECUTION_FAILED,
    CONNECTION_LOST,
    TIMEOUT;

    fun getUserMessage(): String = when (this) {
        SHIZUKU_NOT_RUNNING -> "Shizuku is not running"
        PERMISSION_DENIED -> "Shizuku permission denied"
        SERVICE_BINDING_FAILED -> "Failed to bind service"
        COMMAND_EXECUTION_FAILED -> "Command execution failed"
        CONNECTION_LOST -> "Connection lost"
        TIMEOUT -> "Operation timed out"
    }
}
