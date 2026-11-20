package com.pulchradomina.domain.exploration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorationRulesTest {

    @Test
    fun stepLeft_clampsAtMin() {
        val (x, facingRight) = ExplorationRules.stepLeft(ExplorationRules.MIN_X)
        assertEquals(ExplorationRules.MIN_X, x, 0.001f)
        assertFalse(facingRight)
    }

    @Test
    fun stepRight_clampsAtMax() {
        val (x, facingRight) = ExplorationRules.stepRight(ExplorationRules.MAX_X)
        assertEquals(ExplorationRules.MAX_X, x, 0.001f)
        assertTrue(facingRight)
    }

    @Test
    fun stepRight_movesOnlyByOneTick() {
        // Cada tick do hold anda um passo pequeno; não existe salto por toque.
        val (x, _) = ExplorationRules.stepRight(50f)
        assertEquals(50f + ExplorationRules.STEP_PER_TICK, x, 0.001f)
    }

    @Test
    fun nearbyHotspot_detectsClubDoor() {
        val club = StreetContent.streetHotspots.first { it.isClubDoor }
        val near = ExplorationRules.nearbyHotspot(club.positionPercent)
        assertNotNull(near)
        assertTrue(near!!.isClubDoor)
    }

    @Test
    fun nearbyHotspot_nullWhenFar() {
        assertNull(ExplorationRules.nearbyHotspot(35f))
    }

    @Test
    fun canvasFraction_clamps() {
        assertEquals(0.15f, ExplorationRules.canvasFraction(5f), 0.001f)
        assertEquals(0.85f, ExplorationRules.canvasFraction(99f), 0.001f)
        assertEquals(0.50f, ExplorationRules.canvasFraction(50f), 0.001f)
    }

    @Test
    fun applyDrag_preservesFacingWhenDeltaZero() {
        val (x, facing) = ExplorationRules.applyDrag(40f, facingRight = false, deltaPercent = 0f)
        assertEquals(40f, x, 0.001f)
        assertFalse(facing)
    }

    @Test
    fun itemHint_appearsWithinTenPercent_andDisappearsBeyond() {
        // valores exatos em ponto flutuante: item em 50%, raio de 3 pontos
        assertTrue(ExplorationRules.isNearItem(playerXPercent = 50f, itemXPercent = 50f))
        assertTrue(ExplorationRules.isNearItem(playerXPercent = 47f, itemXPercent = 50f))
        assertTrue(ExplorationRules.isNearItem(playerXPercent = 53f, itemXPercent = 50f))
        assertFalse(ExplorationRules.isNearItem(playerXPercent = 46f, itemXPercent = 50f))
        assertFalse(ExplorationRules.isNearItem(playerXPercent = 54f, itemXPercent = 50f))
    }

    @Test
    fun itemHintRadius_isThreePercent_sameScaleAsHotspots() {
        assertEquals(3f, ExplorationRules.ITEM_HINT_RADIUS, 0f)
    }
}
