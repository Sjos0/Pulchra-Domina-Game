package com.pulchradomina.ui.game.night

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * Luzes da balada no extremo direito do mapa: "vazam" do cenário 2 (ainda inacessível).
 * Espalhadas por toda a área demarcada (faixa do fim do mapa, do topo do contorno até o chão).
 * Cada luz pisca no próprio ritmo e fase; as cores são rosa, azul, vermelho, amarelo e verde.
 */
internal object ClubLights {
    const val FIRST_X = 2630f
    const val LAST_X = 2815f
    /** Área demarcada na tela: do topo do contorno (~670) até o chão (~1300). */
    const val TOP_Y = 660f
    const val BOTTOM_Y = 1290f
    const val COUNT = 16
    /** Raio do halo (unidades de mundo). */
    const val RADIUS = 46f

    class Light(
        val x: Float,
        val y: Float,
        /** ARGB. */
        val colorArgb: Long,
        /** Período do pisco, em segundos. */
        val period: Float,
        val phase: Float
    )

    private val COLORS: List<Long> = listOf(
        0xFFFF4FD8, // rosa
        0xFF3FA9FF, // azul
        0xFFFF3B3B, // vermelho
        0xFFFFD93B, // amarelo
        0xFF3BFF7A  // verde
    )

    private fun frac(v: Float): Float = v - floor(v)

    /** Posições em sequência de baixa discrepância (razões áureas): espalhadas, sem fileira nem padrão. */
    val LIGHTS: List<Light> = List(COUNT) { i ->
        Light(
            x = FIRST_X + (LAST_X - FIRST_X) * frac(i * 0.618034f),
            y = TOP_Y + (BOTTOM_Y - TOP_Y) * frac(i * 0.381966f + 0.25f),
            colorArgb = COLORS[i % COLORS.size],
            period = 0.9f + (i * 0.37f) % 0.9f,
            phase = i * 1.7f
        )
    }

    /** Intensidade 0..1: pisco com acendimento rápido e pausa escura (potência alta da senoide). */
    fun intensity(light: Light, seconds: Float): Float {
        val wave = sin(2.0 * PI * seconds / light.period + light.phase).toFloat() * 0.5f + 0.5f
        return wave.pow(4f)
    }
}
