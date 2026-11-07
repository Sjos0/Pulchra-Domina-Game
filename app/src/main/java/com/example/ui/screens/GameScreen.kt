package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GameState
import com.example.domain.StoryChoice
import com.example.domain.StoryNode
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun GameScreen(
    gameState: GameState,
    currentNode: StoryNode,
    isMenuOpen: Boolean,
    feedbackMessage: String?,
    onAdvance: () -> Unit,
    onChoiceSelected: (StoryChoice) -> Unit,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onSaveGame: () -> Unit,
    onRestartChapter: () -> Unit,
    onGoToTitle: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercepta o botão voltar nativo para abrir o menu do jogo
    BackHandler {
        onOpenMenu()
    }

    val scrollState = rememberScrollState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            ObsidianDark,
            AbyssalPurple,
            ObsidianDark
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_screen"),
        snackbarHost = {
            if (feedbackMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = SurfaceCard,
                    contentColor = PaleText,
                    action = {
                        TextButton(onClick = onDismissFeedback) {
                            Text("OK", color = VioletLight)
                        }
                    }
                ) {
                    Text(feedbackMessage)
                }
            }
        },
        containerColor = ObsidianDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Barra Superior com Indicadores de Relacionamento e Menu
                RelationshipIndicator(
                    confiancaMaveth = gameState.confianca_maveth,
                    pegouFlor = gameState.pegou_flor,
                    pontCap = gameState.pont_cap,
                    onMenuClick = onOpenMenu
                )

                // Avatar e Identidade do Orador Atual
                CharacterAvatar(
                    character = currentNode.character,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Caixa de Diálogo Principal
                DialogueBox(
                    location = currentNode.location,
                    character = currentNode.character,
                    text = currentNode.text,
                    innerThoughts = currentNode.innerThoughts,
                    hasChoices = currentNode.choices.isNotEmpty(),
                    onAdvance = onAdvance,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Área de Escolhas (se disponíveis)
                AnimatedVisibility(
                    visible = currentNode.choices.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentNode.choices.forEach { choice ->
                            ChoiceButton(
                                choice = choice,
                                onSelect = { onChoiceSelected(choice) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Diálogo de Menu do Jogo
            if (isMenuOpen) {
                GameMenuDialog(
                    onDismiss = onCloseMenu,
                    onSave = onSaveGame,
                    onRestart = onRestartChapter,
                    onGoToTitle = onGoToTitle
                )
            }
        }
    }
}
