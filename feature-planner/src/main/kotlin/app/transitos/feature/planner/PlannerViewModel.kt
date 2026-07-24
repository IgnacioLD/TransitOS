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
        _state.update { it.copy(origin = stop, hasSearched = false, journeys = emptyList()) }
    }

    fun setDestination(stop: Stop) {
        _state.update { it.copy(destination = stop, hasSearched = false, journeys = emptyList()) }
    }

    fun setDate(date: LocalDate) {
        _state.update { it.copy(date = date) }
    }

    fun setTravelTime(time: String?) {
        _state.update { it.copy(travelTime = time) }
    }

    fun setTimeMode(mode: TimeMode) {
        _state.update { it.copy(timeMode = mode) }
    }

    fun selectJourney(index: Int) {
        _state.update { it.copy(selectedJourneyIndex = index) }
    }

    fun prefillRoute(originId: String, destinationId: String) {
        val stops = stops.value
        val origin = stops.find { it.id == originId }
        val destination = stops.find { it.id == destinationId }
        if (origin != null && destination != null) {
            _state.update { it.copy(origin = origin, destination = destination) }
            search()
        }
    }

    fun swapEndpoints() {
        _state.update { it.copy(origin = it.destination, destination = it.origin, hasSearched = false, journeys = emptyList()) }
    }

    fun search() {
        val current = _state.value
        val origin = current.origin ?: return
        val destination = current.destination ?: return
        if (origin.id == destination.id) {
            _state.update { it.copy(journeys = emptyList(), errorMessage = null, hasSearched = true) }
            return
        }
        val routeId = routeIdFor(origin.id, destination.id)
        viewModelScope.launch {
            _state.update { it.copy(isPlanning = true, errorMessage = null, hasSearched = true) }
            val result = runCatching {
                repository.planJourney(origin.id, destination.id, current.date, current.travelTime)
            }
            _state.update { s ->
                val journeys = result.getOrDefault(emptyList())
                s.copy(
                    isPlanning = false,
                    journeys = journeys,
                    selectedJourneyIndex = 0,
                    errorMessage = if (journeys.isEmpty()) result.exceptionOrNull()?.message else null,
                    isSaved = routeId in savedRouteIds.value,
                )
            }
        }
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

    private companion object {
        fun routeIdFor(originId: String, destinationId: String): String = "route:$originId:$destinationId"
    }
}
