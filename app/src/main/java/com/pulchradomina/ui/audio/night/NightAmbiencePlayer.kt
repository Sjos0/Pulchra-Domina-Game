package com.pulchradomina.ui.audio.night

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

internal const val NIGHT_CRICKETS_ASSET = "audio/ambience/crickets_night.wav"
internal const val NIGHT_WIND_ASSET = "audio/ambience/gentle_night_wind.wav"

private const val TAG = "NightAudio"

/**
 * Cria um MediaPlayer em loop a partir de um arquivo do assets. Sem compressão no APK (WAV/MP3),
 * então o descritor abre direto. Retorna null se o arquivo não carregar (segue sem o som).
 */
internal fun createLoopPlayer(context: Context, asset: String, contentType: Int): MediaPlayer? {
    return try {
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(contentType)
                .build()
        )
        context.assets.openFd(asset).use { afd ->
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
        mp.isLooping = true
        mp.setVolume(0f, 0f)
        mp.prepare()
        mp
    } catch (e: Exception) {
        Log.w(TAG, "asset não carregou: $asset", e)
        null
    }
}

/** Ganho do player (0–1), sem derrubar o app se o player já foi liberado. */
internal fun MediaPlayer.setGainSafe(volume: Float) {
    val v = volume.coerceIn(0f, 1f)
    try {
        setVolume(v, v)
    } catch (_: IllegalStateException) {
    }
}

/** Duas camadas de ambiente em loop: grilos e vento, com volumes independentes. */
internal class NightAmbiencePlayer(private val context: Context) {
    private var crickets: MediaPlayer? = null
    private var wind: MediaPlayer? = null

    fun start() {
        if (crickets != null || wind != null) return
        crickets = createLoopPlayer(context, NIGHT_CRICKETS_ASSET, AudioAttributes.CONTENT_TYPE_SONIFICATION)
        wind = createLoopPlayer(context, NIGHT_WIND_ASSET, AudioAttributes.CONTENT_TYPE_SONIFICATION)
        crickets?.start()
        wind?.start()
    }

    fun setLevels(cricketsVolume: Float, windVolume: Float) {
        crickets?.setGainSafe(cricketsVolume)
        wind?.setGainSafe(windVolume)
    }

    fun pause() {
        listOf(crickets, wind).forEach { mp ->
            try {
                if (mp?.isPlaying == true) mp.pause()
            } catch (_: IllegalStateException) {
            }
        }
    }

    fun resume() {
        listOf(crickets, wind).forEach { mp ->
            try {
                if (mp != null && !mp.isPlaying) mp.start()
            } catch (_: IllegalStateException) {
            }
        }
    }

    fun release() {
        listOf(crickets, wind).forEach { mp ->
            try {
                mp?.stop()
            } catch (_: IllegalStateException) {
            }
            mp?.release()
        }
        crickets = null
        wind = null
    }
}
