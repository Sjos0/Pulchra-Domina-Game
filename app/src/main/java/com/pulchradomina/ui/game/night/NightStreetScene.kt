package com.pulchradomina.ui.game.night

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val SKY_STOPS: List<Pair<Float, Color>> = listOf(
    0.00f to Color(0xFF0A061D),
    0.25f to Color(0xFF10092A),
    0.45f to Color(0xFF110C29),
    0.65f to Color(0xFF1D123F),
    0.80f to Color(0xFF191134),
    1.00f to Color(0xFF100B22)
)
private val ROAD_FILL = Color(0xFF100C22)
private val DOOR_FRAME = Color(0xFF120C1F)
private val DOOR_GLOW = Color(0xFF582E96)
private val NEON_VIOLET = Color(0xFFA55AFF)

/**
 * Cenário noturno da rua. Desenha, em ordem: céu, nuvens, prédios, calçada e rua,
 * árvores, bancos e postes, Edgar (idle em loop), escuridão da noite e, por último,
 * a luz dos postes somada (aditiva) — assim a luz "passa sobre" o Edgar.
 */
@Composable
fun NightStreetScene(
    bitmaps: NightStreetBitmaps,
    playerWorldX: Float,
    isFacingRight: Boolean,
    modifier: Modifier = Modifier
) {
    // Tempo em draw-phase: só o Canvas redesenha a cada frame, sem recompor a tela.
    val elapsedNanos = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0L) start = now
                elapsedNanos.longValue = now - start
            }
        }
    }

    Canvas(modifier = modifier) {
        val seconds = elapsedNanos.longValue / 1_000_000_000f
        val view = StreetCamera.view(size.width, size.height, playerWorldX)
        if (view.scale <= 0f) return@Canvas

        drawRect(brush = Brush.verticalGradient(colorStops = SKY_STOPS))
        drawLayer(Layer.SKY, bitmaps, view, seconds)
        drawLayer(Layer.FAR, bitmaps, view, seconds)
        drawGround(bitmaps, view)
        drawLayer(Layer.MID, bitmaps, view, seconds)
        drawLayer(Layer.FRONT, bitmaps, view, seconds)
        drawLamps(bitmaps, view)
        drawDoors(view)
        drawEdgar(bitmaps, view, playerWorldX, isFacingRight, seconds)
        drawNightOverlay()
        drawLamplights(view)
    }
}

private fun DrawScope.drawLayer(
    layer: Layer,
    bitmaps: NightStreetBitmaps,
    view: StreetCamera.View,
    seconds: Float
) {
    val worldWidth = NightStreetLayout.WORLD_WIDTH
    for (prop in NightStreetLayout.props) {
        if (prop.layer != layer) continue
        val bmp = bitmaps.sprites.getValue(prop.sprite)
        val widthU = prop.h * bmp.width / bmp.height
        // Nuvens dão a volta no mundo; as demais são fixas no mundo, com parallax.
        val wraps = prop.drift != 0f
        val originU = if (wraps) positiveMod(prop.x + prop.drift * seconds, worldWidth) else prop.x
        val copies = if (wraps) -1..1 else 0..0
        for (k in copies) {
            val centerU = originU + k * worldWidth - view.cameraX * prop.parallax
            drawSpriteUnits(
                bmp = bmp,
                leftU = centerU - widthU / 2f,
                topU = prop.top,
                widthU = widthU,
                heightU = prop.h,
                unitPx = view.scale,
                flip = prop.flip
            )
        }
    }
}

private fun DrawScope.drawGround(bitmaps: NightStreetBitmaps, view: StreetCamera.View) {
    val sidewalk = bitmaps.sprites.getValue(NightSprite.SIDEWALK)
    val road = bitmaps.sprites.getValue(NightSprite.ROAD)
    val s = view.scale

    val roadBottomPx = (NightStreetLayout.ROAD_TOP + NightStreetLayout.ROAD_TILE_HEIGHT) * s
    if (size.height > roadBottomPx) {
        drawRect(color = ROAD_FILL, topLeft = Offset(0f, roadBottomPx), size = Size(size.width, size.height - roadBottomPx))
    }

    val worldWidth = NightStreetLayout.WORLD_WIDTH
    val sidewalkW = NightStreetLayout.SIDEWALK_TILE_WIDTH
    for (k in 0 until ceil(worldWidth / sidewalkW).toInt()) {
        drawSpriteUnits(sidewalk, k * sidewalkW - view.cameraX, NightStreetLayout.GROUND_TOP,
            sidewalkW, NightStreetLayout.SIDEWALK_HEIGHT, s, flip = false)
    }
    val roadW = NightStreetLayout.REF_WIDTH
    for (k in 0 until ceil(worldWidth / roadW).toInt()) {
        drawSpriteUnits(road, k * roadW - view.cameraX, NightStreetLayout.ROAD_TOP,
            roadW, NightStreetLayout.ROAD_TILE_HEIGHT, s, flip = false)
    }
}

private fun DrawScope.drawLamps(bitmaps: NightStreetBitmaps, view: StreetCamera.View) {
    val post = bitmaps.sprites.getValue(NightSprite.LAMP)
    for (lamp in NightStreetLayout.lamps) {
        val widthU = lamp.h * post.width / post.height
        drawSpriteUnits(
            bmp = post,
            leftU = lamp.x - widthU / 2f - view.cameraX,
            topU = lamp.baseY - lamp.h,
            widthU = widthU,
            heightU = lamp.h,
            unitPx = view.scale,
            flip = false
        )
    }
}

private fun DrawScope.drawDoors(view: StreetCamera.View) {
    val s = view.scale
    for (door in NightStreetLayout.doors) {
        val left = (door.x - view.cameraX) * s
        val top = door.top * s
        val w = door.w * s
        val h = door.h * s
        if (left + w <= 0f || left >= size.width) continue
        drawRect(color = DOOR_FRAME, topLeft = Offset(left, top), size = Size(w, h))
        drawRect(color = DOOR_GLOW, topLeft = Offset(left + 3f * s, top + 4f * s), size = Size(w - 6f * s, h - 4f * s))
        if (door.neon) {
            drawRect(color = NEON_VIOLET, topLeft = Offset(left - 2f * s, (door.top - 40f) * s), size = Size(w + 4f * s, 18f * s))
        }
    }
}

private fun DrawScope.drawEdgar(
    bitmaps: NightStreetBitmaps,
    view: StreetCamera.View,
    playerWorldX: Float,
    isFacingRight: Boolean,
    seconds: Float
) {
    val frameCount = NightStreetLayout.EDGAR_FRAME_COUNT
    val frame = (seconds * NightStreetLayout.EDGAR_FPS).toInt() % frameCount
    val heightU = NightStreetLayout.EDGAR_HEIGHT
    val widthU = heightU * NightStreetLayout.EDGAR_ASPECT
    drawSpriteUnits(
        bmp = bitmaps.edgarFrames[frame],
        leftU = playerWorldX - view.cameraX - widthU / 2f,
        topU = NightStreetLayout.EDGAR_FEET_Y - heightU,
        widthU = widthU,
        heightU = heightU,
        unitPx = view.scale,
        flip = isFacingRight // o sprite original olha para a esquerda
    )
}

private fun DrawScope.drawNightOverlay() {
    drawRect(
        color = Color(NightStreetLayout.NIGHT_OVERLAY_ARGB),
        alpha = NightStreetLayout.NIGHT_OVERLAY_ALPHA
    )
}

/** Cada poste soma uma elipse em degraus: luz limitada, com borda de cor visível. */
private fun DrawScope.drawLamplights(view: StreetCamera.View) {
    val s = view.scale
    val color = Color(NightStreetLayout.LIGHT_COLOR_ARGB)
    val bands = NightStreetLayout.LIGHT_BANDS
    for (lamp in NightStreetLayout.lamps) {
        val cx = (lamp.lightX - view.cameraX) * s
        val cy = lamp.lightY * s
        for (band in 0 until bands) {
            val f = 1f - band.toFloat() / bands
            val rx = lamp.lightRx * s * f
            val ry = lamp.lightRy * s * f
            drawOval(
                color = color,
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
                alpha = NightStreetLayout.LIGHT_BAND_ALPHA,
                blendMode = BlendMode.Plus
            )
        }
    }
}

/** Converte unidades de referência em pixels e pula o que está fora da tela. */
private fun DrawScope.drawSpriteUnits(
    bmp: ImageBitmap,
    leftU: Float,
    topU: Float,
    widthU: Float,
    heightU: Float,
    unitPx: Float,
    flip: Boolean
) {
    val left = leftU * unitPx
    val width = widthU * unitPx
    if (left + width <= 0f || left >= size.width) return
    val dstOffset = IntOffset(left.roundToInt(), (topU * unitPx).roundToInt())
    val dstSize = IntSize(max(1, width.roundToInt()), max(1, (heightU * unitPx).roundToInt()))
    if (flip) {
        withTransform({ scale(-1f, 1f, pivot = Offset(left + width / 2f, topU * unitPx)) }) {
            drawImage(bmp, dstOffset = dstOffset, dstSize = dstSize, filterQuality = FilterQuality.None)
        }
    } else {
        drawImage(bmp, dstOffset = dstOffset, dstSize = dstSize, filterQuality = FilterQuality.None)
    }
}

private fun positiveMod(value: Float, modulo: Float): Float = ((value % modulo) + modulo) % modulo
