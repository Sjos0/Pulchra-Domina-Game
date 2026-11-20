package com.pulchradomina.domain.exploration

/**
 * Regras puras da exploração na rua (sem Compose).
 * Posições em % do mundo (o mundo tem 3 telas de largura).
 * A tela só aplica estes resultados ao estado visual.
 */
object ExplorationRules {
    const val START_X = 14f
    const val MIN_X = 2f
    const val MAX_X = 97.5f
    /**
     * Passo por tick de MOVE_TICK_MS (≈ 235 u/s). Walk e som derivam da distância andada:
     * mudar aqui desacelera a animação e os passos juntos, sem o pé deslizar.
     */
    const val STEP_PER_TICK = 0.25f
    /** Arrasto em px → % do mundo. */
    const val DRAG_PERCENT_PER_PX = 0.08f
    const val MOVE_TICK_MS = 30L
    /** Distância máxima (em % do mundo) para considerar hotspot “próximo”. */
    const val HOTSPOT_RADIUS = 4f
    /**
     * Raio (pontos percentuais do mundo, mesma escala de HOTSPOT_RADIUS) em que o aviso de toque
     * aparece acima de um item do chão. Aviso só visual: não dispara ação. Era 10 (sensível demais, v1.0.31).
     */
    const val ITEM_HINT_RADIUS = 3f
    /** Limites do personagem na tela (fração da largura), para o balão de hotspot. */
    const val CANVAS_MIN_FRACTION = 0.15f
    const val CANVAS_MAX_FRACTION = 0.85f

    fun stepLeft(x: Float): Pair<Float, Boolean> {
        val next = (x - STEP_PER_TICK).coerceIn(MIN_X, MAX_X)
        return next to false // facingRight = false
    }

    fun stepRight(x: Float): Pair<Float, Boolean> {
        val next = (x + STEP_PER_TICK).coerceIn(MIN_X, MAX_X)
        return next to true
    }

    fun applyDrag(x: Float, facingRight: Boolean, deltaPercent: Float): Pair<Float, Boolean> {
        val next = (x + deltaPercent).coerceIn(MIN_X, MAX_X)
        val facing = when {
            deltaPercent > 0f -> true
            deltaPercent < 0f -> false
            else -> facingRight
        }
        return next to facing
    }

    fun nearbyHotspot(
        playerXPercent: Float,
        hotspots: List<StreetHotspot> = StreetContent.streetHotspots
    ): StreetHotspot? {
        return hotspots.firstOrNull { spot ->
            kotlin.math.abs(spot.positionPercent - playerXPercent) < HOTSPOT_RADIUS
        }
    }

    /** Aviso do item aparece a até [ITEM_HINT_RADIUS] pontos percentuais dele (inclusive). */
    fun isNearItem(playerXPercent: Float, itemXPercent: Float): Boolean =
        kotlin.math.abs(itemXPercent - playerXPercent) <= ITEM_HINT_RADIUS

    /** Fração da largura (limitada) onde o balão de hotspot fica, a partir da posição na tela (0..100). */
    fun canvasFraction(screenXPercent: Float): Float =
        (screenXPercent / 100f).coerceIn(CANVAS_MIN_FRACTION, CANVAS_MAX_FRACTION)
}
