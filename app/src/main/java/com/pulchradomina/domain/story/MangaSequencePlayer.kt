package com.pulchradomina.domain.story

/**
 * Controle da sequência de painéis manga (sem UI).
 */
class MangaSequencePlayer(
    private val panels: List<MangaPanel> = MangaContent.prologueSequence
) {
    val size: Int get() = panels.size
    val panelList: List<MangaPanel> get() = panels

    fun panelAt(index: Int): MangaPanel = panels[index.coerceIn(0, panels.lastIndex)]

    fun isLast(index: Int): Boolean = index >= panels.lastIndex

    /**
     * @return Pair(novo índice, terminou a sequência)
     */
    fun advance(index: Int): Pair<Int, Boolean> {
        return if (index < panels.lastIndex) {
            (index + 1) to false
        } else {
            index to true
        }
    }
}
