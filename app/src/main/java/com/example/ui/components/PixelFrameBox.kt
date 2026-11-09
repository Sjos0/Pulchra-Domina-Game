package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Caixa pixel com fundo de madeira e moldura 9-slice (cantos + bordas repetidas).
 * As peças da moldura são desenhadas interligadas — sem gaps entre segmentos.
 */
@Composable
fun PixelFrameBox(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val tl = ImageBitmap.imageResource(id = R.drawable.frame_tl)
    val tr = ImageBitmap.imageResource(id = R.drawable.frame_tr)
    val bl = ImageBitmap.imageResource(id = R.drawable.frame_bl)
    val br = ImageBitmap.imageResource(id = R.drawable.frame_br)
    val top = ImageBitmap.imageResource(id = R.drawable.frame_top)
    val bottom = ImageBitmap.imageResource(id = R.drawable.frame_bottom)
    val left = ImageBitmap.imageResource(id = R.drawable.frame_left)
    val right = ImageBitmap.imageResource(id = R.drawable.frame_right)

    Box(
        modifier = modifier
            .drawWithContent {
                // Fundo madeira (ligeiramente inset para a moldura cobrir as bordas)
                // O Image abaixo desenha o fill; aqui só a moldura por cima do content
                drawContent()
                drawPixelFrame(
                    tl = tl, tr = tr, bl = bl, br = br,
                    top = top, bottom = bottom, left = left, right = right
                )
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.box_wood_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content
        )
    }
}

private fun DrawScope.drawPixelFrame(
    tl: ImageBitmap,
    tr: ImageBitmap,
    bl: ImageBitmap,
    br: ImageBitmap,
    top: ImageBitmap,
    bottom: ImageBitmap,
    left: ImageBitmap,
    right: ImageBitmap
) {
    val w = size.width
    val h = size.height

    // Escala cantos para ~ densidades: usar tamanho nativo em px convertendo
    // ImageBitmap já está em px; desenhamos em px de canvas
    val cw = maxOf(tl.width, tr.width, bl.width, br.width).toFloat()
    val ch = maxOf(tl.height, tr.height, bl.height, br.height).toFloat()
    val scale = minOf(
        (w * 0.18f) / cw,
        (h * 0.22f) / ch,
        1.2f
    ).coerceAtLeast(0.5f)

    val cornerW = cw * scale
    val cornerH = ch * scale
    val edgeTopH = top.height * scale
    val edgeBotH = bottom.height * scale
    val edgeLeftW = left.width * scale
    val edgeRightW = right.width * scale

    // Bordas horizontais (entre cantos), com overlap de 1px para evitar frestas
    tileHorizontal(top, cornerW - 1f, 0f, w - cornerW + 1f, edgeTopH)
    tileHorizontal(bottom, cornerW - 1f, h - edgeBotH, w - cornerW + 1f, edgeBotH)

    // Bordas verticais
    tileVertical(left, 0f, cornerH - 1f, edgeLeftW, h - cornerH + 1f)
    tileVertical(right, w - edgeRightW, cornerH - 1f, edgeRightW, h - cornerH + 1f)

    // Cantos por cima (interligam visualmente)
    drawImageScaled(tl, 0f, 0f, cornerW, cornerH)
    drawImageScaled(tr, w - cornerW, 0f, cornerW, cornerH)
    drawImageScaled(bl, 0f, h - cornerH, cornerW, cornerH)
    drawImageScaled(br, w - cornerW, h - cornerH, cornerW, cornerH)
}

private fun DrawScope.drawImageScaled(
    image: ImageBitmap,
    x: Float,
    y: Float,
    dstW: Float,
    dstH: Float
) {
    drawImage(
        image = image,
        dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1))
    )
}

private fun DrawScope.tileHorizontal(
    image: ImageBitmap,
    x0: Float,
    y: Float,
    x1: Float,
    dstH: Float
) {
    if (x1 <= x0) return
    val tileW = (image.width * (dstH / image.height)).coerceAtLeast(1f)
    var x = x0
    while (x < x1) {
        val remaining = x1 - x
        val w = minOf(tileW, remaining)
        // src width proportion
        val srcW = ((w / tileW) * image.width).toInt().coerceIn(1, image.width)
        drawImage(
            image = image,
            srcOffset = androidx.compose.ui.unit.IntOffset(0, 0),
            srcSize = androidx.compose.ui.unit.IntSize(srcW, image.height),
            dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(w.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1))
        )
        x += w
    }
}

private fun DrawScope.tileVertical(
    image: ImageBitmap,
    x: Float,
    y0: Float,
    dstW: Float,
    y1: Float
) {
    if (y1 <= y0) return
    val tileH = (image.height * (dstW / image.width)).coerceAtLeast(1f)
    var y = y0
    while (y < y1) {
        val remaining = y1 - y
        val h = minOf(tileH, remaining)
        val srcH = ((h / tileH) * image.height).toInt().coerceIn(1, image.height)
        drawImage(
            image = image,
            srcOffset = androidx.compose.ui.unit.IntOffset(0, 0),
            srcSize = androidx.compose.ui.unit.IntSize(image.width, srcH),
            dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
        )
        y += h
    }
}
