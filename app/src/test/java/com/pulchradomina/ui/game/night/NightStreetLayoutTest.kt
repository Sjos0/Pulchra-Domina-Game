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
    fun edgar_isLargerThanTheFirstVersion_andStandsOnTheSidewalk() {
        // v1 = 260 unidades (marca vermelha da referência). v2 = 330: personagem em destaque.
        assertTrue(NightStreetLayout.EDGAR_HEIGHT >= 330f)
        assertTrue(NightStreetLayout.EDGAR_FEET_Y > NightStreetLayout.GROUND_TOP)
        assertTrue(NightStreetLayout.EDGAR_FEET_Y < NightStreetLayout.ROAD_TOP)
    }

    @Test
    fun sidewalk_isTallerThanTheFirstVersion_andRoadStartsBelowIt() {
        assertTrue(NightStreetLayout.SIDEWALK_HEIGHT >= 120f)
        assertEquals(
            NightStreetLayout.GROUND_TOP + NightStreetLayout.SIDEWALK_HEIGHT,
            NightStreetLayout.ROAD_TOP,
            0.001f
        )
    }

    @Test
    fun lampXPositions_areUnchanged() {
        // Pedido do usuário: não mudar a posição X dos postes (valores da v1).
        val xs = NightStreetLayout.lamps.map { it.x }
        assertEquals(listOf(266f, 623f, 1207f, 1564f, 2148f, 2505f), xs)
    }

    @Test
    fun everyProp_isInsideTheWorld() {
        NightStreetLayout.props.forEach { prop ->
            assertTrue("x fora do mundo: ${prop.sprite}", prop.x in 0f..worldWidth)
        }
    }

    @Test
    fun lampCones_startAtTheLampAndWidenToTheGround() {
        NightStreetLayout.lamps.forEach { lamp ->
            assertTrue(lamp.x in 0f..worldWidth)
            // ápice logo abaixo da cabeça do poste, base no chão do Edgar
            assertTrue(lamp.apexY > lamp.baseY - lamp.h)
            assertTrue(lamp.apexY < lamp.groundY)
            assertEquals(NightStreetLayout.EDGAR_FEET_Y, lamp.groundY, 0.001f)
            // cone estreito no topo e largo no chão
            assertTrue(lamp.topHalf < lamp.baseHalf)
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
    fun clubHotspot_isAtTheFarEndOfTheWorld() {
        val club = StreetContent.streetHotspots.single { it.isClubDoor }
        assertTrue(club.positionPercent > 90f)
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
