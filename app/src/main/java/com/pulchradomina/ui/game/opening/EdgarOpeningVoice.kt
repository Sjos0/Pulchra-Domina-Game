package com.pulchradomina.ui.game.opening

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Player de um clip de voz por vez. Cada frase tem o seu clip: começar uma frase
 * para o clip anterior, e nunca toca o áudio mestre inteiro.
 */
internal class OpeningVoicePlayer(private val appContext: Context) {

    private var player: MediaPlayer? = null

    @Volatile
    var completed = false
        private set

    /** Inicia o clip. Retorna false se o recurso não carregou (o controlador usa tempo como fallback). */
    fun start(rawRes: Int): Boolean {
        release()
        completed = false
        val mp = MediaPlayer.create(appContext, rawRes) ?: return false
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        mp.isLooping = false
        mp.setOnCompletionListener { completed = true }
        mp.start()
        player = mp
        return true
    }

    fun positionMs(): Int = try {
        player?.currentPosition ?: 0
    } catch (_: IllegalStateException) {
        0
    }

    val hasPlayer: Boolean get() = player != null

    /** Para e libera o clip atual. Idempotente. */
    fun release() {
        val mp = player ?: return
        player = null
        try {
            mp.stop()
        } catch (_: IllegalStateException) {
        }
        mp.release()
    }
}
