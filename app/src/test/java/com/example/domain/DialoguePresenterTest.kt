package com.example.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialoguePresenterTest {

    @Test
    fun startNode_isLinear() {
        val node = ChapterOneStoryRepository.requireStartNode()
        assertFalse(DialoguePresenter.hasChoices(node))
        assertTrue(DialoguePresenter.canAdvanceLinear(node))
    }

    @Test
    fun choiceNode_hasChoices() {
        val node = ChapterOneStoryRepository.getNode("inicio_edgar")!!
        assertTrue(DialoguePresenter.hasChoices(node))
        assertFalse(DialoguePresenter.canAdvanceLinear(node))
    }
}
