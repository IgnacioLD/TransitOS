package com.glossostudio.transitos.core.ui

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Guards the authored metro fragments: the whole look depends on lines turning
 * only in 45 and 90 degree steps and on every station actually sitting on a
 * line. A typo in a coordinate would otherwise ship as a visibly broken plan.
 */
class MetroNetworkGeometryTest {

    private val networks = mapOf(
        "hero" to MetrovalenciaNetworks.hero,
        "badge" to MetrovalenciaNetworks.badge,
    )

    @Test
    fun `every network is valid`() {
        networks.forEach { (_, network) ->
            assertThat(network.isValid).isTrue()
            assertThat(network.lines).isNotEmpty()
        }
    }

    @Test
    fun `segments only turn in 45 and 90 degree steps`() {
        networks.forEach { (name, network) ->
            network.lines.forEach { line ->
                line.points.zipWithNext().forEach { (a, b) ->
                    val dx = abs(b.x - a.x)
                    val dy = abs(b.y - a.y)
                    val axisAligned = dx < EPSILON || dy < EPSILON
                    val diagonal = abs(dx - dy) < EPSILON
                    assertWithMessage("$name / ${line.id} segment ($a -> $b)")
                        .that(axisAligned || diagonal)
                        .isTrue()
                }
            }
        }
    }

    @Test
    fun `every station lies on its line`() {
        networks.forEach { (name, network) ->
            network.lines.forEach { line ->
                line.stops.forEach { stop ->
                    assertWithMessage("$name / ${line.id} station (${stop.x}, ${stop.y})")
                        .that(distanceToPolyline(stop, line.points) < 1f)
                        .isTrue()
                }
            }
        }
    }

    @Test
    fun `every line has a terminus and the network has an interchange`() {
        networks.forEach { (name, network) ->
            network.lines.forEach { line ->
                assertWithMessage("$name / ${line.id} terminus")
                    .that(line.stops.any { it.kind == MetroStopKind.TERMINAL })
                    .isTrue()
            }
            assertWithMessage("$name interchange")
                .that(network.lines.any { line -> line.stops.any { it.kind == MetroStopKind.INTERCHANGE } })
                .isTrue()
        }
    }

    @Test
    fun `hero lines use distinct official palette colours`() {
        val colors = MetrovalenciaNetworks.hero.lines.map { it.color }
        assertThat(colors.toSet()).hasSize(colors.size)
    }

    private fun distanceToPolyline(stop: MetroStop, points: List<Offset>): Float {
        var best = Float.MAX_VALUE
        for (i in 0 until points.lastIndex) {
            val start = points[i]
            val end = points[i + 1]
            val segment = end - start
            val lengthSquared = segment.x * segment.x + segment.y * segment.y
            if (lengthSquared <= 0f) continue
            val t = (
                ((stop.x - start.x) * segment.x + (stop.y - start.y) * segment.y) / lengthSquared
                ).coerceIn(0f, 1f)
            val closestX = start.x + segment.x * t
            val closestY = start.y + segment.y * t
            best = minOf(best, hypot(stop.x - closestX, stop.y - closestY))
        }
        return best
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
