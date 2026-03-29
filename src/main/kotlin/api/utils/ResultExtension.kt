package api.utils

inline fun <T> Result<T>.handleResult(
    onSuccess: (T) -> Unit,
    onError: (String) -> Unit
) {
    this.onSuccess(onSuccess)
        .onFailure { onError(it.message ?: "Unknown error") }
}