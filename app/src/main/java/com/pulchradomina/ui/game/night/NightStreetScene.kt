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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val SKY_STOPS: Array<Pair<Float, Color>> = arrayOf(
    0.00f to Color(0xFF0A061D),
    0.25f to Color(0xFF10092A),
    0.45f to Color(0xFF110C29),
    0.65f to Color(0xFF1D123F),
    0.80f to Color(0xFF191134),
    1.00f to Color(0xFF100B22)
)
private val ROAD_FILL = Color(0xFF100C22)

/**
 * Cenário noturno da rua, na ordem de profundidade (fundo -> frente):
 *  céu/lua/nuvens -> prédios -> árvores -> arbustos -> bancos   (escurecidos por camada)
 *  -> calçada -> rua -> postes -> Edgar                           (plano do jogador)
 *  -> escuridão da noite -> cone de luz e lâmpada (aditivo)
 * A luz nasce da lâmpada e alarga até o chão; o Edgar só fica iluminado dentro do cone.
 */
@Composable
fun NightStreetScene(
    bitmaps: NightStreetBitmaps,
    playerWorldX: Float,
    isFacingRight: Boolean,
    /** Alpha do Edgar (0 = invisível, usado até o monólogo começar). */
    edgarAlpha: Float = 1f,
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

        drawRect(brush = Brush.verticalGradient(*SKY_STOPS))
        drawLayer(Layer.SKY, bitmaps, view, seconds, tint = null)
        drawLayer(Layer.FAR, bitmaps, view, seconds, tint = NightStreetLayout.FAR_TINT_ARGB)
        drawLayer(Layer.TREE, bitmaps, view, seconds, tint = NightStreetLayout.TREE_TINT_ARGB)
        drawLayer(Layer.BUSH, bitmaps, view, seconds, tint = NightStreetLayout.BUSH_TINT_ARGB)
        drawLayer(Layer.FRONT, bitmaps, view, seconds, tint = NightStreetLayout.FRONT_TINT_ARGB)
        drawGround(bitmaps, view)
        drawLampPosts(bitmaps, view)
        drawEdgar(bitmaps, view, playerWorldX, isFacingRight, seconds, edgarAlpha)
        drawNightOverlay()
        drawLampCones(view)
        drawLampBulbs(view)
    }
}

/** Props de uma camada. Camadas de fundo recebem tint (escurecimento multiplicativo). */
private fun DrawScope.drawLayer(
    layer: Layer,
    bitmaps: NightStreetBitmaps,
    view: StreetCamera.View,
    seconds: Float,
    tint: Long?
) {
    val worldWidth = NightStreetLayout.WORLD_WIDTH
    val filter = tint?.let { ColorFilter.tint(Color(it), BlendMode.Modulate) }
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
                flip = prop.flip,
                colorFilter = filter
            )
        }
    }
}

private fun DrawScope.drawGround(bitmaps: NightStreetBitmaps, view: StreetCamera.View) {
    val sidewalk = bitmaps.sprites.getValue(NightSprite.SIDEWALK)
    val road = bitmaps.sprites.getValue(NightSprite.ROAD)
    val s = view.scale
    val worldWidth = NightStreetLayout.WORLD_WIDTH

    val roadBottomPx = (NightStreetLayout.ROAD_TOP + NightStreetLayout.ROAD_TILE_HEIGHT) * s
    if (size.height > roadBottomPx) {
        drawRect(color = ROAD_FILL, topLeft = Offset(0f, roadBottomPx), size = Size(size.width, size.height - roadBottomPx))
    }

    val sidewalkW = NightStreetLayout.SIDEWALK_TILE_WIDTH
    for (k in 0 until ceil(worldWidth / sidewalkW).toInt()) {
        drawSpriteUnits(
            bmp = sidewalk,
            leftU = k * sidewalkW - view.cameraX,
            topU = NightStreetLayout.GROUND_TOP,
            widthU = sidewalkW,
            heightU = NightStreetLayout.SIDEWALK_HEIGHT,
            unitPx = s,
            flip = false
        )
    }
    val roadW = NightStreetLayout.REF_WIDTH
    for (k in 0 until ceil(worldWidth / roadW).toInt()) {
        drawSpriteUnits(
            bmp = road,
            leftU = k * roadW - view.cameraX,
            topU = NightStreetLayout.ROAD_TOP,
            widthU = roadW,
            heightU = NightStreetLayout.ROAD_TILE_HEIGHT,
            unitPx = s,
            flip = false
        )
    }
}

private fun DrawScope.drawLampPosts(bitmaps: NightStreetBitmaps, view: StreetCamera.View) {
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

private fun DrawScope.drawEdgar(
    bitmaps: NightStreetBitmaps,
    view: StreetCamera.View,
    playerWorldX: Float,
    isFacingRight: Boolean,
    seconds: Float,
    alpha: Float
) {
    if (alpha <= 0f) return
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
        flip = isFacingRight, // o sprite original olha para a esquerda
        alpha = alpha
    )
}

private fun DrawScope.drawNightOverlay() {
    drawRect(
        color = Color(NightStreetLayout.NIGHT_OVERLAY_ARGB),
        alpha = NightStreetLayout.NIGHT_OVERLAY_ALPHA
    )
}

/**
 * Cone de luz 2D: ápice estreito na lâmpada, base larga no chão.
 * Degraus = trapézios aninhados que compartilham o ápice, então o topo fica mais forte.
 */
private fun DrawScope.drawLampCones(view: StreetCamera.View) {
    val s = view.scale
    val color = Color(NightStreetLayout.LIGHT_COLOR_ARGB)
    val bands = NightStreetLayout.LIGHT_BANDS
    for (lamp in NightStreetLayout.lamps) {
        val apexX = (lamp.x - view.cameraX) * s
        val apexY = lamp.apexY * s
        val groundY = lamp.groundY * s
        val topHalf = lamp.topHalf * s
        for (band in 0 until bands) {
            val f = 1f - band.toFloat() / bands
            val bottomY = apexY + (groundY - apexY) * f
            val bottomHalf = topHalf + (lamp.baseHalf * s - topHalf) * f
            val path = Path().apply {
                moveTo(apexX - topHalf, apexY)
                lineTo(apexX + topHalf, apexY)
                lineTo(apexX + bottomHalf, bottomY)
                lineTo(apexX - bottomHalf, bottomY)
                close()
            }
            drawPath(path, color, alpha = NightStreetLayout.LIGHT_BAND_ALPHA, blendMode = BlendMode.Plus)
        }
    }
}

/** Lâmpada acesa: retângulo quente logo abaixo da cabeça do poste (o sprite a traz apagada). */
private fun DrawScope.drawLampBulbs(view: StreetCamera.View) {
    val s = view.scale
    val bulb = Color(NightStreetLayout.BULB_COLOR_ARGB).copy(alpha = 0.92f)
    for (lamp in NightStreetLayout.lamps) {
        val w = lamp.bulbW * s
        val h = lamp.bulbH * s
        drawRect(
            color = bulb,
            topLeft = Offset((lamp.x - view.cameraX) * s - w / 2f, (lamp.apexY - lamp.bulbH * 0.4f) * s),
            size = Size(w, h)
        )
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
    flip: Boolean,
    colorFilter: ColorFilter? = null,
    alpha: Float = 1f
) {
    val left = leftU * unitPx
    val width = widthU * unitPx
    if (left + width <= 0f || left >= size.width) return
    val dstOffset = IntOffset(left.roundToInt(), (topU * unitPx).roundToInt())
    val dstSize = IntSize(max(1, width.roundToInt()), max(1, (heightU * unitPx).roundToInt()))
    if (flip) {
        withTransform({ scale(-1f, 1f, pivot = Offset(left + width / 2f, topU * unitPx)) }) {
            drawImage(bmp, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha, colorFilter = colorFilter, filterQuality = FilterQuality.None)
        }
    } else {
        drawImage(bmp, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha, colorFilter = colorFilter, filterQuality = FilterQuality.None)
    }
}

private fun positiveMod(value: Float, modulo: Float): Float = ((value % modulo) + modulo) % modulo
