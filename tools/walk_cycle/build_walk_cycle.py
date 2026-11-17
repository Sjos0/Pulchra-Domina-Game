#!/usr/bin/env python3
"""
Walk cycle do Edgar + som de passo (lateral).

Gera a partir da sprite sheet do walk e do vídeo com o som de passos:
  1. Quadros do walk em app/src/main/assets/night_street/walk/walk_NN.png
     (mesma escala e mesma linha de pés do idle: 3680 px de figura = 330 unidades)
  2. Contatos do pé (quadros em que o pé toca o chão): são os quadros onde o afastamento
     entre os pés é máximo. Escolhe 2 por ciclo (um por passo).
  3. Som de um passo: recorte do primeiro passo do áudio do vídeo, em res/raw/sfx_footstep.mp3
  4. Kotlin gerado: ui/game/night/WalkCycleLayout.kt

Uso:
  python3 tools/walk_cycle/build_walk_cycle.py \
      --sprite-dir "<pasta extraída do zip do walk>" \
      --edgar-dir  "<pasta extraída do zip do idle (frames/)>" \
      --video      "<vídeo com o som de passos>.mp4" \
      [--repo <raiz do repo>]
"""
import argparse
import glob
import json
import os
import subprocess
import sys

import numpy as np
from PIL import Image
from scipy import ndimage
import scipy.io.wavfile as wf

IDLE_HEIGHT_PX = 3680       # altura da figura do idle no quadro 4096² (ver build_night_street_assets)
EDGAR_HEIGHT_U = 330        # altura do Edgar no jogo (unidades de referência)
UNITS_PER_PX = EDGAR_HEIGHT_U / IDLE_HEIGHT_PX
TEXTURE_HEIGHT = 300        # altura da textura no APK
FOOT_BAND_PX = 120          # faixa dos pés medida acima do chão
MIN_CONTACT_GAP = 25        # quadros mínimos entre dois contatos (um por passo)
SFX_LEN_S = 0.33            # duração do recorte do passo


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit(f"falhou: {' '.join(cmd)}\n{r.stderr}")
    return r.stdout


def alpha_bbox(im):
    a = np.array(im.getchannel("A")) > 8
    ys, xs = np.where(a)
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def idle_center_x(edgar_dir):
    """Centro horizontal da figura do idle (união dos quadros) — o walk fica centrado nele."""
    boxes = [alpha_bbox(Image.open(p).convert("RGBA")) for p in sorted(glob.glob(os.path.join(edgar_dir, "frames", "frame_*.png")))]
    x0 = min(b[0] for b in boxes)
    x1 = max(b[2] for b in boxes)
    return (x0 + x1) / 2


def walk_meta(sprite_dir):
    """Nomes e durações dos quadros. Os quadros são lidos um a um (4096² cada: não cabem todos na RAM)."""
    meta = json.load(open(os.path.join(sprite_dir, "metadata.json")))
    names = [f["filename"] for f in meta["frames"]]
    durations = {f["duration"] for f in meta["frames"]}
    return names, meta["meta"]["fps"], durations


def open_frame(sprite_dir, name):
    return Image.open(os.path.join(sprite_dir, "frames", name + ".png")).convert("RGBA")


def foot_spread(im):
    """Afastamento horizontal dos pés (faixa inferior da figura), em px."""
    a = np.array(im.getchannel("A")) > 8
    ys, xs = np.where(a)
    ybot = ys.max()
    band = a[ybot - FOOT_BAND_PX: ybot + 1]
    _, bx = np.where(band)
    return int(bx.max() - bx.min())


def pick_contacts(spreads):
    """Dois contatos por ciclo: máximos locais circulares mais fortes, separados ≥ MIN_CONTACT_GAP."""
    n = len(spreads)
    peaks = [i for i in range(n) if spreads[i] >= spreads[(i - 1) % n] and spreads[i] >= spreads[(i + 1) % n]]

    def cd(a, b):
        d = abs(a - b)
        return min(d, n - d)

    chosen = []
    for i in sorted(peaks, key=lambda i: -spreads[i]):
        if all(cd(i, j) >= MIN_CONTACT_GAP for j in chosen):
            chosen.append(i)
        if len(chosen) == 2:
            break
    if len(chosen) != 2:
        sys.exit(f"esperava 2 contatos por ciclo, achei {len(chosen)}")
    return sorted(chosen)


def footstep_sample(video, workdir, out_mp3):
    """Recorte do primeiro passo do áudio (primeiro transiente depois do silêncio inicial)."""
    wav = os.path.join(workdir, "sfx_mono48k.wav")
    run(["ffmpeg", "-v", "error", "-y", "-i", video, "-vn", "-ac", "1", "-ar", "48000", wav])
    sr, x = wf.read(wav)
    x = x.astype(np.float32) / 32768
    # ataque: primeira amostra (em janelas de 1 ms) depois do silêncio inicial que passa de -30 dB
    win = int(0.001 * sr)
    n = len(x) // win
    db = 20 * np.log10(np.sqrt((x[: n * win].reshape(n, win) ** 2).mean(1)) + 1e-9)
    first_loud = next(i for i in range(n) if i > 300 and db[i] > -30)
    onset = first_loud * 0.001
    start = max(0.0, onset - 0.002)   # o impacto cai em t = 0 do som, para sincronizar com o contato
    seg = x[int(start * sr): int((start + SFX_LEN_S) * sr)].copy()
    fade = int(0.03 * sr)
    seg[-fade:] *= np.linspace(1, 0, fade)
    seg = seg / max(1e-6, np.abs(seg).max()) * 0.8
    raw_wav = os.path.join(workdir, "footstep.wav")
    wf.write(raw_wav, sr, (seg * 32767).astype(np.int16))
    run(["ffmpeg", "-v", "error", "-y", "-i", raw_wav, "-c:a", "libmp3lame", "-b:a", "128k", "-ac", "1", out_mp3])
    return onset, len(seg) / sr


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--sprite-dir", required=True)
    ap.add_argument("--edgar-dir", required=True)
    ap.add_argument("--video", required=True)
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
    ap.add_argument("--workdir", default="/tmp/walk_cycle")
    args = ap.parse_args()

    os.makedirs(args.workdir, exist_ok=True)
    out_assets = os.path.join(args.repo, "app/src/main/assets/night_street/walk")
    out_raw = os.path.join(args.repo, "app/src/main/res/raw")
    out_kt = os.path.join(args.repo, "app/src/main/java/com/pulchradomina/ui/game/night/WalkCycleLayout.kt")
    os.makedirs(out_assets, exist_ok=True)
    os.makedirs(out_raw, exist_ok=True)

    names, fps, durations = walk_meta(args.sprite_dir)
    if durations != {33}:
        print("aviso: durações por quadro:", durations, "(usando fps da sprite)")
    n = len(names)

    # passo 1 (medição): caixa comum, afastamento dos pés por quadro
    cx = idle_center_x(args.edgar_dir)
    boxes, spreads = [], []
    for name in names:
        im = open_frame(args.sprite_dir, name)
        boxes.append(alpha_bbox(im))
        spreads.append(foot_spread(im))
        im.close()
    half = max(max(abs(b[0] - cx), abs(b[2] - cx)) for b in boxes)
    x0, x1 = int(np.floor(cx - half)), int(np.ceil(cx + half))
    y0 = min(b[1] for b in boxes) - 4
    y1 = max(b[3] for b in boxes) + 1
    box_w_px, box_h_px = x1 - x0, y1 - y0
    tex_w = round(TEXTURE_HEIGHT * box_w_px / box_h_px)

    # passo 2 (recorte): cada quadro na mesma caixa, na mesma escala do idle
    for i, name in enumerate(names):
        im = open_frame(args.sprite_dir, name)
        im.crop((x0, y0, x1, y1)).resize((tex_w, TEXTURE_HEIGHT), Image.BOX).save(
            os.path.join(out_assets, f"walk_{i:02d}.png"), optimize=True)
        im.close()

    # contatos (a partir das medidas do passo 1)
    contacts = pick_contacts(spreads)
    mean_step_px = float(np.mean([spreads[c] for c in contacts]))
    step_u = mean_step_px * UNITS_PER_PX
    cycle_u = 2 * step_u
    phases = [c / n * cycle_u for c in contacts]

    # 3) som de passo
    onset, sfx_len = footstep_sample(args.video, args.workdir, os.path.join(out_raw, "sfx_footstep.mp3"))

    # 4) Kotlin gerado
    w_u = box_w_px * UNITS_PER_PX
    h_u = box_h_px * UNITS_PER_PX
    kt = f"""// GERADO por tools/walk_cycle/build_walk_cycle.py — não editar à mão.
package com.pulchradomina.ui.game.night

/**
 * Walk cycle lateral do Edgar (quadros em night_street/walk/walk_NN.png, centrados no idle, pés na linha do chão).
 * A animação anda pela distância percorrida: cada ciclo = [CYCLE_UNITS] unidades.
 * Contatos: quadros em que um pé toca o chão; cada um dispara um passo.
 */
internal object WalkCycleLayout {{
    const val FRAME_COUNT = {n}
    /** Fps nativo da sprite sheet (referência). */
    const val NATIVE_FPS = {fps}
    const val FRAME_WIDTH_U = {w_u:.2f}f
    const val FRAME_HEIGHT_U = {h_u:.2f}f
    /** Distância percorrida num ciclo completo (dois passos), em unidades de referência. */
    const val CYCLE_UNITS = {cycle_u:.2f}f
    /** Quadros (0-based) em que um pé toca o chão, na ordem do ciclo. */
    val CONTACT_FRAMES: IntArray = intArrayOf({", ".join(str(c) for c in contacts)})
    /** Mesma escala do idle: a figura de 3680 px tem 330 unidades. */
    const val UNITS_PER_SOURCE_PX = {UNITS_PER_PX:.6f}f
}}
"""
    with open(out_kt, "w", encoding="utf-8") as f:
        f.write(kt)

    print(f"quadros: {n} (fps nativo {fps}) | caixa {box_w_px}x{box_h_px} px -> textura {tex_w}x{TEXTURE_HEIGHT}")
    print(f"centro idle x={cx:.0f} | figura {w_u:.1f} x {h_u:.1f} u")
    print(f"contatos (0-based): {contacts} | afastamento médio {mean_step_px:.0f} px = {step_u:.1f} u por passo")
    print(f"ciclo: {cycle_u:.1f} u | fases dos contatos: {[round(p,1) for p in phases]}")
    print(f"som: primeiro passo em {onset:.3f}s do vídeo, recorte de {sfx_len:.2f}s -> res/raw/sfx_footstep.mp3")
    print("Kotlin:", out_kt)


if __name__ == "__main__":
    main()
