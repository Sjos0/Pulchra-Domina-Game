package com.pulchradomina.ui.theme

import androidx.compose.ui.graphics.Color
import com.pulchradomina.domain.story.CharacterType
import com.pulchradomina.domain.story.MacroRouteInfo
import com.pulchradomina.domain.story.MangaPanel
import com.pulchradomina.domain.story.StoryPalette

// Paleta Gótica e Sombria de Pulchra Domina (só UI chrome — sem espelho de personagem)
val ObsidianDark = Color(0xFF09070E)
val AbyssalPurple = Color(0xFF130F1F)
val SurfaceGothic = Color(0xFF1B162C)
val SurfaceCard = Color(0xFF241D3B)
val BorderGothic = Color(0xFF382D5C)

val RoseDark = Color(0xFFE056FD)
val SilverMist = Color(0xFFE2E8F0)
val PaleText = Color(0xFFE0E0EC)
val MutedText = Color(0xFF9E9AA7)

val BlackHydrangea = Color(0xFF1C142B)
val HydrangeaGlow = Color(0xFF7B2CBF)

/** Converte ARGB de domínio para Color de UI. */
fun Long.toComposeColor(): Color = Color(this)

// Personagens e acentos de história: fonte única = StoryPalette
val EdgarColor = StoryPalette.EDGAR.toComposeColor()
val MavethColor = StoryPalette.MAVETH.toComposeColor()
val AllanColor = StoryPalette.ALLAN.toComposeColor()
val NoorColor = StoryPalette.NOOR.toComposeColor()
val NarratorColor = StoryPalette.NARRATOR.toComposeColor()
val VioletLight = StoryPalette.VIOLET_LIGHT.toComposeColor()
val VioletAccent = StoryPalette.VIOLET_ACCENT.toComposeColor()

val CharacterType.uiColor: Color
    get() = colorArgb.toComposeColor()

val MacroRouteInfo.uiColor: Color
    get() = colorArgb.toComposeColor()

val MangaPanel.uiAccent: Color
    get() = accentColorArgb.toComposeColor()
