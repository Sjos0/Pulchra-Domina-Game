package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.GameState
import com.example.ui.components.PixelWoodButton
import com.example.ui.theme.MutedText
import com.example.ui.theme.PixelFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TITLE_BG_HOLD_MS = 10_000L
private const val TITLE_BG_FADE_MS = 1_200

@Composable
fun TitleScreen(
    savedGame: GameState?,
    onNewGame: () -> Unit,
    onContinueGame: (GameState) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // 0 = arte padrão; 1 = arte secundária (caveira). Loop só enquanto esta tela está composta.
    val altAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(TITLE_BG_HOLD_MS)
            altAlpha.animateTo(1f, animationSpec = tween(TITLE_BG_FADE_MS))
            delay(TITLE_BG_HOLD_MS)
            altAlpha.animateTo(0f, animationSpec = tween(TITLE_BG_FADE_MS))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("title_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.title_background),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(id = R.drawable.title_background_alt),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = altAlpha.value }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(240.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PixelWoodButton(
                    text = "Novo Jogo",
                    onClick = onNewGame,
                    height = 56.dp,
                    fontSize = 15.sp,
                    testTag = "new_game_button"
                )

                if (savedGame != null) {
                    PixelWoodButton(
                        text = "Continuar",
                        onClick = { onContinueGame(savedGame) },
                        height = 52.dp,
                        fontSize = 14.sp,
                        testTag = "continue_button"
                    )
                }

                PixelWoodButton(
                    text = "Galeria de Rotas (3a – 3f)",
                    onClick = onOpenGallery,
                    height = 52.dp,
                    fontSize = 12.sp,
                    testTag = "gallery_button"
                )

                PixelWoodButton(
                    text = "Verificar Atualizações",
                    onClick = onOpenUpdates,
                    height = 52.dp,
                    fontSize = 12.sp,
                    testTag = "check_updates_button"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "v${BuildConfig.VERSION_NAME} • Android 8.0+",
                    color = MutedText,
                    fontFamily = PixelFontFamily,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
