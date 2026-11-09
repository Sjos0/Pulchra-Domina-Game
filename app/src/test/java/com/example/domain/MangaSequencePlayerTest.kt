package com.example.domain

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
}
