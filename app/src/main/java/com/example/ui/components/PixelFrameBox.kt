package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Caixa pixel com moldura de canos e madeira no interior.
 *
 * - A altura segue o conteúdo (não ocupa a tela inteira).
 * - A madeira é recortada em cover, centralizada: nunca achatada.
 * - [woodRes] escolhe a madeira (ex.: box_wood ou box_wood_flower).
 * - [tint] escurece só o interior (ex.: cards bloqueados); a moldura fica intacta.
 */
@Composable
fun PixelFrameBox(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
    woodRes: Int = R.drawable.box_wood,
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val wood = ImageBitmap.imageResource(id = woodRes)
    val cornerTl = ImageBitmap.imageResource(id = R.drawable.box_corner_tl)
    val cornerTr = ImageBitmap.imageResource(id = R.drawable.box_corner_tr)
    val cornerBl = ImageBitmap.imageResource(id = R.drawable.box_corner_bl)
    val cornerBr = ImageBitmap.imageResource(id = R.drawable.box_corner_br)
    val edgeH = ImageBitmap.imageResource(id = R.drawable.box_edge_h)
    val edgeV = ImageBitmap.imageResource(id = R.drawable.box_edge_v)

    Box(
        modifier = modifier.drawWithContent {
            val s = size.width / DESIGN_WIDTH
            val th = (TOP_THICKNESS * s).roundToInt()
            val sd = (SIDE_THICKNESS * s).roundToInt()
            val cw = (CORNER_W * s).roundToInt()
            val ch = (CORNER_H * s).roundToInt()
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()

            // 1) madeira só no interior da moldura
            drawWoodCover(wood, sd, th, w - sd, h - th)
            // 1b) tonalidade opcional, também só no interior
            if (tint.alpha > 0f) {
                drawRect(
                    color = tint,
                    topLeft = Offset(sd.toFloat(), th.toFloat()),
                    size = Size((w - 2 * sd).toFloat(), (h - 2 * th).toFloat())
                )
            }
            // 2) conteúdo
            drawContent()
            // 3) bordas esticadas
            drawStretch(edgeH, cw, 0, w - 2 * cw, th)
            drawStretch(edgeH, cw, h - th, w - 2 * cw, th)
            drawStretch(edgeV, 0, ch, sd, h - 2 * ch)
            drawStretch(edgeV, w - sd, ch, sd, h - 2 * ch)
            // 4) cantos por cima
            drawStretch(cornerTl, 0, 0, cw, ch)
            drawStretch(cornerTr, w - cw, 0, cw, ch)
            drawStretch(cornerBl, 0, h - ch, cw, ch)
            drawStretch(cornerBr, w - cw, h - ch, cw, ch)
        }
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

private const val DESIGN_WIDTH = 1032f
private const val TOP_THICKNESS = 45f
private const val SIDE_THICKNESS = 43f
private const val CORNER_W = 55f
private const val CORNER_H = 46f

/** Preenche a área (x0, y0, x1, y1) com a madeira em cover, centralizada, sem esticar. */
private fun DrawScope.drawWoodCover(wood: ImageBitmap, x0: Int, y0: Int, x1: Int, y1: Int) {
    val dw = x1 - x0
    val dh = y1 - y0
    if (dw <= 0 || dh <= 0) return
    val scale = max(dw.toFloat() / wood.width, dh.toFloat() / wood.height)
    val sw = (dw / scale).roundToInt().coerceIn(1, wood.width)
    val sh = (dh / scale).roundToInt().coerceIn(1, wood.height)
    drawImage(
        image = wood,
        srcOffset = IntOffset((wood.width - sw) / 2, (wood.height - sh) / 2),
        srcSize = IntSize(sw, sh),
        dstOffset = IntOffset(x0, y0),
        dstSize = IntSize(dw, dh),
        filterQuality = FilterQuality.None
    )
}

/** Desenha a imagem esticada no retângulo, sem filtro, para manter o pixel art. */
private fun DrawScope.drawStretch(image: ImageBitmap, x: Int, y: Int, dw: Int, dh: Int) {
    if (dw <= 0 || dh <= 0) return
    drawImage(
        image = image,
        dstOffset = IntOffset(x, y),
        dstSize = IntSize(dw, dh),
        filterQuality = FilterQuality.None
    )
}
