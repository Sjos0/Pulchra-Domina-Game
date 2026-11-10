package com.pulchradomina.ui.boot

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.pulchradomina.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Sequência de abertura:
 * 1) Splash estático (splash_loading) — sem indicador
 * 2) Vídeo crow_intro (fade de áudio 3.5s → 4.0s)
 * 3) Logo estático (logo_hold) por 1s
 * 4) Fade para preto ~0.7s → Title
 */
private enum class BootPhase {
    SPLASH,
    VIDEO,
    HOLD,
    FADE_OUT
}

@Composable
fun BootSequenceScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableStateOf(BootPhase.SPLASH) }
    val fadeAlpha = remember { Animatable(0f) }

    // Tempo mínimo de splash antes de ir ao vídeo (sensação de "carregou")
    LaunchedEffect(Unit) {
        delay(1200)
        phase = BootPhase.VIDEO
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (phase) {
            BootPhase.SPLASH -> {
                Image(
                    painter = painterResource(id = R.drawable.splash_loading),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            BootPhase.VIDEO -> {
                var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
                var videoReady by remember { mutableStateOf(false) }

                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    val mp = MediaPlayer()
                                    mediaPlayer = mp
                                    try {
                                        val afd = ctx.resources.openRawResourceFd(R.raw.crow_intro)
                                        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                        mp.setSurface(Surface(st))
                                        mp.isLooping = false
                                        mp.setVolume(1f, 1f)
                                        mp.setOnPreparedListener {
                                            it.start()
                                            videoReady = true
                                        }
                                        mp.setOnCompletionListener { phase = BootPhase.HOLD }
                                        mp.setOnErrorListener { _, _, _ ->
                                            phase = BootPhase.HOLD
                                            true
                                        }
                                        mp.prepareAsync()
                                    } catch (_: Exception) {
                                        phase = BootPhase.HOLD
                                    }
                                }
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    try { mediaPlayer?.release() } catch (_: Exception) {}
                                    mediaPlayer = null
                                    return true
                                }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { /* no-op */ }
                )

                // Por cima do VideoView até o primeiro frame — evita o cinza
                if (!videoReady) {
                    Image(
                        painter = painterResource(id = R.drawable.splash_loading),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Fade de áudio: 3.5s → 4.0s (volume 1 → 0)
                LaunchedEffect(phase) {
                    if (phase != BootPhase.VIDEO) return@LaunchedEffect
                    val startFadeMs = 3500L
                    val endFadeMs = 4000L
                    val startTime = System.currentTimeMillis()
                    while (isActive && phase == BootPhase.VIDEO) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val mp = mediaPlayer
                        if (mp != null && elapsed >= startFadeMs) {
                            val t = ((elapsed - startFadeMs).toFloat() / (endFadeMs - startFadeMs)).coerceIn(0f, 1f)
                            val vol = 1f - t
                            try {
                                mp.setVolume(vol, vol)
                            } catch (_: Exception) { }
                            if (t >= 1f) break
                        }
                        // Safety: se o vídeo passar de ~4.2s sem completion
                        if (elapsed >= 4200L) {
                            phase = BootPhase.HOLD
                            break
                        }
                        delay(16)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        try {
                            mediaPlayer?.release()
                        } catch (_: Exception) { }
                    }
                }
            }

            BootPhase.HOLD -> {
                Image(
                    painter = painterResource(id = R.drawable.logo_hold),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                LaunchedEffect(Unit) {
                    delay(1000)
                    phase = BootPhase.FADE_OUT
                }
            }

            BootPhase.FADE_OUT -> {
                // Mantém logo_hold por baixo enquanto o preto sobe
                Image(
                    painter = painterResource(id = R.drawable.logo_hold),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = fadeAlpha.value }
                        .background(Color.Black)
                )
                LaunchedEffect(Unit) {
                    fadeAlpha.snapTo(0f)
                    fadeAlpha.animateTo(1f, animationSpec = tween(durationMillis = 700))
                    onFinished()
                }
            }
        }
    }
}
