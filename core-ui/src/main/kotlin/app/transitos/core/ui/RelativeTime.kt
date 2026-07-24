package app.transitos.core.ui

/**
 * Renders an [epochMs] as a relative-time bucket: just-now, minutes or hours,
 * with the numeric value. Used by cards and chips to give feedback that the
 * displayed data is fresh without showing a wall-clock timestamp.
 *
 * The composable resolves each bucket to a localised string via
 * [stringResource], so this helper stays a pure, Android-free function that
 * is trivially testable.
 */
sealed interface RelativeTime {
    data object JustNow : RelativeTime
    data class Minutes(val value: Long) : RelativeTime
    data class Hours(val value: Long) : RelativeTime
}

fun relativeTime(epochMs: Long, nowMs: Long = System.currentTimeMillis()): RelativeTime {
    val delta = (nowMs - epochMs).coerceAtLeast(0L)
    return when {
        delta < SECONDS_PER_MINUTE * MS_PER_SECOND -> RelativeTime.JustNow
        delta < SECONDS_PER_HOUR * MS_PER_SECOND ->
            RelativeTime.Minutes(delta / (SECONDS_PER_MINUTE * MS_PER_SECOND))
        else ->
            RelativeTime.Hours(delta / (SECONDS_PER_HOUR * MS_PER_SECOND))
    }
}

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
