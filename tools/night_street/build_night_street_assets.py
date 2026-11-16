#!/usr/bin/env python3
"""
Pipeline do cenário noturno (Pulchra Domina).

A partir dos PNGs originais (4096² com sprites isolados) gera:
  1. app/src/main/assets/night_street/*.png              sprites recortados pela caixa alfa e reduzidos
  2. app/src/main/assets/night_street/edgar/idle_NN.png  frames do idle, reduzidos
  3. app/src/main/java/com/pulchradomina/ui/game/night/NightStreetLayout.kt  (arquivo gerado)
  4. (opcional) prévias PNG: composição contra a referência e takes da câmera em vários pontos

Uso:
  python3 tools/night_street/build_night_street_assets.py \
      --source "<extraído de Pulchara domina Assets.zip>/Assets/Pulchara Domina" \
      --edgar-dir "<extraído do zip do Edgar>" \
      --reference "<referência 941x1672 sem marcações>.png" \
      --preview-dir /tmp/previews

Unidades: "unidades de referência" = pixels da imagem 941 x 1672 (9:16).
Para mudar a cena, edite as constantes e listas abaixo e rode de novo. Não edite o Kotlin gerado.
"""
import argparse
import glob
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

# ------------------------------------------------------------ geometria
REF_W, REF_H = 941, 1672
BLOCK_W = REF_W              # cada bloco = uma tela 9:16 da referência
BLOCKS = 3                   # mundo = 3 telas
WORLD_W = BLOCK_W * BLOCKS

GROUND_TOP = 1212            # topo (borda de trás) da calçada
SIDEWALK_H = 120             # calçada mais alta que a v1 (66)
ROAD_TOP = GROUND_TOP + SIDEWALK_H
ROAD_TILE_H = 234

# Personagem: altura de desenho e pés. v1 = 260; v2 = 330 (+27%).
EDGAR_H = 330
EDGAR_FEET_Y = 1290          # pés na calçada, no terço da frente
EDGAR_TEX_H = 384            # altura da textura no APK (reduzida do 4096 original)
EDGAR_FRAMES = 67
EDGAR_FPS = 35

ASSET_SCALE = 1.0            # pixels de textura por unidade de referência
START_PERCENT = 14.0         # Edgar começa entre os dois postes, como na referência

# Postes: X e base/altura NÃO mudam (pedido do usuário). Só o sprite é usado como está.
LAMP_BASE = 1222

# Cone de luz (ápice na lâmpada, base no chão). Frações da altura do poste.
CONE_TOP_HALF_RATIO = 0.05   # meia largura no ápice (conectado à lâmpada)
CONE_BASE_HALF_RATIO = 0.38  # meia largura no chão
LIGHT_COLOR_ARGB = 0xFFFFCD8C
LIGHT_BANDS = 9              # degraus de intensidade (mais degraus = transição mais suave)
LIGHT_BAND_ALPHA = 0.05      # por degrau; o ápice soma todos os degraus
BULB_COLOR_ARGB = 0xFFFFE6B4
BULB_W_RATIO = 0.11          # largura da lâmpada, em fração da altura do poste
BULB_H_RATIO = 0.035

# Escurecimento por camada de fundo (multiplicação) + noite global
LAYER_DARKEN = {"FAR": 0.50, "TREE": 0.58, "BUSH": 0.62, "FRONT": 0.74}
NIGHT_OVERLAY_ARGB = 0xFF07041A
NIGHT_OVERLAY_ALPHA = 0.30

# ------------------------------------------------------------ fontes
SPRITE_SOURCES = {
    "MOON":    "Cenario_noite/Lua/SpriteXP_20261114_094043.png",
    "CLOUD_1": "Cenario_noite/Nuvens/SpriteXP_20261114_094352.png",
    "CLOUD_2": "Cenario_noite/Nuvens/SpriteXP_20261114_183016.png",
    "CLOUD_3": "Cenario_noite/Nuvens/SpriteXP_20261114_183232.png",
    "CLOUD_4": "Cenario_noite/Nuvens/SpriteXP_20261114_183329.png",
    "CITY_1":  "Cenario_noite/Fundo predios/SpriteXP_20261114_224842.png",
    "CITY_2":  "Cenario_noite/Fundo predios/SpriteXP_20261114_224449.png",
    "CITY_3":  "Cenario_noite/Fundo predios/SpriteXP_20261114_224616.png",
    "CITY_4":  "Cenario_noite/Fundo predios/SpriteXP_20261114_224637.png",
    "TREE_1":  "Cenario_noite/árvores/SpriteXP_20261113_135835.png",
    "TREE_2":  "Cenario_noite/árvores/SpriteXP_20261113_135524.png",
    "TREE_3":  "Cenario_noite/árvores/SpriteXP_20261113_135901.png",
    "TREE_4":  "Cenario_noite/árvores/SpriteXP_20261113_135938.png",
    "TREE_5":  "Cenario_noite/árvores/SpriteXP_20261113_135958.png",
    "TREE_6":  "Cenario_noite/árvores/SpriteXP_20261113_135744.png",
    "BUSH_1":  "Cenario_noite/Arbustos/SpriteXP_20261114_233136.png",
    "BUSH_2":  "Cenario_noite/Arbustos/SpriteXP_20261114_233207.png",
    "BENCH_1": "Cenario_noite/Bancos/SpriteXP_20261114_234025.png",
    "BENCH_2": "Cenario_noite/Bancos/SpriteXP_20261114_234057.png",
    "LAMP":    "Cenario_noite/Poste/SpriteXP_20261114_234301.png",
}
# (arquivo, largura em unidades ou None = pela proporção do asset, altura em unidades)
TILE_SOURCES = {
    "SIDEWALK": ("Cenario_noite/calçada/SpriteXP_20261114_233417.png", None, SIDEWALK_H),
    "ROAD":     ("Cenario_noite/Rua/SpriteXP_20261114_233359.png", BLOCK_W, ROAD_TILE_H),
}

# ------------------------------------------------------------ cena
# Camadas de desenho (fundo -> frente):
#   SKY  lua/nuvens (sem escurecer)
#   FAR  prédios (escuro)
#   TREE árvores (escuro)
#   BUSH arbustos, atrás dos bancos (escuro)
#   FRONT bancos, atrás da calçada (escuro)
#   [calçada, rua, postes, Edgar] = plano do jogador
def props_spec():
    P = []
    def sky(s, b, x, cy, h, flip=False, drift=3.0):
        P.append(dict(s=s, b=b, x=x, cy=cy, h=h, layer="SKY", par=0.18, flip=flip, drift=drift))
    def far(s, b, x, h, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP, h=h, layer="FAR", par=0.5, flip=flip, drift=0.0))
    def tree(s, b, x, h, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP + 6, h=h, layer="TREE", par=1.0, flip=flip, drift=0.0))
    def bush(s, b, x, h, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP + 4, h=h, layer="BUSH", par=1.0, flip=flip, drift=0.0))
    def bench(s, b, x, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP + 2, h=72, layer="FRONT", par=1.0, flip=flip, drift=0.0))
    def moon(b, x, cy, h):
        P.append(dict(s="MOON", b=b, x=x, cy=cy, h=h, layer="SKY", par=0.03, flip=False, drift=0.0))

    # bloco 0 — composição da referência
    moon(0, 677, 253, 126)
    sky("CLOUD_1", 0, 200, 240, 150)
    sky("CLOUD_2", 0, 830, 110, 132, drift=3.4)
    sky("CLOUD_3", 0, 640, 285, 150, drift=2.6)
    sky("CLOUD_4", 0, 880, 410, 110, drift=3.8)
    far("CITY_1", 0, 620, 560)
    far("CITY_2", 0, 170, 430)
    tree("TREE_1", 0, 150, 740)
    tree("TREE_2", 0, 790, 560)
    bush("BUSH_1", 0, 390, 88)
    bench("BENCH_1", 0, 178)
    bench("BENCH_2", 0, 720)
    # bloco 1 — variação
    sky("CLOUD_3", 1, 220, 255, 140, flip=True, drift=3.0)
    sky("CLOUD_4", 1, 700, 150, 120, drift=2.2)
    sky("CLOUD_1", 1, 860, 340, 120, flip=True, drift=3.2)
    far("CITY_3", 1, 560, 500)
    far("CITY_4", 1, 130, 460)
    tree("TREE_3", 1, 160, 700)
    tree("TREE_4", 1, 800, 520)
    bush("BUSH_2", 1, 400, 84)
    bench("BENCH_2", 1, 178)
    bench("BENCH_1", 1, 720, flip=True)
    # bloco 2 — variação
    sky("CLOUD_2", 2, 260, 200, 130, drift=2.8)
    sky("CLOUD_1", 2, 640, 320, 140, flip=True, drift=3.6)
    far("CITY_1", 2, 600, 520, flip=True)
    far("CITY_2", 2, 190, 420, flip=True)
    tree("TREE_5", 2, 160, 720)
    tree("TREE_6", 2, 780, 540, flip=True)
    bush("BUSH_1", 2, 400, 88, flip=True)
    bench("BENCH_1", 2, 178)
    bench("BENCH_2", 2, 720)
    return P

# X, base e altura de cada poste: IGUAIS à v1 (pedido: não mudar a posição dos postes)
LAMPS_SPEC = [
    dict(b=0, x=266, h=419), dict(b=0, x=623, h=305),
    dict(b=1, x=266, h=419), dict(b=1, x=623, h=305),
    dict(b=2, x=266, h=419), dict(b=2, x=623, h=305),
]

# ------------------------------------------------------------ helpers
def world_x(block, x_local):
    return block * BLOCK_W + x_local

def load_cropped(path):
    im = Image.open(path).convert("RGBA")
    a = np.array(im.getchannel("A"))
    ys, xs = np.where(a > 8)
    box = (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)
    return im.crop(box)

def resize_rgba(im, w, h):
    # premultiplicado evita halos coloridos nas bordas dos sprites
    return im.convert("RGBa").resize((w, h), Image.BOX).convert("RGBA")

def head_bottom_ratio(lamp_crop):
    """Fração da altura do sprite do poste onde termina a cabeça (a lâmpada fica logo abaixo)."""
    a = np.array(lamp_crop.getchannel("A")) > 8
    counts = a.sum(axis=1)
    positive = counts[counts > 0]
    pole = int(np.bincount(positive).argmax())  # largura mais comum = pólo
    # cabeça = sequência contígua de linhas largas que começa no topo (o pedestal da base também é largo)
    y = 0
    while y < len(counts) and counts[y] > 2 * pole:
        y += 1
    return y / lamp_crop.height

def lamp_geometry(lamp, head_ratio):
    top = LAMP_BASE - lamp["h"]
    apex_y = top + head_ratio * lamp["h"]
    return dict(
        x=world_x(lamp["b"], lamp["x"]),
        apex_y=apex_y,
        ground_y=EDGAR_FEET_Y,
        top_half=CONE_TOP_HALF_RATIO * lamp["h"],
        base_half=CONE_BASE_HALF_RATIO * lamp["h"],
        bulb_w=BULB_W_RATIO * lamp["h"],
        bulb_h=BULB_H_RATIO * lamp["h"],
    )

def fk(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return (s if "." in s else s + ".0") + "f"

def sprite_top(p):
    return p["cy"] - p["h"] / 2 if p["layer"] == "SKY" else p["base"] - p["h"]

def darken_argb(f):
    c = round(255 * f)
    return 0xFF000000 | (c << 16) | (c << 8) | c

# ------------------------------------------------------------ build
def build(args):
    out_assets = os.path.join(args.repo, "app/src/main/assets/night_street")
    out_kt = os.path.join(args.repo, "app/src/main/java/com/pulchradomina/ui/game/night/NightStreetLayout.kt")
    os.makedirs(out_assets, exist_ok=True)
    os.makedirs(os.path.dirname(out_kt), exist_ok=True)

    props = [dict(p, gx=world_x(p["b"], p["x"])) for p in props_spec()]

    master_h = {}
    for p in props:
        master_h[p["s"]] = max(master_h.get(p["s"], 0), p["h"])
    master_h["LAMP"] = max(l["h"] for l in LAMPS_SPEC)

    meta = {}
    for key, rel in SPRITE_SOURCES.items():
        im = load_cropped(os.path.join(args.source, rel))
        th = max(1, round(master_h[key] * ASSET_SCALE))
        tw = max(1, round(th * im.width / im.height))
        fname = f"{key.lower()}.png"
        resize_rgba(im, tw, th).save(os.path.join(out_assets, fname), optimize=True)
        meta[key] = dict(file=fname, aspect=im.width / im.height)

    head_ratio = head_bottom_ratio(load_cropped(os.path.join(args.source, SPRITE_SOURCES["LAMP"])))

    tile_w = {}
    for key, (rel, tw_u, th_u) in TILE_SOURCES.items():
        im = load_cropped(os.path.join(args.source, rel))
        if tw_u is None:
            tw_u = round(th_u * im.width / im.height)   # calçada: mantém a proporção do asset
        tile_w[key] = tw_u
        fname = f"{key.lower()}.png"
        resize_rgba(im, round(tw_u * ASSET_SCALE), round(th_u * ASSET_SCALE)).save(
            os.path.join(out_assets, fname), optimize=True)
        meta[key] = dict(file=fname, aspect=im.width / im.height)

    frame_paths = sorted(glob.glob(os.path.join(args.edgar_dir, "frames", "frame_*.png")))
    if len(frame_paths) != EDGAR_FRAMES:
        sys.exit(f"Esperava {EDGAR_FRAMES} frames do Edgar, achei {len(frame_paths)}")
    boxes = []
    for fp in frame_paths:
        a = np.array(Image.open(fp).getchannel("A"))
        ys, xs = np.where(a > 8)
        boxes.append((int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1))
    ub = (min(b[0] for b in boxes), min(b[1] for b in boxes),
          max(b[2] for b in boxes), max(b[3] for b in boxes))
    edgar_dir = os.path.join(out_assets, "edgar")
    os.makedirs(edgar_dir, exist_ok=True)
    for f in glob.glob(os.path.join(edgar_dir, "*.png")):
        os.remove(f)
    edgar_aspect = (ub[2] - ub[0]) / (ub[3] - ub[1])
    ew = round(EDGAR_TEX_H * edgar_aspect)
    for i, fp in enumerate(frame_paths):
        crop = Image.open(fp).convert("RGBA").crop(ub)
        resize_rgba(crop, ew, EDGAR_TEX_H).save(os.path.join(edgar_dir, f"idle_{i:02d}.png"), optimize=True)

    lamps = [lamp_geometry(l, head_ratio) for l in LAMPS_SPEC]

    # ---------------- Kotlin gerado
    sprite_names = [f'    {key}("{meta[key]["file"]}")' for key in list(SPRITE_SOURCES) + list(TILE_SOURCES)]
    prop_lines = []
    for p in props:
        prop_lines.append(
            f'        Prop(NightSprite.{p["s"]}, x = {fk(p["gx"])}, top = {fk(sprite_top(p))}, '
            f'h = {fk(p["h"])}, layer = Layer.{p["layer"]}, parallax = {fk(p["par"])}, '
            f'flip = {"true" if p["flip"] else "false"}, drift = {fk(p["drift"])}),')
    lamp_lines = [
        f'        Lamp(x = {fk(g["x"])}, baseY = {fk(LAMP_BASE)}, h = {fk(l["h"])}, '
        f'apexY = {fk(g["apex_y"])}, groundY = {fk(g["ground_y"])}, topHalf = {fk(g["top_half"])}, '
        f'baseHalf = {fk(g["base_half"])}, bulbW = {fk(g["bulb_w"])}, bulbH = {fk(g["bulb_h"])}),'
        for l, g in zip(LAMPS_SPEC, lamps)
    ]

    kt = f"""// GERADO por tools/night_street/build_night_street_assets.py — não editar à mão.
package com.pulchradomina.ui.game.night

/** Sprites do cenário noturno (arquivos em app/src/main/assets/night_street/). */
enum class NightSprite(val file: String) {{
{",\n".join(sprite_names)};
}}

/** Camadas de fundo -> frente. A calçada, a rua, os postes e o Edgar ficam fora destas camadas. */
enum class Layer {{ SKY, FAR, TREE, BUSH, FRONT }}

/**
 * Prop no mundo, em unidades de referência (941 x 1672).
 * [x] centro horizontal, [top] topo, [h] altura. [parallax] 1f acompanha a câmera.
 * [drift] deslocamento lento em unidades por segundo (nuvens).
 */
data class Prop(
    val sprite: NightSprite,
    val x: Float,
    val top: Float,
    val h: Float,
    val layer: Layer,
    val parallax: Float,
    val flip: Boolean,
    val drift: Float
)

/**
 * Poste. [apexY] = lâmpada (ápice do cone); [groundY] = chão onde o cone termina.
 * [topHalf]/[baseHalf] = meia largura do cone no ápice e no chão.
 */
data class Lamp(
    val x: Float,
    val baseY: Float,
    val h: Float,
    val apexY: Float,
    val groundY: Float,
    val topHalf: Float,
    val baseHalf: Float,
    val bulbW: Float,
    val bulbH: Float
)

internal object NightStreetLayout {{
    const val REF_WIDTH = {REF_W}f
    const val REF_HEIGHT = {REF_H}f
    const val BLOCK_COUNT = {BLOCKS}
    const val WORLD_WIDTH = {WORLD_W}f
    const val GROUND_TOP = {GROUND_TOP}f
    const val SIDEWALK_HEIGHT = {SIDEWALK_H}f
    const val SIDEWALK_TILE_WIDTH = {fk(tile_w["SIDEWALK"])}
    const val ROAD_TOP = {ROAD_TOP}f
    const val ROAD_TILE_HEIGHT = {ROAD_TILE_H}f
    const val EDGAR_HEIGHT = {EDGAR_H}f
    const val EDGAR_FEET_Y = {EDGAR_FEET_Y}f
    const val EDGAR_ASPECT = {fk(edgar_aspect)}
    const val EDGAR_FRAME_COUNT = {EDGAR_FRAMES}
    const val EDGAR_FPS = {EDGAR_FPS}
    const val LIGHT_BANDS = {LIGHT_BANDS}
    const val LIGHT_BAND_ALPHA = {fk(LIGHT_BAND_ALPHA)}
    const val LIGHT_COLOR_ARGB = 0x{LIGHT_COLOR_ARGB:08X}
    const val BULB_COLOR_ARGB = 0x{BULB_COLOR_ARGB:08X}
    const val NIGHT_OVERLAY_ALPHA = {fk(NIGHT_OVERLAY_ALPHA)}
    const val NIGHT_OVERLAY_ARGB = 0x{NIGHT_OVERLAY_ARGB:08X}
    /** Escurecimento multiplicativo por camada de fundo (cor ARGB para ColorFilter.tint). */
    const val FAR_TINT_ARGB = 0x{darken_argb(LAYER_DARKEN["FAR"]):08X}
    const val TREE_TINT_ARGB = 0x{darken_argb(LAYER_DARKEN["TREE"]):08X}
    const val BUSH_TINT_ARGB = 0x{darken_argb(LAYER_DARKEN["BUSH"]):08X}
    const val FRONT_TINT_ARGB = 0x{darken_argb(LAYER_DARKEN["FRONT"]):08X}

    val props: List<Prop> = listOf(
{chr(10).join(prop_lines)}
    )

    val lamps: List<Lamp> = listOf(
{chr(10).join(lamp_lines)}
    )
}}
"""
    with open(out_kt, "w", encoding="utf-8") as f:
        f.write(kt)
    print(f"assets: {len(meta)} sprites/tiles, {len(frame_paths)} frames Edgar ({ew}x{EDGAR_TEX_H})")
    print(f"sidewalk tile: {tile_w['SIDEWALK']}x{SIDEWALK_H}  head ratio: {head_ratio:.4f}")
    print("Kotlin:", out_kt)
    return dict(props=props, meta=meta, out_assets=out_assets, edgar_aspect=edgar_aspect,
                lamps=lamps, tile_w=tile_w)

# ------------------------------------------------------------ prévia (espelha a ordem do Kotlin)
SKY_STOPS = [(0.0, (10, 6, 29)), (0.25, (16, 10, 41)), (0.45, (17, 12, 41)),
             (0.65, (29, 18, 63)), (0.8, (25, 17, 52)), (1.0, (16, 11, 34))]

def sky_gradient(W, H):
    grad = np.zeros((H, W, 4), dtype=np.uint8)
    for y in range(H):
        t = y / max(1, H - 1)
        for (t0, c0), (t1, c1) in zip(SKY_STOPS, SKY_STOPS[1:]):
            if t0 <= t <= t1:
                k = (t - t0) / (t1 - t0)
                grad[y, :, :3] = [round(c0[i] + (c1[i] - c0[i]) * k) for i in range(3)]
                break
        grad[y, :, 3] = 255
    return grad

def render_view(built, cam_units, scale, vp_w, vp_h, edgar_x_units, facing_right, frame, cache):
    """Compõe a tela como o Canvas faz: céu, camadas, calçada, rua, postes, Edgar, noite, luz."""
    meta, base = built["meta"], built["out_assets"]
    canvas = Image.fromarray(sky_gradient(vp_w, vp_h), "RGBA")

    def sprite(file, w_px, h_px):
        key = (file, w_px, h_px)
        if key not in cache:
            cache[key] = Image.open(os.path.join(base, file)).convert("RGBA").resize(
                (max(1, round(w_px)), max(1, round(h_px))), Image.BOX)
        return cache[key]

    def paste(file, left_u, top_u, w_u, h_u, flip=False, darken=None, par=1.0):
        left_px = (left_u - cam_units * par) * scale
        w_px, h_px = w_u * scale, h_u * scale
        if left_px + w_px <= 0 or left_px >= vp_w:
            return
        im = sprite(file, w_px, h_px)
        if flip:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        if darken is not None:
            arr = np.array(im).astype(np.float32)
            arr[..., :3] *= darken
            im = Image.fromarray(arr.astype(np.uint8), "RGBA")
        canvas.alpha_composite(im, (round(left_px), round((top_u) * scale)))

    def layer(name, darken):
        for p in built["props"]:
            if p["layer"] != name:
                continue
            w_u = p["h"] * meta[p["s"]]["aspect"]
            if p["drift"]:
                # nuvens dão a volta no mundo; t = 0 nas prévias
                for k in (-1, 0, 1):
                    paste(meta[p["s"]]["file"], p["gx"] - w_u / 2 + k * WORLD_W, sprite_top(p), w_u, p["h"],
                          p["flip"], None, p["par"])
            else:
                paste(meta[p["s"]]["file"], p["gx"] - w_u / 2, sprite_top(p), w_u, p["h"], p["flip"],
                      darken, p["par"])

    layer("SKY", None)
    layer("FAR", LAYER_DARKEN["FAR"])
    layer("TREE", LAYER_DARKEN["TREE"])
    layer("BUSH", LAYER_DARKEN["BUSH"])
    layer("FRONT", LAYER_DARKEN["FRONT"])

    # calçada e rua (rua = faixa + preenchimento abaixo)
    sw_w = built["tile_w"]["SIDEWALK"]
    for k in range(int(np.ceil(WORLD_W / sw_w))):
        paste(meta["SIDEWALK"]["file"], k * sw_w, GROUND_TOP, sw_w, SIDEWALK_H)
    road_bottom = (ROAD_TOP + ROAD_TILE_H) * scale
    if road_bottom < vp_h:
        ImageDraw.Draw(canvas).rectangle([0, road_bottom, vp_w, vp_h], fill=(16, 12, 34, 255))
    for k in range(int(np.ceil(WORLD_W / BLOCK_W))):
        paste(meta["ROAD"]["file"], k * BLOCK_W, ROAD_TOP, BLOCK_W, ROAD_TILE_H)

    # postes (sprite) e Edgar
    for lamp, g in zip(LAMPS_SPEC, built["lamps"]):
        w_u = lamp["h"] * meta["LAMP"]["aspect"]
        paste(meta["LAMP"]["file"], g["x"] - w_u / 2, LAMP_BASE - lamp["h"], w_u, lamp["h"])
    ed_w = EDGAR_H * built["edgar_aspect"]
    ed = Image.open(os.path.join(base, "edgar", f"idle_{frame:02d}.png")).convert("RGBA")
    ed = ed.resize((max(1, round(ed_w * scale)), max(1, round(EDGAR_H * scale))), Image.BOX)
    if facing_right:
        ed = ed.transpose(Image.FLIP_LEFT_RIGHT)
    ex = round((edgar_x_units - cam_units - ed_w / 2) * scale)
    ey = round((EDGAR_FEET_Y - EDGAR_H) * scale)
    canvas.alpha_composite(ed, (ex, ey))

    # noite: escurece tudo
    rgb = np.array(canvas.convert("RGB")).astype(np.float32)
    overlay = np.array([(NIGHT_OVERLAY_ARGB >> s) & 0xFF for s in (16, 8, 0)], np.float32)
    rgb = rgb * (1 - NIGHT_OVERLAY_ALPHA) + overlay * NIGHT_OVERLAY_ALPHA

    # luz: cone de cada lâmpada, ápice na lâmpada e base no chão (aditivo, em degraus)
    H, W = rgb.shape[:2]
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    yy_u, xx_u = yy / scale, xx / scale + cam_units
    light = np.array([(LIGHT_COLOR_ARGB >> s) & 0xFF for s in (16, 8, 0)], np.float32)
    for g in built["lamps"]:
        apex, ground = g["apex_y"], g["ground_y"]
        for i in range(LIGHT_BANDS):
            f = 1 - i / LIGHT_BANDS
            bottom_y = apex + (ground - apex) * f
            bottom_half = g["top_half"] + (g["base_half"] - g["top_half"]) * f
            half = g["top_half"] + (bottom_half - g["top_half"]) * (yy_u - apex) / max(1e-6, bottom_y - apex)
            mask = ((yy_u >= apex) & (yy_u <= bottom_y) & (np.abs(xx_u - g["x"]) <= half)).astype(np.float32)
            rgb += LIGHT_BAND_ALPHA * light[None, None, :] * mask[..., None]

    # lâmpada acesa: retângulo quente logo abaixo da cabeça do poste
    out = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(out, "RGBA")
    bulb = [(BULB_COLOR_ARGB >> s) & 0xFF for s in (16, 8, 0)]
    for g in built["lamps"]:
        x0 = (g["x"] - cam_units - g["bulb_w"] / 2) * scale
        x1 = (g["x"] - cam_units + g["bulb_w"] / 2) * scale
        y0 = (g["apex_y"] - g["bulb_h"] * 0.4) * scale
        y1 = (g["apex_y"] + g["bulb_h"] * 0.6) * scale
        d.rectangle([x0, y0, x1, y1], fill=(*bulb, 235))
    return out.convert("RGB")

def preview(args, built):
    os.makedirs(args.preview_dir, exist_ok=True)
    cache = {}
    # 1) tela do celular (720x1270, como o print) em vários pontos do caminho
    vp_w, vp_h = 720, 1270
    scale = vp_h / REF_H
    visible = vp_w / scale
    shots = [
        ("01_inicio_olhando_esquerda", START_PERCENT, False, 0),
        ("02_inicio_olhando_direita", START_PERCENT, True, 0),
        ("03_andando_direita_25pct", 25.0, True, 12),
        ("04_passa_no_poste_1", 31.0, True, 24),
        ("05_meio_do_mundo_50pct", 50.0, True, 6),
        ("06_entre_postes_70pct", 70.0, True, 30),
        ("07_fim_do_caminho_96pct", 96.0, True, 18),
    ]
    tiles = []
    for name, pct, right, frame in shots:
        ex = pct / 100 * WORLD_W
        cam = min(max(0.0, ex - visible / 2), WORLD_W - visible)
        im = render_view(built, cam, scale, vp_w, vp_h, ex, right, frame, cache)
        im.save(os.path.join(args.preview_dir, f"{name}.png"))
        tiles.append((name, im))
    sheet_w = len(tiles) * (vp_w // 2 + 8)
    sheet = Image.new("RGB", (sheet_w, vp_h // 2 + 30), (30, 30, 30))
    for i, (name, im) in enumerate(tiles):
        sheet.paste(im.resize((vp_w // 2, vp_h // 2)), (i * (vp_w // 2 + 8), 30))
    ImageDraw.Draw(sheet).text((8, 8), "Edgar em 7 pontos do caminho (inicio -> fim)", fill=(255, 255, 255))
    sheet.save(os.path.join(args.preview_dir, "00_sequencia_movimento.png"))

    # 2) mundo inteiro (3 telas) e comparação com a referência
    S = 0.5
    world = render_view(built, 0, S, int(WORLD_W * S), int(REF_H * S), START_PERCENT / 100 * WORLD_W,
                        True, 0, cache)
    world.save(os.path.join(args.preview_dir, "10_mundo_3_telas.png"))
    if args.reference:
        ref = Image.open(args.reference).convert("RGB").resize((REF_W // 2, REF_H // 2))
        block0 = render_view(built, 0, S, REF_W // 2, REF_H // 2, START_PERCENT / 100 * WORLD_W,
                             True, 0, cache)
        comp = Image.new("RGB", (REF_W // 2 * 2 + 10, REF_H // 2), (40, 40, 40))
        comp.paste(ref, (0, 0))
        comp.paste(block0, (REF_W // 2 + 10, 0))
        comp.save(os.path.join(args.preview_dir, "11_referencia_vs_bloco0.png"))

    # 3) detalhe do poste 2: cone e Edgar iluminado
    Z = 1.6
    lamp = built["lamps"][1]
    vw = 560
    cam = min(max(0.0, lamp["x"] - vw / Z / 2), WORLD_W - vw / Z)
    full = render_view(built, cam, Z, vw, int(REF_H * Z), lamp["x"] - 40, True, 0, cache)
    # recorte vertical: do topo do poste até o chão, com folga
    top_px = int((lamp["apex_y"] - 170) * Z)
    bottom_px = int((EDGAR_FEET_Y + 40) * Z)
    full.crop((0, top_px, vw, bottom_px)).save(os.path.join(args.preview_dir, "12_detalhe_poste2_luz.png"))
    print("prévias em:", args.preview_dir, sorted(os.listdir(args.preview_dir)))

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--source", required=True, help="pasta 'Assets/Pulchara Domina' extraída")
    ap.add_argument("--edgar-dir", required=True, help="pasta extraída do zip do Edgar (contém frames/)")
    ap.add_argument("--reference", default=None, help="PNG da referência 941x1672 (para a prévia)")
    ap.add_argument("--preview-dir", default=None, help="pasta para as prévias")
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
    args = ap.parse_args()
    built = build(args)
    if args.preview_dir:
        preview(args, built)

if __name__ == "__main__":
    main()
