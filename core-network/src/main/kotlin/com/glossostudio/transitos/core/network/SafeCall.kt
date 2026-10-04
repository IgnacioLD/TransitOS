package com.glossostudio.transitos.core.network

import com.glossostudio.transitos.core.result.AppError
import com.glossostudio.transitos.core.result.AppResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Runs a network operation and turns any failure into [AppResult.Failure].
 * This is the single place where transport exceptions become [AppError]s, so
 * providers never catch [Exception] themselves. Cancellation always propagates.
 */
@Suppress("TooGenericExceptionCaught") // The one sanctioned catch-all boundary.
suspend inline fun <T> safeCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Exception) {
    AppResult.Failure(throwable.toAppError())
}

/** Classifies a transport/decoding failure into the app's typed error surface. */
fun Throwable.toAppError(): AppError {
    val text = message.orEmpty()
    return when (this) {
        // No DNS resolution is what a device without connectivity reports.
        is UnknownHostException -> AppError.Offline(text)

        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            AppError.Timeout(text)

        is HttpStatusException -> when (status) {
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> AppError.Unauthorized(text)
            HTTP_NOT_FOUND -> AppError.NotFound(text)
            else -> AppError.Server(status, text)
        }

        is SerializationException, is ContentConvertException -> AppError.Parsing(text)

        is IOException -> AppError.Network(text)

        else -> AppError.Unknown(text, this)
    }
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
