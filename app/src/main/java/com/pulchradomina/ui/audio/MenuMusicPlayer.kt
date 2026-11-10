package com.pulchradomina.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.pulchradomina.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val MENU_MUSIC_TARGET_VOLUME = 0.6f

private const val VOLUME_TICK_MS = 50L

/**
 * Trilha do menu. Loop nativo do MediaPlayer + envelope de volume a cada 50 ms.
 * Não usa o volume do sistema como substituto: setVolume é o ganho do player (0–1);
 * o volume de mídia do aparelho continua aplicado por cima pelo sistema.
 */
class MenuMusicPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var volumeJob: Job? = null
    private var released = false

    /** Recria o player na posição 0 e sobe o volume pelo fade-in. */
    fun start() {
        if (released) return
        releasePlayer()
        val mp = MediaPlayer.create(appContext, R.raw.pulchra_domina_soundtrack) ?: return
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mp.isLooping = true
        mp.setVolume(0f, 0f)
        mp.start()
        player = mp
        startVolumeLoop()
    }

    fun pause() {
        volumeJob?.cancel()
        volumeJob = null
        val mp = player ?: return
        try {
            if (mp.isPlaying) mp.pause()
        } catch (_: IllegalStateException) {
        }
    }

    fun resume() {
        if (released) return
        val mp = player ?: return
        try {
            if (!mp.isPlaying) mp.start()
        } catch (_: IllegalStateException) {
            return
        }
        startVolumeLoop()
    }

    fun release() {
        if (released) return
        released = true
        releasePlayer()
        scope.cancel()
    }

    private fun startVolumeLoop() {
        volumeJob?.cancel()
        volumeJob = scope.launch {
            while (isActive) {
                val mp = player ?: break
                val volume = try {
                    menuMusicVolumeAt(
                        positionMs = mp.currentPosition.toLong(),
                        durationMs = mp.duration.toLong(),
                        targetVolume = MENU_MUSIC_TARGET_VOLUME
                    )
                } catch (_: IllegalStateException) {
                    break
                }
                try {
                    mp.setVolume(volume, volume)
                } catch (_: IllegalStateException) {
                    break
                }
                delay(VOLUME_TICK_MS)
            }
        }
    }

    private fun releasePlayer() {
        volumeJob?.cancel()
        volumeJob = null
        val mp = player ?: return
        player = null
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: IllegalStateException) {
        }
        mp.release()
    }
}
