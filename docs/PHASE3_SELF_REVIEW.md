# Auto-revisão Fase 3 (antes do Claude)

## O que mudou
- Lógica de movimento/hotspot saiu de números mágicos na `ExplorationScreen` → `ExplorationRules`
- Avanço de painéis manga → `MangaSequencePlayer`
- Flags de diálogo VN → `DialoguePresenter`
- Telas ainda possuem arte Compose (Canvas, layouts) — intencional na Fase 3

## Inconsistências / riscos que eu já vi
1. **`ExplorationRules.applyDrag`**: se `deltaPercent == 0`, o caller não chama; se chamar com 0, `facingRight` vira `true` por default (não preserva direção). Mitigado na screen com `if (deltaX != 0f)`.
2. **`MangaSequencePlayer`**: usa lista default `MangaContent.prologueSequence`; se no futuro houver outro prólogo, injetar lista no construtor (já suportado).
3. **`DialoguePresenter`**: não altera ViewModel; só a UI. `canAdvanceLinear` ainda não é usado no `DialogueBox` (o advance continua via callback). Oportunidade Fase 3.1: DialogueBox consultar presenter.
4. **Pacote ainda `com.example`**: Fase 4; não renomear agora para não quebrar assinatura/update.
5. **Testes unitários no CI**: o workflow atual só builda APK debug; testes não rodam no Actions a menos que se adicione step `./gradlew test`. Recomendação ao Claude: validar se o workflow deve incluir `test`.
6. **Duplicação de conteúdo**: `StreetContent` / `MangaContent` continuam como objetos de dados — ok; presenters não devem embutir texto.

## Comportamento que deve permanecer idêntico
- Passo 1.4f / 30ms, limites 10–90%, raio hotspot 8.5%
- 4 painéis manga; último toque chama `onFinishCutscene`
- Skip manga ainda chama `onFinishCutscene` direto
- GameScreen: choices e advance iguais, só via presenter

## API pública
- `MainActivity` / `GameViewModel` não mudaram nesta fase
