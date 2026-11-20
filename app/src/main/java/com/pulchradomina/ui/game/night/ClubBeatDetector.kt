package com.pulchradomina.ui.game.night

/**
 * Detector de batidas sobre a energia de graves (bumbo/baixo) do som que está tocando.
 *
 * Batida = energia acima de uma média móvel (limiar relativo) e acima de um piso absoluto,
 * com intervalo mínimo entre batidas (não dispara duas vezes no mesmo bumbo).
 * Puro: recebe os valores, não toca em áudio, então é testável em JVM.
 */
internal class ClubBeatDetector(
    /** Intervalo mínimo entre batidas (≈ até 214 BPM). */
    private val minIntervalNanos: Long = 280_000_000L,
    /** Suavização da média móvel por captura (~0,6 s com capturas a 20 Hz). */
    private val meanSmoothing: Float = 0.08f,
    /** Quantas vezes acima da média a energia precisa estar. */
    private val ratio: Float = 1.45f,
    /** Piso absoluto, para o silêncio não virar batida. */
    private val floor: Float = 12f
) {
    private var mean = 0f
    private var primed = false
    private var lastBeat = NONE

    /** Retorna true se esta captura marca uma batida. */
    fun onEnergy(nowNanos: Long, energy: Float): Boolean {
        val spaced = lastBeat == NONE || nowNanos - lastBeat >= minIntervalNanos
        val isBeat = primed && spaced && energy > floor && energy > mean * ratio
        mean = if (!primed) energy else mean + (energy - mean) * meanSmoothing
        primed = true
        if (isBeat) lastBeat = nowNanos
        return isBeat
    }

    fun reset() {
        mean = 0f
        primed = false
        lastBeat = NONE
    }

    companion object {
        private const val NONE = Long.MIN_VALUE
        /** Limite superior da faixa de graves, em Hz. */
        const val BASS_HIGH_HZ = 150f

        /**
         * Energia dos graves (até [BASS_HIGH_HZ]) de uma captura FFT do Visualizer.
         * Formato do Visualizer: bytes com [DC, Nyquist, Re1, Im1, Re2, Im2, ...], com captureSize bytes.
         */
        fun bassEnergy(fft: ByteArray, samplingRate: Int): Float {
            val n = fft.size
            if (n < 4 || samplingRate <= 0) return 0f
            val binHz = samplingRate.toFloat() / n
            val kMax = (BASS_HIGH_HZ / binHz).toInt().coerceIn(1, n / 2 - 1)
            var sum = 0f
            for (k in 1..kMax) {
                val re = fft[2 * k].toFloat()
                val im = fft[2 * k + 1].toFloat()
                sum += re * re + im * im
            }
            return sum
        }
    }
}
