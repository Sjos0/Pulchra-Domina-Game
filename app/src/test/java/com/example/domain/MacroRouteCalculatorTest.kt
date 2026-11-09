package com.example.domain

import com.example.data.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroRouteCalculatorTest {

    @Test
    fun lowScore_isSpeedrunRoute3f() {
        val state = GameState(pont_cap = 1, confianca_maveth = 0, pegou_flor = false)
        assertEquals("3f", MacroRouteCalculator.calculateMacroRoute(state).code)
    }

    @Test
    fun idealPath_isRoute3a() {
        val state = GameState(pont_cap = 8, confianca_maveth = 3, pegou_flor = true)
        assertEquals("3a", MacroRouteCalculator.calculateMacroRoute(state).code)
    }

    @Test
    fun flowerWithoutHighScore_isRoute3b() {
        val state = GameState(pont_cap = 4, confianca_maveth = 1, pegou_flor = true)
        assertEquals("3b", MacroRouteCalculator.calculateMacroRoute(state).code)
    }

    @Test
    fun noFlowerHighTrust_isRoute3c() {
        val state = GameState(pont_cap = 5, confianca_maveth = 2, pegou_flor = false)
        assertEquals("3c", MacroRouteCalculator.calculateMacroRoute(state).code)
    }
}
