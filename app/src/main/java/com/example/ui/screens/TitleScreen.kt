package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GameState
import com.example.ui.components.PixelWoodButton
import com.example.ui.theme.MutedText
import com.example.ui.theme.PixelFontFamily

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("title_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.title_background),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
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
            Spacer(modifier = Modifier.height(220.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixelWoodButton(
                    text = "Capítulo 1: O Encontro na Balada",
                    onClick = {},
                    enabled = false,
                    height = 48.dp,
                    fontSize = 11.sp,
                    contentColor = Color(0xFFE8D5A3)
                )

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
                    text = "v1.0.5 • Android 8.0+",
                    color = MutedText,
                    fontFamily = PixelFontFamily,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
