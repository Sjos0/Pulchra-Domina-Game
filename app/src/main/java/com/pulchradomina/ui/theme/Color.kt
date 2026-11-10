package com.pulchradomina.ui.theme

import androidx.compose.ui.graphics.Color
import com.pulchradomina.domain.story.CharacterType
import com.pulchradomina.domain.story.MacroRouteInfo
import com.pulchradomina.domain.story.MangaPanel

// Paleta Gótica e Sombria de Pulchra Domina
val ObsidianDark = Color(0xFF09070E)
val AbyssalPurple = Color(0xFF130F1F)
val SurfaceGothic = Color(0xFF1B162C)
val SurfaceCard = Color(0xFF241D3B)
val BorderGothic = Color(0xFF382D5C)

// Destaques e Acentos
val VioletAccent = Color(0xFF9D4EDD)
val VioletLight = Color(0xFFC77DFF)
val RoseDark = Color(0xFFE056FD)
val SilverMist = Color(0xFFE2E8F0)
val PaleText = Color(0xFFE0E0EC)
val MutedText = Color(0xFF9E9AA7)

// Cores dos Personagens (espelham StoryPalette)
val EdgarColor = Color(0xFF819FA6)
val MavethColor = Color(0xFFBD93F9)
val AllanColor = Color(0xFF50FA7B)
val NoorColor = Color(0xFFFFB86C)
val NarratorColor = Color(0xFFA5B4FC)
val BlackHydrangea = Color(0xFF1C142B)
val HydrangeaGlow = Color(0xFF7B2CBF)

/** Converte ARGB de domínio para Color de UI. */
fun Long.toComposeColor(): Color = Color(this)

val CharacterType.uiColor: Color
    get() = colorArgb.toComposeColor()

val MacroRouteInfo.uiColor: Color
    get() = colorArgb.toComposeColor()

val MangaPanel.uiAccent: Color
    get() = accentColorArgb.toComposeColor()
