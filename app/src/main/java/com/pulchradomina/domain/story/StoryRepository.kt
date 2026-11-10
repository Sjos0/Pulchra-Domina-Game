package com.pulchradomina.domain.story

import com.pulchradomina.data.model.GameState

/**
 * Porta de acesso à história do capítulo atual.
 * UI e ViewModel dependem desta abstração (Dependency Inversion).
 */
interface StoryRepository {
    val nodes: Map<String, StoryNode>
    val routes: Map<String, MacroRouteInfo>
    val startNodeId: String

    fun getNode(id: String): StoryNode?
    fun requireStartNode(): StoryNode
    fun calculateMacroRoute(state: GameState): MacroRouteInfo
}
