package com.pulchradomina.ui.audio.night

import com.pulchradomina.ui.game.night.FootstepPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NightMixTest {

    @Test
    fun club_isNearlyInaudibleOnTheLeft_andLoudOnTheRight() {
        assertEquals(NightMix.CLUB_LEFT, NightMix.clubVolume(0f), 0.001f)
        assertEquals(NightMix.CLUB_RIGHT, NightMix.clubVolume(1f), 0.001f)
        assertEquals(0.05f, NightMix.clubVolume(0f), 0.001f)
    }

    @Test
    fun club_isMoreMuffledTheFurtherLeftItGoes() {
        assertEquals(1f, NightMix.clubMuffle(0f), 0.001f)
        assertEquals(0f, NightMix.clubMuffle(1f), 0.001f)
        var previous = Float.MAX_VALUE
        for (i in 0..20) {
            val m = NightMix.clubMuffle(i / 20f)
            assertTrue(m <= previous + 1e-6f)
            previous = m
        }
    }

    @Test
    fun footsteps_capAtHalfOnTheLeft_andAlmostSilentOnTheRight() {
        assertEquals(0.5f, NightMix.footstepScale(0f), 0.001f)
        assertEquals(0.01f, NightMix.footstepScale(1f), 0.001f)
        for (i in 0..20) assertTrue(NightMix.footstepScale(i / 20f) <= 0.5f + 1e-6f)
        assertEquals(FootstepPlayer.DEFAULT_VOLUME * 0.5f, NightMix.footstepVolume(0f), 0.001f)
    }

    @Test
    fun ambience_isLivelierOnTheLeft() {
        assertEquals(1f, NightMix.ambienceScale(0f), 0.001f)
        assertEquals(0.3f, NightMix.ambienceScale(1f), 0.001f)
        assertEquals(0.25f, NightMix.CRICKETS_BASE, 0.001f)
        assertEquals(0.12f, NightMix.WIND_BASE, 0.001f)
    }

    @Test
    fun stingers_riseOnTheLeft_andStayQuietOnTheRight() {
        assertEquals(1f, NightMix.stingerGain(0f), 0.001f)
        assertEquals(0.15f, NightMix.stingerGain(1f), 0.001f)
    }

    @Test
    fun smoothstep_isClampedAndSymmetric() {
        assertEquals(0f, NightMix.smoothstep(-3f), 0f)
        assertEquals(1f, NightMix.smoothstep(3f), 0f)
        assertEquals(0.5f, NightMix.smoothstep(0.5f), 0.001f)
    }

    @Test
    fun voiceDuck_cutsTheAmbienceHarder_thanTheClub() {
        // Pack v5: ambiente x0.4 e balada x0.5 durante a fala (o ambiente some mais)
        assertEquals(0.4f, NightMix.VOICE_DUCK_AMBIENCE, 0.001f)
        assertEquals(0.5f, NightMix.VOICE_DUCK_CLUB, 0.001f)
        assertTrue(NightMix.VOICE_DUCK_AMBIENCE < NightMix.VOICE_DUCK_CLUB)
    }
}
