package com.pulchradomina.domain.story

import com.pulchradomina.data.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameSessionTest {

    private lateinit var session: GameSession

    @Before
    fun setUp() {
        session = GameSession(ChapterOneStoryRepository)
    }

    @Test
    fun newGame_startsAtInicio() {
        val snap = session.newGame()
        assertEquals("inicio", snap.gameState.nodeId)
        assertEquals("inicio", snap.currentNode.id)
        assertFalse(snap.isChapterFinished)
        assertEquals(0, snap.gameState.pont_cap)
    }

    @Test
    fun applyChoice_updatesScoreAndNode() {
        val start = session.newGame()
        // go to node with choices
        val edgar = session.advanceLinear(start)
        assertNotNull(edgar)
        val choice = edgar!!.currentNode.choices.first()
        val after = session.applyChoice(edgar, choice)
        assertNotNull(after)
        assertEquals(choice.nextNodeId, after!!.gameState.nodeId)
        assertEquals(choice.pontCapDelta, after.gameState.pont_cap)
    }

    @Test
    fun continueFrom_endingNode_marksFinished() {
        val ending = ChapterOneStoryRepository.nodes.values.first { it.isChapterEnding }
        val state = GameState(nodeId = ending.id, pont_cap = 8, confianca_maveth = 3, pegou_flor = true)
        val snap = session.continueFrom(state)
        assertTrue(snap.isChapterFinished)
        assertNotNull(snap.calculatedRoute)
        assertEquals("3a", snap.calculatedRoute!!.code)
    }
}
