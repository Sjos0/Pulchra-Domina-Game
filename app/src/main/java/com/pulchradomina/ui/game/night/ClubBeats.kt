package com.pulchradomina.ui.game.night

import kotlin.math.floor

/**
 * Relógio do pulso da balada: 120 BPM fixos, padrão para este cenário. NÃO segue a música.
 *
 * Anda só enquanto a faixa toca ([setRunning]): pausa congela o tempo, volta continua de onde parou,
 * então a luz não pula quando o jogo volta. Escrito pelo player ([com.pulchradomina.ui.audio.night.ClubMusicPlayer])
 * e lido pelo desenho do cenário; por isso as funções são sincronizadas.
 */
internal object ClubBeats {
    /** Andamento do pulso. */
    const val BPM = 120f
    /** Duração de uma batida, em segundos (0,5 s a 120 BPM). */
    const val BEAT_S = 60f / BPM

    private const val NONE = Long.MIN_VALUE
    private var accumulatedNs = 0L
    private var runningSince = NONE
    private var running = false

    /** Liga ou desliga o relógio no instante [nowNanos] (System.nanoTime). Repetir o mesmo estado não faz nada. */
    @Synchronized
    fun setRunning(on: Boolean, nowNanos: Long) {
        if (on == running) return
        if (running) {
            accumulatedNs += nowNanos - runningSince
        } else {
            runningSince = nowNanos
        }
        running = on
    }

    /** Segundos de reprodução acumulados (sem contar pausas). */
    @Synchronized
    fun elapsedS(nowNanos: Long): Float {
        val live = if (running) nowNanos - runningSince else 0L
        return (accumulatedNs + live) / 1_000_000_000f
    }

    /** Batidas desde o início. A cor da luz muda a cada duas. */
    fun beatCount(nowNanos: Long): Int = floor(elapsedS(nowNanos) / BEAT_S).toInt()

    /** Segundos desde a última batida, ou -1 se o relógio não está rodando. */
    fun secondsSinceBeat(nowNanos: Long): Float {
        if (!isRunning()) return -1f
        return elapsedS(nowNanos) % BEAT_S
    }

    @Synchronized
    fun isRunning(): Boolean = running

    /** Zera tudo (balada descarregada). */
    @Synchronized
    fun reset() {
        accumulatedNs = 0L
        runningSince = NONE
        running = false
    }
}
