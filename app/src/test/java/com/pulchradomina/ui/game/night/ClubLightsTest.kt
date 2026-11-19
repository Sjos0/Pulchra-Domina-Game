package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubLightsTest {

    @Test
    fun lights_sitAtTheRightEnd_ofTheWorld() {
        assertTrue(ClubLights.LIGHTS.size >= 5)
        for (light in ClubLights.LIGHTS) {
            assertTrue(light.x >= 2600f && light.x <= NightStreetLayout.WORLD_WIDTH)
        }
    }

    @Test
    fun lights_coverTheMarkedArea_fromTheTopOutlineDownToTheGround() {
        val ys = ClubLights.LIGHTS.map { it.y }
        assertTrue(ys.all { it >= ClubLights.TOP_Y && it <= ClubLights.BOTTOM_Y })
        assertTrue((ys.maxOrNull() ?: 0f) - (ys.minOrNull() ?: 0f) > 500f)
        assertTrue(ClubLights.LIGHTS.size >= 12)
    }

    @Test
    fun eachColor_appearsAtLeastThreeTimes() {
        val counts = ClubLights.LIGHTS.groupingBy { it.colorArgb }.eachCount()
        assertTrue(counts.values.all { it >= 3 })
    }

    @Test
    fun lights_useTheFiveColors_andTheyDiffer() {
        assertEquals(5, ClubLights.LIGHTS.map { it.colorArgb }.distinct().size)
    }

    @Test
    fun blink_staysBetweenZeroAndOne_andActuallyPulses() {
        val light = ClubLights.LIGHTS.first()
        var min = 1f
        var max = 0f
        for (i in 0..200) {
            val k = ClubLights.intensity(light, i * 0.02f)
            assertTrue(k in 0f..1f)
            min = minOf(min, k)
            max = maxOf(max, k)
        }
        assertTrue(max - min > 0.5f)
    }
}
