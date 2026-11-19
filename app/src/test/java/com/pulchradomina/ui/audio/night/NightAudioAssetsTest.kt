package com.pulchradomina.ui.audio.night

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Os áudios precisam estar no APK com os nomes que o código usa (cópia do Sfx_Game, sem reencode). */
class NightAudioAssetsTest {

    private val assets = File("src/main/assets")

    private fun assertAsset(path: String) {
        val f = File(assets, path)
        assertTrue("falta asset: $path", f.isFile && f.length() > 0L)
    }

    @Test
    fun ambience_loops_areThere() {
        assertAsset(NIGHT_CRICKETS_ASSET)
        assertAsset(NIGHT_WIND_ASSET)
    }

    @Test
    fun club_track_isThere() {
        assertAsset(NIGHT_CLUB_ASSET)
    }

    @Test
    fun stingers_allVariations_arePresent() {
        for (set in NIGHT_STINGERS) {
            assertTrue(set.assets.size == 4)
            set.assets.forEach { assertAsset(it) }
        }
    }

    @Test
    fun stingerIntervals_matchTheDirectorsSpec() {
        val byName = NIGHT_STINGERS.associateBy { it.name }
        assertTrue(byName.getValue("crow").minMs == 90_000L && byName.getValue("crow").maxMs == 180_000L)
        assertTrue(byName.getValue("leaves").minMs == 35_000L && byName.getValue("leaves").maxMs == 75_000L)
        assertTrue(byName.getValue("distant_metal").minMs == 120_000L && byName.getValue("distant_metal").maxMs == 240_000L)
    }
}
