package com.pulchradomina.ui.audio.night

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.util.Log
import com.pulchradomina.ui.game.night.ClubBeats
import kotlin.math.abs

internal const val NIGHT_CLUB_ASSET = "audio/music/club_balada_crazy.mp3"

private const val TAG = "ClubMusicPlayer"
private const val MUFFLE_EPSILON = 0.01f

/**
 * Balada do clube, em loop (a faixa tem ~287 s e volta ao início ao acabar).
 *
 * Distância simulada no próprio player (efeitos presos à sessão de áudio dele):
 *  - reverb: EnvironmentalReverb. Perto do clube, pouca cauda e nível baixo; longe, cauda longa e mais úmida.
 *  - muffle: Equalizer. Longe, os agudos (acima de ~1 kHz) caem até ~12 dB, o que abafa a faixa.
 * Pulso da luz: relógio de 120 BPM em [ClubBeats], não a música. Ele anda só enquanto a faixa toca
 * (pausa e volta junto), e o player avisa quando começa, pausa, volta e para.
 * Se o aparelho não aceitar um dos efeitos, a faixa segue sem ele e o log registra o motivo.
 */
internal class ClubMusicPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var reverb: EnvironmentalReverb? = null
    private var equalizer: Equalizer? = null
    private var lastMuffle = -1f
    private var volume = 0f

    fun start() {
        if (player != null) return
        val mp = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                context.assets.openFd(NIGHT_CLUB_ASSET).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                isLooping = true
                setVolume(0f, 0f)
                prepare()
            }
        } catch (e: Exception) {
            Log.w(TAG, "balada não carregou", e)
            return
        }
        attachEffects(mp.audioSessionId)
        mp.start()
        player = mp
        ClubBeats.setRunning(true, System.nanoTime())
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        player?.setGainSafe(volume)
    }

    /** 0 = limpa (perto do clube), 1 = muito abafada (longe). */
    fun setMuffle(value: Float) {
        val m = value.coerceIn(0f, 1f)
        if (abs(m - lastMuffle) < MUFFLE_EPSILON) return
        lastMuffle = m
        reverb?.let { rv ->
            try {
                rv.reverbLevel = NightMix.lerp(-4500f, 1000f, m).toInt().toShort()
                rv.decayTime = NightMix.lerp(1400f, 5000f, m).toInt()
            } catch (e: RuntimeException) {
                Log.w(TAG, "reverb ajustado sem efeito", e)
            }
        }
        equalizer?.let { eq ->
            try {
                val minLevel = eq.bandLevelRange[0].toFloat()
                for (band in 0 until eq.numberOfBands) {
                    val index = band.toShort()
                    val centerHz = eq.getCenterFreq(index) / 1000
                    val level = if (centerHz >= HIGH_CUT_FROM_HZ) minLevel * HIGH_CUT_DEPTH * m else 0f
                    eq.setBandLevel(index, level.toInt().toShort())
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "equalizador ajustado sem efeito", e)
            }
        }
    }

    fun pause() {
        try {
            player?.let { if (it.isPlaying) it.pause() }
        } catch (_: IllegalStateException) {
        }
        ClubBeats.setRunning(false, System.nanoTime())
    }

    fun resume() {
        try {
            player?.let { if (!it.isPlaying) it.start() }
        } catch (_: IllegalStateException) {
        }
        if (player != null) ClubBeats.setRunning(true, System.nanoTime())
    }

    fun release() {
        reverb?.release()
        reverb = null
        equalizer?.release()
        equalizer = null
        ClubBeats.reset()
        val mp = player ?: return
        player = null
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: IllegalStateException) {
        }
        mp.release()
    }

    private fun attachEffects(sessionId: Int) {
        try {
            reverb = EnvironmentalReverb(0, sessionId).apply { enabled = true }
        } catch (e: RuntimeException) {
            Log.w(TAG, "EnvironmentalReverb indisponível neste aparelho", e)
        }
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Equalizer indisponível neste aparelho", e)
        }
    }

    private companion object {
        /** Agudos a partir daqui entram no corte do muffle. */
        const val HIGH_CUT_FROM_HZ = 1000
        /** Fração do limite da banda (mB) usada no corte máximo (~ -12 dB). */
        const val HIGH_CUT_DEPTH = 0.8f
    }
}
