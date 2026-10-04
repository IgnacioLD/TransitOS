package com.glossostudio.transitos.core.repository

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.LineGeometry
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

/**
 * The single transit data contract. Every provider module implements this against
 * its own data source; the rest of the app depends only on this interface.
 *
 * Methods return [Flow]s so the UI observes continuous updates (essential for
 * live arrivals). Implementations decide their own refresh cadence and caching
 * strategy, the contract is silent on those.
 *
 * Failures are values, never exceptions: data streams emit [AppResult.Loading]
 * first, then [AppResult.Success] or [AppResult.Failure]. Once a stream has
 * delivered data, a later failed refresh keeps the last good value instead of
 * replacing it with a failure, and the implementation retries on its own.
 *
 * [planJourney] is a one-shot suspend because a journey plan is a discrete
 * query, not an observable stream. An empty success means the operator found
 * no route for the given O/D/date.
 */
public interface TransitRepository {
    public fun observeStops(): Flow<AppResult<List<Stop>>>
    public fun observeLines(): Flow<AppResult<List<Line>>>

    /**
     * The operator-published shape of every line, for drawing the network on a
     * map. Providers that do not publish geometry return an empty list, so the
     * UI can fall back to bundled offline data.
     */
    public fun observeLineGeometries(): Flow<List<LineGeometry>> = flowOf(emptyList())

    public fun observeArrivals(stopId: String): Flow<AppResult<List<Arrival>>>
    public fun observeAlerts(): Flow<AppResult<List<Alert>>>

    /**
     * @param originStopId canonical stop id (e.g. `"mv:12"`).
     * @param destinationStopId canonical stop id.
     * @param date the service date to plan for.
     * @param hora `"HH:mm"` or null for current time.
     * @param isDeparture true = depart at [hora], false = arrive by [hora].
     * @param minTransferMinutes buffer added to tight transfers.
     * @return planned journey alternatives (an empty list if no route exists).
     */
    public suspend fun planJourney(
        originStopId: String,
        destinationStopId: String,
        date: LocalDate,
        hora: String? = null,
        isDeparture: Boolean = true,
        minTransferMinutes: Int = 5,
    ): AppResult<List<Journey>>

    /** Re-fetches everything now: live data, and anything still failing. */
    public suspend fun refresh()
}
