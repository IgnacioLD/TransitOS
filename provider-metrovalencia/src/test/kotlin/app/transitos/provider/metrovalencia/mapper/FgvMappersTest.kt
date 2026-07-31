package com.glossostudio.transitos.provider.metrovalencia.mapper

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.TransportMode
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvIncidenciaDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvJourneyAlternativeDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvLineDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPasoDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPlanificadorDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPlanificadorLineaDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPrevisionDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvStationDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvTrainDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvTransbordoDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvTransbordoPlanificadorDto
import com.glossostudio.transitos.provider.metrovalencia.mapper.LineDisplayInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class FgvMappersTest {

    @Test
    fun `station dto maps to stop with prefixed id and coordinates`() {
        val dto = FgvStationDto(
            estacionIdFgv = 12,
            nombre = "Benimaclet",
            latitud = 39.4848,
            longitud = -0.3623,
            transbordo = 1,
        )

        val stop = dto.toStop()

        assertThat(stop.id).isEqualTo("mv:12")
        assertThat(stop.name).isEqualTo("Benimaclet")
        assertThat(stop.operatorId).isEqualTo("metrovalencia")
        assertThat(stop.mode).isEqualTo(TransportMode.METRO)
        assertThat(stop.lat).isEqualTo(39.4848)
        assertThat(stop.lon).isEqualTo(-0.3623)
    }

    @Test
    fun `station dto tolerates null coordinates`() {
        val dto = FgvStationDto(estacionIdFgv = 1, nombre = "X", latitud = null, longitud = null)

        val stop = dto.toStop()

        assertThat(stop.lat).isNull()
        assertThat(stop.lon).isNull()
    }

    @Test
    fun `line dto maps with short name and parsed color`() {
        val dto = FgvLineDto(lineaIdFgv = 1, nombreCorto = "L1", color = "#FEC601")

        val line = dto.toLine()

        assertThat(line.id).isEqualTo("mv:1")
        assertThat(line.name).isEqualTo("L1")
        // RGB hex becomes ARGB with full alpha.
        assertThat(line.color).isEqualTo(0xFFFEC601L)
    }

    @Test
    fun `line dto with no color leaves color null`() {
        val dto = FgvLineDto(lineaIdFgv = 2, nombreCorto = "L2", color = null)

        assertThat(dto.toLine().color).isNull()
    }

    @Test
    fun `parseArgbHexOrNull handles RRGGBB, AARRGGBB and rejects garbage`() {
        assertThat(parseArgbHexOrNull("#FEC601")).isEqualTo(0xFFFEC601L)
        assertThat(parseArgbHexOrNull("#80FEC601")).isEqualTo(0x80FEC601L)
        assertThat(parseArgbHexOrNull("FEC601")).isEqualTo(0xFFFEC601L)
        assertThat(parseArgbHexOrNull("#XYZ")).isNull()
        assertThat(parseArgbHexOrNull("#12")).isNull()
    }

    @Test
    fun `prevision expands into one arrival per train with line display info`() {
        val prevision = FgvPrevisionDto(
            lineId = 3L,
            trains = listOf(
                FgvTrainDto(destino = "Aeroport", seconds = 120),
                FgvTrainDto(destino = "Rafelbunyol", seconds = 480, vehicle = 3083),
            ),
        )
        val lineByFgvId = mapOf(
            3L to Line(
                id = "mv:3", name = "L3", operatorId = "metrovalencia",
                mode = TransportMode.METRO, color = 0xFFCE2348L,
            ),
        )

        val arrivals = prevision.toArrivals(
            stopId = "mv:12",
            nowEpochMs = 1_000_000L,
            lineByFgvId = lineByFgvId,
        )

        assertThat(arrivals).hasSize(2)
        assertThat(arrivals[0].destination).isEqualTo("Aeroport")
        assertThat(arrivals[0].minutesAway).isEqualTo(2)
        assertThat(arrivals[0].estimatedEpochMs).isEqualTo(1_120_000L)
        assertThat(arrivals[0].lineId).isEqualTo("mv:3")
        assertThat(arrivals[0].stopId).isEqualTo("mv:12")
        assertThat(arrivals[0].isRealTime).isTrue()
        assertThat(arrivals[0].lineShortName).isEqualTo("L3")
        assertThat(arrivals[0].lineColor).isEqualTo(0xFFCE2348L)
        assertThat(arrivals[0].vehicleId).isNull()

        assertThat(arrivals[1].destination).isEqualTo("Rafelbunyol")
        assertThat(arrivals[1].minutesAway).isEqualTo(8)
        assertThat(arrivals[1].vehicleId).isEqualTo("3083")
    }

    @Test
    fun `prevision with no trains yields empty list`() {
        val prevision = FgvPrevisionDto(lineId = 1L, trains = emptyList())

        assertThat(
            prevision.toArrivals(stopId = "mv:1", nowEpochMs = 0L, lineByFgvId = emptyMap()),
        ).isEmpty()
    }

    @Test
    fun `prevision falls back to line or lineaIdInterna when lineId missing`() {
        val prevision = FgvPrevisionDto(
            lineId = null, line = 5L, trains = listOf(
                FgvTrainDto(destino = "X", seconds = 60),
            ),
        )

        val arrivals = prevision.toArrivals(
            stopId = "mv:1",
            nowEpochMs = 0L,
            lineByFgvId = emptyMap(),
        )

        assertThat(arrivals).hasSize(1)
        assertThat(arrivals[0].lineId).isEqualTo("mv:5")
        // No line lookup entry → display hints stay null and UI falls back to neutral.
        assertThat(arrivals[0].lineShortName).isNull()
        assertThat(arrivals[0].lineColor).isNull()
    }

    @Test
    fun `incidencia maps to alert with resolved line name`() {
        val dto = FgvIncidenciaDto(id = 42, lineaId = 3)
        val lookup = mapOf(
            3L to LineDisplayInfo(canonicalId = "mv:3", shortName = "L3", color = 0xFFDD052CL),
        )

        val alert = dto.toAlert(
            lineByInternalId = lookup,
            translations = emptyList(),
            locale = "es",
        )

        assertThat(alert.id).isEqualTo("mv:42")
        assertThat(alert.lineIds).containsExactly("mv:3")
        assertThat(alert.title).isEqualTo("Línea L3 con incidencias")
        assertThat(alert.severity).isEqualTo(Alert.Severity.WARNING)
        assertThat(alert.body).isNull()
        assertThat(alert.operatorId).isEqualTo("metrovalencia")
        assertThat(alert.lineShortName).isEqualTo("L3")
        assertThat(alert.lineColor).isEqualTo(0xFFDD052CL)
    }

    @Test
    fun `incidencia falls back to raw line id when name unknown`() {
        val dto = FgvIncidenciaDto(id = 1, lineaId = 99)

        val alert = dto.toAlert(
            lineByInternalId = emptyMap(),
            translations = emptyList(),
            locale = "es",
        )

        assertThat(alert.title).isEqualTo("Línea 99 con incidencias")
        assertThat(alert.lineShortName).isNull()
        assertThat(alert.lineColor).isNull()
    }

    @Test
    fun `journey alternative maps with flattened sorted departures and metadata`() {
        val alternative = FgvJourneyAlternativeDto(
            estacion_origen = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
            estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
            tarifas = "A",
            huella_de_carbono = 1.64,
            duracion_minutos = 27,
            distancia = 11434,
            transbordos = listOf(
                FgvTransbordoDto(
                    estacion_origen_transbordo = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
                    estacion_destino_transbordo = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
                    destinos = listOf("Rafelbunyol"),
                    horas = mapOf(
                        "05" to listOf("05:25", "05:55"),
                        "23" to listOf("23:00"),
                        "06" to listOf("06:25"),
                    ),
                ),
            ),
        )
        val date = LocalDate.parse("2026-07-23")

        val journey = alternative.toJourney(date)!!

        assertThat(journey.date).isEqualTo(date)
        assertThat(journey.durationMinutes).isEqualTo(27)
        assertThat(journey.distanceMeters).isEqualTo(11434)
        assertThat(journey.fareZone).isEqualTo("A")
        assertThat(journey.carbonKg).isEqualTo(1.64)
        assertThat(journey.hasTransfers).isFalse()
        assertThat(journey.legs).hasSize(1)
        val leg = journey.legs.single()
        assertThat(leg.originName).isEqualTo("Benimaclet")
        assertThat(leg.destinationName).isEqualTo("La Pobla de Farnals")
        assertThat(leg.headsigns).containsExactly("Rafelbunyol")
        // Sorted across hours, not insertion order.
        assertThat(leg.departures).containsExactly("05:25", "05:55", "06:25", "23:00").inOrder()
        // First and last departure across the sorted list.
        assertThat(leg.departures.first()).isEqualTo("05:25")
        assertThat(leg.departures.last()).isEqualTo("23:00")
    }

    @Test
    fun `journey alternative with no transbordos returns null`() {
        val alternative = FgvJourneyAlternativeDto(
            estacion_origen = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
            estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
            duracion_minutos = 0,
            transbordos = emptyList(),
        )

        assertThat(alternative.toJourney(LocalDate.parse("2026-07-23"))).isNull()
    }

    @Test
    fun `fecha formats as dd slash MM slash yyyy for FGV`() {
        assertThat(LocalDate.parse("2026-07-23").formatAsFgvFecha()).isEqualTo("23/07/2026")
        assertThat(LocalDate.parse("2026-12-01").formatAsFgvFecha()).isEqualTo("01/12/2026")
    }

    @Test
    fun `planificador direct journey maps with real times and duration`() {
        val dto = FgvPlanificadorDto(
            duracion_minutos = 27,
            estacion_origen = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
            estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
            pasos = listOf(
                FgvPasoDto(
                    orden = 1,
                    hora_salida = "10:00",
                    hora_llegada = "10:27",
                    estacion_origen = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
                    estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
                    tren_origen = "Rafelbunyol",
                    linea_origen = FgvPlanificadorLineaDto(
                        color = "#FEC601", nombre_corto = "L3", lineaIdFgv = 3,
                    ),
                ),
            ),
        )

        val journey = dto.toJourney(LocalDate.parse("2026-07-23"))!!

        assertThat(journey.legs).hasSize(1)
        assertThat(journey.legs[0].departureTime).isEqualTo("10:00")
        assertThat(journey.legs[0].arrivalTime).isEqualTo("10:27")
        assertThat(journey.legs[0].waitMinutes).isNull()
        // Duration from actual times, not the API's duracion_minutos.
        assertThat(journey.durationMinutes).isEqualTo(27)
    }

    @Test
    fun `planificador transfer journey computes real wait from schedule gap`() {
        val dto = FgvPlanificadorDto(
            duracion_minutos = 40,
            estacion_origen = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
            estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
            pasos = listOf(
                FgvPasoDto(
                    orden = 1,
                    hora_salida = "10:00",
                    hora_llegada = "10:15",
                    estacion_origen = FgvStationDto(estacionIdFgv = 12, nombre = "Benimaclet"),
                    estacion_destino = FgvStationDto(estacionIdFgv = 20, nombre = "Almassera"),
                    tren_origen = "Rafelbunyol",
                    linea_origen = FgvPlanificadorLineaDto(color = "#FEC601", nombre_corto = "L3"),
                    transbordo = FgvTransbordoPlanificadorDto(minEspera = 3),
                ),
                FgvPasoDto(
                    orden = 2,
                    hora_salida = "10:22",
                    hora_llegada = "10:40",
                    estacion_origen = FgvStationDto(estacionIdFgv = 20, nombre = "Almassera"),
                    estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "La Pobla de Farnals"),
                    tren_origen = "Castelló",
                    linea_origen = FgvPlanificadorLineaDto(color = "#FF6600", nombre_corto = "L5"),
                ),
            ),
        )

        val journey = dto.toJourney(LocalDate.parse("2026-07-23"))!!

        assertThat(journey.legs).hasSize(2)
        // Real wait is 10:22 - 10:15 = 7 minutes, not the minEspera of 3.
        assertThat(journey.legs[1].waitMinutes).isEqualTo(7)
        // Duration from 10:00 to 10:40 = 40 minutes.
        assertThat(journey.durationMinutes).isEqualTo(40)
    }

    @Test
    fun `planificador transfer falls back to minEspera when times unparseable`() {
        val dto = FgvPlanificadorDto(
            duracion_minutos = 0,
            estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "Dest"),
            pasos = listOf(
                FgvPasoDto(
                    hora_salida = "10:00",
                    hora_llegada = "garbage",
                    estacion_origen = FgvStationDto(estacionIdFgv = 1, nombre = "A"),
                    estacion_destino = FgvStationDto(estacionIdFgv = 3, nombre = "B"),
                    transbordo = FgvTransbordoPlanificadorDto(minEspera = 5),
                ),
                FgvPasoDto(
                    hora_salida = "10:10",
                    hora_llegada = "10:20",
                    estacion_origen = FgvStationDto(estacionIdFgv = 3, nombre = "B"),
                    estacion_destino = FgvStationDto(estacionIdFgv = 2, nombre = "Dest"),
                ),
            ),
        )

        val journey = dto.toJourney(LocalDate.parse("2026-07-23"))!!

        // Unparseable arrival → fallback to minEspera.
        assertThat(journey.legs[1].waitMinutes).isEqualTo(5)
    }

    @Test
    fun `timeDiffMinutes handles same-hour, cross-hour, and overnight`() {
        assertThat(timeDiffMinutes("10:00", "10:27")).isEqualTo(27)
        assertThat(timeDiffMinutes("10:45", "11:15")).isEqualTo(30)
        assertThat(timeDiffMinutes("23:50", "00:10")).isEqualTo(20)
    }

    @Test
    fun `timeDiffMinutes returns null for unparseable input`() {
        assertThat(timeDiffMinutes("garbage", "10:00")).isNull()
        assertThat(timeDiffMinutes("10:00", "")).isNull()
    }

    @Test
    fun `addMinutesToTime wraps past midnight`() {
        assertThat(addMinutesToTime("10:00", 15)).isEqualTo("10:15")
        assertThat(addMinutesToTime("23:50", 20)).isEqualTo("00:10")
        assertThat(addMinutesToTime("09:00", 0)).isEqualTo("09:00")
    }
}
