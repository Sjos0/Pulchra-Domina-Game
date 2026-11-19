package com.pulchradomina.ui.game.night

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Luzes da balada no extremo direito do mapa: "vazam" do cenário 2 (ainda inacessível).
 * Cada luz pisca no próprio ritmo e fase; as cores são rosa, azul, vermelho, amarelo e verde.
 */
internal object ClubLights {
    const val FIRST_X = 2630f
    const val LAST_X = 2810f
    const val CENTER_Y = 880f
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

    val LIGHTS: List<Light> = List(10) { i ->
        val f = i / 9f
        Light(
            x = FIRST_X + (LAST_X - FIRST_X) * f,
            y = CENTER_Y + ((i * 37) % 5 - 2) * 18f,
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
