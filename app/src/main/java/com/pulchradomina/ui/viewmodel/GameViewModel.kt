package com.pulchradomina.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulchradomina.data.save.SavePort
import com.pulchradomina.data.save.SaveManagerPort
import com.pulchradomina.data.model.GameState
import com.pulchradomina.domain.story.ChapterOneStoryRepository
import com.pulchradomina.domain.story.GameSession
import com.pulchradomina.domain.story.SessionSnapshot
import com.pulchradomina.domain.story.StoryChoice
import com.pulchradomina.domain.story.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orquestra navegação + sessão + persistência.
 * Regras de história: [GameSession] / [StoryRepository].
 * Persistência: [SavePort].
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val save: SavePort = SaveManagerPort(application.applicationContext)
    private val story: StoryRepository = ChapterOneStoryRepository
    private val session = GameSession(story)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val savedGameFlow = save.gameStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val unlockedRoutesFlow = save.unlockedRoutesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun startNewGame() {
        val snap = session.newGame()
        publish(snap, screen = AppScreen.MANGA_CUTSCENE, clearMenu = true)
        persist(snap.gameState)
    }

    fun finishMangaCutscene() {
        navigateTo(AppScreen.EXPLORATION)
    }

    fun enterClubFromStreet() {
        val node = session.enterClubNode()
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            currentNode = node,
            feedbackMessage = "Você entrou no Clube Nocturne."
        )
        persist(_uiState.value.gameState)
    }

    fun continueGame(savedState: GameState) {
        val snap = session.continueFrom(savedState)
        val screen = if (snap.isChapterFinished) AppScreen.RESULT else AppScreen.GAME
        publish(snap, screen = screen, clearMenu = true)
    }

    fun advanceLinearNode() {
        val snap = session.advanceLinear(currentSnapshot()) ?: return
        applySessionResult(snap)
    }

    fun makeChoice(choice: StoryChoice) {
        val snap = session.applyChoice(currentSnapshot(), choice) ?: return
        applySessionResult(snap)
    }

    fun manualSave() {
        persist(_uiState.value.gameState)
        _uiState.value = _uiState.value.copy(feedbackMessage = "Jogo salvo")
    }

    fun restartChapter() {
        startNewGame()
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            isMenuOpen = false
        )
    }

    fun finishBootSequence() {
        navigateTo(AppScreen.TITLE)
    }

    fun setMenuOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isMenuOpen = open)
    }

    fun setUpdateDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showUpdateDialog = show)
    }

    fun dismissFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    private fun currentSnapshot(): SessionSnapshot {
        val s = _uiState.value
        return SessionSnapshot(
            gameState = s.gameState,
            currentNode = s.currentNode,
            calculatedRoute = s.calculatedRoute,
            feedbackMessage = s.feedbackMessage,
            isChapterFinished = s.currentScreen == AppScreen.RESULT
        )
    }

    private fun applySessionResult(snap: SessionSnapshot) {
        if (snap.isChapterFinished) {
            publish(snap, screen = AppScreen.RESULT, clearMenu = true)
            viewModelScope.launch {
                save.saveGame(snap.gameState)
                snap.calculatedRoute?.let { save.unlockRoute(it.code) }
            }
        } else {
            publish(snap, screen = _uiState.value.currentScreen)
            persist(snap.gameState)
        }
    }

    private fun publish(
        snap: SessionSnapshot,
        screen: AppScreen,
        clearMenu: Boolean = false
    ) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            gameState = snap.gameState,
            currentNode = snap.currentNode,
            calculatedRoute = snap.calculatedRoute,
            feedbackMessage = snap.feedbackMessage,
            isMenuOpen = if (clearMenu) false else _uiState.value.isMenuOpen
        )
    }

    private fun persist(state: GameState) {
        viewModelScope.launch {
            save.saveGame(state)
        }
    }
}
