package app.transitos.feature.planner

import app.transitos.core.model.Journey
import app.transitos.core.model.Stop
import kotlinx.datetime.LocalDate

/**
 * Immutable state for the Planner screen. [origin], [destination] and [date]
 * drive [journey]: whenever all three are present the ViewModel re-plans and
 * surfaces the result (or null when no route exists for the pair).
 */
data class PlannerUiState(
    val origin: Stop? = null,
    val destination: Stop? = null,
    val date: LocalDate,
    val journey: Journey? = null,
    val isPlanning: Boolean = false,
    val errorMessage: String? = null,
) {
    /** True when both endpoints are selected — the minimum needed to plan. */
    val canPlan: Boolean get() = origin != null && destination != null && origin != destination
}
