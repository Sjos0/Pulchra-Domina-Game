package com.pulchradomina.ui.game.night

import com.pulchradomina.domain.exploration.ExplorationRules
import com.pulchradomina.domain.exploration.StreetContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NightStreetLayoutTest {

    private val worldWidth = NightStreetLayout.WORLD_WIDTH
    private val assetsDir = File("src/main/assets/night_street")

    @Test
    fun world_isThreeReferenceScreensWide() {
        assertEquals(3f * NightStreetLayout.REF_WIDTH, worldWidth, 0.001f)
    }

    @Test
    fun edgar_heightIsTheRedMarkOfTheReference() {
        // Marca vermelha medida na referência 941x1672: ~260 unidades (≈15,5% da altura)
        val fraction = NightStreetLayout.EDGAR_HEIGHT / NightStreetLayout.REF_HEIGHT
        assertEquals(0.155f, fraction, 0.002f)
    }

    @Test
    fun everyProp_isInsideTheWorld() {
        NightStreetLayout.props.forEach { prop ->
            assertTrue("x fora do mundo: ${prop.sprite}", prop.x in 0f..worldWidth)
        }
    }

    @Test
    fun lamps_lightStaysInsideTheWorldAndIsLimited() {
        NightStreetLayout.lamps.forEach { lamp ->
            assertTrue(lamp.lightX in 0f..worldWidth)
            // luz limitada: menor que metade de uma tela de referência
            assertTrue(lamp.lightRx < NightStreetLayout.REF_WIDTH / 2f)
            assertTrue(lamp.lightRy > 0f)
        }
    }

    @Test
    fun start_isBetweenTheFirstTwoLamps() {
        val startUnits = ExplorationRules.START_X / 100f * worldWidth
        val firstTwo = NightStreetLayout.lamps.take(2).sortedBy { it.x }
        assertTrue(startUnits > firstTwo[0].x && startUnits < firstTwo[1].x)
    }

    @Test
    fun hotspots_areInsideTheWorldAndNotAtStart() {
        StreetContent.streetHotspots.forEach { spot ->
            assertTrue(spot.positionPercent in 0f..100f)
        }
        assertNull(ExplorationRules.nearbyHotspot(ExplorationRules.START_X))
    }

    @Test
    fun clubDoor_isAtTheFarEndOfTheWorld() {
        val club = NightStreetLayout.doors.single { it.neon }
        assertTrue(club.x > worldWidth * 0.9f)
    }

    @Test
    fun everySprite_andEdgarFrame_existsOnDisk() {
        NightSprite.entries.forEach { sprite ->
            assertTrue("falta asset ${sprite.file}", File(assetsDir, sprite.file).exists())
        }
        for (i in 0 until NightStreetLayout.EDGAR_FRAME_COUNT) {
            val name = "edgar/idle_%02d.png".format(i)
            assertTrue("falta frame $name", File(assetsDir, name).exists())
        }
    }
}
