package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.LocalIndication
import com.example.ui.theme.PressDarkenIndication
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.UpdatesDialog
import com.example.ui.screens.ExplorationScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.MangaCutsceneScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.RoutesGalleryScreen
import com.example.ui.screens.BootSequenceScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

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
                    onNewGame = { viewModel.startNewGame() },
                    onContinueGame = { state -> viewModel.continueGame(state) },
                    onOpenGallery = { viewModel.navigateTo(AppScreen.GALLERY) },
                    onOpenUpdates = { viewModel.setUpdateDialog(true) },
                    modifier = contentModifier
                )
            }

            AppScreen.MANGA_CUTSCENE -> {
                MangaCutsceneScreen(
                    onFinishCutscene = { viewModel.finishMangaCutscene() },
                    modifier = contentModifier
                )
            }

            AppScreen.EXPLORATION -> {
                ExplorationScreen(
                    onEnterClub = { viewModel.enterClubFromStreet() },
                    onSaveGame = { viewModel.manualSave() },
                    onRestartChapter = { viewModel.restartChapter() },
                    onGoToTitle = { viewModel.navigateTo(AppScreen.TITLE) },
                    modifier = contentModifier
                )
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
}
