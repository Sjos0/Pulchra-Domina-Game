package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Caixa pixel: madeira com aspecto preservado (crop central) + moldura 9-slice.
 * Bordas = azulejos sem pontas; cantos cobrem as emendas.
 */
@Composable
fun PixelFrameBox(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
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
        modifier = modifier.drawWithContent {
            drawContent()
            drawSeamlessFrame(tl, tr, bl, br, top, bottom, left, right)
        }
    ) {
        Image(
            painter = painterResource(id = R.drawable.box_wood_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
            filterQuality = FilterQuality.None
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content
        )
    }
}

private fun DrawScope.drawSeamlessFrame(
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
    val minSide = min(w, h)
    val baseCorner = max(tl.width, tl.height).toFloat()
    val scale = min((minSide * 0.14f) / baseCorner, 1.6f).coerceAtLeast(0.45f)

    val cw = (tl.width * scale).roundToInt().coerceAtLeast(1)
    val ch = (tl.height * scale).roundToInt().coerceAtLeast(1)
    val topH = max(1, (top.height * scale).roundToInt())
    val botH = max(1, (bottom.height * scale).roundToInt())
    val leftW = max(1, (left.width * scale).roundToInt())
    val rightW = max(1, (right.width * scale).roundToInt())
    val overlap = max(1, (2f * scale).roundToInt())

    tileH(top, cw - overlap, 0, w.roundToInt() - cw + overlap, topH)
    tileH(bottom, cw - overlap, h.roundToInt() - botH, w.roundToInt() - cw + overlap, botH)
    tileV(left, 0, ch - overlap, h.roundToInt() - ch + overlap, leftW)
    tileV(right, w.roundToInt() - rightW, ch - overlap, h.roundToInt() - ch + overlap, rightW)

    blit(tl, 0, 0, cw, ch)
    blit(tr, w.roundToInt() - cw, 0, cw, ch)
    blit(bl, 0, h.roundToInt() - ch, cw, ch)
    blit(br, w.roundToInt() - cw, h.roundToInt() - ch, cw, ch)
}

private fun DrawScope.blit(image: ImageBitmap, x: Int, y: Int, dw: Int, dh: Int) {
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(x, y),
        dstSize = IntSize(dw.coerceAtLeast(1), dh.coerceAtLeast(1)),
        filterQuality = FilterQuality.None
    )
}

private fun DrawScope.tileH(image: ImageBitmap, x0: Int, y: Int, x1: Int, dstH: Int) {
    if (x1 <= x0) return
    val tileW = max(1f, image.width * (dstH.toFloat() / image.height))
    var x = x0.toFloat()
    val end = x1.toFloat()
    while (x < end) {
        val remaining = end - x
        val ww = min(tileW, remaining)
        val srcW = max(1, ((ww / tileW) * image.width).roundToInt().coerceAtMost(image.width))
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(srcW, image.height),
            dstOffset = IntOffset(x.roundToInt(), y),
            dstSize = IntSize(max(1, ww.roundToInt()), max(1, dstH)),
            filterQuality = FilterQuality.None
        )
        x += ww
    }
}

private fun DrawScope.tileV(image: ImageBitmap, x: Int, y0: Int, y1: Int, dstW: Int) {
    if (y1 <= y0) return
    val tileH = max(1f, image.height * (dstW.toFloat() / image.width))
    var y = y0.toFloat()
    val end = y1.toFloat()
    while (y < end) {
        val remaining = end - y
        val hh = min(tileH, remaining)
        val srcH = max(1, ((hh / tileH) * image.height).roundToInt().coerceAtMost(image.height))
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, srcH),
            dstOffset = IntOffset(x, y.roundToInt()),
            dstSize = IntSize(max(1, dstW), max(1, hh.roundToInt())),
            filterQuality = FilterQuality.None
        )
        y += hh
    }
}
