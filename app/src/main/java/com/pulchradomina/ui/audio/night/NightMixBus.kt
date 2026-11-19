package com.pulchradomina.ui.audio.night

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TICK_MS = 33L
private const val SPACE_SMOOTH = 0.12f
private const val DUCK_SMOOTH = 0.15f

/**
 * Ponto único do áudio da rua: ambiente (grilos + vento), balada, stingers e o ducking da fala.
 *
 * Quem decide é [setSpace] (0 = esquerda, 1 = direita): na intro vem da câmera, na exploração do Edgar.
 * O bus suaviza os valores e aplica nos players a cada [TICK_MS]; ninguém mais mexe em volume direto.
 */
internal class NightMixBus(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val ambience = NightAmbiencePlayer(appContext)
    private val club = ClubMusicPlayer(appContext)

    private var targetSpace = 1f
    private var smoothSpace = 1f
    private var duckAmbience = 1f
    private var duckClub = 1f
    private var speaking = false
    private var monologue = false
    private var paused = false
    private var started = false
    private var loop: Job? = null

    private val stingers = NightStingerScheduler(
        context = appContext,
        scope = scope,
        // stingers fora de fala e fora do monólogo, com a rua ativa
        isOpen = { !paused && !speaking && !monologue },
        gain = { NightMix.stingerGain(smoothSpace) }
    )

    fun start() {
        if (started) return
        started = true
        ambience.start()
        club.start()
        stingers.start()
        loop = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    /** Posição espacial alvo: 0 = esquerda (início), 1 = direita (fim, clube). */
    fun setSpace(t: Float) {
        targetSpace = NightMix.clamp01(t)
    }

    /** [speaking]: clip de voz tocando (ducking). [monologue]: monólogo inteiro (bloqueia stingers). */
    fun setSpeech(speaking: Boolean, monologue: Boolean) {
        this.speaking = speaking
        this.monologue = monologue
    }

    /** Posição alvo do passo (passos usam o mesmo eixo). */
    fun footstepVolume(): Float = NightMix.footstepVolume(targetSpace)

    fun pause() {
        paused = true
        ambience.pause()
        club.pause()
    }

    fun resume() {
        paused = false
        ambience.resume()
        club.resume()
    }

    fun release() {
        loop?.cancel()
        loop = null
        stingers.stop()
        ambience.release()
        club.release()
    }

    private fun tick() {
        smoothSpace += (targetSpace - smoothSpace) * SPACE_SMOOTH
        val ambDuck = if (speaking) NightMix.VOICE_DUCK_AMBIENCE else 1f
        val clubDuck = if (speaking) NightMix.VOICE_DUCK_CLUB else 1f
        duckAmbience += (ambDuck - duckAmbience) * DUCK_SMOOTH
        duckClub += (clubDuck - duckClub) * DUCK_SMOOTH

        val amb = NightMix.ambienceScale(smoothSpace) * duckAmbience
        ambience.setLevels(NightMix.CRICKETS_BASE * amb, NightMix.WIND_BASE * amb)
        club.setVolume(NightMix.clubVolume(smoothSpace) * duckClub)
        club.setMuffle(NightMix.clubMuffle(smoothSpace))
    }
}
