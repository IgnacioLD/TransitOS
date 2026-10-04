package com.glossostudio.transitos.provider.metrovalencia

import com.glossostudio.transitos.core.network.NetworkConfig
import com.glossostudio.transitos.core.network.NetworkMonitor
import com.glossostudio.transitos.core.network.installTransitOsPlugins
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import com.glossostudio.transitos.core.result.AppError
import com.glossostudio.transitos.core.result.AppResult
import com.glossostudio.transitos.provider.metrovalencia.api.MetrovalenciaApi
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.net.UnknownHostException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetrovalenciaRepositoryTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val stationsJson = """[{"id":226,"estacion_id_FGV":12,"nombre":"Benimaclet"}]"""
    private val arrivalsJson = """{"previsiones":[]}"""

    /** Routes requests to [handler]; set [offline] to make every call fail like airplane mode. */
    private class Server(var handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        var offline = false
        val calls = mutableListOf<String>()
    }

    private val online = MutableStateFlow(true)

    private fun TestScope.repository(server: Server): MetrovalenciaRepository {
        val engine = MockEngine.create {
            // Run the engine on the test scheduler so virtual time stays exact.
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
                server.calls += request.url.encodedPath.substringAfterLast("/V/")
                if (server.offline) throw UnknownHostException("www.fgv.es")
                server.handler(this, request)
            }
        }
        val client = HttpClient(engine) { installTransitOsPlugins(NetworkConfig()) }
        val config = MetrovalenciaConfig()
        return MetrovalenciaRepository(
            api = MetrovalenciaApi(client, config),
            config = config,
            ioScope = backgroundScope,
            settings = object : ProviderSettingsRepository {
                override fun observeProviderBackend(providerId: String): Flow<String> = flowOf("fgv")
                override suspend fun setProviderBackend(providerId: String, backendId: String) = Unit
            },
            networkMonitor = object : NetworkMonitor {
                override val isOnline: Flow<Boolean> = online
            },
            nowMs = { testScheduler.currentTime },
        )
    }

    private fun okServer() = Server { request ->
        when {
            request.url.encodedPath.endsWith("estaciones") -> respond(stationsJson, HttpStatusCode.OK, json)
            request.url.encodedPath.contains("horarios-prevision-3") -> respond(arrivalsJson, HttpStatusCode.OK, json)
            else -> respond("[]", HttpStatusCode.OK, json)
        }
    }

    @Test
    fun `an offline start fails, then recovers on backoff instead of waiting hours`() = runTest {
        val server = okServer().apply { offline = true }
        val repo = repository(server)
        val emissions = mutableListOf<AppResult<*>>()
        backgroundScope.launch { repo.observeStops().collect { emissions += it } }
        runCurrent()

        assertThat(emissions.first()).isEqualTo(AppResult.Loading)
        assertThat((emissions.last() as AppResult.Failure).error).isInstanceOf(AppError.Offline::class.java)

        server.offline = false
        advanceTimeBy(10_000) // a few backoff steps, far below the 6 h catalogue interval
        runCurrent()

        assertThat(emissions.last()).isInstanceOf(AppResult.Success::class.java)
    }

    @Test
    fun `regaining connectivity retries immediately`() = runTest {
        val server = okServer().apply { offline = true }
        online.value = false
        val repo = repository(server)
        val emissions = mutableListOf<AppResult<*>>()
        backgroundScope.launch { repo.observeStops().collect { emissions += it } }
        runCurrent()
        advanceTimeBy(60_000) // let the backoff grow well past a few seconds
        runCurrent()
        assertThat(emissions.last()).isInstanceOf(AppResult.Failure::class.java)

        server.offline = false
        online.value = true
        runCurrent() // no time passes: the retry must not wait for the backoff

        assertThat(emissions.last()).isInstanceOf(AppResult.Success::class.java)
    }

    @Test
    fun `a failed refresh after data loaded keeps the last good value`() = runTest {
        val server = okServer()
        val repo = repository(server)
        val emissions = mutableListOf<AppResult<*>>()
        backgroundScope.launch { repo.observeStops().collect { emissions += it } }
        runCurrent()
        assertThat(emissions.last()).isInstanceOf(AppResult.Success::class.java)

        server.offline = true
        repo.refresh()
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()

        assertThat(emissions.none { it is AppResult.Failure }).isTrue()
    }

    @Test
    fun `refresh wakes every arrival poller`() = runTest {
        val server = okServer()
        val repo = repository(server)
        backgroundScope.launch { repo.observeArrivals("mv:12").collect { } }
        backgroundScope.launch { repo.observeArrivals("mv:13").collect { } }
        runCurrent()
        val before = server.calls.count { it.startsWith("horarios-prevision-3") }

        repo.refresh()
        runCurrent()

        val after = server.calls.count { it.startsWith("horarios-prevision-3") }
        assertThat(after - before).isEqualTo(2)
    }

    @Test
    fun `planner failures reported in the body become errors, empty results stay a success`() = runTest {
        val server = okServer()
        val repo = repository(server)
        val date = LocalDate(2026, 10, 5)

        server.handler = { request ->
            if (request.url.encodedPath.endsWith("estaciones")) {
                respond("""[{"id":226,"estacion_id_FGV":12,"nombre":"Benimaclet"},{"id":267,"estacion_id_FGV":55,"nombre":"Empalme"}]""", HttpStatusCode.OK, json)
            } else {
                respond("""{"status":400,"error":-1,"resultado":[]}""", HttpStatusCode.OK, json)
            }
        }
        val failed = repo.planJourney("mv:12", "mv:55", date, "10:00")
        assertThat((failed as AppResult.Failure).error).isEqualTo(AppError.Server(400, "FGV planner answered status 400"))

        server.handler = { request ->
            if (request.url.encodedPath.endsWith("estaciones")) {
                respond("""[{"id":226,"estacion_id_FGV":12,"nombre":"Benimaclet"},{"id":267,"estacion_id_FGV":55,"nombre":"Empalme"}]""", HttpStatusCode.OK, json)
            } else {
                respond("""{"status":200,"error":0,"resultado":[]}""", HttpStatusCode.OK, json)
            }
        }
        assertThat(repo.planJourney("mv:12", "mv:55", date, "10:00")).isEqualTo(AppResult.Success(emptyList<Any>()))
    }
}
