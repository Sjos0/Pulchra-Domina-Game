package com.pulchradomina.ui.game.night

import com.pulchradomina.domain.exploration.ExplorationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WalkCycleTest {

    private val frameNanos = 16_666_667L
    private val speed = 282.5f // unidades/s (≈ STEP_PER_TICK do jogo)
    private val cycle get() = WalkCycleLayout.CYCLE_UNITS

    /** Anda [units] unidades a [speed], quadro a quadro, e devolve o total de passos. */
    private fun walk(c: WalkCycleController, startX: Float, units: Float, startNanos: Long = 0L): Pair<Int, Float> {
        var x = startX
        var t = startNanos
        var steps = 0
        val perFrame = speed * frameNanos / 1e9f
        var moved = 0f
        while (moved < units) {
            x += perFrame
            moved += perFrame
            t += frameNanos
            steps += c.update(t, x, allowSteps = true)
        }
        return steps to x
    }

    @Test
    fun twoCycles_giveFourFootsteps_twoPerCycle() {
        val c = WalkCycleController(latencySeconds = 0f)
        c.update(0L, 0f, true)
        val (steps, _) = walk(c, 0f, 2 * cycle + 5f)
        assertEquals(4, steps)
    }

    @Test
    fun frameAtEachContact_isTheContactFrame() {
        for (k in 0 until 4) {
            val distance = WalkCycleController.stepDistance(k)
            val expected = WalkCycleLayout.CONTACT_FRAMES[k % 2]
            assertEquals("passo $k", expected, WalkCycleController.frameFor(distance))
        }
    }

    @Test
    fun frameIsAlwaysInRange_overManyCycles() {
        var d = 0f
        while (d < 10 * cycle) {
            val f = WalkCycleController.frameFor(d)
            assertTrue(f in 0 until WalkCycleLayout.FRAME_COUNT)
            d += 0.7f
        }
    }

    @Test
    fun noStepsAndNoDistance_whileStepsNotAllowed() {
        val c = WalkCycleController(latencySeconds = 0f)
        c.update(0L, 0f, allowSteps = false)
        var x = 0f
        var t = 0L
        var steps = 0
        repeat(120) {
            x += 5f
            t += frameNanos
            steps += c.update(t, x, allowSteps = false)
        }
        assertEquals(0, steps)
        assertFalse(c.walking)
    }

    @Test
    fun teleport_isIgnored() {
        val c = WalkCycleController(latencySeconds = 0f)
        c.update(0L, 0f, true)
        val steps = c.update(frameNanos, 113f, allowSteps = true) // recuo do monólogo: salto grande
        assertEquals(0, steps)
        assertFalse(c.walking)
    }

    @Test
    fun walking_holdsBriefly_thenReturnsToIdle() {
        val c = WalkCycleController(latencySeconds = 0f)
        c.update(0L, 0f, true)
        c.update(16_000_000L, 5f, true)
        assertTrue(c.walking)
        c.update(100_000_000L, 5f, true) // parado 84 ms: ainda conta como caminhada
        assertTrue(c.walking)
        c.update(200_000_000L, 5f, true) // parado 184 ms: volta ao idle
        assertFalse(c.walking)
    }

    @Test
    fun atTheGameSpeed_eachStepLandsOnItsContactFrame() {
        // simula o jogo: anda STEP_PER_TICK% do mundo a cada MOVE_TICK_MS, e checa cada passo
        val c = WalkCycleController(latencySeconds = 0f)
        val stepUnits = ExplorationRules.STEP_PER_TICK / 100f * NightStreetLayout.WORLD_WIDTH
        val tickNanos = ExplorationRules.MOVE_TICK_MS * 1_000_000L
        var x = 0f
        var t = 0L
        c.update(t, x, true)
        var steps = 0
        while (steps < 8) {
            // quadros de 60 Hz; o movimento chega a cada tick do jogo
            repeat(2) {
                t += frameNanos
                val tickEdge = t % tickNanos < frameNanos
                if (tickEdge) x += stepUnits
                val n = c.update(t, x, true)
                if (n > 0) {
                    steps += n
                    // som e frame mudam no mesmo quadro de render. O movimento chega em saltos de
                    // ~7 unidades (≈ 2 quadros de animação), então o frame exibido é o contato
                    // ou até 2 quadros depois.
                    val expected = WalkCycleLayout.CONTACT_FRAMES[(steps - 1) % 2]
                    val shown = c.frameIndex
                    val lag = (shown - expected + WalkCycleLayout.FRAME_COUNT) % WalkCycleLayout.FRAME_COUNT
                    assertTrue("passo $steps no frame $shown, contato $expected (atraso $lag)", lag in 0..2)
                }
            }
        }
    }

    @Test
    fun latency_firesTheStepBeforeTheContact() {
        val early = WalkCycleController(latencySeconds = 0.1f)
        val late = WalkCycleController(latencySeconds = 0f)
        early.update(0L, 0f, true)
        late.update(0L, 0f, true)
        // anda até ~10 unidades antes do primeiro contato (≈ 35 ms a essa velocidade)
        val target = WalkCycleController.stepDistance(0) - 10f
        val (e, _) = walk(early, 0f, target)
        val (l, _) = walk(late, 0f, target)
        assertEquals(1, e)
        assertEquals(0, l)
    }
}
