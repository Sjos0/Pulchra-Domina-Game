package com.pulchradomina.ui.game.night

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulchradomina.R
import com.pulchradomina.ui.theme.GameFontFamily
import com.pulchradomina.ui.theme.PaleText

/** Proporção do card (941 x 1672, o PNG enviado com o fundo já transparente). */
private const val TITLE_ASPECT = 941f / 1672f

/** Altura do card em relação à altura da tela (o PNG tem margem vazia em volta do texto). */
private const val TITLE_HEIGHT_FRACTION = 0.72f

/**
 * Sobreposição da intro: véu escuro, barras de cinema (cima e baixo) e o card com o subtítulo.
 * O card aparece inteiro (Capítulo 1 e O PRIMEIRO ENCONTRO juntos); o subtítulo usa a fonte do jogo.
 */
@Composable
internal fun NightIntroOverlay(frame: NightIntroFrame, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (frame.veilAlpha > 0f) {
                drawRect(color = Color.Black, alpha = frame.veilAlpha)
            }
            val bar = size.height * frame.barFraction
            if (bar > 0f) {
                drawRect(color = Color.Black, topLeft = Offset.Zero, size = Size(size.width, bar))
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(0f, size.height - bar),
                    size = Size(size.width, bar)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.chapter1_title),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(TITLE_HEIGHT_FRACTION)
                    .aspectRatio(TITLE_ASPECT)
                    .graphicsLayer { alpha = frame.titleAlpha }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Um... Dois... Três... Maveth",
                color = PaleText.copy(alpha = frame.subtitleAlpha),
                fontFamily = GameFontFamily,
                fontSize = 18.sp
            )
        }
    }
}
