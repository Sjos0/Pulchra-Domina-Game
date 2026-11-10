package com.pulchradomina.domain.story

import androidx.compose.ui.graphics.Color
import com.pulchradomina.ui.theme.*

data class MangaPanel(
    val id: String,
    val title: String,
    val narrativeText: String,
    val dialogueSpeaker: String? = null,
    val dialogueQuote: String? = null,
    val visualType: MangaVisualType,
    val accentColor: Color = VioletLight
)

enum class MangaVisualType {
    HOSPITAL_ROOM,    // Quarto 304 com Allan e monitores cardíacos
    HOSPITAL_CORRIDOR,// Edgar no corredor frio olhando exames
    RAINY_CITY,       // A chuva caindo na metrópole gótica à noite
    CLUB_EXTERIOR     // Edgar diante do reflexo da balada na rua
}

object MangaContent {
    val prologueSequence: List<MangaPanel> = listOf(
        MangaPanel(
            id = "panel_1",
            title = "Painel I • O Silêncio do Quarto 304",
            narrativeText = "O bipe ritmado dos monitores é o único som no quarto do hospital. Allan dorme sob cobertores finos, o rosto pálido e sereno apesar da leucemia que consome suas forças.",
            dialogueSpeaker = "Edgar",
            dialogueQuote = "“Ele sorriu antes de apagar... e me pediu para sair deste hospital por uma noite. 'Vá viver um pouco, Edgar', ele disse.”",
            visualType = MangaVisualType.HOSPITAL_ROOM,
            accentColor = AllanColor
        ),
        MangaPanel(
            id = "panel_2",
            title = "Painel II • O Corredor de Vidro",
            narrativeText = "Nas mãos de Edgar, os relatórios médicos não oferecem milagres. A luz fluorescente das lâmpadas hospitalares drena toda a cor do mundo.",
            dialogueSpeaker = "Pensamento",
            dialogueQuote = "“Como continuar respirando normalmente quando quem você mais ama está partindo em contagem regressiva?”",
            visualType = MangaVisualType.HOSPITAL_CORRIDOR,
            accentColor = EdgarColor
        ),
        MangaPanel(
            id = "panel_3",
            title = "Painel III • O Dilúvio da Meia-Noite",
            narrativeText = "A noite caiu pesada sobre a cidade. O asfalto transformou-se em um espelho negro cortado pela garoa cortante e pelos faróis distantes.",
            dialogueSpeaker = "Narração",
            dialogueQuote = "“Caminhar sem rumo pela chuva tornou-se o único anestésico contra o pânico silencioso no peito.”",
            visualType = MangaVisualType.RAINY_CITY,
            accentColor = VioletAccent
        ),
        MangaPanel(
            id = "panel_4",
            title = "Painel IV • A Entrada do Abismo",
            narrativeText = "No final de um beco iluminado por neons violetas, as batidas graves do Clube Nocturne fazem o chão vibrar. É aqui que os que não conseguem dormir se escondem da própria realidade.",
            dialogueSpeaker = "Edgar",
            dialogueQuote = "“Talvez o barulho ensurdecedor consiga calar o silêncio do quarto 304... ao menos por algumas horas.”",
            visualType = MangaVisualType.CLUB_EXTERIOR,
            accentColor = MavethColor
        )
    )
}
