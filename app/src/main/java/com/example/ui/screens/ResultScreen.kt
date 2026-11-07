package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameState
import com.example.domain.MacroRouteInfo
import com.example.ui.theme.*

@Composable
fun ResultScreen(
    gameState: GameState,
    routeInfo: MacroRouteInfo,
    onRestart: () -> Unit,
    onOpenGallery: () -> Unit,
    onGoToTitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onGoToTitle()
    }

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
            .padding(20.dp)
            .testTag("result_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Badge de Conclusão do Capítulo 1
            Surface(
                color = SurfaceGothic,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderGothic)
            ) {
                Text(
                    text = "Capítulo 1 Concluído • O Encontro na Balada",
                    color = MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Cartão de Destaque da Rota Macro (3a - 3f)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("macro_route_card"),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceCard,
                border = BorderStroke(1.5.dp, routeInfo.color.copy(alpha = 0.8f)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = routeInfo.color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Macro-Rota Desbloqueada: ${routeInfo.code}",
                            color = routeInfo.color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = routeInfo.title,
                        color = PaleText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = routeInfo.subtitle,
                        color = routeInfo.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(
                        color = BorderGothic,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = routeInfo.description,
                        color = PaleText.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Resumo de Estatísticas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceGothic,
                border = BorderStroke(1.dp, BorderGothic)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Suas Decisões no Capítulo 1",
                        color = VioletLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = if (gameState.pegou_flor) VioletLight else MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Hortênsia Preta de Maveth",
                                color = PaleText,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (gameState.pegou_flor) "Aceitou a flor" else "Recusou a flor",
                            color = if (gameState.pegou_flor) VioletLight else RoseDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = RoseDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pontuação de Escolhas",
                                color = PaleText,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "${gameState.pont_cap} pts",
                            color = PaleText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Afinidade com Maveth",
                            color = PaleText,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${gameState.confianca_maveth} pts",
                            color = MavethColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Escolhas tomadas",
                            color = PaleText,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${gameState.escolhas_feitas.size} decisões",
                            color = SilverMist,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Botões de Ação
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("restart_chapter_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explorar Outro Caminho (Reiniciar)")
                }

                OutlinedButton(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("gallery_from_result_button"),
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
                    Text("Ver Todas as Rotas Desbloqueadas")
                }

                OutlinedButton(
                    onClick = onGoToTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("return_title_from_result_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianDark,
                        contentColor = MutedText
                    ),
                    border = BorderStroke(1.dp, BorderGothic),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = SilverMist,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voltar ao Menu Principal", color = SilverMist)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
