package com.pulchradomina.ui.game.night

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bitmaps do cenário já reduzidos no build (nunca o 4096 original).
 * Frames do Edgar: 67 x 115x384 px, cerca de 12 MB decodificados.
 */
class NightStreetBitmaps(
    val sprites: Map<NightSprite, ImageBitmap>,
    val edgarFrames: List<ImageBitmap>
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

            val sprites = NightSprite.entries.associateWith { decode("night_street/${it.file}") }
            val frames = (0 until NightStreetLayout.EDGAR_FRAME_COUNT).map {
                decode("night_street/edgar/idle_%02d.png".format(it))
            }
            NightStreetBitmaps(sprites, frames).also { cached = it }
        }
    }
}
