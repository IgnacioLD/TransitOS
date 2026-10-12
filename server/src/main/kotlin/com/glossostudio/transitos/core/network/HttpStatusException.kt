package com.glossostudio.transitos.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import java.io.IOException

/**
 * An upstream answered with a non-success HTTP status. Mirrors the app's
 * `:core-network` type of the same name so the FGV client code moves over
 * unchanged.
 */
class HttpStatusException(val status: Int, message: String) : IOException(message)

/** Throws [HttpStatusException] unless the response has a 2xx status. */
fun HttpResponse.requireSuccess(): HttpResponse {
    if (!status.isSuccess()) {
        throw HttpStatusException(status.value, "HTTP ${status.value} from ${call.request.url.encodedPath}")
    }
    return this
}
