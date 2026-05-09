package akihz.anlaki.dev.utils

/**
 * Categorized error types with user-friendly messages.
 */
enum class ErrorType {
    SHIZUKU_NOT_RUNNING,
    PERMISSION_DENIED,
    SERVICE_BINDING_FAILED,
    COMMAND_EXECUTION_FAILED,
    CONNECTION_LOST,
    TIMEOUT,
    DISPLAY_NOT_FOUND,
    NO_REFRESH_RATES,
    UNSUPPORTED_API;

    /**
     * Returns a human-readable error message.
     */
    fun getUserMessage(): String = when (this) {
        SHIZUKU_NOT_RUNNING -> "Shizuku is not running"
        PERMISSION_DENIED -> "Shizuku permission denied"
        SERVICE_BINDING_FAILED -> "Failed to bind service"
        COMMAND_EXECUTION_FAILED -> "Command execution failed"
        CONNECTION_LOST -> "Connection lost"
        TIMEOUT -> "Operation timed out"
        DISPLAY_NOT_FOUND -> "Display not found"
        NO_REFRESH_RATES -> "No supported refresh rates detected"
        UNSUPPORTED_API -> "Unsupported Android version"
    }
}