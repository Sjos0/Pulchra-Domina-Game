package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    var phase by remember { mutableStateOf(BootPhase.SPLASH) }
    val fadeAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            BootPhase.VIDEO -> {
                var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.crow_intro}")
                            setVideoURI(uri)
                            setOnPreparedListener { mp ->
                                mediaPlayer = mp
                                mp.isLooping = false
                                mp.setVolume(1f, 1f)
                                start()
                            }
                            setOnCompletionListener {
                                phase = BootPhase.HOLD
                            }
                            setOnErrorListener { _, _, _ ->
                                phase = BootPhase.HOLD
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { /* no-op */ }
                )

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
                    contentScale = ContentScale.Crop,
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
                    contentScale = ContentScale.Crop,
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
