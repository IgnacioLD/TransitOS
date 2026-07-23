package app.transitos.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * A scheduled journey between two stops on a given [date]. Composed of one or
 * more [JourneyLeg]s — direct trips have a single leg, transfers have several.
 *
 * The planner returns rich metadata (duration, distance, fare zone, CO₂) that
 * we surface for users who care about cost and sustainability. Each leg carries
 * the full list of [JourneyLeg.departures] for the day so the UI can show
 * "next train", "last train" and the full schedule from one query.
 */
@Serializable
public data class Journey(
    public val date: LocalDate,
    public val durationMinutes: Int,
    public val distanceMeters: Long,
    public val fareZone: String?,
    public val carbonKg: Double?,
    public val legs: List<JourneyLeg>,
) {
    /** Convenience: the first leg's first departure, or null if empty. */
    public val firstDeparture: String? get() = legs.firstOrNull()?.departures?.firstOrNull()

    /** Convenience: the last leg's last departure, or null if empty. */
    public val lastDeparture: String? get() = legs.lastOrNull()?.departures?.lastOrNull()

    /** True when the journey involves any transfer between legs. */
    public val hasTransfers: Boolean get() = legs.size > 1
}

/**
 * One rideable segment of a [Journey]. A direct trip = 1 leg; a trip with one
 * transfer = 2 legs (the transfer station is the destination of leg 1 and the
 * origin of leg 2).
 *
 * [departures] is a sorted list of `"HH:mm"` strings for [date] — every
 * train that will take this leg. The UI can show first/last/next and the full
 * day's schedule from this single field.
 */
@Serializable
public data class JourneyLeg(
    public val originName: String,
    public val destinationName: String,
    public val headsigns: List<String>,
    public val departures: List<String>,
)
