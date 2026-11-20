package com.pulchradomina.ui.game.night

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Aviso de item no chão: círculo branco vazado com uma mão branca dentro, sobre o item.
 * Aparece quando o Edgar chega a até ITEM_HINT_RADIUS do item e some quando ele se afasta
 * (a opacidade anima em NightStreetScene, para não piscar).
 *
 * O círculo é vazado de propósito: o item continua visível por baixo, e o aviso não compete com a arte.
 */
internal object ItemHint {
    internal const val RADIUS_FACTOR = 0.9f   // raio do círculo em relação à altura do item
    internal const val GLYPH_FACTOR = 1.1f    // lado da mão em relação ao raio
    internal const val BOB_UNITS = 1.0f       // flutuação suave, em unidades de referência

    /** Path da mão vindo do SVG (tools/night_street/sources/hand_hint.svg), criado uma vez. */
    internal val handPath: Path by lazy {
        PathParser().parsePathString(NightStreetLayout.ITEM_HINT_HAND_PATH).toPath()
    }
}

/**
 * [alpha] 0..1 (animado). [seconds] dá a flutuação. Só desenha se houver alpha.
 */
internal fun DrawScope.drawItemHint(
    item: Prop,
    cameraX: Float,
    unitPx: Float,
    alpha: Float,
    seconds: Float
) {
    if (alpha <= 0f) return
    val grow = 0.7f + 0.3f * alpha                      // entra crescendo, do menor para o tamanho cheio
    val bob = -ItemHint.BOB_UNITS * alpha * sin(2f * PI.toFloat() * seconds / 1.6f)
    val cx = (item.x - cameraX) * unitPx
    val cy = (item.top + item.h / 2f + bob) * unitPx
    val r = item.h * ItemHint.RADIUS_FACTOR * unitPx * grow
    val white = Color.White.copy(alpha = alpha)

    drawCircle(
        color = white,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = max(2f, r * 0.08f))
    )
    val side = r * ItemHint.GLYPH_FACTOR
    withTransform({
        translate(cx - side / 2f, cy - side / 2f)
        scale(side / 24f, side / 24f, pivot = Offset.Zero)
    }) {
        drawPath(ItemHint.handPath, color = white)
    }
}
