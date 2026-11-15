package com.pulchradomina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulchradomina.ui.boot.BootSequenceScreen
import com.pulchradomina.ui.components.UpdatesDialog
import com.pulchradomina.ui.game.ExplorationScreen
import com.pulchradomina.ui.game.GameScreen
import com.pulchradomina.ui.game.MangaCutsceneScreen
import com.pulchradomina.ui.game.ResultScreen
import com.pulchradomina.ui.game.RoutesGalleryScreen
import com.pulchradomina.ui.theme.BorderGothic
import com.pulchradomina.ui.theme.PaleText
import com.pulchradomina.ui.theme.PressDarkenIndication
import com.pulchradomina.ui.theme.MyApplicationTheme
import com.pulchradomina.ui.theme.SurfaceCard
import com.pulchradomina.ui.theme.VioletLight
import com.pulchradomina.ui.title.TitleScreen
import com.pulchradomina.ui.viewmodel.AppScreen
import com.pulchradomina.ui.viewmodel.GameViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Transição do Novo Jogo: menu → preto → "carregando" → cena. */
private const val NEW_GAME_FADE_OUT_MS = 450
private const val NEW_GAME_FADE_IN_MS = 800
private const val NEW_GAME_MIN_LOADING_MS = 900L
private const val NEW_GAME_LOAD_TIMEOUT_MS = 15_000L

/** Ponte entre a tela da rua (diz quando o cenário está pronto) e a transição. */
private class SceneGate {
    var pending: CompletableDeferred<Unit>? = null
}

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalIndication provides PressDarkenIndication) {
                MyApplicationTheme {
                    PulchraDominaApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PulchraDominaApp(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedGame by viewModel.savedGameFlow.collectAsStateWithLifecycle()
    val unlockedRoutes by viewModel.unlockedRoutesFlow.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val blackout = remember { Animatable(0f) }
    var showLoading by remember { mutableStateOf(false) }
    var transitionBusy by remember { mutableStateOf(false) }
    val sceneGate = remember { SceneGate() }

    fun startNewGameWithTransition() {
        if (transitionBusy) return
        transitionBusy = true
        scope.launch {
            val ready = CompletableDeferred<Unit>()
            sceneGate.pending = ready
            blackout.animateTo(1f, tween(NEW_GAME_FADE_OUT_MS))
            val loadStart = System.currentTimeMillis()
            showLoading = true
            viewModel.startNewGame()
            withTimeoutOrNull(NEW_GAME_LOAD_TIMEOUT_MS) { ready.await() }
            val spent = System.currentTimeMillis() - loadStart
            if (spent < NEW_GAME_MIN_LOADING_MS) delay(NEW_GAME_MIN_LOADING_MS - spent)
            showLoading = false
            blackout.animateTo(0f, tween(NEW_GAME_FADE_IN_MS))
            sceneGate.pending = null
            transitionBusy = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Boot fica fullscreen; demais telas respeitam status/navigation bars
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black
        ) { innerPadding ->
            val contentModifier = if (uiState.currentScreen == AppScreen.BOOT_SEQUENCE) {
                Modifier.fillMaxSize()
            } else {
                Modifier.padding(innerPadding)
            }

            when (uiState.currentScreen) {
                AppScreen.BOOT_SEQUENCE -> {
                    BootSequenceScreen(
                        onFinished = { viewModel.finishBootSequence() },
                        modifier = contentModifier
                    )
                }

                AppScreen.TITLE -> {
                    TitleScreen(
                        savedGame = savedGame,
                        onNewGame = { startNewGameWithTransition() },
                        onContinueGame = { state -> viewModel.continueGame(state) },
                        onOpenGallery = { viewModel.navigateTo(AppScreen.GALLERY) },
                        onOpenUpdates = { viewModel.setUpdateDialog(true) },
                        modifier = contentModifier
                    )
                }

                AppScreen.MANGA_CUTSCENE -> {
                    // Fora do fluxo de Novo Jogo (esboço cancelado); mantido para não quebrar o código.
                    MangaCutsceneScreen(
                        onFinishCutscene = { viewModel.finishMangaCutscene() },
                        modifier = contentModifier
                    )
                }

                AppScreen.EXPLORATION -> {
                    // key(newGameId): novo jogo sempre começa com o Edgar no ponto inicial.
                    key(uiState.newGameId) {
                        ExplorationScreen(
                            onEnterClub = { viewModel.enterClubFromStreet() },
                            onSaveGame = { viewModel.manualSave() },
                            onRestartChapter = { viewModel.restartChapter() },
                            onGoToTitle = { viewModel.navigateTo(AppScreen.TITLE) },
                            onSceneReady = { sceneGate.pending?.complete(Unit) },
                            modifier = contentModifier
                        )
                    }
                }

                AppScreen.GAME -> {
                    GameScreen(
                        gameState = uiState.gameState,
                        currentNode = uiState.currentNode,
                        isMenuOpen = uiState.isMenuOpen,
                        feedbackMessage = uiState.feedbackMessage,
                        onAdvance = { viewModel.advanceLinearNode() },
                        onChoiceSelected = { choice -> viewModel.makeChoice(choice) },
                        onOpenMenu = { viewModel.setMenuOpen(true) },
                        onCloseMenu = { viewModel.setMenuOpen(false) },
                        onSaveGame = { viewModel.manualSave() },
                        onRestartChapter = { viewModel.restartChapter() },
                        onGoToTitle = { viewModel.navigateTo(AppScreen.TITLE) },
                        onDismissFeedback = { viewModel.dismissFeedback() },
                        modifier = contentModifier
                    )
                }

                AppScreen.RESULT -> {
                    val routeInfo = uiState.calculatedRoute
                    if (routeInfo != null) {
                        ResultScreen(
                            gameState = uiState.gameState,
                            routeInfo = routeInfo,
                            onRestart = { viewModel.restartChapter() },
                            onOpenGallery = { viewModel.navigateTo(AppScreen.GALLERY) },
                            onGoToTitle = { viewModel.navigateTo(AppScreen.TITLE) },
                            modifier = contentModifier
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.TITLE)
                    }
                }

                AppScreen.GALLERY -> {
                    RoutesGalleryScreen(
                        unlockedRoutes = unlockedRoutes,
                        onBack = { viewModel.navigateTo(AppScreen.TITLE) },
                        modifier = contentModifier
                    )
                }
            }

            if (uiState.showUpdateDialog) {
                UpdatesDialog(onDismiss = { viewModel.setUpdateDialog(false) })
            }
        }

        if (blackout.value > 0f || showLoading) {
            SceneTransitionOverlay(alpha = blackout.value, showLoading = showLoading)
        }
    }
}

/** Véu preto da transição (bloqueia toques enquanto existe) e o popup de carregamento. */
@Composable
private fun SceneTransitionOverlay(alpha: Float, showLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showLoading) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderGothic)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = VioletLight,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Carregando o jogo…",
                        color = PaleText,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
