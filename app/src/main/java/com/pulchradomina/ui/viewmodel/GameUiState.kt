package com.pulchradomina.ui.viewmodel

import com.pulchradomina.data.model.GameState
import com.pulchradomina.domain.story.ChapterOneStoryRepository
import com.pulchradomina.domain.story.MacroRouteInfo
import com.pulchradomina.domain.story.StoryNode

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
