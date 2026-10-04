package com.glossostudio.transitos.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import java.io.IOException

/**
 * An upstream answered with a non-success HTTP status. Thrown by
 * [requireSuccess] so status failures reach [toAppError] as a typed value.
 */
class HttpStatusException(val status: Int, message: String) : IOException(message)

/** Throws [HttpStatusException] unless the response has a 2xx status. */
fun HttpResponse.requireSuccess(): HttpResponse {
    if (!status.isSuccess()) {
        throw HttpStatusException(status.value, "HTTP ${status.value} from ${call.request.url.encodedPath}")
    }
    return this
}
