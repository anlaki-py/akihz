package akihz.anlaki.dev.utils

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val errorType: ErrorType, val message: String = errorType.getUserMessage()) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getErrorOrNull(): Error? = this as? Error

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (ErrorType, String) -> Unit): Result<T> {
        if (this is Error) action(errorType, message)
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(errorType: ErrorType, message: String = errorType.getUserMessage()): Result<Nothing> = Error(errorType, message)
    }
}
