package com.pulchradomina.domain.story

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaSequencePlayerTest {

    private val player = MangaSequencePlayer()

    @Test
    fun advance_incrementsUntilLast() {
        val (next, finished) = player.advance(0)
        assertEquals(1, next)
        assertFalse(finished)
    }

    @Test
    fun advance_onLast_signalsFinished() {
        val last = player.size - 1
        val (idx, finished) = player.advance(last)
        assertEquals(last, idx)
        assertTrue(finished)
        assertTrue(player.isLast(last))
    }

    @Test
    fun panelAt_clampsOutOfRange() {
        val first = player.panelAt(-5)
        val last = player.panelAt(999)
        assertEquals(player.panelAt(0).id, first.id)
        assertEquals(player.panelAt(player.size - 1).id, last.id)
    }
}
