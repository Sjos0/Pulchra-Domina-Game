package com.pulchradomina.ui.game.night

/**
 * Câmera lateral do cenário noturno (sem Compose, testável).
 *
 * A escala é uniforme: a altura da cena corresponde à altura da referência (1672 unidades),
 * então o Edgar sempre ocupa a mesma fração da tela, em qualquer aparelho.
 * A câmera segue o Edgar e para nas bordas do mundo.
 */
internal object StreetCamera {

    data class View(
        /** Pixels (ou dp) por unidade de referência. */
        val scale: Float,
        /** Borda esquerda visível, em unidades de mundo. */
        val cameraX: Float,
        /** Largura visível, em unidades de mundo. */
        val visibleWidth: Float
    )

    fun view(viewportWidth: Float, viewportHeight: Float, playerWorldX: Float): View {
        val worldWidth = NightStreetLayout.WORLD_WIDTH
        if (viewportHeight <= 0f || viewportWidth <= 0f) return View(0f, 0f, worldWidth)
        val scale = viewportHeight / NightStreetLayout.REF_HEIGHT
        val visible = (viewportWidth / scale).coerceAtMost(worldWidth)
        val cameraX = (playerWorldX - visible / 2f).coerceIn(0f, worldWidth - visible)
        return View(scale, cameraX, visible)
    }

    /**
     * Mesma escala e largura de [view], com a borda esquerda fixa em [cameraX] (intro cinematográfica).
     * A borda é limitada às bordas do mundo.
     */
    fun viewAt(viewportWidth: Float, viewportHeight: Float, cameraX: Float): View {
        val base = view(viewportWidth, viewportHeight, 0f)
        return base.copy(cameraX = cameraX.coerceIn(0f, NightStreetLayout.WORLD_WIDTH - base.visibleWidth))
    }

    /** Posição de tela (mesma unidade de [View.scale]) de um ponto do mundo. */
    fun screenX(view: View, worldX: Float): Float = (worldX - view.cameraX) * view.scale

    /** Converte a posição do jogador (% do mundo) em unidades de mundo. */
    fun worldXOf(percent: Float): Float = percent / 100f * NightStreetLayout.WORLD_WIDTH
}
