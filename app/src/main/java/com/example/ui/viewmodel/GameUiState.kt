package com.example.ui.viewmodel

import com.example.data.model.GameState
import com.example.domain.ChapterOneStoryRepository
import com.example.domain.MacroRouteInfo
import com.example.domain.StoryNode

/** Estado imutável da UI do jogo (sem lógica). */
data class GameUiState(
    val currentScreen: AppScreen = AppScreen.BOOT_SEQUENCE,
    val gameState: GameState = GameState(),
    val currentNode: StoryNode = ChapterOneStoryRepository.requireStartNode(),
    val calculatedRoute: MacroRouteInfo? = null,
    val isMenuOpen: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val feedbackMessage: String? = null
)
