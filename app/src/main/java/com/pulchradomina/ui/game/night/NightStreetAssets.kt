package com.pulchradomina.ui.game.night

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bitmaps do cenário já reduzidos no build (nunca o 4096 original).
 * Frames do Edgar: 67 x 115x384 px, cerca de 12 MB decodificados.
 * Rim do Edgar: uma máscara de 1 px por frame e lado (direita, esquerda), em ALPHA_8 (1 byte por pixel).
 */
class NightStreetBitmaps(
    val sprites: Map<NightSprite, ImageBitmap>,
    val edgarFrames: List<ImageBitmap>,
    val walkFrames: List<ImageBitmap>,
    val edgarRim: List<Pair<ImageBitmap, ImageBitmap>>,
    val walkRim: List<Pair<ImageBitmap, ImageBitmap>>
)

/** Carrega uma vez em IO e reaproveita entre visitas à rua. */
object NightStreetAssets {

    @Volatile
    private var cached: NightStreetBitmaps? = null

    fun cachedOrNull(): NightStreetBitmaps? = cached

    suspend fun load(context: Context): NightStreetBitmaps {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            val assets = context.applicationContext.assets
            fun decode(path: String): ImageBitmap {
                val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it) }
                    ?: error("Imagem do cenário inválida: $path")
                return bitmap.asImageBitmap()
            }
            // máscara: só o canal alfa importa, então 1 byte por pixel (ALPHA_8)
            fun decodeMask(path: String): ImageBitmap {
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ALPHA_8 }
                val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
                    ?: error("Máscara do cenário inválida: $path")
                return bitmap.asImageBitmap()
            }

            val sprites = NightSprite.entries.associateWith { decode("night_street/${it.file}") }
            val frames = (0 until NightStreetLayout.EDGAR_FRAME_COUNT).map {
                decode("night_street/edgar/idle_%02d.png".format(it))
            }
            val walk = (0 until WalkCycleLayout.FRAME_COUNT).map {
                decode("night_street/walk/walk_%02d.png".format(it))
            }
            val edgarRim = (0 until NightStreetLayout.EDGAR_FRAME_COUNT).map {
                decodeMask("night_street/edgar/idle_%02d_rim_r.png".format(it)) to
                    decodeMask("night_street/edgar/idle_%02d_rim_l.png".format(it))
            }
            val walkRim = (0 until WalkCycleLayout.FRAME_COUNT).map {
                decodeMask("night_street/walk/walk_%02d_rim_r.png".format(it)) to
                    decodeMask("night_street/walk/walk_%02d_rim_l.png".format(it))
            }
            NightStreetBitmaps(sprites, frames, walk, edgarRim, walkRim).also { cached = it }
        }
    }
}
