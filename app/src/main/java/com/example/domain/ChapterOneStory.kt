package com.example.domain

import com.example.data.model.GameState

/**
 * Fachada de compatibilidade do Capítulo 1.
 * Preferir [StoryRepository] / [ChapterOneStoryRepository] em código novo.
 */
object ChapterOneStory {
    val ROUTES: Map<String, MacroRouteInfo>
        get() = MacroRouteCalculator.ROUTES

    val nodes: Map<String, StoryNode>
        get() = ChapterOneNodes.nodes

    fun calculateMacroRoute(state: GameState): MacroRouteInfo =
        MacroRouteCalculator.calculateMacroRoute(state)

    fun getNode(id: String): StoryNode? = ChapterOneNodes.nodes[id]
}
