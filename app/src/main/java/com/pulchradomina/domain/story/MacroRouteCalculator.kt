package com.pulchradomina.domain.story

import com.pulchradomina.data.model.GameState

/**
 * Catálogo e regras das macro-rotas 3a–3f.
 * Responsabilidade única: decidir rota a partir do GameState.
 */
object MacroRouteCalculator {

    val ROUTES = mapOf(
        "3a" to MacroRouteInfo(
            code = "3a",
            title = "O Pacto das Pétalas Negras",
            subtitle = "Rota Ideal — Comunhão Obscura",
            description = "Você aceitou a hortênsia preta de Maveth com profunda entrega. Uma quietude gélida acalmou a tempestade em seu peito, ligando o seu luto ao abismo fascinante daquela presença enigmática.",
            badge = "Rota Ideal",
            colorArgb = StoryPalette.VIOLET_ACCENT
        ),
        "3b" to MacroRouteInfo(
            code = "3b",
            title = "O Fio da Meia-Noite",
            subtitle = "Rota Mediana — Conexão Silenciosa",
            description = "A flor descansa em sua mão, mas suas palavras foram cautelosas. Maveth reconhece sua dor, embora a distância entre a vida e o esquecimento permaneça em suspenso.",
            badge = "Rota Mediana (Flor)",
            colorArgb = StoryPalette.MAVETH
        ),
        "3c" to MacroRouteInfo(
            code = "3c",
            title = "A Sombra no Reflexo",
            subtitle = "Rota Mediana — Fascínio sem Aceitação",
            description = "Você não tocou a hortênsia negra, mas os olhos azuis de Maveth gravaram um enigma na sua mente. A recusa gerou um silêncio compartilhado que você não consegue esquecer.",
            badge = "Rota Mediana (Maveth)",
            colorArgb = StoryPalette.NARRATOR
        ),
        "3d" to MacroRouteInfo(
            code = "3d",
            title = "O Elo Solar de Noor",
            subtitle = "Rota Mediana — A Âncora do Agora",
            description = "Ao hesitar diante do abismo, a energia abrasiva de Noor te arrancou da penumbra. A vida lá fora é caótica e dolorosa, mas você escolheu respirar seu calor ácido por mais uma noite.",
            badge = "Rota Mediana (Noor)",
            colorArgb = StoryPalette.NOOR
        ),
        "3e" to MacroRouteInfo(
            code = "3e",
            title = "O Retorno ao Concreto",
            subtitle = "Rota Sem Flor — O Peso do Real",
            description = "Você rejeitou o misticismo e as flores noturnas. A dura verdade é que Allan está definhando no quarto 304, e nenhum conto gótico alterará os exames de sangue do seu amigo.",
            badge = "Rota Sem Flor",
            colorArgb = StoryPalette.ALLAN
        ),
        "3f" to MacroRouteInfo(
            code = "3f",
            title = "A Fuga Cega",
            subtitle = "Rota Speedrun — Evasão Absoluta",
            description = "Você não quis conversa, não fez perguntas e abandonou o clube correndo para a chuva. O vazio continuou o mesmo, intocado e mudo.",
            badge = "Nota Mínima / Fuga",
            colorArgb = StoryPalette.EDGAR
        )
    )

    fun calculateMacroRoute(state: GameState): MacroRouteInfo {
        val hasFlor = state.pegou_flor
        val pont = state.pont_cap
        val conf = state.confianca_maveth
        val interactedNoor = state.escolhas_feitas.any { it.contains("noor", ignoreCase = true) }

        val code = when {
            // 3f: Speedrun ou fuga imediata com baixíssima interação
            pont <= 1 -> "3f"
            // 3a: Ideal (pegou flor + alta afinidade + alta pontuação)
            hasFlor && pont >= 7 && conf >= 3 -> "3a"
            // 3d: Prioridade e conexão viva com Noor
            interactedNoor && !hasFlor && pont >= 4 -> "3d"
            // 3b: Mediana com flor
            hasFlor -> "3b"
            // 3c: Curiosidade e conexão com Maveth sem a flor
            !hasFlor && conf >= 2 -> "3c"
            // 3e: Recusa explícita e foco na realidade sem flor
            else -> "3e"
        }

        return ROUTES[code] ?: ROUTES.values.first()
    }
}
