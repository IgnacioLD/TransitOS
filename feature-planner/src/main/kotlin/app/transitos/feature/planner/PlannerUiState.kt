package app.transitos.feature.planner

import app.transitos.core.model.Journey
import app.transitos.core.model.Stop
import kotlinx.datetime.LocalDate

data class PlannerUiState(
    val origin: Stop? = null,
    val destination: Stop? = null,
    val date: LocalDate,
    val journeys: List<Journey> = emptyList(),
    val selectedJourneyIndex: Int = 0,
    val isPlanning: Boolean = false,
    val isSaved: Boolean = false,
    val arriveBy: String? = null,
    val errorMessage: String? = null,
) {
    val canPlan: Boolean get() = origin != null && destination != null && origin != destination
    val selectedJourney: Journey? get() = journeys.getOrNull(selectedJourneyIndex)
}
