package com.pulchradomina.ui.audio

const val FADE_IN_MS = 2000L
const val FADE_OUT_MS = 1000L

/**
 * Volume da trilha do menu em função da posição na faixa.
 * Puro: sem Android, para o loop do MediaPlayer e para teste unitário.
 */
fun menuMusicVolumeAt(positionMs: Long, durationMs: Long, targetVolume: Float): Float {
    if (durationMs <= 0L) return 0f
    val p = positionMs.coerceIn(0L, durationMs - 1L)
    if (p < FADE_IN_MS) {
        return targetVolume * p.toFloat() / FADE_IN_MS.toFloat()
    }
    val fadeOutStart = durationMs - FADE_OUT_MS
    if (p >= fadeOutStart) {
        return targetVolume * (durationMs - p).toFloat() / FADE_OUT_MS.toFloat()
    }
    return targetVolume
}
