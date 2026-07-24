package app.transitos.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transitos.core.model.Alert
import app.transitos.core.model.Stop
import app.transitos.core.repository.TransitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    private val repository: TransitRepository,
) : ViewModel() {

    val stops: StateFlow<List<Stop>> = repository.observeStops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alerts: StateFlow<List<Alert>> = repository.observeAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun stopByName(name: String): Stop? =
        stops.value.find { it.name.equals(name, ignoreCase = true) }

    fun observeArrivals(stopId: String) = repository.observeArrivals(stopId)

    val alertedLineNames: Set<String>
        get() = alerts.value.mapNotNull { it.lineShortName }.toSet()
}
