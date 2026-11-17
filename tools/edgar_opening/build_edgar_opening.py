#!/usr/bin/env python3
"""
Assets da primeira interação (monólogo do Edgar) — gera a partir do pacote do plano:

  1. Corta o áudio mestre (edgar_opening_full.mp3) em 3 clips, um por frase.
     Cada corte fica no meio da pausa mais longa dentro da janela de tempo do
     dialogue_lines.txt (ajuste fino: parâmetro CUT_WINDOWS).
  2. Valida a estrutura do áudio contra o texto: cada frase tem N trechos de voz
     (pedaços separados por reticências) e cada trecho de texto separado por
     vírgula corresponde a um trecho de voz. Se a contagem não bater, o script para.
  3. Gera os segmentos de sincronização texto ↔ áudio (tempo em ms + índice de caractere),
     usados pelo typewriter (o texto avança conforme a posição do player).
  4. Recorta o fundo preto dos retratos (flood fill a partir da borda) e reduz para 1000 px de altura.
  5. Escreve:
       app/src/main/res/raw/edgar_opening_line_0N.mp3
       app/src/main/res/drawable-nodpi/edgar_line_0N.png
       app/src/main/java/com/pulchradomina/ui/game/opening/EdgarOpeningData.kt   (gerado)

Uso:
  python3 tools/edgar_opening/build_edgar_opening.py \
      --pack "<pasta extraída do pack v3>/resources" [--repo <raiz do repo>] [--preview-dir <pasta>]
"""
import argparse
import os
import re
import subprocess
import sys

import numpy as np
import scipy.io.wavfile as wf
from PIL import Image
from scipy import ndimage

# Janelas (s) onde cada corte pode cair, a partir dos tempos aproximados do txt.
CUT_WINDOWS = [(6.0, 7.5), (15.0, 16.6)]

# Trechos de texto por frase: pedaços separados por reticências (...). Cada pedaço
# corresponde a uma ou mais vozes (uma por trecho separado por vírgula).
EXPECTED_PIECES = [
    ["Mais uma noite...", "Parece que o silêncio daquele quarto vai me consumindo aos poucos."],
    ["Sair pra caminhar por essas ruas frias não resolve nada...",
     "mas ficar lá, parado, com a cabeça cheia de pensamentos, é ainda pior."],
    ["Eu só queria encontrar alguma coisa que fizesse barulho...", "qualquer coisa.",
     "Só pra conseguir abafar tudo isso aqui dentro...", "nem que fosse por alguns minutos."],
]

PORTRAIT_HEIGHT = 1000
BURST_THRESHOLD_DB = -55.0
BURST_MIN_S = 0.12
BURST_MERGE_S = 0.12


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit(f"falhou: {' '.join(cmd)}\n{r.stderr}")
    return r.stdout


def probe_duration(path):
    return float(run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                      "-of", "default=nw=1:nk=1", path]).strip())


def read_dialogue(path):
    """Lê LINE_01..03 do dialogue_lines.txt (sem comentários)."""
    lines, current = {}, None
    for raw in open(path, encoding="utf-8"):
        s = raw.strip()
        if not s or s.startswith("#"):
            continue
        m = re.match(r"^LINE_(\d\d):$", s)
        if m:
            current = int(m.group(1))
            continue
        if current is not None:
            lines[current] = s
            current = None
    return [lines[i] for i in sorted(lines)]


def bursts_of(samples, sr):
    """Trechos de voz: energia acima do limiar, com preenchimento de lacunas curtas."""
    win = int(0.01 * sr)
    n = len(samples) // win
    rms = np.sqrt((samples[: n * win].reshape(n, win) ** 2).mean(1))
    db = 20 * np.log10(rms + 1e-9)
    voiced = db > BURST_THRESHOLD_DB
    runs, i = [], 0
    while i < n:
        if voiced[i]:
            j = i
            while j < n and voiced[j]:
                j += 1
            runs.append([i, j])
            i = j
        else:
            i += 1
    merged = []
    for r in runs:
        if merged and (r[0] - merged[-1][1]) * 0.01 < BURST_MERGE_S:
            merged[-1][1] = r[1]
        else:
            merged.append(r)
    return [(a * 0.01, b * 0.01) for a, b in merged if (b - a) * 0.01 >= BURST_MIN_S]


def choose_cuts(bursts):
    """Corte = meio da maior pausa dentro de cada janela."""
    cuts = []
    for lo, hi in CUT_WINDOWS:
        gaps = [(bursts[k][1], bursts[k + 1][0]) for k in range(len(bursts) - 1)]
        cands = [(g[1] - g[0], g) for g in gaps if lo <= g[0] <= hi or lo <= g[1] <= hi]
        if not cands:
            sys.exit(f"nenhuma pausa na janela {lo}-{hi}s")
        _, g = max(cands)
        cuts.append((g[0] + g[1]) / 2)
    return cuts


def split_commas(piece):
    """'a, b, c.' -> ['a,', ' b,', ' c.'] — cada parte fica com a vírgula à esquerda."""
    parts = re.split(r"(?<=,)", piece)
    return [p for p in parts if p]


def build_sync(line_idx, pieces, bursts_in_line, clip_start_s, line_text):
    """Cada trecho de texto (separado por vírgula) ganha a janela de voz correspondente."""
    segs = []
    burst_k = 0
    text_pos = 0
    for piece in pieces:
        for sub in split_commas(piece):
            if burst_k >= len(bursts_in_line):
                sys.exit(f"frase {line_idx}: mais trechos de texto que de voz em '{piece}'")
            b0, b1 = bursts_in_line[burst_k]
            start = max(0, round((b0 - clip_start_s) * 1000))
            end = round((b1 - clip_start_s) * 1000)
            idx = line_text.index(sub, text_pos)
            segs.append((start, end, idx, idx + len(sub)))
            text_pos = idx + len(sub)
            burst_k += 1
    if burst_k != len(bursts_in_line):
        sys.exit(f"frase {line_idx}: sobraram {len(bursts_in_line) - burst_k} trechos de voz")
    # o último trecho cobre o resto do texto (espaço/pontuação final)
    s0, e0, c0, _ = segs[-1]
    segs[-1] = (s0, e0, c0, len(line_text))
    return segs


def cut_portrait(src, dst):
    im = Image.open(src).convert("RGB")
    a = np.array(im).astype(np.int16)
    mx = a.max(axis=2)
    # fundo = região escura, ligada à borda da imagem
    dark = mx <= 14
    labels, _ = ndimage.label(dark)
    border_labels = set(np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]])))
    border_labels.discard(0)
    bg = np.isin(labels, list(border_labels))
    alpha = np.where(bg, 0, 255).astype(np.float32)
    alpha = ndimage.gaussian_filter(alpha, 0.8)
    rgba = np.dstack([np.array(im), alpha.clip(0, 255).astype(np.uint8)])
    out = Image.fromarray(rgba, "RGBA")
    w = round(out.width * PORTRAIT_HEIGHT / out.height)
    out = out.resize((w, PORTRAIT_HEIGHT), Image.LANCZOS)
    out.save(dst, optimize=True)
    return (w, PORTRAIT_HEIGHT), round(bg.mean() * 100, 1)


def kotlin_str(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pack", required=True, help="pasta resources/ do pacote v3")
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
    ap.add_argument("--workdir", default="/tmp/edgar_opening")
    args = ap.parse_args()

    master = os.path.join(args.pack, "audio", "edgar_opening_full.mp3")
    texts = read_dialogue(os.path.join(args.pack, "dialogue_lines.txt"))
    assert len(texts) == 3, "esperava 3 linhas no dialogue_lines.txt"
    for t, pieces in zip(texts, EXPECTED_PIECES):
        if " ".join(pieces) != t:
            sys.exit(f"texto do txt difere do esperado:\n  txt: {t}\n  pieces: {' '.join(pieces)}")

    os.makedirs(args.workdir, exist_ok=True)
    wav = os.path.join(args.workdir, "master16k.wav")
    run(["ffmpeg", "-v", "error", "-y", "-i", master, "-ac", "1", "-ar", "16000", wav])
    sr, x = wf.read(wav)
    x = x.astype(np.float32) / 32768
    bursts = bursts_of(x, sr)
    total = probe_duration(master)

    expected_counts = [sum(len(split_commas(p)) for p in pcs) for pcs in EXPECTED_PIECES]
    cuts = choose_cuts(bursts)
    starts = [0.0, cuts[0], cuts[1]]
    ends = [cuts[0], cuts[1], total]
    per_line = []
    k = 0
    for li, cnt in enumerate(expected_counts):
        group = bursts[k:k + cnt]
        k += cnt
        if len(group) != cnt:
            sys.exit(f"frase {li + 1}: esperava {cnt} trechos de voz, achei {len(group)} "
                     f"(bursts={len(bursts)}). Verifique o áudio antes de seguir.")
        if not (starts[li] <= group[0][0] and group[-1][1] <= ends[li]):
            sys.exit(f"frase {li + 1}: trechos de voz fora do clip")
        per_line.append(group)
    if k != len(bursts):
        sys.exit(f"sobraram {len(bursts) - k} trechos de voz sem frase")

    raw_dir = os.path.join(args.repo, "app/src/main/res/raw")
    drw_dir = os.path.join(args.repo, "app/src/main/res/drawable-nodpi")
    kt_dir = os.path.join(args.repo, "app/src/main/java/com/pulchradomina/ui/game/opening")
    os.makedirs(raw_dir, exist_ok=True)
    os.makedirs(drw_dir, exist_ok=True)
    os.makedirs(kt_dir, exist_ok=True)

    report, kt_lines = [], []
    for li in range(3):
        dur = ends[li] - starts[li]
        out = os.path.join(raw_dir, f"edgar_opening_line_{li + 1:02d}.mp3")
        fade_out_st = max(0.0, dur - 0.02)
        run(["ffmpeg", "-v", "error", "-y", "-ss", f"{starts[li]:.4f}", "-i", master,
             "-t", f"{dur:.4f}", "-af", f"afade=t=in:st=0:d=0.01,afade=t=out:st={fade_out_st:.4f}:d=0.02",
             "-c:a", "libmp3lame", "-b:a", "128k", "-ac", "1", "-ar", "44100", out])
        real = probe_duration(out)
        segs = build_sync(li + 1, EXPECTED_PIECES[li], per_line[li], starts[li], texts[li])
        dst = os.path.join(drw_dir, f"edgar_line_{li + 1:02d}.png")
        size, bg_pct = cut_portrait(os.path.join(args.pack, "portraits", f"edgar_line_{li + 1:02d}.png"), dst)
        report.append((li + 1, real, len(segs), size, bg_pct))

        seg_lines = ",\n".join(
            f"            SyncSegment(startMs = {s}, endMs = {e}, charStart = {c0}, charEnd = {c1})"
            for s, e, c0, c1 in segs
        )
        kt_lines.append(
            f"    OpeningLineData(\n"
            f"        text = {kotlin_str(texts[li])},\n"
            f"        clipDurationMs = {round(real * 1000)},\n"
            f"        segments = listOf(\n{seg_lines}\n        )\n    )"
        )

    kt = (
        "// GERADO por tools/edgar_opening/build_edgar_opening.py — não editar à mão.\n"
        "package com.pulchradomina.ui.game.opening\n\n"
        "/** Janela de voz de um trecho do texto: tempo (ms, dentro do clip) e índices de caractere. */\n"
        "data class SyncSegment(val startMs: Int, val endMs: Int, val charStart: Int, val charEnd: Int)\n\n"
        "data class OpeningLineData(val text: String, val clipDurationMs: Int, val segments: List<SyncSegment>)\n\n"
        "internal object EdgarOpeningData {\n"
        "    val lines: List<OpeningLineData> = listOf(\n"
        + ",\n".join(kt_lines) + "\n    )\n}\n"
    )
    with open(os.path.join(kt_dir, "EdgarOpeningData.kt"), "w", encoding="utf-8") as f:
        f.write(kt)

    print(f"bursts de voz: {len(bursts)}  cortes: {[round(c, 3) for c in cuts]} s")
    for li, real, nseg, size, bg in report:
        print(f"frase {li}: clip {real:.3f} s | trechos de sync {nseg} | retrato {size[0]}x{size[1]} | fundo removido {bg}%")
    print("Kotlin:", os.path.join(kt_dir, "EdgarOpeningData.kt"))


if __name__ == "__main__":
    main()
