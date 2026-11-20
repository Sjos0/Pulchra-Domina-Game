package com.pulchradomina.ui.game.night

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import com.pulchradomina.domain.exploration.ExplorationRules
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private val SKY_STOPS: Array<Pair<Float, Color>> = arrayOf(
    0.00f to Color(0xFF0A061D),
    0.25f to Color(0xFF10092A),
    0.45f to Color(0xFF110C29),
    0.65f to Color(0xFF1D123F),
    0.80f to Color(0xFF191134),
    1.00f to Color(0xFF100B22)
)
private val ROAD_FILL = Color(0xFF100C22)

/** Uma ColorFilter por cor da balada (criadas uma vez; o desenho só escolhe uma). */
private val CLUB_FILTERS: List<ColorFilter> = ClubLights.SEQUENCE.map { ColorFilter.tint(Color(it), BlendMode.SrcIn) }

/** Estado da luz da balada num frame: nível (0..0.9) e batidas tocadas (define a cor). */
private class ClubFrame(val level: Float, val beatCount: Int) {
    val filter: ColorFilter get() = CLUB_FILTERS[ClubLights.colorIndexFor(beatCount)]
}

/** Tempo do frame e tempo desde a última batida viram o nível e a cor da luz. */
private fun clubFrameNow(): ClubFrame {
    val now = System.nanoTime()
    val pulse = ClubLights.pulseAt(ClubBeats.secondsSinceBeat(now))
    return ClubFrame(ClubLights.level(pulse), ClubBeats.beatCount(now))
}

/**
 * Cenário noturno da rua, na ordem de profundidade (fundo -> frente):
 *  céu/lua/nuvens -> prédios -> árvores -> arbustos -> bancos   (escurecidos por camada; a borda
 *      voltada para a balada recebe Rim, aditivo)
 *  -> calçada -> rua -> postes -> Edgar                           (plano do jogador)
 *  -> escuridão da noite -> faixa da balada no chão -> cone e lâmpada dos postes (aditivo)
 *  -> pontos de neon da balada na borda direita (aditivo)
 * A balada fica fora da tela, à direita: a luz chega pelas bordas e pelo chão, pulsando nas batidas.
 */
@Composable
fun NightStreetScene(
    bitmaps: NightStreetBitmaps,
    playerWorldX: Float,
    isFacingRight: Boolean,
    /** Alpha do Edgar (0 = invisível, usado até o monólogo começar). */
    edgarAlpha: Float = 1f,
    /** false durante o monólogo: não anda, não conta passo, não toca som. */
    allowSteps: Boolean = true,
    /** Chamado a cada contato do pé (um passo). */
    onFootstep: () -> Unit = {},
    /** Câmera fixa (intro cinematográfica). Null = câmera segue o Edgar. */
    cameraWorldX: Float? = null,
    modifier: Modifier = Modifier
) {
    // Tempo e walk em draw-phase: o Canvas redesenha a cada frame, sem recompor a tela.
    val elapsedNanos = remember { mutableLongStateOf(0L) }
    val walk = remember { WalkCycleController() }
    val currentX by rememberUpdatedState(playerWorldX)
    val currentAllowSteps by rememberUpdatedState(allowSteps)
    val currentOnFootstep by rememberUpdatedState(onFootstep)
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0L) start = now
                elapsedNanos.longValue = now - start
                val steps = walk.update(now, currentX, currentAllowSteps)
                repeat(steps) { currentOnFootstep() }
            }
        }
    }

    // Aviso do item no chão: aparece a até 3% do mundo do item (só com o Edgar visível) e some ao afastar.
    val item = remember { NightStreetLayout.groundProps.firstOrNull { it.sprite == NightSprite.ITEM } }
    val nearItem = edgarAlpha > 0.5f && item != null &&
        ExplorationRules.isNearItem(
            playerXPercent = playerWorldX / NightStreetLayout.WORLD_WIDTH * 100f,
            itemXPercent = item.x / NightStreetLayout.WORLD_WIDTH * 100f
        )
    val itemHint = remember { Animatable(0f) }
    LaunchedEffect(nearItem) {
        // entrada e saída com 300 ms; a saída é a entrada ao contrário (no tempo e na curva)
        itemHint.animateTo(
            targetValue = if (nearItem) 1f else 0f,
            animationSpec = tween(
                durationMillis = ItemHint.ANIM_MS,
                easing = if (nearItem) FastOutSlowInEasing else ItemHint.REVERSED_EASING
            )
        )
    }

    Canvas(modifier = modifier) {
        val seconds = elapsedNanos.longValue / 1_000_000_000f
        val view = cameraWorldX?.let { StreetCamera.viewAt(size.width, size.height, it) }
            ?: StreetCamera.view(size.width, size.height, playerWorldX)
        if (view.scale <= 0f) return@Canvas
        val club = clubFrameNow()

        drawRect(brush = Brush.verticalGradient(*SKY_STOPS))
        drawLayer(Layer.SKY, bitmaps, view, seconds, tint = null, club = club)
        drawLayer(Layer.FAR, bitmaps, view, seconds, tint = NightStreetLayout.FAR_TINT_ARGB, club = club)
        drawLayer(Layer.TREE, bitmaps, view, seconds, tint = NightStreetLayout.TREE_TINT_ARGB, club = club)
        drawLayer(Layer.BUSH, bitmaps, view, seconds, tint = NightStreetLayout.BUSH_TINT_ARGB, club = club)
        drawLayer(Layer.FRONT, bitmaps, view, seconds, tint = NightStreetLayout.FRONT_TINT_ARGB, club = club)
        drawGround(bitmaps, view)
        drawGroundProps(bitmaps, view, seconds)
        drawLampPosts(bitmaps, view)
        drawEdgar(bitmaps, view, playerWorldX, isFacingRight, seconds, edgarAlpha, walk)
        drawNightOverlay()
        drawClubSpill(bitmaps, view, club)
        drawLampCones(bitmaps, view)
        drawLampBulbs(view)
        item?.let { drawItemHint(it, view.cameraX, view.scale, itemHint.value) }
    }
}

/**
 * Props de uma camada. Camadas de fundo recebem tint (escurecimento multiplicativo).
 * Árvores e bancos recebem Rim: só a borda voltada para a balada, colorida, aditiva.
 * O Rim é desenhado junto do prop, então quem está na frente o cobre (profundidade preservada).
 */
private fun DrawScope.drawLayer(
    layer: Layer,
    bitmaps: NightStreetBitmaps,
    view: StreetCamera.View,
    seconds: Float,
    tint: Long?,
    club: ClubFrame
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
            val rim = NightStreetLayout.rimMasks[prop.sprite]
            if (rim != null && k == 0) {
                // espelhado, a borda da direita da tela é a borda esquerda do arquivo
                val rimSprite = if (prop.flip) rim.second else rim.first
                drawSpriteUnits(
                    bmp = bitmaps.sprites.getValue(rimSprite),
                    leftU = centerU - widthU / 2f,
                    topU = prop.top,
                    widthU = widthU,
                    heightU = prop.h,
                    unitPx = view.scale,
                    flip = prop.flip,
                    colorFilter = club.filter,
                    alpha = ClubLights.rimAlpha(prop.x, worldWidth, club.level),
                    blendMode = BlendMode.Plus
                )
            }
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

/**
 * Itens no chão da calçada (ex.: a flor do Maveth). O item interativo tem um contorno dourado
 * que pulsa devagar por cima; a arte não muda.
 */
private fun DrawScope.drawGroundProps(bitmaps: NightStreetBitmaps, view: StreetCamera.View, seconds: Float) {
    for (prop in NightStreetLayout.groundProps) {
        val bmp = bitmaps.sprites.getValue(prop.sprite)
        val widthU = prop.h * bmp.width / bmp.height
        val leftU = prop.x - widthU / 2f - view.cameraX
        drawSpriteUnits(
            bmp = bmp,
            leftU = leftU,
            topU = prop.top,
            widthU = widthU,
            heightU = prop.h,
            unitPx = view.scale,
            flip = prop.flip
        )
        if (prop.sprite == NightSprite.ITEM) {
            // halo dourado que pulsa ao redor da flor: 0,30 a 0,85 de opacidade, ciclo de 1,8 s
            val pulse = 0.5f + 0.5f * sin(2f * PI.toFloat() * seconds / 1.8f)
            val ind = bitmaps.sprites.getValue(NightSprite.ITEM_INDICATOR)
            val indH = prop.h * NightStreetLayout.ITEM_INDICATOR_SCALE_H
            val indW = indH * ind.width / ind.height
            drawSpriteUnits(
                bmp = ind,
                leftU = prop.x - indW / 2f - view.cameraX,
                topU = prop.top + prop.h / 2f - indH / 2f,
                widthU = indW,
                heightU = indH,
                unitPx = view.scale,
                flip = false,
                alpha = 0.30f + 0.55f * pulse,
                blendMode = BlendMode.Plus
            )
        }
    }
}

/**
 * Faixa de luz que entra pela borda direita: sobre a calçada e a rua, forte junto da balada e fraca
 * para o centro. Textura de pixel art, colorida pela balada (mesma cor do Rim).
 */
private fun DrawScope.drawClubSpill(bitmaps: NightStreetBitmaps, view: StreetCamera.View, club: ClubFrame) {
    val leftU = NightStreetLayout.WORLD_WIDTH - NightStreetLayout.CLUB_SPILL_W - view.cameraX
    drawSpriteUnits(
        bmp = bitmaps.sprites.getValue(NightSprite.CLUB_SPILL),
        leftU = leftU,
        topU = NightStreetLayout.GROUND_TOP,
        widthU = NightStreetLayout.CLUB_SPILL_W,
        heightU = NightStreetLayout.CLUB_SPILL_H,
        unitPx = view.scale,
        flip = false,
        colorFilter = club.filter,
        alpha = ClubLights.spillAlpha(club.level),
        blendMode = BlendMode.Plus
    )
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
    alpha: Float,
    walk: WalkCycleController
) {
    if (alpha <= 0f) return
    // andando: walk cycle (pelo deslocamento); parado: idle em loop
    val bmp: ImageBitmap
    val heightU: Float
    val widthU: Float
    if (walk.walking) {
        bmp = bitmaps.walkFrames[walk.frameIndex]
        heightU = WalkCycleLayout.FRAME_HEIGHT_U
        widthU = WalkCycleLayout.FRAME_WIDTH_U
    } else {
        val frame = (seconds * NightStreetLayout.EDGAR_FPS).toInt() % NightStreetLayout.EDGAR_FRAME_COUNT
        bmp = bitmaps.edgarFrames[frame]
        heightU = NightStreetLayout.EDGAR_HEIGHT
        widthU = heightU * NightStreetLayout.EDGAR_ASPECT
    }
    drawSpriteUnits(
        bmp = bmp,
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
 * Luz de cada lâmpada: textura de cone gerada pelo pipeline (borda suave, intensidade decrescente,
 * dithering), esticada no retângulo do cone (ápice na lâmpada, base no chão). Aditiva, uma imagem por poste.
 */
private fun DrawScope.drawLampCones(bitmaps: NightStreetBitmaps, view: StreetCamera.View) {
    val light = bitmaps.sprites.getValue(NightSprite.LAMP_LIGHT)
    for (lamp in NightStreetLayout.lamps) {
        drawSpriteUnits(
            bmp = light,
            leftU = lamp.x - lamp.baseHalf - view.cameraX,
            topU = lamp.apexY,
            widthU = 2f * lamp.baseHalf,
            heightU = lamp.groundY - lamp.apexY,
            unitPx = view.scale,
            flip = false,
            blendMode = BlendMode.Plus
        )
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
    alpha: Float = 1f,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    val left = leftU * unitPx
    val width = widthU * unitPx
    if (left + width <= 0f || left >= size.width) return
    val dstOffset = IntOffset(left.roundToInt(), (topU * unitPx).roundToInt())
    val dstSize = IntSize(max(1, width.roundToInt()), max(1, (heightU * unitPx).roundToInt()))
    if (flip) {
        withTransform({ scale(-1f, 1f, pivot = Offset(left + width / 2f, topU * unitPx)) }) {
            drawImage(bmp, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha, colorFilter = colorFilter, blendMode = blendMode, filterQuality = FilterQuality.None)
        }
    } else {
        drawImage(bmp, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha, colorFilter = colorFilter, blendMode = blendMode, filterQuality = FilterQuality.None)
    }
}

private fun positiveMod(value: Float, modulo: Float): Float = ((value % modulo) + modulo) % modulo
