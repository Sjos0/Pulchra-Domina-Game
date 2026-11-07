package com.example.domain

data class StreetHotspot(
    val id: String,
    val title: String,
    val promptAction: String,
    val positionPercent: Float, // 0f a 100f ao longo da rua
    val iconName: String,
    val description: String,
    val quote: String,
    val isClubDoor: Boolean = false
)

object StreetContent {
    val streetHotspots = listOf(
        StreetHotspot(
            id = "bus_stop",
            title = "Ponto de Ônibus • Linha 402 Hospital",
            promptAction = "Examinar Horários",
            positionPercent = 18f,
            iconName = "bus",
            description = "A placa de metal está enferrujada pela garoa fria. O último ônibus em direção à ala sul do hospital já passou há quarenta minutos.",
            quote = "“Se eu pegar o primeiro ônibus da manhã, ainda chego antes que o médico passe a ronda. Preciso aguentar esta noite.”"
        ),
        StreetHotspot(
            id = "puddle_reflection",
            title = "Poça de Chuva sob Luz Violeta",
            promptAction = "Olhar Reflexo",
            positionPercent = 50f,
            iconName = "water",
            description = "A água acumulada na sarjeta reflete os neons roxos do clube distorcidos pelas gotas de chuva.",
            quote = "“Olhe para você, Edgar. Olheiras profundas, palidez cadavérica. Parece mais um fantasma do que um homem vivo.”"
        ),
        StreetHotspot(
            id = "club_entrance",
            title = "Entrada do Clube Nocturne",
            promptAction = "Entrar no Clube",
            positionPercent = 84f,
            iconName = "door",
            description = "A porta pesada de metal vibra com as batidas abafadas do som. Um letreiro neon arroxeado pisca em intervalos lentos: 'NOCTURNE'.",
            quote = "“O ar frio da rua acaba aqui. Lá dentro, a névoa, a música e as sombras aguardam.”",
            isClubDoor = true
        )
    )
}
