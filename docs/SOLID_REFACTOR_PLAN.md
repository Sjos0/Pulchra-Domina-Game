# Plano de Refatoração SOLID — Pulchra Domina

Objetivo: cada arquivo/módulo com **uma responsabilidade**, mudanças isoladas, manutenção segura.

## Estado atual (mapa)

| Área | Arquivos | Problema |
|------|----------|----------|
| UI + navegação | `MainActivity`, `GameViewModel` | ViewModel concentra navegação + regras + save |
| História | `ChapterOneStory` (~370 linhas) | Conteúdo + lógica de rota no mesmo lugar |
| Telas | `ExplorationScreen` (~650), `MangaCutsceneScreen` | UI + regras misturadas |
| Update | `UpdateManager` + `UpdatesDialog` | OK (já bem separado) |
| Persistência | `SaveManager` | OK |
| Tema | `Color`, `Type`, `Theme` | OK |

## Princípios aplicados

- **S** — Uma razão para mudar por classe
- **O** — Estender rotas/capítulos sem editar o motor
- **L** — Contratos de tela/estado substituíveis
- **I** — Interfaces finas (`StoryRepository`, `SavePort`, `UpdatePort`)
- **D** — UI depende de abstrações, não de `ChapterOneStory` concreto

## Fases (ordem segura)

### Fase 0 — UI isolada (esta versão)
- [x] `PixelWoodButton` — só visual de botão do menu
- [x] Fontes pixel em `Type.kt`
- [x] TitleScreen só orquestra layout

### Fase 1 — Domínio de história
- Extrair `ChapterOneContent` (nós/texto) de `ChapterOneStory`
- Extrair `MacroRouteCalculator` (pontuação → rota 3a–3f)
- `StoryRepository` interface + impl Capítulo 1

### Fase 2 — ViewModel fino
- `NavigationController` / estado de `AppScreen`
- `GameSession` (estado de partida + escolhas)
- `GameViewModel` só combina fluxos (sem regras de rota)

### Fase 3 — Telas
- Exploration: `ExplorationPresenter` + `ExplorationScreen` burra
- Manga: player de painéis separado do conteúdo
- Game: `DialoguePresenter` + componentes já existentes

### Fase 4 — Pacotes
```
com.pulchradomina/
  domain/story/
  domain/exploration/
  data/save/
  data/update/
  ui/title/
  ui/game/
  ui/boot/
```
(renome de applicationId fica para release estável)

## Regras de execução
1. Uma fase por PR/release quando possível
2. Não misturar arte + refator grande no mesmo commit se risco alto
3. Testar boot → menu → novo jogo após cada fase
4. Manter minSdk 26 e assinatura debug estável

## Próximo passo após v1.0.5
Começar **Fase 1** (separar conteúdo da história do calculador de rotas).

## Progresso

### Fase 1 — concluída (v1.0.6)
- `ChapterOneNodes` — só nós/escolhas
- `MacroRouteCalculator` — só rotas 3a–3f
- `StoryRepository` + `ChapterOneStoryRepository`
- `ChapterOneStory` — fachada de compatibilidade
- `GameViewModel` depende de `StoryRepository`
- `AppScreen` / `GameUiState` em arquivos próprios
- Testes unitários de rota em `MacroRouteCalculatorTest`

### Fase 2 — parcial
- ViewModel mais fino (ainda combina navegação + sessão; save isolado em SaveManager)



### Fase 2 — concluída (v1.0.8)
- `GameSession` + `SessionSnapshot` — regras de partida (escolha, avanço, fim de capítulo)
- `NavigationState` — chrome de navegação (documentado; UI state ainda em GameUiState)
- `SavePort` + `SaveManagerPort` — ViewModel não depende de DataStore direto
- `GameViewModel` — só orquestra sessão + navegação + persistência
- Testes: `GameSessionTest` + `MacroRouteCalculatorTest`

Próximo: Fase 3 (telas Exploration/Manga/Game mais “burras”).


### Fase 3 — concluída (v1.0.9)
- `ExplorationRules` — movimento, limites, hotspot, fração do canvas
- `MangaSequencePlayer` — avanço de painéis / fim da sequência
- `DialoguePresenter` — flags de escolha/avanço linear para VN
- Telas Exploration/Manga/Game consomem presenters; arte Compose permanece nas screens
- Testes: `ExplorationRulesTest`, `MangaSequencePlayerTest`, `DialoguePresenterTest`

Próximo: Fase 4 (pacotes `com.pulchradomina` / applicationId — só com release estável).
