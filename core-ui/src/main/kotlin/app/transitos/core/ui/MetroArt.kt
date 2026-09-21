package com.glossostudio.transitos.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * TransitOS's own illustration language: a fragment of a schematic metro plan.
 *
 * Everything here follows the same grammar, so the artwork reads consistently
 * wherever it appears:
 *  - lines are straight polylines that only turn in 45 and 90 degree steps,
 *    exactly like a real transit diagram, never loose curves;
 *  - stations sit on the lines as perpendicular ticks;
 *  - interchanges are double circles;
 *  - line ends carry a heavier terminal bar.
 *
 * The networks are plain data ([MetroNetwork]) in a normalised design space, so
 * new fragments can be authored without touching the renderer, and the same
 * fragment can be framed differently (cropped as a watermark, contained inside a
 * badge). Colours come from the official Metrovalencia palette so the app's
 * artwork matches the lines the user actually rides.
 */

/** What a mark on a line represents. */
enum class MetroStopKind { STOP, INTERCHANGE, TERMINAL }

/** A station placed in the normalised design space of a [MetroNetwork]. */
data class MetroStop(
    val x: Float,
    val y: Float,
    val kind: MetroStopKind = MetroStopKind.STOP,
)

/**
 * One line of the network: a 45/90 polyline plus the stations that sit on it.
 * Points are in the network's own design space; the renderer scales and centres
 * the whole thing.
 */
data class MetroLine(
    val id: String,
    val color: Color,
    val points: List<Offset>,
    val stops: List<MetroStop> = emptyList(),
)

/** A collection of [MetroLine]s that together read as a fragment of a plan. */
data class MetroNetwork(val lines: List<MetroLine>) {
    val isValid: Boolean
        get() = lines.isNotEmpty() && lines.all { it.points.size >= 2 }
}

/**
 * Official Metrovalencia line colours, mirrored from
 * `MetrovalenciaMapData.metroLines` so the artwork and the map agree.
 */
object MetrovalenciaPalette {
    val L1 = Color(0xFFFEC601)
    val L2 = Color(0xFFE60096)
    val L3 = Color(0xFFDD052C)
    val L4 = Color(0xFF014A99)
    val L5 = Color(0xFF008F71)
    val L6 = Color(0xFF8884BF)
    val L7 = Color(0xFFF28D01)
    val L8 = Color(0xFF82CEE6)
    val L9 = Color(0xFFB8804F)
    val L10 = Color(0xFFB7DD79)
}

/**
 * The fragments TransitOS draws. [hero] is a wide, cropped plan for the home
 * departure board; [badge] is a compact, self-contained plan for empty and
 * error states.
 */
object MetrovalenciaNetworks {

    /**
     * Wide fragment (design box 150 x 96). Four lines criss-cross with six
     * interchanges; some ends run off the box so that cropping still reads as a
     * larger map.
     */
    val hero: MetroNetwork = MetroNetwork(
        lines = listOf(
            line(
                id = "L2",
                color = MetrovalenciaPalette.L2,
                points = listOf(
                    Offset(42f, 0f), Offset(42f, 24f), Offset(66f, 48f),
                    Offset(66f, 96f),
                ),
                stops = listOf(
                    stop(42f, 12f),
                    stop(60f, 42f),
                    stop(66f, 72f),
                    stop(66f, 84f),
                ),
            ),
            line(
                id = "L3",
                color = MetrovalenciaPalette.L3,
                points = listOf(
                    Offset(0f, 36f), Offset(54f, 36f), Offset(78f, 12f),
                    Offset(114f, 12f), Offset(138f, 36f), Offset(150f, 36f),
                ),
                stops = listOf(
                    stop(18f, 36f),
                    stop(42f, 36f),
                    stop(66f, 24f),
                    stop(90f, 12f),
                    stop(102f, 12f),
                    stop(120f, 18f),
                    stop(144f, 36f),
                ),
            ),
            line(
                id = "L1",
                color = MetrovalenciaPalette.L1,
                points = listOf(
                    Offset(6f, 96f), Offset(6f, 72f), Offset(30f, 48f),
                    Offset(78f, 48f), Offset(102f, 24f), Offset(150f, 24f),
                ),
                stops = listOf(
                    stop(6f, 92f),
                    stop(16f, 62f),
                    stop(24f, 54f),
                    stop(36f, 48f),
                    stop(72f, 48f),
                    stop(90f, 36f),
                    stop(114f, 24f),
                    stop(138f, 24f),
                ),
            ),
            line(
                id = "L5",
                color = MetrovalenciaPalette.L5,
                points = listOf(
                    Offset(0f, 84f), Offset(36f, 84f), Offset(60f, 60f),
                    Offset(90f, 60f), Offset(114f, 36f), Offset(114f, 12f),
                    Offset(150f, 12f),
                ),
                stops = listOf(
                    stop(24f, 84f),
                    stop(48f, 72f),
                    stop(78f, 60f),
                    stop(102f, 48f),
                    stop(138f, 12f),
                ),
            ),
        ),
    ).withInterchanges(
        listOf(
            Offset(6f, 84f),
            Offset(54f, 36f),
            Offset(66f, 48f),
            Offset(66f, 60f),
            Offset(114f, 12f),
            Offset(126f, 24f),
        ),
    )

    /**
     * Compact fragment (design box 100 x 100). Three lines, two interchanges,
     * fully contained so it can be framed inside a circular badge.
     */
    val badge: MetroNetwork = MetroNetwork(
        lines = listOf(
            line(
                id = "L3",
                color = MetrovalenciaPalette.L3,
                points = listOf(
                    Offset(24f, 4f), Offset(24f, 28f), Offset(48f, 52f),
                    Offset(48f, 96f),
                ),
                stops = listOf(
                    stop(24f, 16f),
                    stop(36f, 40f),
                    stop(48f, 64f),
                    stop(48f, 88f),
                ),
            ),
            line(
                id = "L5",
                color = MetrovalenciaPalette.L5,
                points = listOf(
                    Offset(4f, 92f), Offset(36f, 92f), Offset(60f, 68f),
                    Offset(96f, 68f),
                ),
                stops = listOf(
                    stop(20f, 92f),
                    stop(42f, 86f),
                    stop(78f, 68f),
                    stop(90f, 68f),
                ),
            ),
            line(
                id = "L1",
                color = MetrovalenciaPalette.L1,
                points = listOf(
                    Offset(0f, 64f), Offset(24f, 64f), Offset(44f, 44f),
                    Offset(72f, 44f), Offset(92f, 24f), Offset(100f, 24f),
                ),
                stops = listOf(
                    stop(12f, 64f),
                    stop(34f, 54f),
                    stop(58f, 44f),
                    stop(82f, 34f),
                    stop(96f, 24f),
                ),
            ),
        ),
    ).withInterchanges(
        listOf(
            Offset(42f, 46f),
            Offset(48f, 80f),
        ),
    )

    private fun line(id: String, color: Color, points: List<Offset>, stops: List<MetroStop>) =
        MetroLine(id = id, color = color, points = points, stops = stops)

    private fun stop(x: Float, y: Float) = MetroStop(x, y)
}

/**
 * Injects an [MetroStopKind.INTERCHANGE] into every line that passes through
 * one of [points], marks polyline ends as terminals, and drops any regular stop
 * that would collide with a symbol. Doing it here keeps the geometry definitions
 * readable and makes it impossible for a crossing to be highlighted on one line
 * but not the other.
 */
private fun MetroNetwork.withInterchanges(points: List<Offset>): MetroNetwork {
    val onLineTolerance = 0.75f
    val collisionDistance = 4f

    fun Offset.distanceTo(other: Offset) = hypot(x - other.x, y - other.y)

    return copy(
        lines = lines.map { line ->
            val crossings = points.filter { onPolyline(it, line.points, onLineTolerance) }
            val terminals = listOf(line.points.first(), line.points.last())
            val decor = crossings + terminals
            val keptStops = line.stops.filter { stop ->
                decor.none { symbol ->
                    Offset(stop.x, stop.y).distanceTo(symbol) < collisionDistance
                }
            }
            val crossingStops = crossings.map { MetroStop(it.x, it.y, MetroStopKind.INTERCHANGE) }
            val terminalStops = terminals.map { MetroStop(it.x, it.y, MetroStopKind.TERMINAL) }
            line.copy(stops = keptStops + crossingStops + terminalStops)
        },
    )
}

/** True when [point] lies within [tolerance] of any segment of [points]. */
private fun onPolyline(point: Offset, points: List<Offset>, tolerance: Float): Boolean {
    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end = points[i + 1]
        val segment = end - start
        val lengthSquared = segment.x * segment.x + segment.y * segment.y
        if (lengthSquared <= 0f) continue
        val t = (
            ((point.x - start.x) * segment.x + (point.y - start.y) * segment.y) /
                lengthSquared
            ).coerceIn(0f, 1f)
        val closest = Offset(start.x + segment.x * t, start.y + segment.y * t)
        if (hypot(point.x - closest.x, point.y - closest.y) <= tolerance) return true
    }
    return false
}

/** How the network is fitted into the drawing surface. */
enum class MetroFrame {
    /** Fill the surface, cropping overflow. Good for full-bleed watermarks. */
    COVER,

    /** Fit the whole fragment inside the surface. Good for badges. */
    CONTAIN,
}

/**
 * Visual treatment for a [MetroNetwork]. Colours carry their own alpha; the
 * renderer only multiplies [lineAlpha] over the line colour so the network can
 * be faded as a whole without touching the source data.
 */
data class MetroArtStyle(
    val frame: MetroFrame,
    val strokeScale: Float,
    val lineAlpha: Float,
    val casingColor: Color? = null,
    val casingScale: Float = 0.65f,
    val stationColor: Color,
    val ringColor: Color,
    val terminalColor: Color,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val contentScale: Float = 1f,
    val trainLineId: String? = null,
    val trainColor: Color,
    val pulseInterchanges: Boolean = false,
)

/**
 * Draws [network] into [size], framed by [style] and animated by [trainProgress]
 * (0..1 along the train's line) and [interchangePulse] (0..1 halo growth).
 *
 * The whole transform is computed up front and each line is cached as a [Path],
 * so the per-frame cost is only the moving dot.
 */
private fun DrawScope.drawMetroNetwork(
    geometry: MetroGeometry,
    style: MetroArtStyle,
    trainProgress: Float,
    interchangePulse: Float,
) {
    val unit = geometry.unit
    if (unit <= 0f) return

    val lineWidth = unit
    val casingWidth = unit * (1f + style.casingScale)
    val tickLength = unit * 1.05f
    val tickWidth = unit * 0.42f
    val terminalLength = unit * 2.1f
    val terminalWidth = unit * 0.6f
    val interchangeRadius = unit * 1f
    val pathStyle = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    geometry.lines.forEach { line ->
        if (style.casingColor != null) {
            drawPath(
                path = line.path,
                color = style.casingColor,
                style = Stroke(
                    width = casingWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        drawPath(
            path = line.path,
            color = line.color.copy(alpha = line.color.alpha * style.lineAlpha),
            style = pathStyle,
            blendMode = style.blendMode,
        )
    }

    geometry.lines.forEach { line ->
        line.stops.forEach { stop ->
            when (stop.kind) {
                MetroStopKind.STOP -> drawStationTick(
                    center = stop.position,
                    perpendicular = stop.perpendicular,
                    length = tickLength,
                    width = tickWidth,
                    color = style.stationColor,
                    blendMode = style.blendMode,
                )

                MetroStopKind.TERMINAL -> drawStationTick(
                    center = stop.position,
                    perpendicular = stop.perpendicular,
                    length = terminalLength,
                    width = terminalWidth,
                    color = style.terminalColor,
                    blendMode = style.blendMode,
                )

                MetroStopKind.INTERCHANGE -> {
                    if (style.pulseInterchanges) {
                        drawCircle(
                            color = line.color.copy(alpha = (1f - interchangePulse) * 0.35f),
                            radius = interchangeRadius * (1.35f + interchangePulse * 0.9f),
                            center = stop.position,
                            blendMode = style.blendMode,
                        )
                    }
                    drawCircle(
                        color = style.stationColor,
                        radius = interchangeRadius,
                        center = stop.position,
                        blendMode = style.blendMode,
                    )
                    drawCircle(
                        color = style.ringColor,
                        radius = interchangeRadius,
                        center = stop.position,
                        style = Stroke(width = unit * 0.34f),
                        blendMode = style.blendMode,
                    )
                    drawCircle(
                        color = line.color.copy(alpha = line.color.alpha * style.lineAlpha),
                        radius = interchangeRadius * 0.34f,
                        center = stop.position,
                        blendMode = style.blendMode,
                    )
                }
            }
        }
    }

    val train = style.trainLineId?.let { id -> geometry.lines.firstOrNull { it.line.id == id } }
    if (train != null && train.totalLength > 0f) {
        val (position, tangent) = train.positionAt(trainProgress)
        drawCircle(
            color = style.trainColor.copy(alpha = 0.22f),
            radius = unit * 1.6f,
            center = position,
            blendMode = style.blendMode,
        )
        drawCircle(
            color = style.trainColor,
            radius = unit * 0.62f,
            center = position,
            blendMode = style.blendMode,
        )
        drawCircle(
            color = Color.White,
            radius = unit * 0.24f,
            center = position,
            blendMode = style.blendMode,
        )
        // A faint direction cue so the dot reads as travelling, not static.
        val ahead = position + tangent * (unit * 1.1f)
        drawCircle(
            color = style.trainColor.copy(alpha = 0.35f),
            radius = unit * 0.28f,
            center = ahead,
            blendMode = style.blendMode,
        )
    }
}

private fun DrawScope.drawStationTick(
    center: Offset,
    perpendicular: Offset,
    length: Float,
    width: Float,
    color: Color,
    blendMode: BlendMode,
) {
    val half = perpendicular * (length / 2f)
    drawLine(
        color = color,
        start = center - half,
        end = center + half,
        strokeWidth = width,
        cap = StrokeCap.Round,
        blendMode = blendMode,
    )
}

/**
 * Animated frame for a [MetroNetwork]. Uses [rememberInfiniteTransition] and
 * reads the animated values inside the draw phase, so nothing recomposes and
 * only the canvas redraws.
 */
@Composable
fun MetroNetworkArtwork(
    network: MetroNetwork,
    style: MetroArtStyle,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val animation = rememberMetroAnimation(animate)
    Box(
        modifier = modifier.drawWithCache {
            val geometry = metroGeometry(network, style, size)
            onDrawBehind {
                if (geometry != null) {
                    drawMetroNetwork(
                        geometry = geometry,
                        style = style,
                        trainProgress = animation.trainProgress.value,
                        interchangePulse = animation.interchangePulse.value,
                    )
                }
            }
        },
    )
}

private class MetroAnimation(
    val trainProgress: State<Float>,
    val interchangePulse: State<Float>,
)

@Composable
private fun rememberMetroAnimation(animate: Boolean): MetroAnimation {
    // Static artwork (for example inside an error medallion) should not spin up
    // an infinite transition at all.
    if (!animate) return MetroAnimation(CONSTANT_ZERO, CONSTANT_PULSE)

    // The animated values are wrapped in [State] and only read inside the draw
    // phase, so the canvas redraws without ever recomposing its parents.
    val transition = rememberInfiniteTransition(label = "metro-art")
    val trainProgress: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "metro-train",
    )
    val pulse: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "metro-pulse",
    )
    return MetroAnimation(
        trainProgress = trainProgress,
        interchangePulse = pulse,
    )
}

private val CONSTANT_ZERO: State<Float> = constantState(0f)
private val CONSTANT_PULSE: State<Float> = constantState(0.4f)

private fun constantState(value: Float): State<Float> = object : State<Float> {
    override val value: Float = value
}

private class StopGeometry(
    val position: Offset,
    val perpendicular: Offset,
    val kind: MetroStopKind,
)

private class LineGeometry(
    val line: MetroLine,
    val color: Color,
    val path: Path,
    val points: List<Offset>,
    val cumulative: FloatArray,
    val totalLength: Float,
    val stops: List<StopGeometry>,
) {
    /** Position and direction at [fraction] (0..1) of the line's length. */
    fun positionAt(fraction: Float): Pair<Offset, Offset> {
        val target = fraction.coerceIn(0f, 1f) * totalLength
        var index = 0
        while (index < cumulative.lastIndex && cumulative[index + 1] < target) index++
        val start = points[index]
        val end = points[index + 1]
        val segLength = cumulative[index + 1] - cumulative[index]
        val t = if (segLength <= 0f) 0f else (target - cumulative[index]) / segLength
        val direction = (end - start)
        val length = hypot(direction.x, direction.y)
        val tangent = if (length <= 0f) Offset(1f, 0f) else Offset(direction.x / length, direction.y / length)
        return Offset(
            start.x + direction.x * t,
            start.y + direction.y * t,
        ) to tangent
    }
}

private class MetroGeometry(
    val lines: List<LineGeometry>,
    val unit: Float,
)

private fun metroGeometry(
    network: MetroNetwork,
    style: MetroArtStyle,
    size: Size,
): MetroGeometry? {
    if (!network.isValid || size.minDimension <= 0f) return null

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    network.lines.forEach { line ->
        line.points.forEach { point ->
            minX = min(minX, point.x)
            minY = min(minY, point.y)
            maxX = max(maxX, point.x)
            maxY = max(maxY, point.y)
        }
    }
    val designWidth = maxX - minX
    val designHeight = maxY - minY
    if (designWidth <= 0f || designHeight <= 0f) return null

    val fit = when (style.frame) {
        MetroFrame.COVER -> max(size.width / designWidth, size.height / designHeight)
        MetroFrame.CONTAIN -> min(size.width / designWidth, size.height / designHeight)
    }
    val scale = fit * style.contentScale
    if (scale <= 0f) return null

    val drawnWidth = designWidth * scale
    val drawnHeight = designHeight * scale
    val offsetX = (size.width - drawnWidth) / 2f - minX * scale
    val offsetY = (size.height - drawnHeight) / 2f - minY * scale

    fun map(point: Offset) = Offset(offsetX + point.x * scale, offsetY + point.y * scale)

    val lines = network.lines.map { line ->
        val points = line.points.map(::map)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val cumulative = FloatArray(points.size)
        var total = 0f
        for (i in 1 until points.size) {
            total += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
            cumulative[i] = total
        }
        val stops = line.stops.map { stop ->
            val position = map(Offset(stop.x, stop.y))
            StopGeometry(
                position = position,
                perpendicular = perpendicularAt(points, position),
                kind = stop.kind,
            )
        }
        LineGeometry(
            line = line,
            color = line.color,
            path = path,
            points = points,
            cumulative = cumulative,
            totalLength = total,
            stops = stops,
        )
    }

    return MetroGeometry(
        lines = lines,
        unit = size.minDimension * style.strokeScale,
    )
}

/**
 * Direction perpendicular to the segment nearest to [position]. Stations are
 * drawn as ticks, so they need the local line direction even when placed
 * between vertices.
 */
private fun perpendicularAt(points: List<Offset>, position: Offset): Offset {
    var bestDistance = Float.MAX_VALUE
    var bestDirection = Offset(1f, 0f)
    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end = points[i + 1]
        val segment = end - start
        val lengthSquared = segment.x * segment.x + segment.y * segment.y
        if (lengthSquared <= 0f) continue
        val t = (
            ((position.x - start.x) * segment.x + (position.y - start.y) * segment.y) /
                lengthSquared
            ).coerceIn(0f, 1f)
        val closest = Offset(start.x + segment.x * t, start.y + segment.y * t)
        val distance = hypot(position.x - closest.x, position.y - closest.y)
        if (distance < bestDistance) {
            bestDistance = distance
            val length = sqrt(lengthSquared)
            bestDirection = Offset(segment.x / length, segment.y / length)
        }
    }
    return Offset(-bestDirection.y, bestDirection.x)
}
