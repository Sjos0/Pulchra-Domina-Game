package com.pulchradomina.ui.game.night

/**
 * Estado das batidas da balada, escrito pelo áudio ([com.pulchradomina.ui.audio.night.ClubMusicPlayer])
 * e lido pelo desenho do cenário. Um único escritor (a captura do áudio); leitura por volatile.
 *
 * Não há timer nem BPM fixo: a luz só pulsa quando uma batida real foi detectada no som que está tocando.
 */
internal object ClubBeats {
    @Volatile
    private var lastBeatNanos: Long = NONE

    @Volatile
    private var beats: Int = 0

    /** Registra uma batida detectada no instante [nowNanos] (System.nanoTime). */
    fun onBeat(nowNanos: Long) {
        lastBeatNanos = nowNanos
        beats += 1
    }

    /** Batidas desde o início da captura. A cor da luz depende disto. */
    fun beatCount(): Int = beats

    /** Segundos desde a última batida, ou -1 se nunca houve uma. */
    fun secondsSinceBeat(nowNanos: Long): Float {
        val last = lastBeatNanos
        if (last == NONE) return -1f
        return (nowNanos - last) / 1_000_000_000f
    }

    /** Zera o estado (balada parada por completo). */
    fun reset() {
        lastBeatNanos = NONE
        beats = 0
    }

    private const val NONE = Long.MIN_VALUE
}
