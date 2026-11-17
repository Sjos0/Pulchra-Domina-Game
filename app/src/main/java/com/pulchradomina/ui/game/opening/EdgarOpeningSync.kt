package com.pulchradomina.ui.game.opening

import com.pulchradomina.R

/** Recursos do monólogo: uma voz (clip) e um retrato por frase, na ordem do txt. */
internal object EdgarOpeningAssets {
    val voices: List<Int> = listOf(
        R.raw.edgar_opening_line_01,
        R.raw.edgar_opening_line_02,
        R.raw.edgar_opening_line_03
    )
    val portraits: List<Int> = listOf(
        R.drawable.edgar_line_01,
        R.drawable.edgar_line_02,
        R.drawable.edgar_line_03
    )
}

/**
 * Typewriter sincronizado com a voz: quantos caracteres mostrar na posição [positionMs] do clip.
 * Cada trecho de voz (entre pausas) revela o seu trecho de texto de forma linear.
 * Função pura: testada em JVM.
 */
internal object OpeningSync {

    fun visibleChars(line: OpeningLineData, positionMs: Int): Int {
        for (seg in line.segments) {
            if (positionMs < seg.startMs) return seg.charStart
            if (positionMs < seg.endMs) {
                val span = (seg.endMs - seg.startMs).coerceAtLeast(1)
                val t = (positionMs - seg.startMs).toFloat() / span
                return seg.charStart + Math.round(t * (seg.charEnd - seg.charStart))
            }
        }
        return line.text.length
    }
}
