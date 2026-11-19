package com.pulchradomina.ui.audio.night

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Um tipo de stinger: intervalo aleatório entre ocorrências e as variações (arquivos do assets). */
internal data class StingerSet(
    val name: String,
    val minMs: Long,
    val maxMs: Long,
    val assets: List<String>
)

/** Corvo 90–180 s, folhas 35–75 s, metal 120–240 s. Quatro variações de cada. */
internal val NIGHT_STINGERS: List<StingerSet> = listOf(
    StingerSet("crow", 90_000L, 180_000L, (1..4).map { "audio/stingers/crow/crow_0$it.mp3" }),
    StingerSet("leaves", 35_000L, 75_000L, (1..4).map { "audio/stingers/leaves/leaves_0$it.mp3" }),
    StingerSet("distant_metal", 120_000L, 240_000L, (1..4).map { "audio/stingers/distant_metal/metal_0$it.mp3" })
)

private const val TAG = "NightStingers"
private const val GATE_RECHECK_MS = 500L
private const val MIN_AUDIBLE_GAIN = 0.02f
internal const val STINGER_BASE_VOLUME = 0.35f

/**
 * Agenda os stingers da rua. Cada tipo tem o seu intervalo aleatório; a variação nunca repete a anterior.
 * Só toca quando [isOpen] é verdadeiro (fora de fala e do monólogo); se o portão estiver fechado na hora
 * do sorteio, espera até abrir.
 */
internal class NightStingerScheduler(
    context: Context,
    private val scope: CoroutineScope,
    private val isOpen: () -> Boolean,
    private val gain: () -> Float,
    private val random: Random = Random.Default
) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val appContext = context.applicationContext
    private val soundIds = HashMap<String, IntArray>()
    private val jobs = mutableListOf<Job>()

    fun start() {
        if (jobs.isNotEmpty()) return
        for (set in NIGHT_STINGERS) {
            soundIds[set.name] = IntArray(set.assets.size) { i -> loadAsset(set.assets[i]) }
            jobs += scope.launch { loop(set) }
        }
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        pool.release()
    }

    private suspend fun CoroutineScope.loop(set: StingerSet) {
        val ids = soundIds.getValue(set.name)
        var last = -1
        while (isActive) {
            delay(random.nextLong(set.minMs, set.maxMs + 1))
            while (isActive && !isOpen()) delay(GATE_RECHECK_MS)
            if (!isActive) break
            val g = gain()
            if (g < MIN_AUDIBLE_GAIN || ids.isEmpty()) continue
            var pick = random.nextInt(ids.size)
            if (ids.size > 1 && pick == last) pick = (pick + 1 + random.nextInt(ids.size - 1)) % ids.size
            last = pick
            val v = STINGER_BASE_VOLUME * g
            pool.play(ids[pick], v, v, 1, 0, 1f)
        }
    }

    private fun loadAsset(asset: String): Int = try {
        appContext.assets.openFd(asset).use { afd -> pool.load(afd, 1) }
    } catch (e: Exception) {
        Log.w(TAG, "stinger não carregou: $asset", e)
        0
    }
}
