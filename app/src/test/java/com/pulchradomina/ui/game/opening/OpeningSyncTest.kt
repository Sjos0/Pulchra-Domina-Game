package com.pulchradomina.ui.game.opening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpeningSyncTest {

    private val lines = EdgarOpeningData.lines

    @Test
    fun threeLines_withTheExactPhrasesOfTheBrief() {
        assertEquals(
            listOf(
                "Mais uma noite... Parece que o silêncio daquele quarto vai me consumindo aos poucos.",
                "Sair pra caminhar por essas ruas frias não resolve nada... mas ficar lá, parado, com a cabeça cheia de pensamentos, é ainda pior.",
                "Eu só queria encontrar alguma coisa que fizesse barulho... qualquer coisa. Só pra conseguir abafar tudo isso aqui dentro... nem que fosse por alguns minutos."
            ),
            lines.map { it.text }
        )
    }

    @Test
    fun typewriter_isEmptyAtStart_andFullAtTheEndOfTheClip() {
        lines.forEach { line ->
            assertEquals(0, OpeningSync.visibleChars(line, 0))
            assertEquals(line.text.length, OpeningSync.visibleChars(line, line.clipDurationMs))
        }
    }

    @Test
    fun typewriter_neverGoesBackwards() {
        lines.forEach { line ->
            var previous = 0
            for (t in 0..line.clipDurationMs step 10) {
                val shown = OpeningSync.visibleChars(line, t)
                assertTrue("recuou em ${t}ms: $previous -> $shown", shown >= previous)
                previous = shown
            }
        }
    }

    @Test
    fun segments_areOrderedInsideTheClip_andOnlySkipWhitespace() {
        lines.forEach { line ->
            var prevEnd = 0
            line.segments.forEachIndexed { i, seg ->
                assertTrue("segmento $i fora do clip", seg.endMs <= line.clipDurationMs)
                assertTrue("segmento $i invertido", seg.endMs > seg.startMs)
                assertTrue("segmento $i começa antes do anterior", seg.startMs >= (line.segments.getOrNull(i - 1)?.endMs ?: 0))
                assertTrue("texto pulado em $i", seg.charStart >= prevEnd)
                val skipped = line.text.substring(prevEnd, seg.charStart)
                assertTrue("pulou texto não-branco em $i: '$skipped'", skipped.isBlank())
                prevEnd = seg.charEnd
            }
            assertEquals(line.text.length, prevEnd)
        }
    }

    @Test
    fun segmentCount_matchesTheVoiceBursts() {
        // frase 1: 2 trechos (reticências) · frase 2: 5 (reticências + vírgulas) · frase 3: 4
        assertEquals(listOf(2, 5, 4), lines.map { it.segments.size })
    }

    @Test
    fun clips_haveRealDurations_andReadableSpeed() {
        lines.forEach { line ->
            assertTrue("clip ${line.clipDurationMs}ms", line.clipDurationMs in 6_500..10_500)
            val cps = line.text.length * 1000f / line.clipDurationMs
            assertTrue("velocidade ${cps} cps fora do legível", cps in 10f..25f)
        }
    }

    @Test
    fun voiceAndPortraitFiles_existForEachLine() {
        for (i in 1..3) {
            val n = "%02d".format(i)
            assertTrue("falta voz $n", File("src/main/res/raw/edgar_opening_line_$n.mp3").exists())
            assertTrue("falta retrato $n", File("src/main/res/drawable-nodpi/edgar_line_$n.png").exists())
        }
    }
}
