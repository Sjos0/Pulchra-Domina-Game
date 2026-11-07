package com.example.domain

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class CharacterType(
    val displayName: String,
    val title: String,
    val color: Color
) {
    EDGAR("Edgar", "Protagonista", EdgarColor),
    MAVETH("Maveth", "A Dama das Sombras", MavethColor),
    ALLAN("Allan", "Melhor Amigo", AllanColor),
    NOOR("Noor", "O Contraponto Dourado", NoorColor),
    NARRATOR("Narração", "", NarratorColor)
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
    val nextNodeId: String? = null, // Para avanço linear quando não há escolhas imediatas
    val isChapterEnding: Boolean = false
)

data class MacroRouteInfo(
    val code: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val badge: String,
    val color: Color
)
