package com.pulchradomina.ui.game.night

import com.pulchradomina.domain.exploration.ExplorationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightIntroTimelineTest {

    private val visible = 900f
    private val endCamera = NightStreetLayout.WORLD_WIDTH - visible
    private val t = NightIntroTimeline

    @Test
    fun beforeTheIntro_cameraIsAtTheEnd_andEdgarIsHidden() {
        val f = t.frame(-1L, visible)
        assertEquals(endCamera, f.cameraX, 0.001f)
        assertFalse(f.edgarVisible)
        assertEquals(0f, f.barFraction, 0f)
        assertFalse(f.done)
    }

    @Test
    fun cameraHoldsAtTheEnd_untilThePanStarts() {
        assertEquals(endCamera, t.frame(t.HOLD_END_MS - 1, visible).cameraX, 0.001f)
        assertEquals(endCamera, t.frame(t.HOLD_END_MS, visible).cameraX, 0.001f)
    }

    @Test
    fun cameraPansMonotonically_fromTheEndToTheStart() {
        var previous = Float.MAX_VALUE
        var ms = t.PAN_START_MS
        while (ms <= t.PAN_END_MS) {
            val x = t.frame(ms, visible).cameraX
            assertTrue("câmera voltou em $ms ms", x <= previous + 0.0001f)
            previous = x
            ms += 250
        }
        assertEquals(0f, t.frame(t.PAN_END_MS, visible).cameraX, 0.01f)
    }

    @Test
    fun pan_isSlowerThanWalking() {
        val averageUnitsPerSec = endCamera / (t.PAN_MS / 1000f)
        assertTrue(averageUnitsPerSec < t.WALK_UNITS_PER_SEC)
    }

    @Test
    fun letterbox_closesDuringThePan_andOpensAtTheArrival() {
        assertEquals(0f, t.frame(t.PAN_START_MS, visible).barFraction, 0.001f)
        assertEquals(t.BARS_FRACTION, t.frame(t.PAN_START_MS + t.BARS_CLOSE_MS, visible).barFraction, 0.001f)
        assertEquals(t.BARS_FRACTION, t.frame(t.PAN_END_MS, visible).barFraction, 0.001f)
        assertEquals(0f, t.frame(t.PAN_END_MS + t.BARS_OPEN_MS, visible).barFraction, 0.001f)
    }

    @Test
    fun card_fadesInWithTheVeil_thenSubtitle_andAllFadeOutAtTheArrival() {
        assertEquals(1f, t.frame(t.TITLE_FADE_START_MS + t.TITLE_FADE_MS, visible).titleAlpha, 0.001f)
        assertEquals(0f, t.frame(t.TITLE_FADE_START_MS - 1, visible).titleAlpha, 0f)
        assertEquals(0f, t.frame(t.SUBTITLE_FADE_START_MS - 1, visible).subtitleAlpha, 0f)
        assertEquals(1f, t.frame(t.SUBTITLE_FADE_START_MS + t.SUBTITLE_FADE_MS, visible).subtitleAlpha, 0.001f)
        assertEquals(0f, t.frame(t.PAN_END_MS + t.BARS_OPEN_MS, visible).titleAlpha, 0.001f)
        assertEquals(0f, t.frame(t.PAN_END_MS + t.BARS_OPEN_MS, visible).subtitleAlpha, 0.001f)
        assertTrue(t.frame(t.PAN_START_MS + 500, visible).veilAlpha > 0f)
    }

    @Test
    fun edgar_isHiddenDuringThePan_andStartsWalkingFromTheLeftEdge() {
        assertFalse(t.frame(t.PAN_END_MS - 1, visible).edgarVisible)
        val arrival = t.frame(t.PAN_END_MS, visible)
        assertTrue(arrival.edgarVisible)
        assertTrue(arrival.edgarWalking)
        assertEquals(NightIntroTimeline.EDGAR_SPAWN_X, arrival.edgarWorldX, 0.001f)
        assertTrue("spawn precisa estar fora da tela", arrival.edgarWorldX < 0f)
    }

    @Test
    fun edgar_walksToTheIdlePoint_atThePlayersWalkSpeed() {
        val start = StreetCamera.worldXOf(ExplorationRules.START_X)
        val atStop = t.frame(t.WALK_END_MS, visible)
        assertEquals(start, atStop.edgarWorldX, 0.01f)
        assertFalse(atStop.edgarWalking)

        val seconds = t.WALK_IN_MS / 1000f
        val speed = (start - NightIntroTimeline.EDGAR_SPAWN_X) / seconds
        assertEquals(t.WALK_UNITS_PER_SEC, speed, t.WALK_UNITS_PER_SEC * 0.01f)
    }

    @Test
    fun intro_isDoneOnlyAfterTheIdlePause_soTheMonologueComesAfterTheIdle() {
        assertFalse(t.frame(t.WALK_END_MS, visible).done)
        assertFalse(t.frame(t.DONE_MS - 1, visible).done)
        assertTrue(t.frame(t.DONE_MS, visible).done)
    }

    @Test
    fun footstepVolume_staysLow_andRisesAsEdgarApproaches() {
        val early = t.frame(t.FOOTSTEP_START_MS, visible).footstepVolume
        val late = t.frame(t.PAN_END_MS - 1, visible).footstepVolume
        assertEquals(t.FOOTSTEP_MIN_VOLUME, early, 0.001f)
        assertEquals(t.FOOTSTEP_MAX_VOLUME, late, 0.01f)
        assertTrue(t.FOOTSTEP_MAX_VOLUME < 0.5f)
    }
}
