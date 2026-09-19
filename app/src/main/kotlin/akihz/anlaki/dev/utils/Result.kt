package akihz.anlaki.dev.utils

/**
 * A sealed class representing the result of an operation that can either succeed or fail.
 *
 * @param T the type of the success value
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val errorType: ErrorType, val message: String = errorType.getUserMessage()) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    /**
     * Returns data on success, null on error.
     * @return data or null.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Returns error on failure, null on success.
     * @return error or null.
     */
    fun getErrorOrNull(): Error? = this as? Error

    /**
     * Maps success data to a new result.
     * @param transform maps data. @return new result.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Runs action on success.
     * @param action runs with data. @return this result.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Runs action on error.
     * @param action runs with type and message. @return this result.
     */
    inline fun onError(action: (ErrorType, String) -> Unit): Result<T> {
        if (this is Error) action(errorType, message)
        return this
    }

    companion object {
        /**
         * Creates a success result.
         * @param data success value. @return success result.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        /**
         * Creates an error result.
         * @param errorType error kind. @param message custom message.
         */
        fun error(errorType: ErrorType, message: String = errorType.getUserMessage()): Result<Nothing> = Error(errorType, message)
    }
}