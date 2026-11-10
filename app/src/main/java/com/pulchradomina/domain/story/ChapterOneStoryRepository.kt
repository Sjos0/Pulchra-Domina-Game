package com.pulchradomina.domain.story

import com.pulchradomina.data.model.GameState

/**
 * Implementação do Capítulo 1: compõe conteúdo + calculador de rotas.
 */
object ChapterOneStoryRepository : StoryRepository {
    override val nodes: Map<String, StoryNode>
        get() = ChapterOneNodes.nodes

    override val routes: Map<String, MacroRouteInfo>
        get() = MacroRouteCalculator.ROUTES

    override val startNodeId: String = "inicio"

    override fun getNode(id: String): StoryNode? = ChapterOneNodes.nodes[id]

    override fun requireStartNode(): StoryNode =
        getNode(startNodeId) ?: nodes.values.first()

    override fun calculateMacroRoute(state: GameState): MacroRouteInfo =
        MacroRouteCalculator.calculateMacroRoute(state)
}
