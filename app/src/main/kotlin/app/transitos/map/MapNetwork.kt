package com.glossostudio.transitos.map

import android.graphics.Color as AndroidColor
import com.glossostudio.transitos.core.model.LineGeometry
import com.glossostudio.transitos.core.model.Stop
import org.osmdroid.util.GeoPoint

/**
 * A station as the map draws it. [stopId] is the canonical repository id
 * (`"mv:12"`) so live arrivals can be fetched without matching by name; it is
 * null only for the bundled offline fallback, whose ids do not exist.
 */
internal data class MapStation(
    val name: String,
    val position: GeoPoint,
    val lines: List<String>,
    val stopId: String? = null,
)

/** A line as the map draws it: an authoritative polyline plus its stations. */
internal data class MapLine(
    val name: String,
    val color: Int,
    val path: List<GeoPoint>,
    val isTram: Boolean,
    val stationNames: List<String>,
)

/** Everything the map needs: the drawable lines and the station catalogue. */
internal data class MapNetwork(
    val lines: List<MapLine>,
    val stations: List<MapStation>,
) {
    private val stationByName: Map<String, MapStation> by lazy {
        stations.associateBy { it.name }
    }

    fun stationFor(name: String): MapStation? = stationByName[name]

    fun lineColor(name: String): Int? = lines.firstOrNull { it.name == name }?.color

    companion object {
        /**
         * Builds the network from the operator's published geometry and stops
         * catalogue. Lines are drawn from [geometries] (the real track), and
         * stations are placed at their real catalogue coordinates so markers
         * always line up with the drawn alignment.
         */
        fun fromLive(
            geometries: List<LineGeometry>,
            stops: List<Stop>,
        ): MapNetwork {
            val stopById = stops
                .filter { it.lat != null && it.lon != null && it.name.isNotBlank() }
                .associateBy { it.id }

            val lines = geometries.mapNotNull { geometry ->
                val path = geometry.points.map { GeoPoint(it.lat, it.lon) }
                if (path.size < 2) return@mapNotNull null
                MapLine(
                    name = geometry.shortName,
                    color = geometry.color?.toInt() ?: AndroidColor.GRAY,
                    path = path,
                    isTram = geometry.isTram,
                    stationNames = geometry.stationIds.mapNotNull { stopById[it]?.name },
                )
            }

            val stations = linkedMapOf<String, MapStation>()
            geometries.forEach { geometry ->
                geometry.stationIds.forEach { stopId ->
                    val stop = stopById[stopId] ?: return@forEach
                    val existing = stations[stopId]
                    stations[stopId] = MapStation(
                        name = stop.name,
                        position = GeoPoint(stop.lat!!, stop.lon!!),
                        lines = (existing?.lines.orEmpty() + geometry.shortName).distinct(),
                        stopId = stopId,
                    )
                }
            }
            return MapNetwork(lines = lines, stations = stations.values.toList())
        }

        /**
         * The bundled offline network, used until (or instead of) the live one.
         * It mirrors the hardcoded [metroLines] so the map is never blank.
         */
        fun fallback(): MapNetwork = MapNetwork(
            lines = metroLines.map { line ->
                MapLine(
                    name = line.name,
                    color = line.color,
                    path = line.path,
                    isTram = line.tram,
                    stationNames = line.stations.map { it.first },
                )
            },
            stations = stationPositions.map { (name, position) ->
                MapStation(
                    name = name,
                    position = position,
                    lines = stationLines[name].orEmpty(),
                    stopId = null,
                )
            },
        )
    }
}
