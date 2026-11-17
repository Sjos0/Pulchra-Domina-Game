package com.pulchradomina.ui.game.opening

import com.pulchradomina.domain.exploration.ExplorationRules
import com.pulchradomina.ui.game.night.NightStreetLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Medidas únicas do monólogo. Controlador e overlay leem daqui, para que uma mudança de tamanho
 * não deixe a outra para trás (foi essa a causa do busto que aparecia na entrada).
 */
internal object OpeningLayout {
    /** Caixa de fala: ocupa a faixa dos controles e sobe um pouco acima dela. */
    val BOX_HEIGHT: Dp = 168.dp
    /** Distância da caixa até a base da tela (encostada na área dos controles). */
    val BOX_BOTTOM_MARGIN: Dp = 10.dp
    /** Busto: altura fixa; a base encosta na linha de cima da caixa. */
    val PORTRAIT_HEIGHT: Dp = 400.dp
    /** Proporção largura/altura dos retratos (563 x 1000 px, arredondado). */
    const val PORTRAIT_ASPECT = 0.565f

    /** Deslocamento para deixar caixa + busto totalmente abaixo da borda (com folga de 20 dp). */
    val SLIDE_OFFSCREEN_DP: Float =
        BOX_BOTTOM_MARGIN.value + BOX_HEIGHT.value + PORTRAIT_HEIGHT.value + 20f

    /** Centro horizontal do busto em dp (o busto encosta na borda esquerda). */
    fun portraitCenterDp(): Float = PORTRAIT_HEIGHT.value * PORTRAIT_ASPECT / 2f

    /**
     * Posição do Edgar no mundo (em %) para ele ficar atrás do busto durante o monólogo.
     * Com a câmera no início do mundo, a tela mostra o mundo a partir de x = 0.
     */
    fun edgarBehindPortraitPercent(sceneHeightDp: Float): Float {
        val scale = sceneHeightDp / NightStreetLayout.REF_HEIGHT
        if (scale <= 0f) return ExplorationRules.START_X
        val worldX = portraitCenterDp() / scale
        return (worldX / NightStreetLayout.WORLD_WIDTH * 100f)
            .coerceIn(ExplorationRules.MIN_X, ExplorationRules.MAX_X)
    }
}
