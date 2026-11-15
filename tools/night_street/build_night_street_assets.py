#!/usr/bin/env python3
"""
Pipeline do cenário noturno (Pulchra Domina).

A partir dos PNGs originais (4096² com sprites isolados e fundo transparente) gera:
  1. app/src/main/assets/night_street/*.png          sprites recortados pela caixa alfa e reduzidos
  2. app/src/main/assets/night_street/edgar/idle_NN.png  67 frames do idle, reduzidos
  3. app/src/main/java/com/pulchradomina/ui/game/night/NightStreetLayout.kt  layout (arquivo gerado)
  4. (opcional) prévia PNG da composição ao lado da referência, para conferência

Uso:
  python3 tools/night_street/build_night_street_assets.py \
      --source "<extraído de Pulchara domina Assets.zip>/Assets/Pulchara Domina" \
      --edgar-dir "<extraído de SpriteXP_20261113_150321_poseidle.zip>" \
      --reference "<referência 941x1672 sem marcações>.png" \
      --preview previa.png

Unidades: "unidades de referência" = pixels da imagem 941 x 1672 (9:16).
A altura do Edgar é a do quadrado vermelho da referência (ver EDGAR_* abaixo).
Para mudar a cena, edite as listas deste arquivo e rode de novo; não edite o Kotlin gerado.
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

GROUND_TOP = 1212            # topo da calçada
SIDEWALK_H = 66
ROAD_TOP = GROUND_TOP + SIDEWALK_H
ROAD_TILE_H = 234
SIDEWALK_TILE_W = 412

# Marca vermelha da referência (medida em 941x1672): topo ~995, base ~1254.
EDGAR_FEET_Y = 1254
EDGAR_H = 260                # topo 994 -> base 1254 (traço do desenho incluído)
EDGAR_TEX_H = 384            # altura da textura no APK (reduzida do 4096 original)
EDGAR_FRAMES = 67
EDGAR_FPS = 35

ASSET_SCALE = 1.0            # pixels de textura por unidade de referência
START_PERCENT = 14.0         # Edgar começa entre os dois postes, como na referência

# Luz procedural (mesmos valores vão para o Kotlin)
LIGHT_COLOR_ARGB = 0xFFFFCD8C
LIGHT_BANDS = 8              # degraus de intensidade (look pixel art)
LIGHT_BAND_ALPHA = 0.05
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
TILE_SOURCES = {
    # chave: (arquivo, largura da tile em unidades, altura em unidades)
    "SIDEWALK": ("Cenario_noite/calçada/SpriteXP_20261114_233417.png", SIDEWALK_TILE_W, SIDEWALK_H),
    "ROAD":     ("Cenario_noite/Rua/SpriteXP_20261114_233359.png", BLOCK_W, ROAD_TILE_H),
}

# ------------------------------------------------------------ cena
# Props: sprite, bloco, x local, altura. Camada SKY usa centro_y; as demais usam base (chão).
# parallax: 1.0 acompanha a câmera; menor = mais longe. drift: unidades/s (nuvens).
def props_spec():
    P = []
    def sky(s, b, x, cy, h, flip=False, drift=3.0):
        P.append(dict(s=s, b=b, x=x, cy=cy, h=h, layer="SKY", par=0.18, flip=flip, drift=drift))
    def far(s, b, x, h, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP, h=h, layer="FAR", par=0.5, flip=flip, drift=0.0))
    def mid(s, b, x, h, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP + 6, h=h, layer="MID", par=1.0, flip=flip, drift=0.0))
    def bush(s, b, x, h, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP + 14, h=h, layer="MID", par=1.0, flip=flip, drift=0.0))
    def bench(s, b, x, flip=False):
        P.append(dict(s=s, b=b, x=x, base=GROUND_TOP + 12, h=72, layer="FRONT", par=1.0, flip=flip, drift=0.0))
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
    mid("TREE_1", 0, 150, 740)
    mid("TREE_2", 0, 790, 560)
    bush("BUSH_1", 0, 390, 60)
    bench("BENCH_1", 0, 178)
    bench("BENCH_2", 0, 720)
    # bloco 1 — variação
    sky("CLOUD_3", 1, 220, 255, 140, flip=True, drift=3.0)
    sky("CLOUD_4", 1, 700, 150, 120, drift=2.2)
    sky("CLOUD_1", 1, 860, 340, 120, flip=True, drift=3.2)
    far("CITY_3", 1, 560, 500)
    far("CITY_4", 1, 130, 460)
    mid("TREE_3", 1, 160, 700)
    mid("TREE_4", 1, 800, 520)
    bush("BUSH_2", 1, 400, 56)
    bench("BENCH_2", 1, 178)
    bench("BENCH_1", 1, 720, flip=True)
    # bloco 2 — variação + fim do caminho (clube)
    sky("CLOUD_2", 2, 260, 200, 130, drift=2.8)
    sky("CLOUD_1", 2, 640, 320, 140, flip=True, drift=3.6)
    far("CITY_1", 2, 600, 520, flip=True)
    far("CITY_2", 2, 190, 420, flip=True)
    mid("TREE_5", 2, 160, 720)
    mid("TREE_6", 2, 780, 540, flip=True)
    bush("BUSH_1", 2, 400, 60, flip=True)
    bench("BENCH_1", 2, 178)
    bench("BENCH_2", 2, 720)
    return P

LAMPS_SPEC = [
    dict(b=0, x=266, base=1222, h=419), dict(b=0, x=623, base=1222, h=305),
    dict(b=1, x=266, base=1222, h=419), dict(b=1, x=623, base=1222, h=305),
    dict(b=2, x=266, base=1222, h=419), dict(b=2, x=623, base=1222, h=305),
]
DOORS_SPEC = [
    dict(b=0, x=880, w=61, top=1010, h=202, neon=False),   # porta roxa da referência
    dict(b=2, x=790, w=80, top=1000, h=212, neon=True),    # Clube Nocturne
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

def lamp_light(top, base, h):
    """Elipse de luz: começa perto da lanterna e cai até o chão."""
    head_y = top + 0.06 * h
    span = base - head_y
    return dict(cy=head_y + 0.6 * span, rx=0.46 * span, ry=0.62 * span, head_y=head_y)

def fk(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return (s if "." in s else s + ".0") + "f"

def sprite_top(p):
    return p["cy"] - p["h"] / 2 if p["layer"] == "SKY" else p["base"] - p["h"]

# ------------------------------------------------------------ build
def build(args):
    out_assets = os.path.join(args.repo, "app/src/main/assets/night_street")
    out_kt = os.path.join(args.repo, "app/src/main/java/com/pulchradomina/ui/game/night/NightStreetLayout.kt")
    os.makedirs(out_assets, exist_ok=True)
    os.makedirs(os.path.dirname(out_kt), exist_ok=True)

    props = [dict(p, gx=world_x(p["b"], p["x"])) for p in props_spec()]

    # altura mestre por sprite = maior altura usada (evita reamostrar duas vezes)
    master_h = {}
    for p in props:
        master_h[p["s"]] = max(master_h.get(p["s"], 0), p["h"])
    for lamp in LAMPS_SPEC:
        master_h["LAMP"] = max(master_h.get("LAMP", 0), lamp["h"])

    meta = {}
    for key, rel in SPRITE_SOURCES.items():
        im = load_cropped(os.path.join(args.source, rel))
        th = max(1, round(master_h[key] * ASSET_SCALE))
        tw = max(1, round(th * im.width / im.height))
        fname = f"{key.lower()}.png"
        resize_rgba(im, tw, th).save(os.path.join(out_assets, fname), optimize=True)
        meta[key] = dict(file=fname, aspect=im.width / im.height)

    for key, (rel, tw_u, th_u) in TILE_SOURCES.items():
        im = load_cropped(os.path.join(args.source, rel))
        fname = f"{key.lower()}.png"
        resize_rgba(im, round(tw_u * ASSET_SCALE), round(th_u * ASSET_SCALE)).save(
            os.path.join(out_assets, fname), optimize=True)
        meta[key] = dict(file=fname, aspect=im.width / im.height)

    # Edgar: recorte pela união dos alfas dos frames (sem tremor entre frames), reduzido
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

    # Kotlin gerado
    sprite_names = []
    for key in list(SPRITE_SOURCES) + list(TILE_SOURCES):
        sprite_names.append(f'    {key}("{meta[key]["file"]}")')

    prop_lines = []
    for p in props:
        m = meta[p["s"]]
        prop_lines.append(
            f'        Prop(NightSprite.{p["s"]}, x = {fk(p["gx"])}, top = {fk(sprite_top(p))}, '
            f'h = {fk(p["h"])}, layer = Layer.{p["layer"]}, parallax = {fk(p["par"])}, '
            f'flip = {"true" if p["flip"] else "false"}, drift = {fk(p["drift"])}),')

    lamp_lines = []
    for lamp in LAMPS_SPEC:
        gx = world_x(lamp["b"], lamp["x"])
        lt = lamp_light(lamp["base"] - lamp["h"], lamp["base"], lamp["h"])
        lamp_lines.append(
            f'        Lamp(x = {fk(gx)}, baseY = {fk(lamp["base"])}, h = {fk(lamp["h"])}, '
            f'lightX = {fk(gx)}, lightY = {fk(lt["cy"])}, lightRx = {fk(lt["rx"])}, lightRy = {fk(lt["ry"])}),')

    door_lines = [
        f'        Door(x = {fk(world_x(d["b"], d["x"]))}, w = {fk(d["w"])}, top = {fk(d["top"])}, '
        f'h = {fk(d["h"])}, neon = {"true" if d["neon"] else "false"}),'
        for d in DOORS_SPEC
    ]

    kt = f"""// GERADO por tools/night_street/build_night_street_assets.py — não editar à mão.
package com.pulchradomina.ui.game.night

/** Sprites do cenário noturno (arquivos em app/src/main/assets/night_street/). */
enum class NightSprite(val file: String) {{
{",\n".join(sprite_names)};
}}

enum class Layer {{ SKY, FAR, MID, FRONT }}

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

/** Poste e a elipse de luz procedural que ele projeta no chão. */
data class Lamp(
    val x: Float,
    val baseY: Float,
    val h: Float,
    val lightX: Float,
    val lightY: Float,
    val lightRx: Float,
    val lightRy: Float
)

/** Porta procedural: [x] borda esquerda, [top] topo. [neon] = letreiro do clube. */
data class Door(val x: Float, val w: Float, val top: Float, val h: Float, val neon: Boolean)

internal object NightStreetLayout {{
    const val REF_WIDTH = {REF_W}f
    const val REF_HEIGHT = {REF_H}f
    const val BLOCK_COUNT = {BLOCKS}
    const val WORLD_WIDTH = {WORLD_W}f
    const val GROUND_TOP = {GROUND_TOP}f
    const val SIDEWALK_HEIGHT = {SIDEWALK_H}f
    const val SIDEWALK_TILE_WIDTH = {SIDEWALK_TILE_W}f
    const val ROAD_TOP = {ROAD_TOP}f
    const val ROAD_TILE_HEIGHT = {ROAD_TILE_H}f
    const val EDGAR_HEIGHT = {EDGAR_H}f
    const val EDGAR_FEET_Y = {EDGAR_FEET_Y}f
    const val EDGAR_ASPECT = {fk(edgar_aspect)}
    const val EDGAR_FRAME_COUNT = {EDGAR_FRAMES}
    const val EDGAR_FPS = {EDGAR_FPS}
    const val LIGHT_BANDS = {LIGHT_BANDS}
    const val LIGHT_BAND_ALPHA = {fk(LIGHT_BAND_ALPHA)}
    const val NIGHT_OVERLAY_ALPHA = {fk(NIGHT_OVERLAY_ALPHA)}
    const val LIGHT_COLOR_ARGB = 0x{LIGHT_COLOR_ARGB:08X}
    const val NIGHT_OVERLAY_ARGB = 0x{NIGHT_OVERLAY_ARGB:08X}

    val props: List<Prop> = listOf(
{chr(10).join(prop_lines)}
    )

    val lamps: List<Lamp> = listOf(
{chr(10).join(lamp_lines)}
    )

    val doors: List<Door> = listOf(
{chr(10).join(door_lines)}
    )

}}
"""
    with open(out_kt, "w", encoding="utf-8") as f:
        f.write(kt)
    print(f"assets: {len(meta)} sprites/tiles, {len(frame_paths)} frames Edgar ({ew}x{EDGAR_TEX_H})")
    print("Kotlin:", out_kt)
    return dict(props=props, meta=meta, out_assets=out_assets, edgar_aspect=edgar_aspect)

# ------------------------------------------------------------ prévia
def render_world(built, S):
    """Compõe o mundo inteiro em escala S (camera em x=0), como na pilha do Kotlin."""
    W, H = int(WORLD_W * S), int(REF_H * S)
    arr = sky_gradient(W, H)
    canvas = Image.fromarray(arr, "RGBA")
    meta, base = built["meta"], built["out_assets"]

    def paste(file, x_px, y_px, w_px, h_px, flip=False):
        im = Image.open(os.path.join(base, file)).convert("RGBA").resize(
            (max(1, round(w_px)), max(1, round(h_px))), Image.BOX)
        if flip:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        canvas.alpha_composite(im, (round(x_px), round(y_px)))

    def draw_layer(name):
        for p in built["props"]:
            if p["layer"] != name:
                continue
            w_u = p["h"] * meta[p["s"]]["aspect"]
            paste(meta[p["s"]]["file"], (p["gx"] - w_u / 2) * S, sprite_top(p) * S, w_u * S, p["h"] * S, p["flip"])

    draw_layer("SKY")
    draw_layer("FAR")
    draw_ground(canvas, meta, base, S)
    draw_layer("MID")
    draw_layer("FRONT")
    for lamp in LAMPS_SPEC:
        gx = world_x(lamp["b"], lamp["x"])
        w_u = lamp["h"] * meta["LAMP"]["aspect"]
        paste(meta["LAMP"]["file"], (gx - w_u / 2) * S, (lamp["base"] - lamp["h"]) * S, w_u * S, lamp["h"] * S)
    d = ImageDraw.Draw(canvas)
    for dd in DOORS_SPEC:
        x0 = world_x(dd["b"], dd["x"]) * S
        x1 = x0 + dd["w"] * S
        y0, y1 = dd["top"] * S, (dd["top"] + dd["h"]) * S
        d.rectangle([x0, y0, x1, y1], fill=(18, 12, 31, 255))
        d.rectangle([x0 + 3, y0 + 4, x1 - 3, y1], fill=(88, 46, 150, 255))
        if dd["neon"]:
            d.rectangle([x0 - 2, (dd["top"] - 40) * S, x1 + 2, (dd["top"] - 22) * S], fill=(165, 90, 255, 255))

    # Edgar (frame 0), feet na marca vermelha
    ed = Image.open(os.path.join(base, "edgar", "idle_00.png")).convert("RGBA")
    ew = EDGAR_H * S * built["edgar_aspect"]
    ex = START_PERCENT / 100 * WORLD_W * S - ew / 2
    ey = (EDGAR_FEET_Y - EDGAR_H) * S
    canvas.alpha_composite(ed.resize((round(ew), round(EDGAR_H * S)), Image.BOX), (round(ex), round(ey)))

    # noite: escurece e depois soma a luz dos postes em degraus
    rgb = np.array(canvas.convert("RGB")).astype(np.float32)
    overlay = np.array([(NIGHT_OVERLAY_ARGB >> s) & 0xFF for s in (16, 8, 0)], np.float32)
    rgb = rgb * (1 - NIGHT_OVERLAY_ALPHA) + overlay * NIGHT_OVERLAY_ALPHA
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    light = np.array([(LIGHT_COLOR_ARGB >> s) & 0xFF for s in (16, 8, 0)], np.float32)
    for lamp in LAMPS_SPEC:
        gx = world_x(lamp["b"], lamp["x"])
        lt = lamp_light(lamp["base"] - lamp["h"], lamp["base"], lamp["h"])
        dist = np.sqrt(((xx - gx * S) / (lt["rx"] * S)) ** 2 + ((yy - lt["cy"] * S) / (lt["ry"] * S)) ** 2)
        for i in range(LIGHT_BANDS):
            mask = (dist < 1 - i / LIGHT_BANDS).astype(np.float32)
            rgb += LIGHT_BAND_ALPHA * light[None, None, :] * mask[..., None]
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB")

def sky_gradient(W, H):
    stops = [(0.0, (10, 6, 29)), (0.25, (16, 10, 41)), (0.45, (17, 12, 41)),
             (0.65, (29, 18, 63)), (0.8, (25, 17, 52)), (1.0, (16, 11, 34))]
    grad = np.zeros((H, W, 4), dtype=np.uint8)
    for y in range(H):
        t = y / (H - 1)
        for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
            if t0 <= t <= t1:
                k = (t - t0) / (t1 - t0)
                grad[y, :, :3] = [round(c0[i] + (c1[i] - c0[i]) * k) for i in range(3)]
                break
        grad[y, :, 3] = 255
    return grad

def draw_ground(canvas, meta, base, S):
    sw = Image.open(os.path.join(base, meta["SIDEWALK"]["file"])).convert("RGBA")
    rd = Image.open(os.path.join(base, meta["ROAD"]["file"])).convert("RGBA")
    sw_pw, sw_ph = max(1, round(SIDEWALK_TILE_W * S)), max(1, round(SIDEWALK_H * S))
    rd_pw, rd_ph = max(1, round(BLOCK_W * S)), max(1, round(ROAD_TILE_H * S))
    sw_s, rd_s = sw.resize((sw_pw, sw_ph), Image.BOX), rd.resize((rd_pw, rd_ph), Image.BOX)
    ImageDraw.Draw(canvas).rectangle([0, (ROAD_TOP + ROAD_TILE_H) * S, WORLD_W * S, REF_H * S],
                                     fill=(16, 12, 34, 255))
    for x in range(0, int(WORLD_W * S), sw_pw):
        canvas.alpha_composite(sw_s, (x, round(GROUND_TOP * S)))
    for x in range(0, int(WORLD_W * S), rd_pw):
        canvas.alpha_composite(rd_s, (x, round(ROAD_TOP * S)))

def preview(args, built):
    S = 0.5
    world = render_world(built, S)
    if args.reference:
        ref = Image.open(args.reference).convert("RGB").resize((REF_W // 2, REF_H // 2))
        block0 = world.crop((0, 0, round(BLOCK_W * S), round(REF_H * S)))
        top = Image.new("RGB", (REF_W // 2 * 2 + 10, REF_H // 2), (40, 40, 40))
        top.paste(ref, (0, 0))
        top.paste(block0.resize((REF_W // 2, REF_H // 2)), (REF_W // 2 + 10, 0))
        final = Image.new("RGB", (max(top.width, world.width), top.height + 10 + world.height), (40, 40, 40))
        final.paste(top, (0, 0))
        final.paste(world, (0, top.height + 10))
    else:
        final = world
    final.save(args.preview)
    print("prévia:", args.preview, final.size)

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--source", required=True, help="pasta 'Assets/Pulchara Domina' extraída")
    ap.add_argument("--edgar-dir", required=True, help="pasta extraída do zip do Edgar (contém frames/)")
    ap.add_argument("--reference", default=None, help="PNG da referência 941x1672 (para a prévia)")
    ap.add_argument("--preview", default=None, help="caminho do PNG de prévia")
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
    args = ap.parse_args()
    built = build(args)
    if args.preview:
        preview(args, built)

if __name__ == "__main__":
    main()
