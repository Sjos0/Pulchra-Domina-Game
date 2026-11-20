package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubBeatsTest {

    private val s = 1_000_000_000L

    @Test
    fun clockIsOff_untilTheMusicRuns() {
        ClubBeats.reset()
        assertFalse(ClubBeats.isRunning())
        assertEquals(-1f, ClubBeats.secondsSinceBeat(5 * s), 0f)
        assertEquals(0, ClubBeats.beatCount(5 * s))
    }

    @Test
    fun at120Bpm_aBeatEveryHalfSecond_andColorChangesEveryTwoBeats() {
        ClubBeats.reset()
        ClubBeats.setRunning(true, 0L)
        assertEquals(0.5f, ClubBeats.BEAT_S, 0.0001f)
        assertEquals(0, ClubBeats.beatCount(0L))
        assertEquals(1, ClubBeats.beatCount(s / 2))
        assertEquals(4, ClubBeats.beatCount(2 * s))
        assertEquals(0.25f, ClubBeats.secondsSinceBeat(s / 4), 0.0001f)
        ClubBeats.reset()
    }

    @Test
    fun pause_freezesTheBeat_andResumeContinuesFromThere() {
        ClubBeats.reset()
        ClubBeats.setRunning(true, 0L)
        ClubBeats.setRunning(false, s)          // tocou 1 s, pausou
        // pausado por 10 s: nada anda
        assertEquals(2, ClubBeats.beatCount(11 * s))
        assertTrue(ClubBeats.secondsSinceBeat(11 * s) < 0f)
        ClubBeats.setRunning(true, 11 * s)      // volta
        assertEquals(2, ClubBeats.beatCount(11 * s))
        assertEquals(3, ClubBeats.beatCount(11 * s + s / 2))
        ClubBeats.reset()
    }

    @Test
    fun repeatedSameState_doesNothing() {
        ClubBeats.reset()
        ClubBeats.setRunning(true, 0L)
        ClubBeats.setRunning(true, s)
        assertEquals(2, ClubBeats.beatCount(s))
        ClubBeats.reset()
    }
}
