package com.glossostudio.transitos.feature.home

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.SavedRoute
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.model.TransportMode
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.HintKeys
import com.glossostudio.transitos.core.repository.HintsPreference
import com.glossostudio.transitos.core.repository.RouteFavoritesRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.core.result.AppError
import com.glossostudio.transitos.core.result.AppResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

    private val benimaclet = Stop(
        id = "mv:12",
        name = "Benimaclet",
        operatorId = "metrovalencia",
        mode = TransportMode.METRO,
        lat = 39.48,
        lon = -0.35,
    )
    private val offline = AppError.Offline("no network")

    private fun TestScope.homeViewModel(
        repository: FakeTransitRepository,
        favoriteIds: Set<String> = emptySet(),
    ): HomeViewModel {
        val viewModel = HomeViewModel(
            repository = repository,
            favorites = FakeFavoritesRepository(favoriteIds),
            routeFavorites = FakeRouteFavoritesRepository(),
            hintsPreference = FakeHintsPreference(),
        )
        // Keep the WhileSubscribed stateIn hot so it mirrors production, where
        // the screen always has an active collector.
        backgroundScope.launch { viewModel.uiState.collect { } }
        return viewModel
    }

    @Test
    fun `failed catalogue with favourites is an error, and recovers when the catalogue loads`() = runTest {
        val repository = FakeTransitRepository(stops = AppResult.Failure(offline))
        val viewModel = homeViewModel(repository, favoriteIds = setOf(benimaclet.id))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(HomeUiState.Error(offline))

        // The provider retries on its own; its next emission must lift the error.
        repository.stops.value = AppResult.Success(listOf(benimaclet))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Ready::class.java)
    }

    @Test
    fun `failed catalogue without favourites still renders home`() = runTest {
        val viewModel = homeViewModel(FakeTransitRepository(stops = AppResult.Failure(offline)))
        advanceUntilIdle()
        val state = viewModel.uiState.value as HomeUiState.Ready
        assertThat(state.favorites).isEmpty()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `retry from the error state asks the repository to refresh`() = runTest {
        val repository = FakeTransitRepository(stops = AppResult.Failure(offline))
        val viewModel = homeViewModel(repository, favoriteIds = setOf(benimaclet.id))
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        assertThat(repository.refreshCount).isEqualTo(1)
    }

    @Test
    fun `failed alerts are reported as unavailable, never as no alerts`() = runTest {
        val repository = FakeTransitRepository(alerts = AppResult.Failure(offline))
        val viewModel = homeViewModel(repository)
        advanceUntilIdle()
        val state = viewModel.uiState.value as HomeUiState.Ready
        assertThat(state.alertsStatus).isEqualTo(AlertsStatus.Unavailable(offline))
        assertThat(state.alerts).isEmpty()
    }

    @Test
    fun `live times that fail or are pending are surfaced per favourite`() = runTest {
        val repository = FakeTransitRepository()
        val viewModel = homeViewModel(repository, favoriteIds = setOf(benimaclet.id))
        advanceUntilIdle()
        val pending = (viewModel.uiState.value as HomeUiState.Ready).favorites.single()
        assertThat(pending.isLoading).isTrue()

        repository.arrivals.value = AppResult.Failure(offline)
        advanceUntilIdle()
        val failed = (viewModel.uiState.value as HomeUiState.Ready).favorites.single()
        assertThat(failed.error).isEqualTo(offline)
        assertThat(failed.isLoading).isFalse()
        assertThat(failed.arrivals).isEmpty()
    }

    private inner class FakeTransitRepository(
        stops: AppResult<List<Stop>> = AppResult.Success(listOf(benimaclet)),
        alerts: AppResult<List<Alert>> = AppResult.Success(emptyList()),
    ) : TransitRepository {
        val stops = MutableStateFlow(stops)
        val alerts = MutableStateFlow(alerts)
        val arrivals = MutableStateFlow<AppResult<List<Arrival>>>(AppResult.Loading)
        var refreshCount = 0

        override fun observeStops(): Flow<AppResult<List<Stop>>> = stops

        override fun observeLines(): Flow<AppResult<List<Line>>> = flowOf(AppResult.Success(emptyList()))

        override fun observeArrivals(stopId: String): Flow<AppResult<List<Arrival>>> = arrivals

        override fun observeAlerts(): Flow<AppResult<List<Alert>>> = alerts

        override suspend fun planJourney(
            originStopId: String,
            destinationStopId: String,
            date: LocalDate,
            hora: String?,
            isDeparture: Boolean,
            minTransferMinutes: Int,
        ): AppResult<List<Journey>> = AppResult.Success(emptyList())

        override suspend fun refresh() {
            refreshCount++
        }
    }

    private class FakeFavoritesRepository(private val ids: Set<String>) : FavoritesRepository {
        override fun observeFavoriteStopIds(): Flow<Set<String>> = flowOf(ids)
        override suspend fun addFavorite(stopId: String) = Unit
        override suspend fun removeFavorite(stopId: String) = Unit
    }

    private class FakeRouteFavoritesRepository : RouteFavoritesRepository {
        override fun observeSavedRoutes(): Flow<List<SavedRoute>> = flowOf(emptyList())
        override suspend fun saveRoute(route: SavedRoute) = Unit
        override suspend fun removeRoute(routeId: String) = Unit
        override suspend fun renameRoute(routeId: String, label: String) = Unit
    }

    private class FakeHintsPreference : HintsPreference {
        private val seen = mutableSetOf<String>()
        override fun observeSeen(key: String): Flow<Boolean> = flowOf(key in seen)
        override fun isSeen(key: String): Boolean = key in seen
        override suspend fun markSeen(key: String) {
            seen += key
        }

        override suspend fun markAllSeen() {
            seen += HintKeys.all
        }

        override suspend fun resetAll() {
            seen.clear()
        }
    }
}
