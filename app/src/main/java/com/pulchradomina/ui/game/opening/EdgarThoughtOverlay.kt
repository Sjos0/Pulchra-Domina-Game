package com.pulchradomina.ui.game.opening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulchradomina.ui.theme.GameFontFamily
import kotlin.math.roundToInt
import kotlin.random.Random

// ---- Dimensões fixas (o pedido: caixa hardcoded; só o texto se adapta dentro dela) ----
private val BOX_HEIGHT: Dp = 112.dp
private val BOX_SIDE_MARGIN: Dp = 12.dp
/** Distância entre a caixa e a base da área da cena (acima dos controles). */
private val BOX_BOTTOM_MARGIN: Dp = 28.dp
private val PORTRAIT_HEIGHT: Dp = 310.dp
/** Centro horizontal do busto, como fração da largura (print: área azul à esquerda do centro). */
private const val PORTRAIT_CENTER_X_FRACTION = 0.43f
private val TEXT_PADDING_START: Dp = 18.dp
private val TEXT_PADDING_TOP: Dp = 12.dp
private val TEXT_PADDING_END: Dp = 28.dp
private val TEXT_PADDING_BOTTOM: Dp = 14.dp
private const val TEXT_MAX_SP = 20
private const val TEXT_MIN_SP = 11
private const val LINE_HEIGHT_FACTOR = 1.35f

private val TEXT_COLOR = Color(0xFFF3EEE4)
private val FRAME_COLOR = Color(0xFFF7F4EE)
private val DIRT_COLORS = listOf(Color(0xFF5B3A1E), Color(0xFF7A4F2A), Color(0xFF3F2814))

/** Marca de sujeira na moldura (posição normalizada 0..1 da caixa). */
private data class DirtMark(val x: Float, val y: Float, val radiusDp: Float, val colorIndex: Int, val alpha: Float)

private fun dirtMarks(count: Int = 70, seed: Int = 7): List<DirtMark> {
    val rnd = Random(seed)
    return List(count) {
        // maior parte nas bordas, onde a sujeira aparece numa moldura de verdade
        val edge = rnd.nextInt(4)
        val along = rnd.nextFloat()
        val inward = rnd.nextFloat() * 0.06f
        val (x, y) = when (edge) {
            0 -> along to inward
            1 -> along to 1f - inward
            2 -> inward to along
            else -> 1f - inward to along
        }
        DirtMark(
            x = x,
            y = y,
            radiusDp = 0.6f + rnd.nextFloat() * 2.6f,
            colorIndex = rnd.nextInt(DIRT_COLORS.size),
            alpha = 0.35f + rnd.nextFloat() * 0.45f
        )
    }
}

/**
 * Monólogo do Edgar: busto ancorado na linha de cima da caixa, gradiente escuro embaixo do busto
 * (esconde o corte da cintura), caixa com moldura branca fina e sujeira marrom, texto em Jost com
 * typewriter sincronizado à voz, seta pequena no canto quando a fala acabou.
 */
@Composable
fun EdgarThoughtOverlay(
    controller: EdgarOpeningController,
    modifier: Modifier = Modifier
) {
    val marks = remember { dirtMarks() }
    val interaction = remember { MutableInteractionSource() }
    val slide = controller.slideDp.value
    val line = EdgarOpeningData.lines[controller.lineIndex]
    val portrait = EdgarOpeningAssets.portraits[controller.lineIndex]

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = controller.boxAlpha.value }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = controller.canAdvance
            ) { controller.advance() }
    ) {
        val boxWidth = maxWidth - BOX_SIDE_MARGIN * 2

        // Busto + gradiente: a base do retrato encosta na linha de cima da caixa.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    x = maxWidth * (PORTRAIT_CENTER_X_FRACTION - 0.5f),
                    y = -(BOX_BOTTOM_MARGIN + BOX_HEIGHT) + slide.dp
                )
        ) {
            Image(
                painter = painterResource(portrait),
                contentDescription = null,
                modifier = Modifier.height(PORTRAIT_HEIGHT)
            )
            // forte embaixo (corte da cintura) -> transparente em cima (rosto)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.95f)
                        )
                    )
            )
        }

        // Caixa de fala: translúcida, moldura branca fina com sujeira marrom.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -BOX_BOTTOM_MARGIN + slide.dp)
                .width(boxWidth)
                .height(BOX_HEIGHT)
                .drawBehind { drawFrame(marks) }
        ) {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val innerWidthPx = with(density) { (boxWidth - TEXT_PADDING_START - TEXT_PADDING_END).toPx().roundToInt() }
            val innerHeightPx = with(density) { (BOX_HEIGHT - TEXT_PADDING_TOP - TEXT_PADDING_BOTTOM).toPx().roundToInt() }
            val fontSp = remember(line.text, innerWidthPx, innerHeightPx) {
                fitFontSize(measurer, line.text, innerWidthPx, innerHeightPx)
            }
            val shown = controller.visibleChars.coerceIn(0, line.text.length)
            val text: AnnotatedString = buildAnnotatedString {
                append(line.text.substring(0, shown))
                // o resto fica invisível: o layout não “pula” enquanto o texto aparece
                withStyle(SpanStyle(color = Color.Transparent)) { append(line.text.substring(shown)) }
            }

            Text(
                text = text,
                style = TextStyle(
                    fontFamily = GameFontFamily,
                    fontSize = fontSp.sp,
                    lineHeight = (fontSp * LINE_HEIGHT_FACTOR).sp,
                    color = TEXT_COLOR,
                    textAlign = TextAlign.Start
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = TEXT_PADDING_START,
                        top = TEXT_PADDING_TOP,
                        end = TEXT_PADDING_END,
                        bottom = TEXT_PADDING_BOTTOM
                    )
            )

            // Seta pequena: aparece quando a escrita e a voz acabam.
            val bob by rememberInfiniteTransition(label = "arrow").animateFloat(
                initialValue = 0f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "arrowBob"
            )
            AnimatedVisibility(
                visible = controller.canAdvance,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 9.dp, bottom = 7.dp),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(120))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Próxima fala",
                    tint = TEXT_COLOR,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { translationY = bob.dp.toPx() }
                )
            }
        }
    }
}

/** Maior fonte (sp) em que o texto cabe na altura da caixa; a largura é fixa. */
private fun fitFontSize(
    measurer: TextMeasurer,
    text: String,
    widthPx: Int,
    heightPx: Int
): Int {
    for (sp in TEXT_MAX_SP downTo TEXT_MIN_SP) {
        val style = TextStyle(
            fontFamily = GameFontFamily,
            fontSize = sp.sp,
            lineHeight = (sp * LINE_HEIGHT_FACTOR).sp
        )
        val layout = measurer.measure(text, style, constraints = Constraints(maxWidth = widthPx))
        if (layout.size.height <= heightPx) return sp
    }
    return TEXT_MIN_SP
}

/** Fundo translúcido + moldura branca fina + sujeira marrom (desenhado atrás do conteúdo). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrame(marks: List<DirtMark>) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xB805030F), Color(0xD605030F))),
        cornerRadius = corner
    )
    drawRoundRect(
        color = FRAME_COLOR.copy(alpha = 0.92f),
        cornerRadius = corner,
        style = Stroke(width = 1.4.dp.toPx())
    )
    marks.forEach { m ->
        val c = Offset(m.x * w, m.y * h)
        drawCircle(
            color = DIRT_COLORS[m.colorIndex].copy(alpha = m.alpha),
            radius = m.radiusDp.dp.toPx(),
            center = c
        )
    }
}
