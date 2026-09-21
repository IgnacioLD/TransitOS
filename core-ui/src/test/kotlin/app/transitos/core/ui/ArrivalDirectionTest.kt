package com.glossostudio.transitos.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArrivalDirectionTest {

    private val esTemplate = "Hacia %1\$s"
    private val enTemplate = "Towards %1\$s"
    private val caTemplate = "Cap a %1\$s"

    @Test
    fun `blank destination falls back to the localised label`() {
        val result = directionText(destination = "", blankLabel = "Sin destino", template = esTemplate)
        assertThat(result).isEqualTo("Sin destino")
    }

    @Test
    fun `whitespace only destination falls back too`() {
        val result = directionText(destination = "   ", blankLabel = "Sin destino", template = esTemplate)
        assertThat(result).isEqualTo("Sin destino")
    }

    @Test
    fun `destination is wrapped in the direction template`() {
        val result = directionText(destination = "Betera", blankLabel = "Sin destino", template = esTemplate)
        assertThat(result).isEqualTo("Hacia Betera")
    }

    @Test
    fun `english and valencian templates format too`() {
        assertThat(directionText("Betera", "No destination", enTemplate)).isEqualTo("Towards Betera")
        assertThat(directionText("", "Sense destí", caTemplate)).isEqualTo("Sense destí")
    }
}
