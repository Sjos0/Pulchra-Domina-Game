# PRD — Pulchra Domina
**Product Requirements Document**  
**Versão:** 1.0 (MVP – Capítulo 1)  
**Data:** 2026-11-06  
**Status:** Pronto para geração inicial no Google AI Studio (Android)

---

## 1. Visão Geral do Produto

**Nome do jogo:** Pulchra Domina  

**Gênero:** Visual Novel / Narrativa Interativa baseada em escolhas (Choice-driven Narrative)  

**Plataforma:** Android (nativo – Kotlin + Jetpack Compose)  

**Tom:** Sombrio, melancólico, dark romance, psicológico, com toques de realismo mágico e suspense.  

**Conceito em uma frase:**  
Um jovem traumatizado, lidando com a doença terminal de seu melhor amigo, encontra uma figura gótica enigmática em uma balada. A partir da escolha de aceitar ou não uma hortênsia preta, o jogador é puxado para uma rede de decisões que exploram luto, trauma, sanidade e a fronteira entre realidade e o sobrenatural.

**Público-alvo:** Jovens adultos e adultos que apreciam Visual Novels, dramas psicológicos, histórias góticas e narrativas com peso emocional (público de Steam, itch.io e Play Store).

**Diferencial:**  
- Sistema de micro-ramificações (impacto imediato) + macro-ramificações (fim de capítulo).  
- Filosofia de design: “não existe escolha errada, apenas caminhos diferentes”.  
- Relacionamentos rastreados que alteram o comportamento dos personagens e abrem rotas exclusivas.  
- Atmosfera fortemente influenciada por Edgar Allan Poe e *La Belle Dame sans Merci*.

---

## 2. Objetivos do MVP (Capítulo 1)

O primeiro protótipo deve ser **100% funcional** no Capítulo 1 (“O Encontro na Balada”), mesmo sem arte final.

### Objetivos prioritários:
1. Motor de narrativa interativa funcionando (leitura de diálogos, escolhas, atualização de variáveis).
2. Sistema de save/load automático e confiável.
3. Micro e macro-ramificações implementadas e testáveis.
4. Variáveis de relacionamento e decisões-chave rastreadas.
5. Fluxo completo do Capítulo 1 jogável do início ao fim.
6. Interface mínima, limpa e legível em modo retrato (9:16), adequada para mobile.

**Não prioritário neste MVP:**
- Arte final / sprite sheets
- Animações complexas
- Sistema de som/música completo
- Múltiplos capítulos
- Sistema de New Game+

---

## 3. Personagens Principais (Capítulo 1)

| Personagem | Papel | Descrição curta | Aparência base (para placeholders) |
|------------|-------|------------------|------------------------------------|
| **Edgar** | Protagonista | Jovem exausto, depressivo, apático. Vive “morto por dentro”. | Homem jovem, cabelos escuros, olhos cansados, roupa casual escura |
| **Allan** | Melhor amigo | Extrovertido, resiliente, bem-humorado. Está no hospital com leucemia. | Homem jovem, sorridente mesmo doente, hospital gown |
| **Maveth** | Figura central | Gótica, pálida, misteriosa e serena. Representa a morte/serenidade. | Mulher pálida, cabelos pretos, olhos azuis profundos, estética gótica, tatuagens de corvos/rosas |
| **Noor** | Contraponto | Vibrante, caótica, ácida. Representa a vida e o “agora”. | Mulher de cabelos dourados, olhos violeta, energia luminosa |

---

## 4. Core Loop e Mecânicas

### Core Loop (Capítulo 1)
1. Jogador lê o texto narrativo + diálogos.
2. Em pontos de escolha (`alter`), o jogador seleciona uma opção.
3. O sistema atualiza imediatamente:
   - Pontuação do capítulo (`pont_cap`)
   - Nível de confiança/relacionamento (`confianca_maveth`, etc.)
   - Flags de decisão (ex: `pegou_flor`)
4. O texto continua (com possível variação baseada nas escolhas anteriores).
5. Ao final do capítulo, o sistema calcula a macro-ramificação e determina o próximo caminho (mesmo que no MVP o jogo apenas registre e mostre o resultado).

### Mecânicas obrigatórias no MVP

**4.1 Sistema de Variáveis**
```kotlin
// Exemplo de estado do jogo
data class GameState(
    var pont_cap: Int = 0,
    var pont_total: Int = 0,
    var confianca_maveth: Int = 0,
    var pegou_flor: Boolean = false,
    var escolhas_feitas: MutableList<String> = mutableListOf(),
    var capitulo_atual: String = "capitulo1"
)
```

**4.2 Tipos de escolha**
- **Micro-ramificação**: muda o texto imediatamente e/ou a pontuação.
- **Macro-ramificação**: no final do capítulo, decide a rota (3a, 3b, 3c… 3f).

**4.3 Sistema de Save**
- Salvamento automático após cada escolha importante.
- Arquivo local (SharedPreferences ou DataStore) ou JSON simples.
- Deve permitir continuar de onde parou.

**4.4 Interface mínima**
- Tela de título simples com o nome “Pulchra Domina”.
- Área de texto principal (scrollável se necessário).
- Botões de escolha claros e grandes (touch-friendly).
- Indicador visual discreto de relacionamento (ex: barra ou valor de `confianca_maveth`).
- Botão de menu (salvar manual / carregar / reiniciar capítulo).

---

## 5. Estrutura do Capítulo 1 – “O Encontro na Balada”

### Fluxo narrativo resumido:
1. Edgar está em uma balada, tentando escapar do peso da doença de Allan.
2. Ele encontra Maveth — uma jovem pálida de estética gótica.
3. Maveth oferece a ele uma **hortênsia preta**.
4. A escolha de **aceitar ou recusar a flor** é o primeiro grande ponto de ramificação.
5. Dependendo das escolhas, Noor pode aparecer (ou não) com mais força.
6. O capítulo termina com Edgar voltando para casa / hospital, e o sistema calcula a nota do capítulo.

### Pontos de escolha mínimos obrigatórios no Capítulo 1:
- Aceitar ou recusar a hortênsia preta (`pegou_flor`)
- Como Edgar responde a Maveth (várias opções de tom: cínico, vulnerável, hostil, curioso)
- Interação (ou ausência) com Noor
- Decisão final do capítulo que influencia a pontuação

**Rotas de saída do Capítulo 1 (macro):**
- **3a** – Ideal (alta pontuação + pegou a flor)
- **3b / 3c / 3d** – Medianas
- **3e** – Sem flor
- **3f** – Speedrun / nota 0 (mínima interação)

No MVP, o jogo deve apenas **registrar e exibir** qual rota foi desbloqueada ao final do Capítulo 1.

---

## 6. Requisitos Técnicos (Android – Google AI Studio)

### Stack recomendada:
- **Linguagem:** Kotlin
- **UI:** Jetpack Compose
- **Arquitetura:** Single Activity + ViewModel + StateFlow / UiState
- **Persistência:** DataStore (Preferences) ou SharedPreferences
- **Orientação:** Portrait (9:16)
- **Tema visual:** Escuro (preto + roxo + acentos pálidos), tipografia legível

### Estrutura de pastas sugerida:
```
app/
 ├── ui/
 │   ├── screens/
 │   │   ├── TitleScreen.kt
 │   │   ├── GameScreen.kt
 │   │   └── ResultScreen.kt
 │   ├── components/
 │   │   ├── DialogueBox.kt
 │   │   ├── ChoiceButton.kt
 │   │   └── RelationshipIndicator.kt
 │   └── theme/
 ├── data/
 │   ├── GameState.kt
 │   ├── SaveManager.kt
 │   └── ChapterRepository.kt
 ├── domain/
 │   └── StoryEngine.kt
 └── MainActivity.kt
```

### Requisitos de qualidade do MVP:
- O app deve rodar sem crashes do início ao fim do Capítulo 1.
- Escolhas devem atualizar o estado corretamente.
- Save/load deve funcionar após fechar e reabrir o app.
- Texto deve ser legível em telas de celular comuns.
- Performance aceitável (sem travamentos perceptíveis).

---

## 7. Conteúdo Placeholder (enquanto não há arte)

- Personagens: usar formas geométricas coloridas ou silhuetas simples + nome do personagem.
- Cenários: fundos sólidos ou gradientes escuros com nome do local (Balada, Hospital, Rua à noite…).
- Nome do jogo e logo textual: “Pulchra Domina” em tipografia gótica ou serifada elegante.

---

## 8. Critérios de Aceite do MVP

O protótipo será considerado bem-sucedido quando:

1. O jogador consegue completar o Capítulo 1 do início ao fim.
2. Todas as escolhas principais alteram variáveis e/ou texto.
3. O sistema registra corretamente se o jogador pegou a flor ou não.
4. Ao final, o app mostra qual macro-rota foi alcançada (3a–3f).
5. É possível salvar e continuar depois.
6. A experiência é legível e utilizável em um smartphone Android real.

---

## 9. Próximos Passos após o MVP

1. Expandir para Capítulos 2+.
2. Implementar as 6 rotas principais (3a–3f).
3. Adicionar sistema de New Game+ / seleção de capítulos.
4. Integrar arte (2D anime + pixel art).
5. Adicionar trilha sonora e efeitos sonoros.
6. Polir UI e adicionar medidores visuais de relacionamento mais elaborados.

---

## 10. Instruções para o Google AI Studio

**Prompt sugerido (pode ser colado diretamente):**

> Crie um aplicativo Android nativo (Kotlin + Jetpack Compose) de Visual Novel chamado **Pulchra Domina**.
>
> O jogo é uma narrativa interativa sombria e psicológica. O MVP deve conter **apenas o Capítulo 1** completo e funcional.
>
> Requisitos obrigatórios:
> - Orientação portrait
> - Sistema de estado do jogo com variáveis: pont_cap, confianca_maveth, pegou_flor, lista de escolhas
> - Tela de título com o nome “Pulchra Domina”
> - Tela de jogo com caixa de diálogo e botões de escolha
> - Salvamento automático usando DataStore
> - Ao final do Capítulo 1, mostrar qual rota macro foi desbloqueada (3a a 3f)
> - Tema visual escuro (preto + roxo)
> - Placeholders visuais simples (sem necessidade de imagens reais ainda)
>
> Use a história base: Edgar encontra Maveth em uma balada e ela oferece uma hortênsia preta. A escolha de aceitar ou recusar a flor é o ponto central do capítulo.
>
> Foque em fazer o motor de escolhas e o fluxo narrativo funcionarem perfeitamente. Arte e sons podem ficar para depois.

---

**Fim do PRD v1.0**  
Este documento deve ser usado como fonte da verdade para a geração do protótipo inicial no Google AI Studio.  
Qualquer mudança de escopo deve ser refletida aqui primeiro.