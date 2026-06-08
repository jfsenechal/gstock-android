package be.marche.gstock.core

/**
 * Simple one-shot result wrapper for network actions (checkout, return, scan, refresh).
 * UI state that needs loading/empty is handled separately in each ViewModel.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onError(block: (String) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) block(message)
    return this
}
