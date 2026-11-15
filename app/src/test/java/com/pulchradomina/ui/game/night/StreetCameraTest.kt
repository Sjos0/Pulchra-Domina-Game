package com.pulchradomina.ui.game.night

import org.junit.Assert.assertEquals
import org.junit.Test

class StreetCameraTest {

    private val worldWidth = NightStreetLayout.WORLD_WIDTH

    @Test
    fun scale_fitsTheReferenceHeight() {
        val view = StreetCamera.view(400f, NightStreetLayout.REF_HEIGHT, 0f)
        assertEquals(1f, view.scale, 0.0001f)
    }

    @Test
    fun cameraStaysAtLeftEdge_whenPlayerIsNearStart() {
        val view = StreetCamera.view(360f, 800f, 100f)
        assertEquals(0f, view.cameraX, 0.001f)
    }

    @Test
    fun cameraClampsAtRightEdge() {
        val view = StreetCamera.view(360f, 800f, worldWidth)
        assertEquals(worldWidth - view.visibleWidth, view.cameraX, 0.001f)
    }

    @Test
    fun cameraCentersOnPlayer_inTheMiddleOfTheWorld() {
        val playerX = 1500f
        val view = StreetCamera.view(360f, 800f, playerX)
        assertEquals(360f / 2f, StreetCamera.screenX(view, playerX), 0.01f)
    }

    @Test
    fun zeroViewport_isSafe() {
        val view = StreetCamera.view(0f, 0f, 500f)
        assertEquals(0f, view.scale, 0f)
    }

    @Test
    fun worldXOf_convertsPercent() {
        assertEquals(worldWidth / 2f, StreetCamera.worldXOf(50f), 0.001f)
    }
}
