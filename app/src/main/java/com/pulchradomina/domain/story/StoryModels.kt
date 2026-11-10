package com.pulchradomina.domain.story

enum class CharacterType(
    val displayName: String,
    val title: String,
    val colorArgb: Long
) {
    EDGAR("Edgar", "Protagonista", StoryPalette.EDGAR),
    MAVETH("Maveth", "A Dama das Sombras", StoryPalette.MAVETH),
    ALLAN("Allan", "Melhor Amigo", StoryPalette.ALLAN),
    NOOR("Noor", "O Contraponto Dourado", StoryPalette.NOOR),
    NARRATOR("Narração", "", StoryPalette.NARRATOR)
}

data class StoryChoice(
    val id: String,
    val text: String,
    val pontCapDelta: Int = 0,
    val confiancaMavethDelta: Int = 0,
    val setsPegouFlor: Boolean? = null,
    val flag: String? = null,
    val nextNodeId: String
)

data class StoryNode(
    val id: String,
    val character: CharacterType,
    val location: String,
    val text: String,
    val innerThoughts: String? = null,
    val choices: List<StoryChoice> = emptyList(),
    val nextNodeId: String? = null,
    val isChapterEnding: Boolean = false
)

data class MacroRouteInfo(
    val code: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val badge: String,
    val colorArgb: Long
)
