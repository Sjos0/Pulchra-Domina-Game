package com.pulchradomina.ui.game.night

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pulchradomina.R

/** Som de um passo (SoundPool: baixa latência para sfx curto). O volume é escolhido por quem toca. */
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

    fun play(volume: Float = DEFAULT_VOLUME) {
        if (soundId != 0) pool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }

    companion object {
        /** Volume normal do andar (walk do jogador). */
        const val DEFAULT_VOLUME = 0.7f
    }
}
