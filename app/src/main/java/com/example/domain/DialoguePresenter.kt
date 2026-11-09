package com.example.domain

/**
 * Decisões de apresentação do diálogo VN (sem Compose).
 * A GameScreen permanece “burra”: só desenha com base nestas flags.
 */
object DialoguePresenter {

    fun hasChoices(node: StoryNode): Boolean = node.choices.isNotEmpty()

    fun canAdvanceLinear(node: StoryNode): Boolean =
        node.choices.isEmpty() && node.nextNodeId != null

    fun isChapterEnding(node: StoryNode): Boolean = node.isChapterEnding

    fun visibleChoices(node: StoryNode): List<StoryChoice> = node.choices
}
