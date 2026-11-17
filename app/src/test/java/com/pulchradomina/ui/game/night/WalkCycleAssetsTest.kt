package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WalkCycleAssetsTest {

    @Test
    fun walkFrames_existOnDisk() {
        for (i in 0 until WalkCycleLayout.FRAME_COUNT) {
            val f = File("src/main/assets/night_street/walk/walk_%02d.png".format(i))
            assertTrue("falta ${f.path}", f.exists())
        }
    }

    @Test
    fun footstepSound_existsInRaw() {
        assertTrue(File("src/main/res/raw/sfx_footstep.mp3").exists())
    }

    @Test
    fun contacts_areInRange_sortedAndTwoPerCycle() {
        val c = WalkCycleLayout.CONTACT_FRAMES
        assertEquals(2, c.size)
        assertTrue(c.all { it in 0 until WalkCycleLayout.FRAME_COUNT })
        assertTrue(c[0] < c[1])
    }

    @Test
    fun walkScale_matchesTheIdleScale() {
        // figura do walk ~ mesma altura que o idle (330 unidades): sem salto de tamanho ao trocar
        assertTrue(kotlin.math.abs(WalkCycleLayout.FRAME_HEIGHT_U - NightStreetLayout.EDGAR_HEIGHT) < 2f)
    }

    @Test
    fun cycle_isTwoStepsOfAboutTheSameLength() {
        val step = WalkCycleLayout.CYCLE_UNITS / 2f
        assertTrue("ciclo ${WalkCycleLayout.CYCLE_UNITS}", step in 130f..150f)
    }
}
