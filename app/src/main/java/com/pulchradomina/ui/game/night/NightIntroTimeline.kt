package com.pulchradomina.ui.game.night

import com.pulchradomina.domain.exploration.ExplorationRules
import kotlin.math.PI
import kotlin.math.cos

/** Estado visual da intro da rua (Novo Jogo) em um instante. */
internal data class NightIntroFrame(
    /** Borda esquerda da câmera, em unidades de mundo. */
    val cameraX: Float,
    /** Altura de cada barra de cinema, em fração da altura da tela. */
    val barFraction: Float,
    /** Véu escuro atrás do card (0..1). */
    val veilAlpha: Float,
    /** Card "Capítulo 1 / O PRIMEIRO ENCONTRO" (0..1). */
    val titleAlpha: Float,
    /** Subtítulo "Um... Dois... Três... Maveth" (0..1). */
    val subtitleAlpha: Float,
    /** Edgar já entrou na cena (começa a andar pela esquerda). */
    val edgarVisible: Boolean,
    /** Posição do Edgar no mundo, em unidades. */
    val edgarWorldX: Float,
    /** Edgar andando na entrada. */
    val edgarWalking: Boolean,
    /** Posição espacial da câmera: 1 = fim (direita, clube), 0 = início (esquerda). Alimenta o mix. */
    val spaceT: Float,
    /** Intro terminou: o monólogo pode começar. */
    val done: Boolean
)

/**
 * Linha do tempo da intro, em ms desde o início (depois do fade do Novo Jogo).
 *
 *   0 .. 2 s       câmera parada no fim do mapa; suspiro; passos baixos
 *   2 .. 14 s      pan lento até o início; barras de cinema fecham; card aparece
 *   14 .. 16,3 s   barras abrem e card some; Edgar entra andando pela esquerda até o ponto
 *   16,3 .. 16,9   Edgar parado (idle); só então o monólogo começa
 *
 * Função pura: [frame] calcula tudo a partir do tempo e da largura visível. Testada em JVM.
 */
internal object NightIntroTimeline {
    const val HOLD_END_MS = 2_000L
    /** Pan ~10% mais lento que a versão anterior (12 000 ms), para apreciar a balada. */
    const val PAN_MS = 13_200L
    const val BARS_CLOSE_MS = 1_200L
    const val BARS_OPEN_MS = 1_000L
    const val BARS_FRACTION = 0.12f
    const val VEIL_FADE_MS = 1_000L
    const val VEIL_ALPHA = 0.6f
    const val TITLE_FADE_START_MS = 3_520L
    const val TITLE_FADE_MS = 1_000L
    const val SUBTITLE_FADE_START_MS = 5_060L
    const val SUBTITLE_FADE_MS = 800L
    /** Onde o Edgar começa a entrada: fora da tela, à esquerda do início do mapa. */
    const val EDGAR_SPAWN_X = -140f
    const val IDLE_BEFORE_MONOLOGUE_MS = 600L
    const val FOOTSTEP_START_MS = 600L
    const val FOOTSTEP_INTERVAL_MS = 600L

    const val PAN_START_MS = HOLD_END_MS
    const val PAN_END_MS = HOLD_END_MS + PAN_MS

    /** Mesma velocidade do andar do jogador (ExplorationRules), em unidades de mundo por segundo. */
    val WALK_UNITS_PER_SEC: Float =
        ExplorationRules.STEP_PER_TICK * 1000f / ExplorationRules.MOVE_TICK_MS / 100f * NightStreetLayout.WORLD_WIDTH

    /** Posição de parada do Edgar (ponto de idle atual, em unidades de mundo). */
    val START_UNITS: Float = StreetCamera.worldXOf(ExplorationRules.START_X)

    val WALK_IN_MS: Long = ((START_UNITS - EDGAR_SPAWN_X) / WALK_UNITS_PER_SEC * 1000f).toLong()
    val WALK_END_MS: Long = PAN_END_MS + WALK_IN_MS
    val DONE_MS: Long = WALK_END_MS + IDLE_BEFORE_MONOLOGUE_MS

    fun frame(elapsedMs: Long, visibleWidth: Float): NightIntroFrame {
        val t = elapsedMs
        val endCamera = (NightStreetLayout.WORLD_WIDTH - visibleWidth).coerceAtLeast(0f)

        val panP = progress(t, PAN_START_MS, PAN_MS)
        val cameraX = endCamera * (1f - easeInOutSine(panP))

        val closeP = progress(t, PAN_START_MS, BARS_CLOSE_MS)
        val openP = progress(t, PAN_END_MS, BARS_OPEN_MS)
        val barFraction = BARS_FRACTION * closeP * (1f - openP)

        val veil = VEIL_ALPHA * progress(t, PAN_START_MS, VEIL_FADE_MS) * (1f - openP)
        val title = progress(t, TITLE_FADE_START_MS, TITLE_FADE_MS) * (1f - openP)
        val subtitle = progress(t, SUBTITLE_FADE_START_MS, SUBTITLE_FADE_MS) * (1f - openP)

        val walkP = progress(t, PAN_END_MS, WALK_IN_MS)
        val edgarX = EDGAR_SPAWN_X + (START_UNITS - EDGAR_SPAWN_X) * walkP

        return NightIntroFrame(
            cameraX = cameraX,
            barFraction = barFraction,
            veilAlpha = veil,
            titleAlpha = title,
            subtitleAlpha = subtitle,
            edgarVisible = t >= PAN_END_MS,
            edgarWorldX = edgarX,
            edgarWalking = t >= PAN_END_MS && t < WALK_END_MS,
            spaceT = 1f - easeInOutSine(panP),
            done = t >= DONE_MS
        )
    }

    private fun progress(t: Long, start: Long, duration: Long): Float =
        ((t - start).toFloat() / duration).coerceIn(0f, 1f)

    private fun easeInOutSine(p: Float): Float = 0.5f - 0.5f * cos(PI.toFloat() * p)
}
