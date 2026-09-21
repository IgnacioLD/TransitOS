package com.glossostudio.transitos.map

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** One simulated train: where it is and which line colour it carries. */
internal data class TrainMarker(
    val position: GeoPoint,
    val color: Int,
)

/** Nominal headway per mode, in seconds. Metro runs more often than tram. */
private const val METRO_HEADWAY_SECONDS = 450.0
private const val TRAM_HEADWAY_SECONDS = 900.0

/** Nominal average commercial speed in m/s, stops included. */
private const val METRO_SPEED_MPS = 9.0
private const val TRAM_SPEED_MPS = 6.0

/** Cap per direction so long lines do not flood the map with markers. */
private const val MAX_TRAINS_PER_DIRECTION = 3

/**
 * Approximate train movement along the published line geometry.
 *
 * **This is not a GPS feed.** FGV publishes no vehicle positions (see
 * `provider-metrovalencia/RESEARCH.md`), so trains are spaced by a nominal
 * headway and driven at a nominal commercial speed along the real alignment.
 * It shows where trains plausibly are at a given moment, not where they are.
 *
 * The line geometries are precomputed once so each animation frame only does a
 * cheap lookup along a cumulative-distance array.
 */
internal class TrainSimulator(lines: List<MapLine>) {

    private class SimLine(
        val color: Int,
        val geometry: PathGeometry,
        val speedMps: Double,
        val headwaySeconds: Double,
        val trainsPerDirection: Int,
    )

    private val simLines: List<SimLine> = lines.mapNotNull { line ->
        val geometry = PathGeometry.of(line.path) ?: return@mapNotNull null
        val speedMps = if (line.isTram) TRAM_SPEED_MPS else METRO_SPEED_MPS
        val headwaySeconds = if (line.isTram) TRAM_HEADWAY_SECONDS else METRO_HEADWAY_SECONDS
        val travelTime = geometry.totalMeters / speedMps
        SimLine(
            color = line.color,
            geometry = geometry,
            speedMps = speedMps,
            headwaySeconds = headwaySeconds,
            trainsPerDirection = (travelTime / headwaySeconds)
                .roundToInt()
                .coerceIn(1, MAX_TRAINS_PER_DIRECTION),
        )
    }

    /** Train positions at [clockSeconds], measured from any fixed epoch. */
    fun markersAt(clockSeconds: Double): List<TrainMarker> {
        val markers = ArrayList<TrainMarker>(simLines.size * 2)
        simLines.forEach { line ->
            val travelTime = line.geometry.totalMeters / line.speedMps
            if (travelTime <= 0.0) return@forEach
            for (direction in 0..1) {
                for (index in 0 until line.trainsPerDirection) {
                    val phase = clockSeconds +
                        index * line.headwaySeconds +
                        direction * line.headwaySeconds / 2.0
                    val fraction = (phase % travelTime) / travelTime
                    val progress = if (direction == 0) fraction else 1.0 - fraction
                    markers.add(
                        TrainMarker(
                            position = line.geometry.pointAt(progress),
                            color = line.color,
                        ),
                    )
                }
            }
        }
        return markers
    }

    private class PathGeometry(
        private val points: List<GeoPoint>,
        private val cumulative: DoubleArray,
        val totalMeters: Double,
    ) {
        fun pointAt(fraction: Double): GeoPoint {
            val target = fraction.coerceIn(0.0, 1.0) * totalMeters
            var index = 0
            while (index < cumulative.lastIndex && cumulative[index + 1] < target) index++
            val start = points[index]
            val end = points[index + 1]
            val segment = cumulative[index + 1] - cumulative[index]
            val t = if (segment <= 0.0) 0.0 else (target - cumulative[index]) / segment
            return GeoPoint(
                start.latitude + (end.latitude - start.latitude) * t,
                start.longitude + (end.longitude - start.longitude) * t,
            )
        }

        companion object {
            fun of(points: List<GeoPoint>): PathGeometry? {
                if (points.size < 2) return null
                val cumulative = DoubleArray(points.size)
                var total = 0.0
                for (i in 1 until points.size) {
                    total += haversineMeters(points[i - 1], points[i])
                    cumulative[i] = total
                }
                if (total <= 0.0) return null
                return PathGeometry(points, cumulative, total)
            }
        }
    }

    private companion object {
        fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
            val earthRadius = 6_371_000.0
            val lat1 = Math.toRadians(a.latitude)
            val lat2 = Math.toRadians(b.latitude)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(b.longitude - a.longitude)
            val h = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            return 2 * earthRadius * asin(min(1.0, sqrt(h)))
        }
    }
}

/**
 * Draws the simulated trains as small line-coloured pills with two windows, so
 * they read as vehicles and stay coherent with the map's line colours. Kept as
 * a native [Overlay] so a frame only repaints one canvas instead of rebuilding
 * markers.
 */
internal class LiveTrainsOverlay : Overlay() {

    var markers: List<TrainMarker> = emptyList()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = AndroidColor.WHITE
    }
    private val window = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
    }
    private val rect = RectF()
    private val screenPoint = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || markers.isEmpty()) return
        val projection = mapView.projection
        val density = mapView.resources.displayMetrics.density
        val width = 22f * density
        val height = 15f * density
        val radius = 5f * density
        val windowRadius = 1.8f * density
        border.strokeWidth = 1.5f * density

        markers.forEach { marker ->
            projection.toPixels(marker.position, screenPoint)
            val left = screenPoint.x - width / 2f
            val top = screenPoint.y - height / 2f
            rect.set(left, top, left + width, top + height)
            fill.color = marker.color
            canvas.drawRoundRect(rect, radius, radius, fill)
            canvas.drawRoundRect(rect, radius, radius, border)
            val windowY = top + height / 2f
            canvas.drawCircle(left + width * 0.32f, windowY, windowRadius, window)
            canvas.drawCircle(left + width * 0.68f, windowY, windowRadius, window)
        }
    }
}
