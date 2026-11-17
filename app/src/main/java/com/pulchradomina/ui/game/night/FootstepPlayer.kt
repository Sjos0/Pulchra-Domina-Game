package com.pulchradomina.ui.game.night

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pulchradomina.R

/** Som de um passo, tocado a cada contato do walk (SoundPool: baixa latência para sfx curto). */
internal class FootstepPlayer(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundId: Int = pool.load(context.applicationContext, R.raw.sfx_footstep, 1)

    fun play() {
        if (soundId != 0) pool.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }

    private companion object {
        const val VOLUME = 0.7f
    }
}
