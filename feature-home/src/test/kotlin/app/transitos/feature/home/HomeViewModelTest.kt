package com.glossostudio.transitos.feature.home

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.SavedRoute
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.model.TransportMode
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.RouteFavoritesRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recovers from error on retry`() = runTest {
        val repository = FakeTransitRepository(failStops = true)
        val viewModel = HomeViewModel(
            repository = repository,
            favorites = FakeFavoritesRepository(),
            routeFavorites = FakeRouteFavoritesRepository(),
        )
        // Keep the WhileSubscribed stateIn hot so it mirrors production, where
        // the screen always has an active collector.
        backgroundScope.launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Error::class.java)

        // The failure is gone; retrying must leave the Error state.
        repository.failStops = false
        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Ready::class.java)
    }

    private class FakeTransitRepository(
        var failStops: Boolean,
    ) : TransitRepository {
        private val stops = listOf(
            Stop(
                id = "mv:benimaclet",
                name = "Benimaclet",
                operatorId = "metrovalencia",
                mode = TransportMode.METRO,
                lat = 39.48,
                lon = -0.35,
            ),
        )

        override fun observeStops(): Flow<List<Stop>> = flow {
            if (failStops) throw IllegalStateException("stops unavailable")
            emit(stops)
        }

        override fun observeLines(): Flow<List<Line>> = flowOf(emptyList())

        override fun observeArrivals(stopId: String): Flow<List<Arrival>> = flowOf(emptyList())

        override fun observeAlerts(): Flow<List<Alert>> = flowOf(emptyList())

        override suspend fun planJourney(
            originStopId: String,
            destinationStopId: String,
            date: LocalDate,
            hora: String?,
            isDeparture: Boolean,
            minTransferMinutes: Int,
        ): List<Journey> = emptyList()

        override suspend fun refresh() = Unit
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        override fun observeFavoriteStopIds(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun addFavorite(stopId: String) = Unit
        override suspend fun removeFavorite(stopId: String) = Unit
    }

    private class FakeRouteFavoritesRepository : RouteFavoritesRepository {
        override fun observeSavedRoutes(): Flow<List<SavedRoute>> = flowOf(emptyList())
        override suspend fun saveRoute(route: SavedRoute) = Unit
        override suspend fun removeRoute(routeId: String) = Unit
        override suspend fun renameRoute(routeId: String, label: String) = Unit
    }
}
