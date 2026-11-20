package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubLightsTest {

    @Test
    fun palette_isTheApprovedOne_magentaAndVioletDominate() {
        val counts = ClubLights.SEQUENCE.toList().groupingBy { it }.eachCount()
        assertEquals(setOf(ClubLights.MAGENTA, ClubLights.VIOLET, ClubLights.CYAN, ClubLights.AMBER), counts.keys)
        assertTrue(counts.getValue(ClubLights.MAGENTA) + counts.getValue(ClubLights.VIOLET) > ClubLights.SEQUENCE.size / 2)
        assertEquals(1, counts.getValue(ClubLights.AMBER))
    }

    @Test
    fun color_changesEveryTwoBeats() {
        assertEquals(0, ClubLights.colorIndexFor(0))
        assertEquals(0, ClubLights.colorIndexFor(1))
        assertEquals(1, ClubLights.colorIndexFor(2))
        assertEquals(1, ClubLights.colorIndexFor(3))
        assertEquals(0, ClubLights.colorIndexFor(-5))
    }

    @Test
    fun pulse_isZeroWithoutRecentBeat() {
        assertEquals(0f, ClubLights.pulseAt(-1f), 0f)
        assertEquals(0f, ClubLights.pulseAt(ClubLights.ACTIVE_WINDOW_S + 0.1f), 0f)
    }

    @Test
    fun pulse_risesFast_thenDecays() {
        val peak = ClubLights.pulseAt(0f)
        val mid = ClubLights.pulseAt(ClubLights.ATTACK_S)
        val late = ClubLights.pulseAt(ClubLights.ATTACK_S + ClubLights.DECAY_S * 2f)
        assertEquals(0f, peak, 0.0001f)
        assertEquals(1f, mid, 0.0001f)
        assertTrue(late < mid && late > 0f)
    }

    @Test
    fun level_staysBetweenBaseAndPeak() {
        assertEquals(ClubLights.BASE_LEVEL, ClubLights.level(0f), 0.0001f)
        assertEquals(ClubLights.BASE_LEVEL + ClubLights.PULSE_LEVEL, ClubLights.level(1f), 0.0001f)
        assertEquals(ClubLights.level(1f), ClubLights.level(5f), 0.0001f)
    }

    @Test
    fun proximity_isZeroInTheMiddle_andOneAtTheRightEdge() {
        val w = NightStreetLayout.WORLD_WIDTH
        assertEquals(0f, ClubLights.proximity(w * 0.2f, w), 0f)
        assertEquals(1f, ClubLights.proximity(w, w), 0.0001f)
        assertTrue(ClubLights.rimAlpha(w * 0.9f, w, 1f) > ClubLights.rimAlpha(w * 0.5f, w, 1f))
    }

    @Test
    fun alphas_areBounded() {
        val w = NightStreetLayout.WORLD_WIDTH
        for (level in listOf(ClubLights.BASE_LEVEL, ClubLights.level(1f))) {
            assertTrue(ClubLights.rimAlpha(w, w, level) in 0f..1f)
            assertTrue(ClubLights.spillAlpha(level) in 0f..1f)
            assertTrue(ClubLights.accentAlpha(level) in 0f..1f)
        }
    }
}
