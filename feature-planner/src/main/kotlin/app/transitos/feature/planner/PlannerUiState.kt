package com.glossostudio.transitos.feature.planner

import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Stop
import kotlinx.datetime.LocalDate

enum class TimeMode { DEPARTURE, ARRIVAL }

data class PlannerUiState(
    val origin: Stop? = null,
    val destination: Stop? = null,
    val date: LocalDate,
    val journeys: List<Journey> = emptyList(),
    val selectedJourneyIndex: Int = 0,
    val isPlanning: Boolean = false,
    val isSaved: Boolean = false,
    val timeMode: TimeMode = TimeMode.DEPARTURE,
    val travelTime: String? = null,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
) {
    val canPlan: Boolean get() = origin != null && destination != null && origin != destination
    val selectedJourney: Journey? get() = journeys.getOrNull(selectedJourneyIndex)
}
