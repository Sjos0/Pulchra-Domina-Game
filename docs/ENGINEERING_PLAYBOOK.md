# Engineering Playbook — Pulchra Domina

Documento de referência das soluções validadas (inclui o trabalho do Claude na v1.0.13).
**Regra:** ao mexer no app, copiar estes padrões. Não reintroduzir abordagens já descartadas.
**Regra de ouro:** nenhuma mudança de UI/arte pode quebrar fluxo, save, update ou assinatura.

Estado de referência: **v1.0.13** (`versionCode` 14) — considerado bom pelo produto.

---

## 1. Moldura pixel (`PixelFrameBox`)

### Problema que existia
- Peças de borda vinham **com pontas/caps** decorativas.
- Ao **tile** repetir a peça inteira, as pontas apareciam no meio da aresta (emendas visíveis).
- Cantos com tamanhos diferentes entre si geravam desalinho.
- `fillMaxSize()` na box fazia o diálogo ocupar a tela inteira.

### Solução adotada (Claude — v1.0.13)
1. **Cantos:** quatro assets derivados do mesmo canto (`box_corner_tl/tr/bl/br`), espelhados, mesmo tamanho lógico (55×46 no design).
2. **Bordas:** faixas **uniformes sem pontas**:
   - `box_edge_h.png` (ex.: 8×45)
   - `box_edge_v.png` (ex.: 43×8)
3. **Esticar** a faixa entre cantos (`drawStretch`), **não** repetir tile com caps.
4. **Escala por largura de design:**
   - `DESIGN_WIDTH = 1032f`
   - `TOP_THICKNESS = 45f`, `SIDE_THICKNESS = 43f`
   - `CORNER_W = 55f`, `CORNER_H = 46f`
   - `scale = size.width / DESIGN_WIDTH` → espessura igual em todo o eixo.
5. **Altura pelo conteúdo** — a box **não** força `fillMaxSize()`; o diálogo envolve o conteúdo.
6. Ordem de desenho:
   1. madeira só no **interior** (inset = espessura da moldura)
   2. conteúdo Compose
   3. bordas esticadas
   4. cantos por cima (cobrem junções)

### O que NÃO fazer de novo
- Não voltar a `frame_top/bottom/left/right` com caps nas pontas.
- Não tile de peça completa que inclui bevel de extremidade.
- Não `resize` que esmague a madeira só para preencher a box.

### Assets atuais
```
box_corner_tl.png / tr / bl / br
box_edge_h.png
box_edge_v.png
box_wood.png          # PNG full, sem perda
```

Arquivo: `app/src/main/java/com/example/ui/components/PixelFrameBox.kt`

---

## 2. Fundo de madeira (corvo)

### Problema
- `resize(w, h)` absoluto **achatava** o corvo.
- JPG com perda desnecessária.

### Solução
- Asset `box_wood.png` (proporção original).
- Desenhar só no retângulo **interno** da moldura.
- Escala tipo **cover** (preenche o interior) + crop central — **sem** alterar aspect ratio.
- Em Compose equivalente: crop/cover no bounds interno, nunca `FillBounds` esticando eixos diferentes.

### O que NÃO fazer
- Esticar X e Y de forma independente.
- Cortar o corvo de propósito no centro da arte (o crop é do excesso de enquadramento, não da silhueta).

---

## 3. Diálogo de atualizações (`UpdatesDialog`)

### Padrões
- Usa `PixelFrameBox` (altura wrap content).
- Botões **Verificar** e **Atualizar** **empilhados**, largura total, texto completo (nunca lado a lado cortando label).
- Fonte do jogo (`GameFontFamily`).

### Fluxo de permissão de instalação (importante)
- **Não** abrir Settings automaticamente ao detectar falta de permissão.
- Marcar `needsPermission = true` e mostrar texto + botão **"Permitir instalação"**.
- Só no clique do usuário → `openInstallPermissionSettings()`.
- Continua valendo: mesma assinatura debug, `UpdateManager` lista `/releases` (não só `/latest`), cleanup de APK só depois do install intent.

Arquivo: `app/src/main/java/com/example/ui/components/UpdatesDialog.kt`

---

## 4. Tipografia

### Decisão de produto (v1.0.13 — aprovada)
- **Jost** (Light / Book / Medium) como `GameFontFamily`.
- Referência visual: geometria tipo Futura / *Backbone*.
- OFL em `docs/fonts/OFL-Jost.txt`.
- Pixelify Sans e Press Start 2P **removidos** do app ativo.

### Ao adicionar texto novo
- Preferir `GameFontFamily` / estilos de `Type.kt`.
- Não reintroduzir fontes pixel no menu sem decisão explícita de produto.

Arquivos: `app/src/main/res/font/jost_*.ttf`, `app/src/main/java/com/example/ui/theme/Type.kt`

---

## 5. Feedback de pressão nos botões

### Padrão
- `PressDarkenIndication` implementado com **`IndicationNodeFactory`** (Compose 1.7+).
- Fornecido globalmente em `MainActivity`:

```kotlin
CompositionLocalProvider(LocalIndication provides PressDarkenIndication) {
    MyApplicationTheme { PulchraDominaApp(...) }
}
```

- `PixelWoodButton` e botões Material herdam o indication — escurecem enquanto pressionados.
- **Não** usar a API antiga de `Indication` deprecated; manter `IndicationNodeFactory`.

Arquivo: `app/src/main/java/com/example/ui/theme/PressDarkenIndication.kt`

---

## 6. Resolução de problemas (método a copiar)

Quando algo visual ou de UX falhar:

1. **Reproduzir no asset em resolução cheia** (não só no preview escalado).
2. **Medir** (bbox, espessura, perfil de luminância) antes de “chutar” crop.
3. Preferir **um perfil consistente** (mesma espessura, mesmo sombreado) a recortar peças heterogêneas.
4. Separar **conteúdo** (madeira/texto) de **chrome** (moldura) na ordem de draw.
5. Mudanças de UI em **wrap content** quando for diálogo/card — evitar `fillMaxSize` por padrão.
6. Permissões e side-effects de sistema: **sempre** atrás de ação explícita do usuário.
7. Depois de refator visual: validar boot → menu → novo jogo → save → verificar update (sem quebrar assinatura).

---

## 7. O que não quebrar (checklist)

- [ ] `applicationId` e `debug.keystore` (update in-app)
- [ ] `minSdk 26` / Android 8+
- [ ] Fluxo boot → title → manga → rua → clube → escolhas
- [ ] `GameSession` / `StoryRepository` / saves
- [ ] `UpdateManager` + release não-prerelease no `main`
- [ ] CI: `testDebugUnitTest` + `assembleDebug`
- [ ] Versão só em `build.gradle.kts` (UI lê `BuildConfig`)

---

## 8. Histórico curto das decisões Claude (v1.0.13)

| Commit   | Ideia |
|----------|--------|
| `8d0f94e` | Frame seamless (edges uniformes + stretch), box wrap-height, permissão com botão |
| `5444d6f` | Jost + PressDarken global |
| `d9ff272` | PressDarken na API IndicationNodeFactory (Compose 1.7) |

Produto validou: **manter como está**.

---

*Última atualização: alinhado à v1.0.13. Qualquer desvio deste playbook precisa de motivo explícito no commit.*


## 9. Pacotes (Fase 4)

- Código em `com.pulchradomina.*`
- `applicationId` permanece `com.aistudio.pulchradomina.vnkz` até decisão explícita de release estável
- Não voltar arquivos para `com.example`


## 10. Cores de domínio (pós Fase 4)

- Domínio guarda ARGB `Long` em `StoryPalette` / `colorArgb` / `accentColorArgb`.
- UI: `EdgarColor` etc. **derivam** de `StoryPalette` (`Color(StoryPalette.EDGAR)`), sem hex duplicado.
- Extensões `uiColor` / `uiAccent` em `Color.kt`.
- Não importar `androidx.compose.ui.graphics.Color` nem `ui.theme` a partir de `domain/`.
