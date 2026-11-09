package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MangaPanel
import com.example.domain.MangaSequencePlayer
import com.example.domain.MangaVisualType
import com.example.ui.theme.*

@Composable
fun MangaCutsceneScreen(
    onFinishCutscene: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sequencePlayer = remember { MangaSequencePlayer() }
    val panels = sequencePlayer.panelList
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentPanel = sequencePlayer.panelAt(currentIndex)

    fun advance() {
        val (next, finished) = sequencePlayer.advance(currentIndex)
        if (finished) {
            onFinishCutscene()
        } else {
            currentIndex = next
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .clickable { advance() }
            .testTag("manga_cutscene_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra Superior de Controle do Mangá
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de Painéis
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    panels.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .width(if (index == currentIndex) 28.dp else 10.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == currentIndex) VioletLight
                                    else if (index < currentIndex) VioletAccent.copy(alpha = 0.5f)
                                    else BorderGothic
                                )
                        )
                    }
                }

                // Botão Pular
                TextButton(
                    onClick = onFinishCutscene,
                    modifier = Modifier.testTag("skip_manga_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pular",
                            color = MutedText,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Pular Cutscene",
                            tint = MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quadro Visual do Mangá (Manga Frame)
            AnimatedContent(
                targetState = currentPanel,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { width -> width / 3 })
                        .togetherWith(fadeOut() + slideOutHorizontally { width -> -width / 3 })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) { panel ->
                MangaPanelCard(panel = panel)
            }

            // Barra Inferior com Indicador de Toque
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AbyssalPurple.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderGothic)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (sequencePlayer.isLast(currentIndex)) "Toque para explorar a rua" else "Toque na tela para o próximo quadro",
                            color = VioletLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = VioletLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaPanelCard(panel: MangaPanel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(2.dp, panel.accentColor.copy(alpha = 0.7f)),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Título do Painel no topo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = panel.title,
                    color = panel.accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Surface(
                    color = panel.accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "MANGA CUTSCENE",
                        color = panel.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Ilustração do Painel Manga (Compose Artística)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianDark)
                    .border(1.dp, BorderGothic, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                MangaArtwork(visualType = panel.visualType, accent = panel.accentColor)
            }

            // Texto Narrativo do Quadro
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceGothic.copy(alpha = 0.9f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = panel.narrativeText,
                    color = PaleText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )

                if (panel.dialogueQuote != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AbyssalPurple)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${panel.dialogueSpeaker ?: "Edgar"}: ${panel.dialogueQuote}",
                            color = panel.accentColor,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaArtwork(visualType: MangaVisualType, accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (visualType) {
            MangaVisualType.HOSPITAL_ROOM -> {
                // Fundo hospitalar frio
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1A1B), Color(0xFF080D0E))
                    )
                )

                // Linha de batimentos cardíacos (ECG)
                val ecgPath = Path().apply {
                    moveTo(0f, height * 0.45f)
                    lineTo(width * 0.25f, height * 0.45f)
                    lineTo(width * 0.30f, height * 0.35f)
                    lineTo(width * 0.35f, height * 0.60f)
                    lineTo(width * 0.40f, height * 0.25f)
                    lineTo(width * 0.45f, height * 0.52f)
                    lineTo(width * 0.50f, height * 0.45f)
                    lineTo(width, height * 0.45f)
                }
                drawPath(
                    path = ecgPath,
                    color = AllanColor.copy(alpha = 0.6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )

                // Silhueta do leito hospitalar
                drawRoundRect(
                    color = Color(0xFF1E2D30),
                    topLeft = Offset(width * 0.15f, height * 0.65f),
                    size = androidx.compose.ui.geometry.Size(width * 0.70f, height * 0.20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                // Luz do monitor
                drawCircle(
                    color = AllanColor.copy(alpha = 0.15f),
                    radius = width * 0.25f,
                    center = Offset(width * 0.75f, height * 0.35f)
                )
            }

            MangaVisualType.HOSPITAL_CORRIDOR -> {
                // Perspectiva linear de corredor hospitalar
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF171B26), Color(0xFF0D0F16))
                    )
                )

                // Linhas de fuga do teto e chão
                drawLine(
                    color = EdgarColor.copy(alpha = 0.3f),
                    start = Offset(0f, 0f),
                    end = Offset(width * 0.5f, height * 0.45f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = EdgarColor.copy(alpha = 0.3f),
                    start = Offset(width, 0f),
                    end = Offset(width * 0.5f, height * 0.45f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = EdgarColor.copy(alpha = 0.4f),
                    start = Offset(0f, height),
                    end = Offset(width * 0.5f, height * 0.55f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = EdgarColor.copy(alpha = 0.4f),
                    start = Offset(width, height),
                    end = Offset(width * 0.5f, height * 0.55f),
                    strokeWidth = 3f
                )

                // Luzes do teto
                for (i in 1..4) {
                    val y = height * (0.05f + i * 0.08f)
                    val w = width * (0.35f - i * 0.06f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.3f),
                        topLeft = Offset((width - w) / 2f, y),
                        size = androidx.compose.ui.geometry.Size(w, 8f)
                    )
                }

                // Silhueta de Edgar caminhando ao fundo
                drawCircle(
                    color = Color.Black,
                    radius = 18f,
                    center = Offset(width * 0.5f, height * 0.50f)
                )
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(width * 0.46f, height * 0.52f),
                    size = androidx.compose.ui.geometry.Size(width * 0.08f, height * 0.12f)
                )
            }

            MangaVisualType.RAINY_CITY -> {
                // Metrópole noturna chuvosa
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B0914), Color(0xFF160E28))
                    )
                )

                // Silhuetas de prédios no fundo
                drawRect(
                    color = Color(0xFF1F1638),
                    topLeft = Offset(width * 0.05f, height * 0.35f),
                    size = androidx.compose.ui.geometry.Size(width * 0.25f, height * 0.65f)
                )
                drawRect(
                    color = Color(0xFF18102E),
                    topLeft = Offset(width * 0.35f, height * 0.25f),
                    size = androidx.compose.ui.geometry.Size(width * 0.30f, height * 0.75f)
                )
                drawRect(
                    color = Color(0xFF231940),
                    topLeft = Offset(width * 0.70f, height * 0.40f),
                    size = androidx.compose.ui.geometry.Size(width * 0.25f, height * 0.60f)
                )

                // Linhas diagonais de chuva cortando a cena
                for (i in 0..40) {
                    val x = (i * 27) % width.toInt()
                    val y = ((i * 43) % height.toInt()).toFloat()
                    drawLine(
                        color = Color(0xFFA5B4FC).copy(alpha = 0.35f),
                        start = Offset(x.toFloat(), y),
                        end = Offset(x.toFloat() - 15f, y + 35f),
                        strokeWidth = 1.5f
                    )
                }

                // Asfalto molhado brilhante
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, VioletAccent.copy(alpha = 0.25f))
                    ),
                    topLeft = Offset(0f, height * 0.75f),
                    size = androidx.compose.ui.geometry.Size(width, height * 0.25f)
                )
            }

            MangaVisualType.CLUB_EXTERIOR -> {
                // Entrada do Clube com letreiro neon
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ObsidianDark, AbyssalPurple)
                    )
                )

                // Brilho do neon do clube
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VioletLight.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    center = Offset(width * 0.5f, height * 0.35f),
                    radius = width * 0.45f
                )

                // Porta pesada de ferro
                drawRoundRect(
                    color = Color(0xFF231B38),
                    topLeft = Offset(width * 0.32f, height * 0.45f),
                    size = androidx.compose.ui.geometry.Size(width * 0.36f, height * 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )

                // Faixa de luz violeta sob a porta
                drawRect(
                    color = VioletLight,
                    topLeft = Offset(width * 0.32f, height - 10f),
                    size = androidx.compose.ui.geometry.Size(width * 0.36f, 6f)
                )
            }
        }
    }
}
