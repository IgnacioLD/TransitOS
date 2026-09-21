package com.glossostudio.transitos.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RelativeTimeTest {

    @Test
    fun `just-now for sub-minute deltas`() {
        val now = 1_000_000L
        assertThat(relativeTime(now, nowMs = now)).isEqualTo(RelativeTime.JustNow)
        assertThat(relativeTime(now - 59_000, nowMs = now)).isEqualTo(RelativeTime.JustNow)
    }

    @Test
    fun `minutes bucket between one minute and one hour`() {
        val now = 10_000_000L
        assertThat(relativeTime(now - 60_000, nowMs = now)).isEqualTo(RelativeTime.Minutes(1))
        assertThat(relativeTime(now - 125_000, nowMs = now)).isEqualTo(RelativeTime.Minutes(2))
        assertThat(relativeTime(now - 3_599_000, nowMs = now)).isEqualTo(RelativeTime.Minutes(59))
    }

    @Test
    fun `hours bucket from one hour onwards`() {
        val now = 20_000_000L
        assertThat(relativeTime(now - 3_600_000, nowMs = now)).isEqualTo(RelativeTime.Hours(1))
        assertThat(relativeTime(now - 7_200_000, nowMs = now)).isEqualTo(RelativeTime.Hours(2))
    }

    @Test
    fun `future timestamps clamp to just-now`() {
        val now = 5_000L
        assertThat(relativeTime(now + 30_000, nowMs = now)).isEqualTo(RelativeTime.JustNow)
    }
}
