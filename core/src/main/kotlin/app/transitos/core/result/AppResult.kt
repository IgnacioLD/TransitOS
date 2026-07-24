package com.glossostudio.transitos.core.result

/**
 * Result type for repository operations. Use cases return [AppResult] so the UI
 * can render Loading / Failure / Success uniformly without try/catch in Compose.
 *
 * This is intentionally not Kotlin's `Result`, that type is discouraged in API
 * signatures. A sealed type also exhausts when switched on.
 */
public sealed interface AppResult<out T> {
    public data object Loading : AppResult<Nothing>

    public data class Success<T>(val value: T) : AppResult<T>

    public data class Failure(val error: AppError) : AppResult<Nothing>
}

public inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
    // Reference the singleton directly: `this` here stays typed as AppResult<T>
    // (a `when` branch against an object case does not smart-cast the receiver),
    // which would widen the whole expression to AppResult<Any?>.
    AppResult.Loading -> AppResult.Loading
}

public fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value

public inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

public inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
