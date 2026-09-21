package com.glossostudio.transitos.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.repository.TransitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    private val repository: TransitRepository,
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = repository.observeAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The drawable network. Live when the provider publishes geometry, the
     * bundled offline fallback otherwise, so the map is never blank.
     */
    internal val network: StateFlow<MapNetwork> = combine(
        repository.observeLineGeometries(),
        repository.observeStops(),
    ) { geometries, stops ->
        if (geometries.isEmpty()) MapNetwork.fallback()
        else MapNetwork.fromLive(geometries, stops)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MapNetwork.fallback(),
    )

    fun observeArrivals(stopId: String) = repository.observeArrivals(stopId)

    val alertedLineNames: Set<String>
        get() = alerts.value.mapNotNull { it.lineShortName }.toSet()
}
