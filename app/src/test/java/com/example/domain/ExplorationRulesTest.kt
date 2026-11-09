package com.example.domain

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
}
