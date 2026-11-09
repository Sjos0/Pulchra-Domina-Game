package com.example.domain

import com.example.data.model.GameState

/**
 * Snapshot de uma partida em andamento (sem UI, sem Android).
 */
data class SessionSnapshot(
    val gameState: GameState,
    val currentNode: StoryNode,
    val calculatedRoute: MacroRouteInfo? = null,
    val feedbackMessage: String? = null,
    val isChapterFinished: Boolean = false
)

/**
 * Regras de sessão do capítulo: novo jogo, continuar, avanço linear, escolha.
 * Responsabilidade única: transformar estado de partida + história.
 * Não persiste e não navega telas.
 */
class GameSession(
    private val story: StoryRepository
) {

    fun newGame(): SessionSnapshot {
        val state = GameState(
            pont_cap = 0,
            pont_total = 0,
            confianca_maveth = 0,
            pegou_flor = false,
            escolhas_feitas = emptyList(),
            capitulo_atual = "capitulo1",
            nodeId = story.startNodeId
        )
        return SessionSnapshot(
            gameState = state,
            currentNode = story.requireStartNode(),
            calculatedRoute = null,
            feedbackMessage = null,
            isChapterFinished = false
        )
    }

    fun continueFrom(savedState: GameState): SessionSnapshot {
        val node = story.getNode(savedState.nodeId) ?: story.requireStartNode()
        val finished = node.isChapterEnding
        val route = if (finished) story.calculateMacroRoute(savedState) else null
        return SessionSnapshot(
            gameState = savedState,
            currentNode = node,
            calculatedRoute = route,
            feedbackMessage = "Progresso restaurado com sucesso",
            isChapterFinished = finished
        )
    }

    fun enterClubNode(): StoryNode = story.requireStartNode()

    /**
     * Avança nó linear. null se não houver próximo.
     */
    fun advanceLinear(current: SessionSnapshot): SessionSnapshot? {
        val nextId = current.currentNode.nextNodeId ?: return null
        val nextNode = story.getNode(nextId) ?: return null
        val updatedState = current.gameState.copy(nodeId = nextId)
        return if (nextNode.isChapterEnding) {
            finishChapter(updatedState, nextNode)
        } else {
            SessionSnapshot(
                gameState = updatedState,
                currentNode = nextNode,
                calculatedRoute = null,
                feedbackMessage = null,
                isChapterFinished = false
            )
        }
    }

    fun applyChoice(current: SessionSnapshot, choice: StoryChoice): SessionSnapshot? {
        val state = current.gameState
        val newFlor = choice.setsPegouFlor ?: state.pegou_flor
        val newPont = state.pont_cap + choice.pontCapDelta
        val newConf = state.confianca_maveth + choice.confiancaMavethDelta
        val updatedChoices = state.escolhas_feitas + listOfNotNull(choice.flag ?: choice.id)
        val nextNode = story.getNode(choice.nextNodeId) ?: return null

        val updatedState = state.copy(
            pont_cap = newPont,
            pont_total = state.pont_total + choice.pontCapDelta,
            confianca_maveth = newConf,
            pegou_flor = newFlor,
            escolhas_feitas = updatedChoices,
            nodeId = choice.nextNodeId
        )

        return if (nextNode.isChapterEnding) {
            finishChapter(updatedState, nextNode)
        } else {
            SessionSnapshot(
                gameState = updatedState,
                currentNode = nextNode,
                calculatedRoute = null,
                feedbackMessage = choiceFeedback(choice),
                isChapterFinished = false
            )
        }
    }

    fun finishChapter(state: GameState, node: StoryNode): SessionSnapshot {
        val route = story.calculateMacroRoute(state)
        return SessionSnapshot(
            gameState = state.copy(macroRotaFinal = route.code),
            currentNode = node,
            calculatedRoute = route,
            feedbackMessage = null,
            isChapterFinished = true
        )
    }

    fun choiceFeedback(choice: StoryChoice): String? {
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
}
