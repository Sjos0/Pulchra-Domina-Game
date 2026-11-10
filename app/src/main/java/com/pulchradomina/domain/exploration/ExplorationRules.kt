package com.pulchradomina.domain.exploration

/**
 * Regras puras da exploração na rua (sem Compose).
 * A tela só aplica estes resultados ao estado visual.
 */
object ExplorationRules {
    const val START_X = 20f
    const val MIN_X = 10f
    const val MAX_X = 90f
    /** Passo por tick (~30ms) enquanto o botão fica pressionado. */
    const val STEP_PER_TICK = 1.4f
    const val MOVE_TICK_MS = 30L
    /** Distância máxima (em % da rua) para considerar hotspot “próximo”. */
    const val HOTSPOT_RADIUS = 8.5f
    /** Limites visuais do personagem no canvas (fração da largura). */
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

    fun canvasFraction(playerXPercent: Float): Float =
        (playerXPercent / 100f).coerceIn(CANVAS_MIN_FRACTION, CANVAS_MAX_FRACTION)
}
