package com.pulchradomina.ui.audio.night

import com.pulchradomina.ui.game.night.FootstepPlayer

/**
 * Curvas do mix da rua. Funções puras (testadas em JVM).
 *
 * t = posição espacial: 0 = esquerda (início, de onde o Edgar vem: grilos, vento, stingers, passos até 50%),
 *                       1 = direita (fim, clube: balada alta com reverb, passos quase mudos).
 */
internal object NightMix {
    /** Volumes-base das camadas de ambiente (pontos de partida; ajustar ouvindo no device). */
    const val CRICKETS_BASE = 0.25f
    const val WIND_BASE = 0.12f

    /** Balada: ~5% na esquerda (muito abafada), ~95% na direita (headroom para suspiro e voz). */
    const val CLUB_LEFT = 0.05f
    const val CLUB_RIGHT = 0.95f

    /** Ambiente mais vivo na esquerda; mais baixo na direita, onde a balada domina. */
    const val AMBIENCE_LEFT = 1f
    const val AMBIENCE_RIGHT = 0.3f

    /** Passos do Edgar: teto de 50% do passo cheio na esquerda; quase inaudíveis na direita. */
    const val FOOTSTEP_LEFT = 0.5f
    const val FOOTSTEP_RIGHT = 0.01f

    /** Stingers (corvo, folhas, metal): sobem à esquerda; discretos à direita. */
    const val STINGER_LEFT = 1f
    const val STINGER_RIGHT = 0.15f

    /** Ducking durante a fala do Edgar. */
    const val VOICE_DUCK_AMBIENCE = 0.4f
    const val VOICE_DUCK_CLUB = 0.5f

    fun clamp01(x: Float): Float = x.coerceIn(0f, 1f)

    fun lerp(a: Float, b: Float, k: Float): Float = a + (b - a) * clamp01(k)

    /** Curva suave (sem quina) entre os dois polos. */
    fun smoothstep(x: Float): Float {
        val t = clamp01(x)
        return t * t * (3f - 2f * t)
    }

    /** 0 na esquerda, 1 na direita. */
    fun clubLinear(t: Float): Float = smoothstep(t)

    fun clubVolume(t: Float): Float = lerp(CLUB_LEFT, CLUB_RIGHT, clubLinear(t))

    /** 1 = muito abafada (esquerda), 0 = limpa (direita). Controla reverb e corte de agudos. */
    fun clubMuffle(t: Float): Float = 1f - clubLinear(t)

    fun ambienceScale(t: Float): Float = lerp(AMBIENCE_LEFT, AMBIENCE_RIGHT, clubLinear(t))

    fun footstepScale(t: Float): Float = lerp(FOOTSTEP_LEFT, FOOTSTEP_RIGHT, clubLinear(t))

    /** Volume de um passo já com a escala espacial aplicada. */
    fun footstepVolume(t: Float): Float = FootstepPlayer.DEFAULT_VOLUME * footstepScale(t)

    fun stingerGain(t: Float): Float = lerp(STINGER_RIGHT, STINGER_LEFT, 1f - clubLinear(t))
}
