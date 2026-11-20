package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Test

class ClubBeatsTest {

    @Test
    fun noBeatYet_meansNegativeSeconds_andZeroCount() {
        ClubBeats.reset()
        assertEquals(-1f, ClubBeats.secondsSinceBeat(5_000_000_000L), 0f)
        assertEquals(0, ClubBeats.beatCount())
    }

    @Test
    fun onBeat_countsAndTimesFromTheBeatInstant() {
        ClubBeats.reset()
        ClubBeats.onBeat(1_000_000_000L)
        ClubBeats.onBeat(1_500_000_000L)
        assertEquals(2, ClubBeats.beatCount())
        assertEquals(0.25f, ClubBeats.secondsSinceBeat(1_750_000_000L), 0.0001f)
        ClubBeats.reset()
    }
}
