package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GameState
import com.example.ui.theme.*

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
        // Fundo oficial (logo + gradiente roxo inferior)
        Image(
            painter = painterResource(id = R.drawable.title_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Vinheta suave no rodapé para legibilidade dos botões
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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Espaço superior livre — o logo já está na imagem de fundo
            Spacer(modifier = Modifier.height(280.dp))

            // Área de botões na região inferior (gradiente da arte)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SurfaceGothic.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderGothic)
                ) {
                    Text(
                        text = "Capítulo 1: O Encontro na Balada",
                        color = VioletAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = onNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("new_game_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Novo Jogo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (savedGame != null) {
                    OutlinedButton(
                        onClick = { onContinueGame(savedGame) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("continue_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AbyssalPurple.copy(alpha = 0.85f),
                            contentColor = PaleText
                        ),
                        border = BorderStroke(1.dp, BorderGothic),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = SilverMist,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continuar", fontSize = 14.sp)
                    }
                }

                OutlinedButton(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gallery_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AbyssalPurple.copy(alpha = 0.7f),
                        contentColor = PaleText
                    ),
                    border = BorderStroke(1.dp, BorderGothic),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = RoseDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galeria de Rotas (3a – 3f)", fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onOpenUpdates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_updates_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AbyssalPurple.copy(alpha = 0.6f),
                        contentColor = MutedText
                    ),
                    border = BorderStroke(1.dp, BorderGothic),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = SilverMist,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificar Atualizações", fontSize = 13.sp, color = SilverMist)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "v1.0.0 • Android 8.0+",
                    color = MutedText,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
