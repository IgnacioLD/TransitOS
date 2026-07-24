package com.glossostudio.transitos.core.repository

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.Stop
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * The single transit data contract. Every provider module implements this against
 * its own data source; the rest of the app depends only on this interface.
 *
 * Methods return cold [Flow]s so the UI observes continuous updates (essential
 * for live arrivals). Implementations decide their own refresh cadence and
 * caching strategy — the contract is silent on those.
 *
 * [planJourney] is the exception: it is a one-shot suspend because a journey
 * plan is a discrete query, not an observable stream. It returns null when the
 * operator cannot find a route for the given O/D/date (different from a
 * network error, which would throw and be caught by the caller).
 */
public interface TransitRepository {
    public fun observeStops(): Flow<List<Stop>>
    public fun observeLines(): Flow<List<Line>>
    public fun observeArrivals(stopId: String): Flow<List<Arrival>>
    public fun observeAlerts(): Flow<List<Alert>>

    /**
     * @param originStopId canonical stop id (e.g. `"mv:12"`).
     * @param destinationStopId canonical stop id.
     * @param date the service date to plan for.
     * @param hora `"HH:mm"` or null for current time.
     * @param isDeparture true = depart at [hora], false = arrive by [hora].
     * @param minTransferMinutes buffer added to tight transfers.
     * @return planned journey alternatives (empty list if no route exists).
     */
    public suspend fun planJourney(
        originStopId: String,
        destinationStopId: String,
        date: LocalDate,
        hora: String? = null,
        isDeparture: Boolean = true,
        minTransferMinutes: Int = 5,
    ): List<Journey>
}
