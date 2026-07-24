package com.glossostudio.transitos.core.result

/**
 * Typed failure surface for the whole app. Network layers map their exceptions
 * into these; the UI switches on them to produce user-facing messages.
 *
 * Keep this list small. Add a variant only when the UI needs to differentiate
 * the response — otherwise reuse [Unknown].
 */
public sealed class AppError(public open val message: String) {
    public data class Offline(public override val message: String) : AppError(message)
    public data class Network(public override val message: String) : AppError(message)
    public data class Timeout(public override val message: String) : AppError(message)
    public data class Unauthorized(public override val message: String) : AppError(message)
    public data class Server(public val code: Int, public override val message: String) : AppError(message)
    public data class NotFound(public override val message: String) : AppError(message)
    public data class Parsing(public override val message: String) : AppError(message)
    public data class Unknown(
        public override val message: String,
        public val cause: Throwable? = null,
    ) : AppError(message)
}
