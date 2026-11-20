package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubBeatDetectorTest {

    private val ms = 1_000_000L

    /** Captura a 20 Hz: energia alta a cada [beatEveryMs] (bumbo), base baixa no resto, tudo escalado por [gain]. */
    private fun run(detector: ClubBeatDetector, beatEveryMs: Long, durationMs: Long, gain: Float = 1f): Int {
        var beats = 0
        var t = 0L
        while (t < durationMs) {
            val energy = (if (t % beatEveryMs < 50) 400f else 30f) * gain
            if (detector.onEnergy(t * ms, energy)) beats++
            t += 50
        }
        return beats
    }

    @Test
    fun steadyBeat_detectsOneBeatPerKick() {
        // 120 BPM = uma batida a cada 500 ms, por 10 s = 20 batidas (a primeira captura só arma o detector)
        val beats = run(ClubBeatDetector(), beatEveryMs = 500, durationMs = 10_000)
        assertTrue("batidas detectadas: $beats", kotlin.math.abs(beats - 20) <= 1)
    }

    @Test
    fun quietPlayback_stillDetectsBeats_likeTheStreetStart() {
        // volume 5% da balada: energia 400x menor, mesmo ritmo. Antes, um piso absoluto zerava isso.
        val beats = run(ClubBeatDetector(), beatEveryMs = 500, durationMs = 10_000, gain = 0.0025f)
        assertTrue("batidas com volume baixo: $beats", kotlin.math.abs(beats - 20) <= 1)
    }

    @Test
    fun silence_neverBeats() {
        val detector = ClubBeatDetector()
        var beats = 0
        for (i in 0 until 400) if (detector.onEnergy(i * 50L * ms, 0f)) beats++
        assertEquals(0, beats)
    }

    @Test
    fun steadyNoise_withoutAKick_doesNotBeat() {
        // ruído constante (sem bumbo): nada de pico sobre a média
        val detector = ClubBeatDetector()
        var beats = 0
        for (i in 0 until 400) {
            val wobble = if (i % 2 == 0) 5.0f else 5.2f
            if (detector.onEnergy(i * 50L * ms, wobble)) beats++
        }
        assertEquals(0, beats)
    }

    @Test
    fun minInterval_blocksDoubleTriggerOnOneKick() {
        val detector = ClubBeatDetector()
        detector.onEnergy(0L, 10f)
        assertTrue(detector.onEnergy(100 * ms, 400f))
        // mesmo bumbo, 50 ms depois: não pode contar de novo
        assertFalse(detector.onEnergy(150 * ms, 500f))
    }

    @Test
    fun afterPause_noStaleBeat_andDetectionResumes() {
        val detector = ClubBeatDetector()
        run(detector, beatEveryMs = 500, durationMs = 2_000)
        // pausa de 5 s sem captura; depois a música volta com energia baixa e um bumbo
        assertFalse(detector.onEnergy(7_000 * ms, 30f))
        assertTrue(detector.onEnergy(7_100 * ms, 400f))
    }

    @Test
    fun bassEnergy_readsLowBins_notHighBins() {
        // captureSize 1024 bytes, 44100 Hz: bin ~43 Hz. Graves = bins 1..3.
        val low = ByteArray(1024)
        low[2] = 100
        val high = ByteArray(1024)
        high[2 * 40] = 100 // bin 40 (~1,7 kHz)
        assertTrue(ClubBeatDetector.bassEnergy(low, 44100) > 0f)
        assertEquals(0f, ClubBeatDetector.bassEnergy(high, 44100), 0f)
    }

    @Test
    fun bassEnergy_handlesEmptyOrBadInput() {
        assertEquals(0f, ClubBeatDetector.bassEnergy(ByteArray(0), 44100), 0f)
        assertEquals(0f, ClubBeatDetector.bassEnergy(ByteArray(1024), 0), 0f)
    }
}
