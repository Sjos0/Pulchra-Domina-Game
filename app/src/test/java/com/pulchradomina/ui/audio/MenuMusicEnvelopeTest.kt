package com.pulchradomina.ui.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuMusicEnvelopeTest {
    private val durationMs = 219216L
    private val target = 1.0f
    private val delta = 0.002f

    @Test
    fun positionZero_isSilent() {
        assertEquals(0.0f, menuMusicVolumeAt(0L, durationMs, target), delta)
    }

    @Test
    fun midpointOfFadeIn_isHalfTarget() {
        assertEquals(0.5f, menuMusicVolumeAt(1000L, durationMs, target), delta)
    }

    @Test
    fun endOfFadeIn_isTarget() {
        assertEquals(1.0f, menuMusicVolumeAt(2000L, durationMs, target), delta)
    }

    @Test
    fun sustainRegion_isTarget() {
        assertEquals(1.0f, menuMusicVolumeAt(100000L, durationMs, target), delta)
    }

    @Test
    fun fadeOutStart_isTarget() {
        assertEquals(1.0f, menuMusicVolumeAt(218216L, durationMs, target), delta)
    }

    @Test
    fun midpointOfFadeOut_isHalfTarget() {
        assertEquals(0.5f, menuMusicVolumeAt(218716L, durationMs, target), delta)
    }

    @Test
    fun lastSampleBeforeLoop_isNearSilence() {
        assertEquals(0.0f, menuMusicVolumeAt(219215L, durationMs, target), delta)
    }

    @Test
    fun nonPositiveDuration_isSilent() {
        assertEquals(0.0f, menuMusicVolumeAt(0L, 0L, target), delta)
    }

    @Test
    fun loopBoundary_startAndLastSampleAreBothNearSilence() {
        val atStart = menuMusicVolumeAt(0L, durationMs, target)
        val atLast = menuMusicVolumeAt(durationMs - 1L, durationMs, target)
        assertEquals(0.0f, atStart, delta)
        assertEquals(0.0f, atLast, delta)
    }
}
