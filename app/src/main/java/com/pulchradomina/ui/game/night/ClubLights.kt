package com.pulchradomina.ui.game.night

import kotlin.math.exp

/**
 * Luz da balada, que fica fora da tela, à direita. Ela chega de três formas (ver NightStreetScene):
 *  - Rim: borda dos prédios e árvores voltada para a direita, colorida pela balada;
 *  - Faixa no chão: luz que entra pela borda direita da calçada e da rua;
 *  - Pontos de neon: pixels dithered colados na borda direita da tela.
 *
 * O pulso vem das batidas detectadas no áudio que está tocando (ver [ClubBeats]).
 * Sem batida recente (música parada ou sem captura), a luz fica no nível de base, sem pulsar.
 * Funções puras, testadas em JVM.
 */
internal object ClubLights {
    /** Cores aprovadas, em ARGB: magenta e violeta dominam, ciano dá contraste, âmbar aparece pouco. */
    const val MAGENTA = 0xFFFF2EC8
    const val VIOLET = 0xFF8A4DFF
    const val CYAN = 0xFF38E6FF
    const val AMBER = 0xFFFFB347

    /** Sequência de cores (uma entrada por grupo de batidas). Magenta 3x, violeta 2x, ciano 2x, âmbar 1x. */
    val SEQUENCE: LongArray = longArrayOf(MAGENTA, VIOLET, MAGENTA, CYAN, VIOLET, MAGENTA, AMBER, CYAN)

    /** A cor muda a cada duas batidas. */
    const val BEATS_PER_COLOR = 2

    /** Pulso: subida rápida (ataque) e queda suave (decaimento), sem estroboscópio. */
    const val ATTACK_S = 0.08f
    const val DECAY_S = 0.25f

    /** Depois disto sem batida, a luz volta ao nível de base (música parada ou sem captura). */
    const val ACTIVE_WINDOW_S = 2.5f

    /** Nível de base (sem música) e amplitude do pulso, 0..1. */
    const val BASE_LEVEL = 0.28f
    const val PULSE_LEVEL = 0.62f

    /** Ganhos máximos por tipo de luz (multiplicam o nível). */
    const val RIM_GAIN = 1.23f
    const val SPILL_GAIN = 0.6f
    const val ACCENT_GAIN = 0.9f

    fun colorIndexFor(beatCount: Int): Int = (beatCount.coerceAtLeast(0) / BEATS_PER_COLOR) % SEQUENCE.size

    fun colorArgbFor(beatCount: Int): Long = SEQUENCE[colorIndexFor(beatCount)]

    /** 0..1 conforme o tempo desde a última batida. Sem batida recente, 0. */
    fun pulseAt(sinceBeatS: Float): Float {
        if (sinceBeatS < 0f || sinceBeatS > ACTIVE_WINDOW_S) return 0f
        return if (sinceBeatS < ATTACK_S) sinceBeatS / ATTACK_S
        else exp(-(sinceBeatS - ATTACK_S) / DECAY_S)
    }

    /** Nível da luz, de BASE_LEVEL (sem pulso) a BASE_LEVEL + PULSE_LEVEL (pico). */
    fun level(pulse: Float): Float = BASE_LEVEL + PULSE_LEVEL * pulse.coerceIn(0f, 1f)

    /** Quanto da balada chega a um ponto do mundo: 0 no meio, 1 junto da borda direita. */
    fun proximity(worldX: Float, worldWidth: Float): Float = smoothstep((worldX / worldWidth - 0.35f) / 0.6f)

    /** Alpha do Rim, limitado a 0..1 (o Canvas não aceita mais que isso). */
    fun rimAlpha(worldX: Float, worldWidth: Float, level: Float): Float =
        (proximity(worldX, worldWidth) * level * RIM_GAIN).coerceIn(0f, 1f)

    fun spillAlpha(level: Float): Float = level * SPILL_GAIN

    fun accentAlpha(level: Float): Float = level * ACCENT_GAIN

    private fun smoothstep(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
