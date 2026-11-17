// GERADO por tools/edgar_opening/build_edgar_opening.py — não editar à mão.
package com.pulchradomina.ui.game.opening

/** Janela de voz de um trecho do texto: tempo (ms, dentro do clip) e índices de caractere. */
data class SyncSegment(val startMs: Int, val endMs: Int, val charStart: Int, val charEnd: Int)

data class OpeningLineData(val text: String, val clipDurationMs: Int, val segments: List<SyncSegment>)

internal object EdgarOpeningData {
    val lines: List<OpeningLineData> = listOf(
    OpeningLineData(
        text = "Mais uma noite... Parece que o silêncio daquele quarto vai me consumindo aos poucos.",
        clipDurationMs = 6975,
        segments = listOf(
            SyncSegment(startMs = 70, endMs = 1870, charStart = 0, charEnd = 17),
            SyncSegment(startMs = 2780, endMs = 6650, charStart = 18, charEnd = 84)
        )
    ),
    OpeningLineData(
        text = "Sair pra caminhar por essas ruas frias não resolve nada... mas ficar lá, parado, com a cabeça cheia de pensamentos, é ainda pior.",
        clipDurationMs = 9300,
        segments = listOf(
            SyncSegment(startMs = 285, endMs = 3245, charStart = 0, charEnd = 58),
            SyncSegment(startMs = 4025, endMs = 4835, charStart = 59, charEnd = 72),
            SyncSegment(startMs = 5105, endMs = 5705, charStart = 72, charEnd = 80),
            SyncSegment(startMs = 6005, endMs = 7715, charStart = 80, charEnd = 115),
            SyncSegment(startMs = 8175, endMs = 8975, charStart = 115, charEnd = 129)
        )
    ),
    OpeningLineData(
        text = "Eu só queria encontrar alguma coisa que fizesse barulho... qualquer coisa. Só pra conseguir abafar tudo isso aqui dentro... nem que fosse por alguns minutos.",
        clipDurationMs = 9900,
        segments = listOf(
            SyncSegment(startMs = 285, endMs = 2975, charStart = 0, charEnd = 58),
            SyncSegment(startMs = 3495, endMs = 4405, charStart = 59, charEnd = 74),
            SyncSegment(startMs = 5115, endMs = 7345, charStart = 75, charEnd = 123),
            SyncSegment(startMs = 8065, endMs = 9605, charStart = 124, charEnd = 157)
        )
    )
    )
}
