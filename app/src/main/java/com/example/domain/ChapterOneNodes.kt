package com.example.domain

/**
 * Conteúdo narrativo do Capítulo 1 (nós e escolhas).
 * Responsabilidade única: dados de história — sem regras de rota.
 */
object ChapterOneNodes {

    val nodes: Map<String, StoryNode> = listOf(
        // Nó Inicial
        StoryNode(
            id = "inicio",
            character = CharacterType.NARRATOR,
            location = "Clube Nocturne — Pista Subterrânea",
            text = "As batidas graves do som ecoam contra as paredes de concreto úmido. As luzes estroboscópicas cortam a fumaça roxa em espasmos violentos.",
            innerThoughts = "Sinto o gosto metálico da ansiedade na garganta. Faz três dias que Allan foi internado na UTI da hematologia...",
            nextNodeId = "inicio_edgar"
        ),
        StoryNode(
            id = "inicio_edgar",
            character = CharacterType.EDGAR,
            location = "Clube Nocturne — Pista Subterrânea",
            text = "\"O que eu estou fazendo aqui?\" — sussurro para mim mesmo, olhando os copos vazios e as pessoas dançando como espectros esquecidos.",
            choices = listOf(
                StoryChoice(
                    id = "c1_balcao",
                    text = "Afastar-se do barulho e procurar um canto isolado no balcão dos fundos.",
                    pontCapDelta = 2,
                    confiancaMavethDelta = 1,
                    flag = "escolheu_isolamento",
                    nextNodeId = "balcao_intro"
                ),
                StoryChoice(
                    id = "c1_bebida",
                    text = "Pedir um drink forte para tentar silenciar as memórias do quarto de hospital.",
                    pontCapDelta = 1,
                    confiancaMavethDelta = 0,
                    flag = "bebeu_sozinho",
                    nextNodeId = "balcao_bebida"
                ),
                StoryChoice(
                    id = "c1_fuga_rapida",
                    text = "Ir direto para a saída de emergência. Este lugar é sufocante demais.",
                    pontCapDelta = 0,
                    confiancaMavethDelta = 0,
                    flag = "tentou_fugir",
                    nextNodeId = "fuga_saida"
                )
            )
        ),

        // Caminho Fuga (Pode levar a 3f se insistir)
        StoryNode(
            id = "fuga_saida",
            character = CharacterType.NARRATOR,
            location = "Corredor de Emergência — Luz Vermelha",
            text = "O ar aqui fora da pista é denso, mas frio. Na penumbra do corredor, encostada em uma coluna desgastada, alguém observa você em absoluto silêncio.",
            nextNodeId = "maveth_aparece"
        ),

        StoryNode(
            id = "balcao_bebida",
            character = CharacterType.NARRATOR,
            location = "Balcão dos Fundos — Penumbra",
            text = "O líquido arde, mas não aquece o gelo que carrego no peito. No espelho atrás das garrafas, vejo alguém se aproximar por trás de mim, com uma elegância sobrenatural.",
            nextNodeId = "maveth_aparece"
        ),

        StoryNode(
            id = "balcao_intro",
            character = CharacterType.NARRATOR,
            location = "Balcão dos Fundos — Luz Violeta",
            text = "O canto mais escuro do clube parece absorver toda a vibração da música. Sentada a duas banquetas de distância, uma figura pálida parece recortada de um século esquecido.",
            nextNodeId = "maveth_aparece"
        ),

        // Maveth surge
        StoryNode(
            id = "maveth_aparece",
            character = CharacterType.MAVETH,
            location = "Penumbra do Clube",
            text = "— Você carrega o peso de quem já viu o fim nos olhos de outra pessoa. Não é o som deste lugar que você quer esquecer, Edgar.",
            innerThoughts = "Como ela sabe meu nome? A pele dela é quase translúcida, e seus olhos azuis parecem mirar direto na minha dor.",
            choices = listOf(
                StoryChoice(
                    id = "c_maveth_curioso",
                    text = "— Quem é você? E como sabe sobre mim?",
                    pontCapDelta = 2,
                    confiancaMavethDelta = 2,
                    flag = "tom_curioso",
                    nextNodeId = "maveth_dialogo_curioso"
                ),
                StoryChoice(
                    id = "c_maveth_cinico",
                    text = "— Se você é mais uma ilusionista de balada barata, escolheu a vítima errada.",
                    pontCapDelta = 1,
                    confiancaMavethDelta = 0,
                    flag = "tom_cinico",
                    nextNodeId = "maveth_dialogo_cinico"
                ),
                StoryChoice(
                    id = "c_maveth_vulneravel",
                    text = "— Meu amigo está morrendo. Se você veio brincar com isso, apenas me deixe em paz.",
                    pontCapDelta = 3,
                    confiancaMavethDelta = 3,
                    flag = "tom_vulneravel",
                    nextNodeId = "maveth_dialogo_vulneravel"
                ),
                StoryChoice(
                    id = "c_maveth_recusa_seca",
                    text = "Ignorar a garota e dar as costas bruscamente.",
                    pontCapDelta = 0,
                    confiancaMavethDelta = -1,
                    flag = "tom_hostil",
                    nextNodeId = "fuga_antecipada"
                )
            )
        ),

        StoryNode(
            id = "maveth_dialogo_cinico",
            character = CharacterType.MAVETH,
            location = "Penumbra do Clube",
            text = "— O cinismo é apenas um lençol fino que os mortais puxam sobre a cabeça para não enxergar a tempestade. Eu me chamo Maveth.",
            nextNodeId = "noor_interrupcao"
        ),

        StoryNode(
            id = "maveth_dialogo_curioso",
            character = CharacterType.MAVETH,
            location = "Penumbra do Clube",
            text = "— Há nomes que ecoam no ar antes mesmo de serem ditos. Meu nome é Maveth. E eu reconheço a quietude dos que velam os vivos.",
            nextNodeId = "noor_interrupcao"
        ),

        StoryNode(
            id = "maveth_dialogo_vulneravel",
            character = CharacterType.MAVETH,
            location = "Penumbra do Clube",
            text = "— Eu não brinco com a partida, Edgar. A transição é a única verdade solene que resta a este mundo. Allan tem sorte de ter alguém tão fiel à sua agonia.",
            nextNodeId = "noor_interrupcao"
        ),

        // Interrupção de Noor
        StoryNode(
            id = "noor_interrupcao",
            character = CharacterType.NOOR,
            location = "Corredor Central",
            text = "— Edgar! Que cara de velório é essa no meio da noite?! Sai dessa penumbra mórbida, vem pra pista antes que o mundo acabe!",
            innerThoughts = "Noor surge tropeçando, rindo alto com seus olhos violeta cintilando sob os holofotes. Ela é o caos em pessoa.",
            choices = listOf(
                StoryChoice(
                    id = "c_noor_aceitar",
                    text = "Agradecer a distração de Noor e se afastar de Maveth por um momento.",
                    pontCapDelta = 2,
                    confiancaMavethDelta = 0,
                    flag = "ouviu_noor",
                    nextNodeId = "conversa_noor"
                ),
                StoryChoice(
                    id = "c_noor_ficar_maveth",
                    text = "Pedir para Noor esperar: — \"Agora não, Noor. Estou conversando com alguém.\"",
                    pontCapDelta = 3,
                    confiancaMavethDelta = 2,
                    flag = "priorizou_maveth",
                    nextNodeId = "noor_recua"
                )
            )
        ),

        StoryNode(
            id = "conversa_noor",
            character = CharacterType.NOOR,
            location = "Luzes Estroboscópicas",
            text = "— Você tá sempre em luto pelo que nem se foi ainda, Edgar. Allan te mandou aqui pra você viver, não pra você ficar parado encarando o chão!",
            innerThoughts = "Ela tem razão... Mas quando olho para trás, Maveth ainda está lá, esperando com uma serenidade infinita.",
            nextNodeId = "maveth_oferta_flor"
        ),

        StoryNode(
            id = "noor_recua",
            character = CharacterType.NOOR,
            location = "Balcão dos Fundos",
            text = "— Tá bom, tá bom... Mas depois não diz que eu não tentei te salvar dessa bad trip. Vou estar no bar principal se você cansar de ser gótico.",
            nextNodeId = "maveth_oferta_flor"
        ),

        // MOMENTO CRUCIAL: A OFERTA DA HORTÊNSIA PRETA
        StoryNode(
            id = "maveth_oferta_flor",
            character = CharacterType.MAVETH,
            location = "Círculo de Sombra",
            text = "Maveth abre a mão direita. Entre seus dedos pálidos repousa uma flor perfeita, de pétalas densas como o veludo da noite mais profunda: uma hortênsia preta, com orvalho prateado que reflete a névoa.",
            innerThoughts = "O ar em volta esfriou subitamente. É impossível uma flor dessas existir no meio desta balada...",
            nextNodeId = "maveth_fala_flor"
        ),

        StoryNode(
            id = "maveth_fala_flor",
            character = CharacterType.MAVETH,
            location = "Círculo de Sombra",
            text = "— Tome esta flor, Edgar. Ela não cura a carne, mas acolhe o peso inevitável da partida. Se você aceitá-la, prometa que não terá medo do que ela irá revelar.",
            choices = listOf(
                StoryChoice(
                    id = "c_flor_aceitar_plena",
                    text = "[Aceitar a Hortênsia Preta] Estender as duas mãos e receber a flor com reverência.",
                    pontCapDelta = 4,
                    confiancaMavethDelta = 3,
                    setsPegouFlor = true,
                    flag = "aceitou_flor_com_reverencia",
                    nextNodeId = "pos_flor_aceita"
                ),
                StoryChoice(
                    id = "c_flor_aceitar_hesitante",
                    text = "[Aceitar Hesitante] — \"Eu não sei o que isso significa... mas eu não aguento mais esse vazio.\" Tocar e pegar a flor.",
                    pontCapDelta = 3,
                    confiancaMavethDelta = 2,
                    setsPegouFlor = true,
                    flag = "aceitou_flor_hesitante",
                    nextNodeId = "pos_flor_aceita"
                ),
                StoryChoice(
                    id = "c_flor_recusar_gentil",
                    text = "[Recusar a Flor] — \"Não. Por mais que doa, eu não quero me apegar a ilusões ou símbolos fúnebres.\"",
                    pontCapDelta = 2,
                    confiancaMavethDelta = 1,
                    setsPegouFlor = false,
                    flag = "recusou_flor_gentil",
                    nextNodeId = "pos_flor_recusada"
                ),
                StoryChoice(
                    id = "c_flor_rejeitar_hostil",
                    text = "[Rejeitar e Afastar-se] — \"Você é doentia. Isso não é poético, é mórbido.\" Dar as costas e sair.",
                    pontCapDelta = 0,
                    confiancaMavethDelta = -2,
                    setsPegouFlor = false,
                    flag = "rejeitou_flor_hostil",
                    nextNodeId = "pos_flor_rejeitada"
                )
            )
        ),

        // Ramos Pós-Flor
        StoryNode(
            id = "pos_flor_aceita",
            character = CharacterType.NARRATOR,
            location = "Balcão dos Fundos",
            text = "Ao tocar as pétalas negras, um arrepio gélido sobe pelos seus pulsos. O som estridente da balada silencia como se estivesse sob a água. O peso em seu peito parece, pela primeira vez em meses, encontrar um lugar para repousar.",
            nextNodeId = "maveth_despedida_flor"
        ),

        StoryNode(
            id = "maveth_despedida_flor",
            character = CharacterType.MAVETH,
            location = "Penumbra do Clube",
            text = "— Guarde-a perto de si, Edgar. Quando visitar Allan amanhã, você entenderá por que certas pétalas só florescem quando o sol se cansa de insistir.",
            nextNodeId = "desfecho_capitulo"
        ),

        StoryNode(
            id = "pos_flor_recusada",
            character = CharacterType.MAVETH,
            location = "Balcão dos Fundos",
            text = "Maveth recolhe a mão devagar, sem mágoa ou surpresa. Seus lábios esboçam uma linha tênue de compaixão austera.",
            innerThoughts = "— \"A recusa também é uma escolha, Edgar. Mas a noite continuará te esperando do lado de fora.\"",
            nextNodeId = "desfecho_capitulo"
        ),

        StoryNode(
            id = "pos_flor_rejeitada",
            character = CharacterType.NARRATOR,
            location = "Corredor de Emergência",
            text = "Você empurra as portas de emergência e o ar gélido da rua golpeia seu rosto. A sensação de estar sendo observado permanece grudada na sua nuca.",
            nextNodeId = "desfecho_capitulo"
        ),

        StoryNode(
            id = "fuga_antecipada",
            character = CharacterType.NARRATOR,
            location = "Calçada Chuvosa",
            text = "Sem olhar para trás, você correu para fora do clube. As poças d'água refletem a cidade febril, mas tudo parece desbotado e oco.",
            nextNodeId = "desfecho_capitulo"
        ),

        // DESFECHO DO CAPÍTULO 1
        StoryNode(
            id = "desfecho_capitulo",
            character = CharacterType.NARRATOR,
            location = "Rua Silenciosa — 03:42 da Madrugada",
            text = "O Clube Nocturne ficou para trás. A garoa fina cai sobre o asfalto enquanto você caminha sozinho em direção ao ponto de ônibus que leva ao hospital.",
            innerThoughts = "O telefone vibra no bolso: uma notificação do quarto de Allan. O Capítulo 1 se fecha, e os fios das suas escolhas começam a tecer o destino do amanhã.",
            isChapterEnding = true
        )
    ).associateBy { it.id }
}
