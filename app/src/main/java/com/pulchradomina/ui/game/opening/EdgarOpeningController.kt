package com.pulchradomina.ui.game.opening

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val OPENING_ENTER_MS = 420
/** Saída em 2× a velocidade da entrada. */
internal const val OPENING_EXIT_MS = OPENING_ENTER_MS / 2
internal const val OPENING_EDGAR_FADE_MS = 900
private const val TICK_MS = 16L

/**
 * Sequência do monólogo de Edgar: uma frase por vez.
 * Entrada (slide up) -> voz + typewriter sincronizados -> espera o toque -> saída (2×).
 * Só avança com toque depois que o texto e a voz terminaram.
 */
class EdgarOpeningController(
    context: Context,
    private val lines: List<OpeningLineData> = EdgarOpeningData.lines,
    private val onFinished: () -> Unit
) {
    private val voice = OpeningVoicePlayer(context.applicationContext)
    private var runStarted = false
    private var tapSignal: CompletableDeferred<Unit>? = null

    val edgarAlpha = Animatable(0f)
    val slideDp = Animatable(OpeningLayout.SLIDE_OFFSCREEN_DP)
    val boxAlpha = Animatable(0f)

    /** Verdadeiro durante o monólogo (controles e caminhada bloqueados). */
    var active by mutableStateOf(false)
        private set
    var lineIndex by mutableIntStateOf(0)
        private set
    var visibleChars by mutableIntStateOf(0)
        private set
    /** Texto e voz da frase terminaram: a seta aparece e o toque avança. */
    var canAdvance by mutableStateOf(false)
        private set

    suspend fun run() {
        if (runStarted) return
        runStarted = true
        active = true
        try {
            lines.forEachIndexed { index, line -> playLine(index, line) }
        } finally {
            voice.release()
        }
        active = false
        onFinished()
    }

    fun advance() {
        if (canAdvance) tapSignal?.complete(Unit)
    }

    fun release() {
        voice.release()
        tapSignal?.cancel()
    }

    private suspend fun playLine(index: Int, line: OpeningLineData) {
        lineIndex = index
        visibleChars = 0
        canAdvance = false

        // Entrada: Edgar aparece (na primeira frase) e a caixa sobe.
        slideDp.snapTo(OpeningLayout.SLIDE_OFFSCREEN_DP)
        boxAlpha.snapTo(0f)
        coroutineScope {
            if (index == 0) launch { edgarAlpha.animateTo(1f, tween(OPENING_EDGAR_FADE_MS)) }
            launch { slideDp.animateTo(0f, tween(OPENING_ENTER_MS, easing = FastOutSlowInEasing)) }
            launch { boxAlpha.animateTo(1f, tween(OPENING_ENTER_MS)) }
        }

        // Voz e typewriter começam juntos. Sem recurso de áudio, usa o relógio.
        val clipMs = line.clipDurationMs
        val clockStart = System.nanoTime()
        val hasAudio = voice.start(EdgarOpeningAssets.voices[index])
        while (true) {
            val elapsed = ((System.nanoTime() - clockStart) / 1_000_000L).toInt()
            val pos = if (hasAudio) voice.positionMs() else elapsed
            visibleChars = OpeningSync.visibleChars(line, pos)
            val finished = if (hasAudio) voice.completed || pos >= clipMs + 800 else elapsed >= clipMs
            if (finished) break
            delay(TICK_MS)
        }
        visibleChars = line.text.length
        voice.release()
        canAdvance = true

        // Espera o toque do jogador.
        val signal = CompletableDeferred<Unit>()
        tapSignal = signal
        signal.await()
        tapSignal = null
        canAdvance = false

        // Saída: inverso da entrada, 2× mais rápido.
        coroutineScope {
            launch { slideDp.animateTo(OpeningLayout.SLIDE_OFFSCREEN_DP, tween(OPENING_EXIT_MS, easing = FastOutLinearInEasing)) }
            launch { boxAlpha.animateTo(0f, tween(OPENING_EXIT_MS)) }
        }
    }
}
