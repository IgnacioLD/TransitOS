package app.transitos.core.ui

/**
 * Renders an [epochMs] as a Spanish relative-time phrase: "justo ahora",
 * "hace 3 min", "hace 2 h". Used by cards and chips to give feedback that the
 * displayed data is fresh without showing a wall-clock timestamp.
 *
 * Pure function — testable without Android.
 */
fun relativeTime(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val delta = (nowMs - epochMs).coerceAtLeast(0L)
    return when {
        delta < SECONDS_PER_MINUTE * MS_PER_SECOND -> "justo ahora"
        delta < SECONDS_PER_HOUR * MS_PER_SECOND -> "hace ${delta / (SECONDS_PER_MINUTE * MS_PER_SECOND)} min"
        else -> "hace ${delta / (SECONDS_PER_HOUR * MS_PER_SECOND)} h"
    }
}

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
