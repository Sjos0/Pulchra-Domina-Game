package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SaveManager
import com.example.data.model.GameState
import com.example.domain.ChapterOneStoryRepository
import com.example.domain.StoryChoice
import com.example.domain.StoryNode
import com.example.domain.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orquestra sessão de jogo + navegação.
 * Regras de história ficam em [StoryRepository]; persistência em [SaveManager].
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val saveManager = SaveManager(application.applicationContext)
    private val story: StoryRepository = ChapterOneStoryRepository

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val savedGameFlow = saveManager.gameStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val unlockedRoutesFlow = saveManager.unlockedRoutesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun startNewGame() {
        val initialState = GameState(
            pont_cap = 0,
            pont_total = 0,
            confianca_maveth = 0,
            pegou_flor = false,
            escolhas_feitas = emptyList(),
            capitulo_atual = "capitulo1",
            nodeId = story.startNodeId
        )
        val initialNode = story.requireStartNode()
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.MANGA_CUTSCENE,
            gameState = initialState,
            currentNode = initialNode,
            calculatedRoute = null,
            isMenuOpen = false,
            feedbackMessage = null
        )
        saveCurrentProgress(initialState)
    }

    fun finishMangaCutscene() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.EXPLORATION)
    }

    fun enterClubFromStreet() {
        val initialNode = story.requireStartNode()
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            currentNode = initialNode,
            feedbackMessage = "Você entrou no Clube Nocturne."
        )
        saveCurrentProgress(_uiState.value.gameState)
    }

    fun continueGame(savedState: GameState) {
        val node = story.getNode(savedState.nodeId) ?: story.requireStartNode()
        val isEnding = node.isChapterEnding
        val route = if (isEnding) story.calculateMacroRoute(savedState) else null

        _uiState.value = _uiState.value.copy(
            currentScreen = if (isEnding) AppScreen.RESULT else AppScreen.GAME,
            gameState = savedState,
            currentNode = node,
            calculatedRoute = route,
            isMenuOpen = false,
            feedbackMessage = "Progresso restaurado com sucesso"
        )
    }

    fun advanceLinearNode() {
        val nextId = _uiState.value.currentNode.nextNodeId ?: return
        val nextNode = story.getNode(nextId) ?: return
        val updatedState = _uiState.value.gameState.copy(nodeId = nextId)

        if (nextNode.isChapterEnding) {
            finishChapter(updatedState, nextNode)
        } else {
            _uiState.value = _uiState.value.copy(
                gameState = updatedState,
                currentNode = nextNode
            )
            saveCurrentProgress(updatedState)
        }
    }

    fun makeChoice(choice: StoryChoice) {
        val current = _uiState.value.gameState
        val newFlor = choice.setsPegouFlor ?: current.pegou_flor
        val newPont = current.pont_cap + choice.pontCapDelta
        val newConf = current.confianca_maveth + choice.confiancaMavethDelta
        val updatedChoices = current.escolhas_feitas + listOfNotNull(choice.flag ?: choice.id)

        val nextNode = story.getNode(choice.nextNodeId) ?: return

        val updatedState = current.copy(
            pont_cap = newPont,
            pont_total = current.pont_total + choice.pontCapDelta,
            confianca_maveth = newConf,
            pegou_flor = newFlor,
            escolhas_feitas = updatedChoices,
            nodeId = choice.nextNodeId
        )

        if (nextNode.isChapterEnding) {
            finishChapter(updatedState, nextNode)
        } else {
            _uiState.value = _uiState.value.copy(
                gameState = updatedState,
                currentNode = nextNode,
                feedbackMessage = choiceFeedback(choice)
            )
            saveCurrentProgress(updatedState)
        }
    }

    private fun choiceFeedback(choice: StoryChoice): String? {
        val parts = mutableListOf<String>()
        if (choice.pontCapDelta != 0) {
            val sign = if (choice.pontCapDelta > 0) "+" else ""
            parts.add("Afinidade $sign${choice.pontCapDelta}")
        }
        if (choice.confiancaMavethDelta != 0) {
            val sign = if (choice.confiancaMavethDelta > 0) "+" else ""
            parts.add("Confiança Maveth $sign${choice.confiancaMavethDelta}")
        }
        if (choice.setsPegouFlor == true) parts.add("Você pegou a hortênsia negra")
        if (choice.setsPegouFlor == false) parts.add("Você recusou a flor")
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun finishChapter(state: GameState, node: StoryNode) {
        val macroRoute = story.calculateMacroRoute(state)
        _uiState.value = _uiState.value.copy(
            gameState = state,
            currentNode = node,
            calculatedRoute = macroRoute,
            currentScreen = AppScreen.RESULT,
            isMenuOpen = false
        )
        viewModelScope.launch {
            saveManager.saveGame(state)
            saveManager.saveUnlockedRoute(macroRoute.code)
        }
    }

    fun manualSave() {
        saveCurrentProgress(_uiState.value.gameState)
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
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.TITLE)
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

    private fun saveCurrentProgress(state: GameState) {
        viewModelScope.launch {
            saveManager.saveGame(state)
        }
    }
}
