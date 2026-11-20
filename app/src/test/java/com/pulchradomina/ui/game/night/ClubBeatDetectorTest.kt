package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubBeatDetectorTest {

    private val ms = 1_000_000L

    /** Captura a 20 Hz: energia alta a cada [beatEveryMs], base baixa no resto. */
    private fun run(detector: ClubBeatDetector, beatEveryMs: Long, durationMs: Long): Int {
        var beats = 0
        var t = 0L
        while (t < durationMs) {
            val energy = if (t % beatEveryMs < 50) 400f else 30f
            if (detector.onEnergy(t * ms, energy)) beats++
            t += 50
        }
        return beats
    }

    @Test
    fun steadyBeat_detectsOneBeatPerKick() {
        // 120 BPM = uma batida a cada 500 ms, por 10 s = 20 batidas
        val beats = run(ClubBeatDetector(), beatEveryMs = 500, durationMs = 10_000)
        assertTrue("batidas detectadas: $beats", kotlin.math.abs(beats - 20) <= 1)
    }

    @Test
    fun silence_neverBeats() {
        val detector = ClubBeatDetector()
        var beats = 0
        for (i in 0 until 400) if (detector.onEnergy(i * 50L * ms, 3f)) beats++
        assertEquals(0, beats)
    }

    @Test
    fun minInterval_blocksDoubleTriggerOnOneKick() {
        val detector = ClubBeatDetector()
        detector.onEnergy(0L, 10f)
        assertTrue(detector.onEnergy(100 * ms, 400f))
        // mesmo bumbo, 100 ms depois: não pode contar de novo
        assertFalse(detector.onEnergy(150 * ms, 500f))
    }

    @Test
    fun afterPause_noStaleBeat_andDetectionResumes() {
        val detector = ClubBeatDetector()
        run(detector, beatEveryMs = 500, durationMs = 2_000)
        // pausa de 5 s sem captura, depois a música volta com energia baixa e um bumbo
        assertTrue(detector.onEnergy(7_000 * ms, 30f) == false)
        assertTrue(detector.onEnergy(7_100 * ms, 400f))
    }

    @Test
    fun bassEnergy_readsLowBins_notHighBins() {
        // captureSize 1024 bytes, 44100 Hz: bin ~43 Hz. Graves = bins 1..3.
        val low = ByteArray(1024)
        low[2] = 100; low[3] = 0 // bin 1 (~43 Hz)
        val high = ByteArray(1024)
        high[2 * 40] = 100 // bin 40 (~1,7 kHz)
        val eLow = ClubBeatDetector.bassEnergy(low, 44100)
        val eHigh = ClubBeatDetector.bassEnergy(high, 44100)
        assertTrue(eLow > 0f)
        assertEquals(0f, eHigh, 0f)
    }

    @Test
    fun bassEnergy_handlesEmptyOrBadInput() {
        assertEquals(0f, ClubBeatDetector.bassEnergy(ByteArray(0), 44100), 0f)
        assertEquals(0f, ClubBeatDetector.bassEnergy(ByteArray(1024), 0), 0f)
    }
}
