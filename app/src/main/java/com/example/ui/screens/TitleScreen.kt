package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianDark,
                        AbyssalPurple,
                        ObsidianDark
                    )
                )
            )
            .padding(24.dp)
            .testTag("title_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Cabeçalho e Identidade Visual
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Emblema Central da Hortênsia Negra
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(2.dp, VioletLight.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pulchra_domina_icon_1794020212151),
                        contentDescription = "Emblema Pulchra Domina",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = "PULCHRA DOMINA",
                    color = VioletLight,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Uma Visual Novel de luto, escolhas e mistério",
                    color = SilverMist.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = SurfaceGothic.copy(alpha = 0.8f),
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botões de Ação
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Novo Jogo
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
                    Text(
                        text = "Novo Jogo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Continuar Jogo (habilitado se houver save)
                if (savedGame != null) {
                    OutlinedButton(
                        onClick = { onContinueGame(savedGame) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("continue_game_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = PaleText
                        ),
                        border = BorderStroke(1.5.dp, VioletLight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = VioletLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continuar Capítulo 1",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Galeria de Rotas
                OutlinedButton(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("routes_gallery_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceGothic,
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
                    Text(
                        text = "Galeria de Rotas (3a – 3f)",
                        fontSize = 14.sp
                    )
                }

                // Verificar Atualizações
                OutlinedButton(
                    onClick = onOpenUpdates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_updates_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AbyssalPurple,
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
                    Text(
                        text = "Verificar Atualizações",
                        fontSize = 13.sp,
                        color = SilverMist
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rodapé
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "v1.0.0 • Compatível com Android 8.0+ (API 26)",
                    color = MutedText,
                    fontSize = 11.sp
                )
                Text(
                    text = "Baseado na estética de Edgar Allan Poe e La Belle Dame sans Merci",
                    color = MutedText.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
