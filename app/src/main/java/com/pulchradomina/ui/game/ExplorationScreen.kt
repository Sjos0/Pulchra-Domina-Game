package com.pulchradomina.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulchradomina.domain.exploration.ExplorationRules
import com.pulchradomina.domain.exploration.StreetContent
import com.pulchradomina.domain.exploration.StreetHotspot
import com.pulchradomina.ui.components.GameMenuDialog
import com.pulchradomina.ui.game.night.NightStreetAssets
import com.pulchradomina.ui.game.night.NightStreetBitmaps
import com.pulchradomina.ui.game.night.NightStreetScene
import com.pulchradomina.ui.game.night.StreetCamera
import com.pulchradomina.ui.game.opening.EdgarOpeningController
import com.pulchradomina.ui.game.opening.EdgarThoughtOverlay
import com.pulchradomina.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Chrome legado da exploração (barra de horário/rua, botão Explorar, balões e diálogo de hotspot).
 * Fora do fluxo visível até ser reformulado. O código continua aqui, só não é composto.
 */
private const val SHOW_LEGACY_EXPLORATION_CHROME = false

/** Estado de carregamento do cenário noturno. */
private sealed interface SceneLoad {
    data object Loading : SceneLoad
    data class Ready(val bitmaps: NightStreetBitmaps) : SceneLoad
    data class Failed(val message: String) : SceneLoad
}

@Composable
fun ExplorationScreen(
    onEnterClub: () -> Unit,
    onSaveGame: () -> Unit,
    onRestartChapter: () -> Unit,
    onGoToTitle: () -> Unit,
    onSceneReady: () -> Unit,
    /** Monólogo de abertura pendente (novo jogo): bloqueia caminhada e controles até terminar. */
    edgarOpeningPending: Boolean = false,
    /** Libera o monólogo depois da transição (fade) para a cena aparecer antes da fala. */
    introAllowed: Boolean = true,
    onEdgarOpeningFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMenuOpen by remember { mutableStateOf(false) }

    BackHandler {
        isMenuOpen = true
    }

    // Assets decodificados em IO. Sucesso ou falha, a tela avisa que está pronta
    // (para o fade de entrada) e a falha aparece na própria cena para depuração.
    val sceneLoad by produceState<SceneLoad>(
        initialValue = NightStreetAssets.cachedOrNull()?.let { SceneLoad.Ready(it) } ?: SceneLoad.Loading
    ) {
        if (value is SceneLoad.Loading) {
            value = try {
                SceneLoad.Ready(NightStreetAssets.load(context))
            } catch (e: Exception) {
                SceneLoad.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }
    LaunchedEffect(sceneLoad) {
        if (sceneLoad !is SceneLoad.Loading) onSceneReady()
    }

    // Posição em % do mundo (3 telas) — regras em ExplorationRules
    var playerXPercent by remember { mutableFloatStateOf(ExplorationRules.START_X) }
    var isFacingRight by remember { mutableStateOf(true) }
    var activeHotspotDialogue by remember { mutableStateOf<StreetHotspot?>(null) }

    val leftInteractionSource = remember { MutableInteractionSource() }
    val rightInteractionSource = remember { MutableInteractionSource() }
    val isLeftPressed by leftInteractionSource.collectIsPressedAsState()
    val isRightPressed by rightInteractionSource.collectIsPressedAsState()

    // Monólogo de abertura: a caminhada fica bloqueada enquanto ele roda.
    val controlsLocked by rememberUpdatedState(edgarOpeningPending)
    val currentOnFinished by rememberUpdatedState(onEdgarOpeningFinished)
    val openingController = remember {
        EdgarOpeningController(context, onFinished = { currentOnFinished() })
    }
    LaunchedEffect(edgarOpeningPending, introAllowed) {
        if (edgarOpeningPending && introAllowed) openingController.run()
    }
    DisposableEffect(Unit) {
        onDispose { openingController.release() }
    }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (edgarOpeningPending) 0f else 1f,
        animationSpec = tween(450),
        label = "controlsAlpha"
    )

    LaunchedEffect(isLeftPressed, isRightPressed) {
        while (!controlsLocked && (isLeftPressed || isRightPressed)) {
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
            // Barra superior de status (legado, fora do fluxo)
            if (SHOW_LEGACY_EXPLORATION_CHROME) {
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
                            text = "Modo Exploração 2D",
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
            }

            // Cena noturna com câmera que segue o Edgar
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (!controlsLocked) {
                                val (x, facing) = ExplorationRules.applyDrag(
                                    playerXPercent,
                                    isFacingRight,
                                    dragAmount.x * ExplorationRules.DRAG_PERCENT_PER_PX
                                )
                                playerXPercent = x
                                isFacingRight = facing
                            }
                        }
                    }
            ) {
                val worldX = StreetCamera.worldXOf(playerXPercent)
                val viewportWidth = maxWidth.value.coerceAtLeast(1f)
                val view = StreetCamera.view(viewportWidth, maxHeight.value, worldX)

                when (val load = sceneLoad) {
                    is SceneLoad.Ready -> NightStreetScene(
                        bitmaps = load.bitmaps,
                        playerWorldX = worldX,
                        isFacingRight = isFacingRight,
                        // Edgar só aparece quando o monólogo começa (fade-in próprio)
                        edgarAlpha = if (edgarOpeningPending) openingController.edgarAlpha.value else 1f,
                        modifier = Modifier.fillMaxSize()
                    )
                    is SceneLoad.Failed -> Text(
                        text = "Erro ao carregar o cenário:\n${load.message}",
                        color = PaleText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                    SceneLoad.Loading -> Unit
                }

                // Balão de hotspot sobre o personagem (acompanha a posição na tela)
                if (SHOW_LEGACY_EXPLORATION_CHROME && nearbyHotspot != null) {
                    val screenPercent = StreetCamera.screenX(view, worldX) / viewportWidth * 100f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 140.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ExplorationRules.canvasFraction(screenPercent))
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
                if (openingController.active) {
                    EdgarThoughtOverlay(
                        controller = openingController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Diálogo de interação da rua (quando examina ponto ou poça)
            AnimatedVisibility(
                visible = SHOW_LEGACY_EXPLORATION_CHROME && activeHotspotDialogue != null,
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

            // Controles na parte inferior (← → e interagir)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsAlpha },
                color = AbyssalPurple.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, BorderGothic)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = if (SHOW_LEGACY_EXPLORATION_CHROME) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            // Sem efeito no clique: o movimento vem só do hold (LaunchedEffect acima).
                            onClick = { },
                            enabled = !edgarOpeningPending,
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

                        OutlinedButton(
                            onClick = { },
                            enabled = !edgarOpeningPending,
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

                    if (SHOW_LEGACY_EXPLORATION_CHROME) {
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
        }

        // Menu de pausa
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
