package com.pulchradomina.ui.game.opening

import com.pulchradomina.ui.game.night.NightStreetLayout
import com.pulchradomina.ui.game.night.StreetCamera
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningLayoutTest {

    /** Altura da faixa de controles abaixo da cena (dp), aproximada. */
    private val controlsHeightDp = 92f
    private val screenWidthDp = 360f

    private fun portraitWidthDp() = OpeningLayout.PORTRAIT_HEIGHT.value * OpeningLayout.PORTRAIT_ASPECT

    @Test
    fun edgar_wholeBodyFitsBehindThePortrait_onPhoneSizedScenes() {
        for (sceneH in listOf(560f, 620f, 660f, 720f)) {
            val worldX = StreetCamera.worldXOf(OpeningLayout.edgarBehindPortraitPercent(sceneH))
            val view = StreetCamera.view(screenWidthDp, sceneH, worldX)
            val centerDp = StreetCamera.screenX(view, worldX)
            val halfEdgarDp = NightStreetLayout.EDGAR_HEIGHT * NightStreetLayout.EDGAR_ASPECT / 2f * view.scale
            assertTrue("centro fora do busto em $sceneH", kotlin.math.abs(centerDp - OpeningLayout.portraitCenterDp()) < 1f)
            assertTrue("corpo do Edgar sai pela esquerda em $sceneH", centerDp - halfEdgarDp >= 0f)
            assertTrue("corpo do Edgar sai pela direita em $sceneH", centerDp + halfEdgarDp <= portraitWidthDp())
        }
    }

    @Test
    fun edgar_verticallyInsideThePortrait() {
        for (sceneH in listOf(560f, 620f, 660f, 720f)) {
            val scale = sceneH / NightStreetLayout.REF_HEIGHT
            val screenH = sceneH + controlsHeightDp
            val portraitBottom = screenH - OpeningLayout.BOX_BOTTOM_MARGIN.value - OpeningLayout.BOX_HEIGHT.value
            val portraitTop = portraitBottom - OpeningLayout.PORTRAIT_HEIGHT.value
            val edgarTop = (NightStreetLayout.EDGAR_FEET_Y - NightStreetLayout.EDGAR_HEIGHT) * scale
            val edgarFeet = NightStreetLayout.EDGAR_FEET_Y * scale
            assertTrue("pés do Edgar abaixo do busto em $sceneH", edgarFeet <= portraitBottom)
            assertTrue("cabeça do Edgar acima do busto em $sceneH", edgarTop >= portraitTop)
        }
    }

    @Test
    fun slideOffscreen_hidesPortraitAndBoxTogether() {
        val needed = OpeningLayout.BOX_BOTTOM_MARGIN.value + OpeningLayout.BOX_HEIGHT.value + OpeningLayout.PORTRAIT_HEIGHT.value
        assertTrue(OpeningLayout.SLIDE_OFFSCREEN_DP >= needed)
    }

    @Test
    fun box_coversTheControlsRow_andPortraitIsLarge() {
        assertTrue(OpeningLayout.BOX_HEIGHT.value >= 150f)
        assertTrue(OpeningLayout.PORTRAIT_HEIGHT.value >= 400f)
    }
}
