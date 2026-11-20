package com.pulchradomina.ui.game.night

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.max

/**
 * Aviso de toque acima de um item do chão: círculo branco vazado com uma mão branca dentro, pequeno,
 * logo acima do item (sem cobrir a arte, que continua com o contorno pulsante).
 *
 * Entrada: fade in que sobe de baixo para cima. Saída: fade out rápido, sem movimento.
 * A animação (progress 0..1) é feita em NightStreetScene com Animatable.
 */
internal object ItemHint {
    /** Duração da entrada e da saída, em ms. */
    internal const val ANIM_MS = 300
    /** Raio do círculo, em relação à altura do item (tamanho normal de ícone de toque). */
    internal const val RADIUS_FACTOR = 0.36f
    /** Distância entre o topo do item e a base do círculo, em relação à altura do item (fica acima do halo). */
    internal const val GAP_FACTOR = 0.20f
    /** Quanto o aviso se desloca na entrada (de baixo para cima) e na saída (de cima para baixo). */
    internal const val RISE_FACTOR = 0.10f
    /** Lado da mão dentro do círculo, em relação ao diâmetro. */
    internal const val GLYPH_FACTOR = 0.6f

    /** Saída = entrada ao contrário: a curva de saída é a da entrada espelhada no tempo. */
    internal val REVERSED_EASING: Easing = Easing { t -> 1f - FastOutSlowInEasing.transform(1f - t) }

    /** Path da mão vindo do SVG (tools/night_street/sources/hand_hint.svg), criado uma vez. */
    internal val handPath: Path by lazy {
        PathParser().parsePathString(NightStreetLayout.ITEM_HINT_HAND_PATH).toPath()
    }
}

/**
 * [progress] 0..1 (animado). Só desenha se houver progresso.
 */
internal fun DrawScope.drawItemHint(item: Prop, cameraX: Float, unitPx: Float, progress: Float) {
    if (progress <= 0f) return
    val rUnits = item.h * ItemHint.RADIUS_FACTOR
    val rise = (1f - progress) * item.h * ItemHint.RISE_FACTOR
    val cxUnits = item.x
    val cyUnits = item.top - item.h * ItemHint.GAP_FACTOR - rUnits + rise

    val cx = (cxUnits - cameraX) * unitPx
    val cy = cyUnits * unitPx
    val r = rUnits * unitPx
    val white = Color.White.copy(alpha = progress)

    drawCircle(
        color = white,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = max(1f, r * 0.12f))
    )
    val side = 2f * r * ItemHint.GLYPH_FACTOR
    withTransform({
        translate(cx - side / 2f, cy - side / 2f)
        scale(side / 24f, side / 24f, pivot = Offset.Zero)
    }) {
        drawPath(ItemHint.handPath, color = white)
    }
}
