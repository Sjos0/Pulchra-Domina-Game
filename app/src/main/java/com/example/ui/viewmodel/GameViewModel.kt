package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SaveManager
import com.example.data.model.GameState
import com.example.domain.ChapterOneStory
import com.example.domain.MacroRouteInfo
import com.example.domain.StoryChoice
import com.example.domain.StoryNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    BOOT_SEQUENCE,
    TITLE,
    MANGA_CUTSCENE,
    EXPLORATION,
    GAME,
    RESULT,
    GALLERY
}

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.BOOT_SEQUENCE,
    val gameState: GameState = GameState(),
    val currentNode: StoryNode = ChapterOneStory.nodes["inicio"]!!,
    val calculatedRoute: MacroRouteInfo? = null,
    val isMenuOpen: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val feedbackMessage: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val saveManager = SaveManager(application.applicationContext)

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
            nodeId = "inicio"
        )
        val initialNode = ChapterOneStory.getNode("inicio") ?: ChapterOneStory.nodes.values.first()
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.MANGA_CUTSCENE, // Novo fluxo: inicia com a cutscene manga colorida
            gameState = initialState,
            currentNode = initialNode,
            calculatedRoute = null,
            isMenuOpen = false,
            feedbackMessage = null
        )
        saveCurrentProgress(initialState)
    }

    fun finishMangaCutscene() {
        // Transição da cutscene manga para a exploração 2D na rua
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.EXPLORATION
        )
    }

    fun enterClubFromStreet() {
        // Transição da rua para o interior do Clube Nocturne (Visual Novel & Escolhas com Maveth)
        val initialNode = ChapterOneStory.getNode("inicio") ?: ChapterOneStory.nodes.values.first()
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            currentNode = initialNode,
            feedbackMessage = "Você entrou no Clube Nocturne."
        )
        saveCurrentProgress(_uiState.value.gameState)
    }

    fun continueGame(savedState: GameState) {
        val node = ChapterOneStory.getNode(savedState.nodeId) ?: ChapterOneStory.getNode("inicio")!!
        val isEnding = node.isChapterEnding
        val route = if (isEnding) ChapterOneStory.calculateMacroRoute(savedState) else null

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
        val nextNode = ChapterOneStory.getNode(nextId) ?: return
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

        val nextNode = ChapterOneStory.getNode(choice.nextNodeId) ?: return

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
                currentNode = nextNode
            )
            saveCurrentProgress(updatedState)
        }
    }

    private fun finishChapter(state: GameState, endingNode: StoryNode) {
        val macroRoute = ChapterOneStory.calculateMacroRoute(state)
        val finalState = state.copy(
            nodeId = endingNode.id,
            macroRotaFinal = macroRoute.code
        )

        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.RESULT,
            gameState = finalState,
            currentNode = endingNode,
            calculatedRoute = macroRoute,
            feedbackMessage = "Capítulo 1 concluído! Rota ${macroRoute.code} desbloqueada."
        )

        viewModelScope.launch {
            saveManager.saveGame(finalState)
            saveManager.saveUnlockedRoute(macroRoute.code)
        }
    }

    private fun saveCurrentProgress(state: GameState) {
        viewModelScope.launch {
            saveManager.saveGame(state)
        }
    }

    fun manualSave() {
        saveCurrentProgress(_uiState.value.gameState)
        _uiState.value = _uiState.value.copy(
            feedbackMessage = "Jogo salvo com sucesso!"
        )
    }

    fun restartChapter() {
        startNewGame()
    }

    fun setMenuOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isMenuOpen = open)
    }

    fun finishBootSequence() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.TITLE)
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            isMenuOpen = false
        )
    }

    fun setUpdateDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showUpdateDialog = show)
    }

    fun dismissFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
