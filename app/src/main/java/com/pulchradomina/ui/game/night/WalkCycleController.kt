package com.pulchradomina.ui.game.night

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** Antecipação do som de passo: o SoundPool tem ~40 ms de latência, então toca antes do contato. */
internal const val FOOTSTEP_LATENCY_S = 0.04f

/** Tempo sem andar até o walk voltar para o idle (evita piscar entre frames de movimento). */
private const val WALK_HOLD_NANOS = 150_000_000L

/** Salto maior que isto por frame não é caminhada (ex.: o recuo do monólogo): é ignorado. */
private const val MAX_PLAUSIBLE_STEP_UNITS = 60f

/** Suavização da velocidade (o movimento chega em passos a cada 30 ms, o frame é ~16 ms). */
private const val SPEED_SMOOTHING = 0.25f

/**
 * Walk cycle guiado pela distância percorrida (sem relógio fixo).
 * Assim o pé e o frame andam juntos em qualquer velocidade, e o som de passo cai
 * exatamente no quadro de contato. Sem deslizar.
 *
 * Eventos: cada contato da sprite (WalkCycleLayout.CONTACT_FRAMES) é um passo.
 */
internal class WalkCycleController(
    private val latencySeconds: Float = FOOTSTEP_LATENCY_S
) {
    private var lastX: Float? = null
    private var lastNanos: Long = -1L
    private var lastMoveNanos: Long = 0L
    private var hasMoved = false
    private var distance = 0f
    private var nextStep = 0

    var speedUnitsPerSec: Float = 0f
        private set

    /** Frame do walk a desenhar (0..FRAME_COUNT-1), válido enquanto [walking]. */
    var frameIndex by mutableIntStateOf(0)
        private set

    var walking by mutableStateOf(false)
        private set

    /**
     * Atualiza com a posição atual do jogador. Retorna quantos passos devem tocar neste frame.
     * [allowSteps] = false durante o monólogo (não conta distância, não toca som).
     */
    fun update(nowNanos: Long, playerWorldX: Float, allowSteps: Boolean): Int {
        val prevX = lastX
        val prevNanos = lastNanos
        lastX = playerWorldX
        lastNanos = nowNanos
        if (!allowSteps || prevX == null || prevNanos < 0L) {
            walking = false
            return 0
        }
        val dt = (nowNanos - prevNanos) / 1_000_000_000f
        val moved = abs(playerWorldX - prevX)
        if (moved > 0f && moved <= MAX_PLAUSIBLE_STEP_UNITS && dt > 0f) {
            distance += moved
            lastMoveNanos = nowNanos
            hasMoved = true
        }
        // velocidade = média móvel de (distância / tempo) a cada frame, inclusive frames parados
        if (dt > 0f) {
            val instant = if (moved <= MAX_PLAUSIBLE_STEP_UNITS) moved / dt else speedUnitsPerSec
            speedUnitsPerSec += (instant - speedUnitsPerSec) * SPEED_SMOOTHING
        }
        val isWalking = hasMoved && nowNanos - lastMoveNanos < WALK_HOLD_NANOS
        walking = isWalking
        frameIndex = frameFor(distance)
        if (!isWalking) return 0

        var steps = 0
        val reached = distance + speedUnitsPerSec * latencySeconds
        while (reached >= stepDistance(nextStep)) {
            nextStep++
            steps++
        }
        return steps
    }

    companion object {
        /** Distância (unidades) do n-ésimo passo: cada ciclo tem 2 contatos, em ordem. */
        fun stepDistance(n: Int): Float {
            val cycle = WalkCycleLayout.CYCLE_UNITS
            val contact = WalkCycleLayout.CONTACT_FRAMES[n % 2]
            return (n / 2) * cycle + contact.toFloat() / WalkCycleLayout.FRAME_COUNT * cycle
        }

        /** Frame do walk para uma distância percorrida. O contato cai exatamente no seu quadro. */
        fun frameFor(distance: Float): Int {
            val cycle = WalkCycleLayout.CYCLE_UNITS
            val phase = ((distance % cycle) + cycle) % cycle / cycle
            return (phase * WalkCycleLayout.FRAME_COUNT + 1e-3f)
                .toInt()
                .coerceIn(0, WalkCycleLayout.FRAME_COUNT - 1)
        }
    }
}
