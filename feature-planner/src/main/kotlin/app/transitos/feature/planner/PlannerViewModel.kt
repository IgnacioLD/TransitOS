package app.transitos.feature.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transitos.core.model.SavedRoute
import app.transitos.core.model.Stop
import app.transitos.core.repository.RouteFavoritesRepository
import app.transitos.core.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Drives the Planner screen. Holds the (origin, destination, date) triple and
 * re-plans whenever it changes. Planning is a one-shot [TransitRepository.planJourney]
 * call — not a polling flow — because the user is asking a discrete question
 * ("how do I get from A to B on day X?").
 *
 * Also exposes the catalogue of stops so the screen's station picker has the
 * full list without needing its own repository injection.
 */
class PlannerViewModel(
    private val repository: TransitRepository,
    private val routeFavorites: RouteFavoritesRepository,
) : ViewModel() {

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    val stops: StateFlow<List<Stop>> = repository.observeStops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val savedRouteIds: StateFlow<Set<String>> = routeFavorites.observeSavedRoutes()
        .map { routes -> routes.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _state = MutableStateFlow(PlannerUiState(date = Clock.System.todayIn(timeZone)))
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    fun setOrigin(stop: Stop) {
        _state.update { it.copy(origin = stop) }
        planIfReady()
    }

    fun setDestination(stop: Stop) {
        _state.update { it.copy(destination = stop) }
        planIfReady()
    }

    fun setDate(date: LocalDate) {
        _state.update { it.copy(date = date) }
        planIfReady()
    }

    /** Swaps origin and destination — common "I want to come back" action. */
    fun prefillRoute(originId: String, destinationId: String) {
        val stops = stops.value
        val origin = stops.find { it.id == originId }
        val destination = stops.find { it.id == destinationId }
        if (origin != null && destination != null) {
            _state.update { it.copy(origin = origin, destination = destination) }
            planIfReady()
        }
    }

    fun swapEndpoints() {
        _state.update { it.copy(origin = it.destination, destination = it.origin) }
        planIfReady()
    }

    fun saveCurrentRoute() {
        val current = _state.value
        val origin = current.origin ?: return
        val destination = current.destination ?: return
        val routeId = routeIdFor(origin.id, destination.id)
        viewModelScope.launch {
            routeFavorites.saveRoute(SavedRoute(id = routeId, originStopId = origin.id, destinationStopId = destination.id))
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun removeCurrentRoute() {
        val current = _state.value
        val origin = current.origin ?: return
        val destination = current.destination ?: return
        val routeId = routeIdFor(origin.id, destination.id)
        viewModelScope.launch {
            routeFavorites.removeRoute(routeId)
            _state.update { it.copy(isSaved = false) }
        }
    }

    private fun planIfReady() {
        val current = _state.value
        val origin = current.origin ?: return
        val destination = current.destination ?: return
        if (origin.id == destination.id) {
            _state.update { it.copy(journey = null, errorMessage = null) }
            return
        }
        val routeId = routeIdFor(origin.id, destination.id)
        viewModelScope.launch {
            _state.update { it.copy(isPlanning = true, errorMessage = null) }
            val result = runCatching { repository.planJourney(origin.id, destination.id, current.date) }
            _state.update { state ->
                state.copy(
                    isPlanning = false,
                    journey = result.getOrNull(),
                    errorMessage = result.exceptionOrNull()?.message,
                    isSaved = routeId in savedRouteIds.value,
                )
            }
        }
    }

    private companion object {
        fun routeIdFor(originId: String, destinationId: String): String = "route:$originId:$destinationId"
    }
}
