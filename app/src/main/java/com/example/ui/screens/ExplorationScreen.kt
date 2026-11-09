package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExplorationRules
import com.example.domain.StreetContent
import com.example.domain.StreetHotspot
import com.example.ui.components.GameMenuDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ExplorationScreen(
    onEnterClub: () -> Unit,
    onSaveGame: () -> Unit,
    onRestartChapter: () -> Unit,
    onGoToTitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    BackHandler {
        isMenuOpen = true
    }

    // Posição / direção — regras em ExplorationRules
    var playerXPercent by remember { mutableFloatStateOf(ExplorationRules.START_X) }
    var isFacingRight by remember { mutableStateOf(true) }
    var activeHotspotDialogue by remember { mutableStateOf<StreetHotspot?>(null) }

    val leftInteractionSource = remember { MutableInteractionSource() }
    val rightInteractionSource = remember { MutableInteractionSource() }
    val isLeftPressed by leftInteractionSource.collectIsPressedAsState()
    val isRightPressed by rightInteractionSource.collectIsPressedAsState()

    LaunchedEffect(isLeftPressed, isRightPressed) {
        while (isLeftPressed || isRightPressed) {
            if (isLeftPressed) {
                val (x, facing) = ExplorationRules.stepLeft(playerXPercent)
                playerXPercent = x
                isFacingRight = facing
            } else if (isRightPressed) {
                val (x, facing) = ExplorationRules.stepRight(playerXPercent)
                playerXPercent = x
                isFacingRight = facing
            }
            delay(ExplorationRules.MOVE_TICK_MS)
        }
    }

    val nearbyHotspot = remember(playerXPercent) {
        ExplorationRules.nearbyHotspot(playerXPercent)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("exploration_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra Superior de Status da Exploração
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AbyssalPurple.copy(alpha = 0.95f),
                border = BorderStroke(0.dp, BorderGothic)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AllanColor)
                            )
                            Text(
                                text = "Rua do Nocturne • 23:48",
                                color = PaleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Modo Exploração 2D (Chuva constante)",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exploration_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu de Pausa",
                            tint = PaleText
                        )
                    }
                }
            }

            // Cenário 2D Side-Scrolling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaX = dragAmount.x / 10f
                            val (x, facing) = ExplorationRules.applyDrag(
                                playerXPercent,
                                isFacingRight,
                                deltaX
                            )
                            playerXPercent = x
                            isFacingRight = facing
                        }
                    }
            ) {
                // Renderização Gráfica do Cenário e Personagem
                Street2DCanvas(
                    playerXPercent = playerXPercent,
                    isFacingRight = isFacingRight
                )

                // Balão de Hotspot próximo sobre a cabeça do personagem
                if (nearbyHotspot != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 140.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        val fraction = ExplorationRules.canvasFraction(playerXPercent)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .wrapContentWidth(Alignment.End)
                                .testTag("hotspot_bubble")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (nearbyHotspot.isClubDoor) VioletAccent else SurfaceCard,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (nearbyHotspot.isClubDoor) VioletLight else BorderGothic
                                ),
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable {
                                        if (nearbyHotspot.isClubDoor) {
                                            onEnterClub()
                                        } else {
                                            activeHotspotDialogue = nearbyHotspot
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (nearbyHotspot.isClubDoor) Icons.Default.Nightlife else Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (nearbyHotspot.isClubDoor) Color.White else VioletLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${nearbyHotspot.promptAction} [Toque]",
                                        color = if (nearbyHotspot.isClubDoor) Color.White else PaleText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Diálogo de Interação da Rua (quando examina ponto ou poça)
            AnimatedVisibility(
                visible = activeHotspotDialogue != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val spot = activeHotspotDialogue
                if (spot != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("street_dialogue_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.2.dp, BorderGothic),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = spot.title,
                                    color = VioletLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { activeHotspotDialogue = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar",
                                        tint = MutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = spot.description,
                                color = PaleText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AbyssalPurple)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = spot.quote,
                                    color = SilverMist,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }

            // Painel de Controles 2D na Parte Inferior (← → e Interagir)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AbyssalPurple.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, BorderGothic)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Controles de Movimentação Lateral
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Botão Andar Esquerda
                        OutlinedButton(
                            onClick = {
                                playerXPercent = (playerXPercent - 5f).coerceIn(10f, 90f)
                                isFacingRight = false
                            },
                            interactionSource = leftInteractionSource,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("btn_walk_left"),
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isLeftPressed) VioletAccent else SurfaceGothic,
                                contentColor = PaleText
                            ),
                            border = BorderStroke(1.5.dp, if (isLeftPressed) VioletLight else BorderGothic),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Andar para Esquerda",
                                tint = if (isLeftPressed) Color.White else PaleText,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Botão Andar Direita
                        OutlinedButton(
                            onClick = {
                                playerXPercent = (playerXPercent + 5f).coerceIn(10f, 90f)
                                isFacingRight = true
                            },
                            interactionSource = rightInteractionSource,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("btn_walk_right"),
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isRightPressed) VioletAccent else SurfaceGothic,
                                contentColor = PaleText
                            ),
                            border = BorderStroke(1.5.dp, if (isRightPressed) VioletLight else BorderGothic),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Andar para Direita",
                                tint = if (isRightPressed) Color.White else PaleText,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Botão Central de Ação / Interagir
                    Button(
                        onClick = {
                            if (nearbyHotspot != null) {
                                if (nearbyHotspot.isClubDoor) {
                                    onEnterClub()
                                } else {
                                    activeHotspotDialogue = nearbyHotspot
                                }
                            }
                        },
                        enabled = nearbyHotspot != null,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("btn_interact_action"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (nearbyHotspot?.isClubDoor == true) VioletAccent else SurfaceCard,
                            disabledContainerColor = SurfaceGothic.copy(alpha = 0.5f),
                            contentColor = Color.White,
                            disabledContentColor = MutedText
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (nearbyHotspot != null) VioletLight else BorderGothic
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (nearbyHotspot?.isClubDoor == true) Icons.Default.Nightlife else Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = nearbyHotspot?.promptAction ?: "Explorar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Diálogo do Menu de Pausa
        if (isMenuOpen) {
            GameMenuDialog(
                onDismiss = { isMenuOpen = false },
                onSave = {
                    onSaveGame()
                    isMenuOpen = false
                },
                onRestart = {
                    onRestartChapter()
                    isMenuOpen = false
                },
                onGoToTitle = {
                    isMenuOpen = false
                    onGoToTitle()
                }
            )
        }
    }
}

@Composable
private fun Street2DCanvas(
    playerXPercent: Float,
    isFacingRight: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Céu Noturno e Neblina Roxa
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF07050C),
                    Color(0xFF130E22),
                    Color(0xFF1E1436)
                )
            )
        )

        // 2. Silhueta dos Prédios de Fundo (Parallax)
        val buildingY = height * 0.30f
        drawRect(
            color = Color(0xFF140F24),
            topLeft = Offset(0f, buildingY),
            size = androidx.compose.ui.geometry.Size(width * 0.28f, height * 0.40f)
        )
        drawRect(
            color = Color(0xFF18122C),
            topLeft = Offset(width * 0.30f, buildingY * 0.85f),
            size = androidx.compose.ui.geometry.Size(width * 0.35f, height * 0.50f)
        )
        drawRect(
            color = Color(0xFF100B1E),
            topLeft = Offset(width * 0.68f, buildingY * 0.75f),
            size = androidx.compose.ui.geometry.Size(width * 0.32f, height * 0.55f)
        )

        // Janelas acesas distantes
        for (row in 0..5) {
            for (col in 0..3) {
                if ((row + col) % 3 == 0) {
                    drawRect(
                        color = Color(0xFFFFD166).copy(alpha = 0.3f),
                        topLeft = Offset(width * 0.35f + col * 26f, buildingY + 20f + row * 24f),
                        size = androidx.compose.ui.geometry.Size(12f, 16f)
                    )
                }
            }
        }

        // 3. Fachada do Clube Nocturne (Lado direito, ~80% a 95% do cenário)
        val clubX = width * 0.75f
        val clubY = height * 0.22f
        drawRect(
            color = Color(0xFF201735),
            topLeft = Offset(clubX, clubY),
            size = androidx.compose.ui.geometry.Size(width * 0.25f, height * 0.50f)
        )

        // Letreiro Neon "NOCTURNE"
        drawRoundRect(
            color = VioletAccent,
            topLeft = Offset(clubX + 10f, clubY + 30f),
            size = androidx.compose.ui.geometry.Size(width * 0.20f, 32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        // Brilho do Neon no ar
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VioletLight.copy(alpha = 0.35f), Color.Transparent)
            ),
            center = Offset(clubX + width * 0.10f, clubY + 46f),
            radius = width * 0.20f
        )

        // Porta do Clube
        drawRoundRect(
            color = Color(0xFF120C1F),
            topLeft = Offset(clubX + 25f, height * 0.44f),
            size = androidx.compose.ui.geometry.Size(width * 0.14f, height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )
        // Luz vazando por baixo da porta do clube
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(VioletLight, Color.Transparent)
            ),
            topLeft = Offset(clubX + 25f, height * 0.70f),
            size = androidx.compose.ui.geometry.Size(width * 0.14f, 25f)
        )

        // 4. Ponto de Ônibus (Lado esquerdo, ~18% do cenário)
        val busX = width * 0.16f
        drawLine(
            color = Color(0xFF6B7280),
            start = Offset(busX, height * 0.42f),
            end = Offset(busX, height * 0.70f),
            strokeWidth = 6f
        )
        drawRoundRect(
            color = Color(0xFF374151),
            topLeft = Offset(busX - 25f, height * 0.40f),
            size = androidx.compose.ui.geometry.Size(50f, 35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        // 5. Poste de Luz da Rua (Central)
        val lampX = width * 0.45f
        drawLine(
            color = Color(0xFF4B5563),
            start = Offset(lampX, height * 0.28f),
            end = Offset(lampX, height * 0.70f),
            strokeWidth = 7f
        )
        // Cone de Luz do poste
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB).copy(alpha = 0.22f), Color.Transparent)
            ),
            center = Offset(lampX, height * 0.30f),
            radius = width * 0.22f
        )

        // 6. Calçada e Asfalto Molhado
        val sidewalkY = height * 0.70f
        // Guia da calçada
        drawRect(
            color = Color(0xFF282438),
            topLeft = Offset(0f, sidewalkY),
            size = androidx.compose.ui.geometry.Size(width, 18f)
        )
        // Asfalto molhado inferior
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF141120), Color(0xFF09070E))
            ),
            topLeft = Offset(0f, sidewalkY + 18f),
            size = androidx.compose.ui.geometry.Size(width, height - (sidewalkY + 18f))
        )

        // Poça de Chuva com reflexo neon centralizada (~50%)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(VioletAccent.copy(alpha = 0.35f), Color.Transparent)
            ),
            topLeft = Offset(width * 0.42f, sidewalkY + 25f),
            size = androidx.compose.ui.geometry.Size(width * 0.22f, 28f)
        )

        // 7. Chuva Caindo
        for (i in 0..50) {
            val rx = (i * 37) % width.toInt()
            val ry = ((i * 53) % height.toInt()).toFloat()
            drawLine(
                color = Color(0xFFA5B4FC).copy(alpha = 0.30f),
                start = Offset(rx.toFloat(), ry),
                end = Offset(rx.toFloat() - 8f, ry + 22f),
                strokeWidth = 1.2f
            )
        }

        // 8. Personagem Edgar (Silhueta estilizada com sobretudo e cachecol)
        val playerX = width * (playerXPercent / 100f)
        val playerY = sidewalkY - 8f // Pé na calçada

        // Sombra sob os pés
        drawOval(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = Offset(playerX - 22f, playerY - 4f),
            size = androidx.compose.ui.geometry.Size(44f, 14f)
        )

        // Corpo / Sobretudo escuro
        val coatWidth = 26f
        val coatHeight = 55f
        drawRoundRect(
            color = Color(0xFF1F2937),
            topLeft = Offset(playerX - coatWidth / 2f, playerY - coatHeight),
            size = androidx.compose.ui.geometry.Size(coatWidth, coatHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        // Cachecol acinzentado de Edgar
        drawRoundRect(
            color = EdgarColor,
            topLeft = Offset(playerX - coatWidth / 2f - 2f, playerY - coatHeight),
            size = androidx.compose.ui.geometry.Size(coatWidth + 4f, 14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // Cabeça / Cabelos escuros
        drawCircle(
            color = Color(0xFF111827),
            radius = 14f,
            center = Offset(playerX, playerY - coatHeight - 12f)
        )

        // Rosto (perfil olhando para esquerda ou direita)
        val faceOffset = if (isFacingRight) 4f else -4f
        drawCircle(
            color = Color(0xFFE5E7EB),
            radius = 9f,
            center = Offset(playerX + faceOffset, playerY - coatHeight - 12f)
        )
        // Olhar melancólico
        drawCircle(
            color = EdgarColor,
            radius = 2f,
            center = Offset(playerX + faceOffset + (if (isFacingRight) 3f else -3f), playerY - coatHeight - 12f)
        )
    }
}
