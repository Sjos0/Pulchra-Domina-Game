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
from PIL import Image, ImageChops, ImageDraw

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
EDGAR_FEET_Y = 1248          # pés no meio do passeio claro (1212..1283); a faixa escura abaixo é o meio-fio
EDGAR_TEX_H = 384            # altura da textura no APK (reduzida do 4096 original)
EDGAR_FRAMES = 67
EDGAR_FPS = 35

ASSET_SCALE = 1.0            # pixels de textura por unidade de referência
START_PERCENT = 14.0         # Edgar começa entre os dois postes, como na referência

# Sprites já prontos no repo (sem fundo, pixels preservados): não passam pelo pipeline do zip.
PREBUILT_SPRITES = {
    "ITEM": "item_maveth_flower.png",               # tools/night_intro/build_intro_assets.py
    "ITEM_INDICATOR": "item_maveth_indicator.png",  # contorno de destaque da flor (pulsa); idem
    "LAMP_LIGHT": "lamp_light.png",                 # gerada por write_lamp_light() (cone de luz dos postes)
    "CLUB_SPILL": "club_spill.png",                 # derive_assets(): luz da balada no chão, perto da borda direita
    "CLUB_ACCENTS": "club_accents.png",             # derive_assets(): pontos de neon pixelados na borda direita
}
# Luz da balada por borda (Rim): máscara só das bordas voltadas para a direita (_R) e para a esquerda (_L,
# usada quando o prop está espelhado). Gerada a partir do alfa dos PNGs do cenário (derive_rim_masks()).
RIM_SPRITES = ["TREE_1", "TREE_2", "TREE_3", "TREE_4", "TREE_5", "TREE_6", "BENCH_1", "BENCH_2"]
for _k in RIM_SPRITES:
    PREBUILT_SPRITES[f"RIM_{_k}_R"] = f"rim_{_k.lower()}_r.png"
    PREBUILT_SPRITES[f"RIM_{_k}_L"] = f"rim_{_k.lower()}_l.png"

# Luz da balada (valores em unidades de referência)
CLUB_SPILL_W = 560.0          # largura da faixa de luz que entra pela borda direita
CLUB_SPILL_H = 420.0          # altura (a partir da calçada)
CLUB_SPILL_PX = 4             # pixel art: 1 px da textura = 4 unidades
CLUB_ACCENT_W = 48            # faixa de pontos de neon, colada na borda direita
CLUB_ACCENT_H = 400
CLUB_ACCENT_TOP = 620         # acima dos prédios da direita, sem cobrir o Edgar
CLUB_ACCENT_MARGIN = 10
# Itens no chão da calçada (base na linha dos pés, desenhados depois do chão e antes do Edgar).
GROUND_PROPS_SPEC = [
    dict(s="ITEM", x=2440, base=1252, h=53),   # v3.1: +10% (48 -> 53), mesmo plano dos pés   # perto do fim da linha, no mesmo plano dos pés
]

# Postes: X e base/altura NÃO mudam (pedido do usuário). Só o sprite é usado como está.
LAMP_BASE = 1222

# Cone de luz (ápice na lâmpada, base no chão). Frações da altura do poste.
CONE_TOP_HALF_RATIO = 0.05   # meia largura no ápice (conectado à lâmpada)
CONE_BASE_HALF_RATIO = 0.38  # meia largura no chão
LIGHT_COLOR_ARGB = 0xFFFFCD8C
# Luz do poste: textura contínua gerada aqui (sem trapézios). Ver light_texture_alpha().
LIGHT_TEX_W = 128            # resolução da textura (largura: -1..1 = ±meia-base do cone)
LIGHT_TEX_H = 256            # resolução da textura (altura: ápice -> chão)
LIGHT_PEAK_ALPHA = 0.50      # intensidade máxima, no ápice (junto da lâmpada)
LIGHT_GROUND_KEEP = 0.78     # quanto da intensidade sobra no chão (0 = some, 1 = igual ao ápice)
LIGHT_EDGE_FEATHER = 0.45    # fração da meia-largura usada na borda suave das laterais
LIGHT_DITHER_LEVELS = 14     # níveis de alfa antes do dithering (pixel art)
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

# Altura nativa do sprite lamp.png no repo (px). Mudar a altura de exibição não reamostra o asset.
LAMP_SPRITE_NATIVE_H = 419
# X e base dos postes: IGUAIS à v1 (pedido: não mudar a posição). Alturas: maior 455, menor 360
# (pedido de proporção: os dois crescem, o maior continua visivelmente mais alto).
# v3: só os postes grandes (um por tela). Os pequenos (x=623, 1564, 2505) foram removidos por decisão de design.
LAMPS_SPEC = [
    dict(b=0, x=266, h=455),
    dict(b=1, x=266, h=455),
    dict(b=2, x=266, h=455),
]
# Razão cabeça/altura do poste (medida na fonte original). Constante: o Kotlin não depende de medir o zip.
LAMP_HEAD_RATIO = 0.0833407

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

def lamp_geometry(lamp):
    top = LAMP_BASE - lamp["h"]
    apex_y = top + LAMP_HEAD_RATIO * lamp["h"]
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

# ------------------------------------------------------------ derivados (a partir dos PNGs do repo)
ROAD_BASE_REL = "tools/night_street/sources/road_base.png"   # asfalto sem textura (entrada de derive_road)
BAYER2 = np.array([[0, 2], [3, 1]], np.float32) / 4.0

def _bayer_tile(h, w, base=BAYER2):
    return np.tile(base, (h // base.shape[0] + 1, w // base.shape[1] + 1))[:h, :w]

def _sides(solid):
    """Para cada pixel sólido: está na borda direita? (vizinho a +d é vazio ou fora da imagem)."""
    H, W = solid.shape
    def right_empty(d):
        out = np.ones_like(solid)
        out[:, :W - d] = ~solid[:, d:]
        return out
    def left_empty(d):
        out = np.ones_like(solid)
        out[:, d:] = ~solid[:, :W - d]
        return out
    return right_empty, left_empty

def rim_masks(alpha):
    """
    Máscaras de borda voltada para a balada (direita), em pixel art:
      - 1 px de borda: opaco;
      - 2º px: pontilhado (Bayer 2x2), para não virar contorno contínuo nem 'adesivo'.
    Retorna (máscara das bordas direitas, máscara das bordas esquerdas), alfa 0..255.
    Usa só o alfa do sprite (pixels da arte não mudam).
    """
    solid = alpha >= 128
    right_empty, left_empty = _sides(solid)
    H, W = solid.shape
    dot = _bayer_tile(H, W) >= 0.5
    out = []
    for edge, first in ((right_empty(1), right_empty), (left_empty(1), left_empty)):
        second = first(2)
        band1 = solid & edge
        band2 = solid & ~edge & second
        m = np.zeros(solid.shape, np.uint8)
        m[band1] = 255
        m[band2] = 175   # 2º px sólido: a borda lê como linha, não como poeira (v3.1)
        out.append(m)
    return out[0], out[1]

def derive_road(out_assets, base_path):
    """
    Asfalto de textura média: manchas claras/escuras de 2x2 px e poucos pontos escuros, espalhados
    de forma determinística (seed fixa) e com wrap horizontal (a faixa se repete sem costura).
    O alfa não muda. O asfalto continua mais escuro que a calçada.
    """
    base = np.array(Image.open(base_path).convert("RGBA")).astype(np.float32)
    H, W = base.shape[:2]
    solid = (base[..., 3] > 8).astype(np.float32)
    rng = np.random.default_rng(20261119)
    delta = np.zeros((H, W), np.float32)
    n = int(W * H * 0.035)
    ys = rng.integers(0, H, n)
    xs = rng.integers(0, W, n)
    d = rng.choice(np.array([-14, -9, 6, 10], np.float32), n)
    for dy in (0, 1):
        for dx in (0, 1):
            np.add.at(delta, ((ys + dy) % H, (xs + dx) % W), d)
    m = int(W * H * 0.006)
    ys2 = rng.integers(0, H, m)
    xs2 = rng.integers(0, W, m)
    np.add.at(delta, (ys2, xs2), -18)
    rgb = base[..., :3] + (delta * solid)[..., None]
    rgb = np.clip(rgb, 0, 255)
    out = np.dstack([rgb, base[..., 3]]).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(os.path.join(out_assets, "road.png"), optimize=True)
    return float(rgb.mean()), float(rgb.std())

def derive_rim_masks(out_assets):
    for key in RIM_SPRITES:
        name = key.lower()
        with Image.open(os.path.join(out_assets, f"{name}.png")) as im:
            alpha = np.array(im.convert("RGBA").getchannel("A"))
        right, left = rim_masks(alpha)
        for suffix, arr in (("r", right), ("l", left)):
            rgba = np.zeros(arr.shape + (4,), np.uint8)
            rgba[..., :3] = 255
            rgba[..., 3] = arr
            Image.fromarray(rgba, "RGBA").save(os.path.join(out_assets, f"rim_{name}_{suffix}.png"), optimize=True)

def derive_club_spill(out_assets):
    """
    Luz da balada no chão: textura de pixel art (1 px = CLUB_SPILL_PX unidades). Forte junto da borda
    direita, cai até o centro; mais forte perto da calçada e some para baixo. Dithering Bayer, sem blur.
    """
    w = round(CLUB_SPILL_W / CLUB_SPILL_PX)
    h = round(CLUB_SPILL_H / CLUB_SPILL_PX)
    u = np.arange(w, dtype=np.float32) / (w - 1)
    v = np.arange(h, dtype=np.float32) / (h - 1)
    a = (u[None, :] ** 2.2) * ((1 - 0.7 * v[:, None]) ** 1.3) * 0.9
    levels = 10
    a = np.floor(a * levels + _bayer_tile(h, w, BAYER4)) / levels
    rgba = np.zeros((h, w, 4), np.uint8)
    rgba[..., :3] = 255
    rgba[..., 3] = np.round(a * 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(out_assets, "club_spill.png"), optimize=True)

def derive_club_accents(out_assets):
    """Pontos de neon pixelados (clusters dithered), colados na borda direita. Mesma cor do momento."""
    w, h = CLUB_ACCENT_W, CLUB_ACCENT_H
    rng = np.random.default_rng(7351)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    alpha = np.zeros((h, w), np.float32)
    bay = _bayer_tile(h, w, BAYER4)
    for _ in range(6):
        cx = rng.uniform(10, w - 8)
        cy = rng.uniform(20, h - 20)
        r = rng.uniform(6.0, 10.0)
        dist = np.sqrt((xx - cx) ** 2 + ((yy - cy) * 1.3) ** 2) / r
        p = np.clip(1.0 - dist, 0, 1)
        alpha = np.maximum(alpha, np.where(p > bay, 1.0, 0.0) * (p > 0))
    rgba = np.zeros((h, w, 4), np.uint8)
    rgba[..., :3] = 255
    rgba[..., 3] = (alpha * 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(out_assets, "club_accents.png"), optimize=True)

def derive_assets(out_assets, road_base):
    """Tudo que o pipeline cria sem o zip original. Determinístico: rodar duas vezes dá o mesmo resultado."""
    road_mean, road_std = derive_road(out_assets, road_base)
    derive_rim_masks(out_assets)
    derive_club_spill(out_assets)
    derive_club_accents(out_assets)
    print(f"asfalto: luminância média {road_mean:.1f}, desvio {road_std:.1f}")

# ------------------------------------------------------------ build
BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], np.float32) / 16.0

def light_texture_alpha():
    """
    Alfa do cone de luz em espaço normalizado (H x W), com:
      - ápice na lâmpada (v = 0) e base no chão (v = 1);
      - meia-largura igual à do cone de jogo (CONE_TOP/BASE_HALF_RATIO), em fração da meia-base;
      - borda lateral suave (sem linha dura);
      - intensidade decrescente do ápice ao chão;
      - dithering Bayer 4x4 nos níveis de alfa (textura de pixel art, sem degraus).
    """
    top_ratio = CONE_TOP_HALF_RATIO / CONE_BASE_HALF_RATIO
    v = (np.arange(LIGHT_TEX_H, dtype=np.float32) + 0.5) / LIGHT_TEX_H
    u = np.abs((np.arange(LIGHT_TEX_W, dtype=np.float32) + 0.5) / LIGHT_TEX_W * 2 - 1)
    half = (top_ratio + (1 - top_ratio) * v)[:, None]
    edge = np.maximum(half * LIGHT_EDGE_FEATHER, 1e-6)
    t = np.clip((half - u[None, :]) / edge, 0.0, 1.0)
    side = t * t * (3 - 2 * t)
    fall = ((1 - LIGHT_GROUND_KEEP * v) ** 1.2)[:, None]   # ainda há luz no chão (ilumina a calçada)
    alpha = LIGHT_PEAK_ALPHA * side * fall
    bayer = np.tile(BAYER4, (LIGHT_TEX_H // 4, LIGHT_TEX_W // 4))
    return np.floor(alpha * LIGHT_DITHER_LEVELS + bayer) / LIGHT_DITHER_LEVELS

def write_lamp_light(out_assets):
    alpha = light_texture_alpha()
    rgba = np.zeros((LIGHT_TEX_H, LIGHT_TEX_W, 4), np.uint8)
    rgba[..., 0] = (LIGHT_COLOR_ARGB >> 16) & 0xFF
    rgba[..., 1] = (LIGHT_COLOR_ARGB >> 8) & 0xFF
    rgba[..., 2] = LIGHT_COLOR_ARGB & 0xFF
    rgba[..., 3] = np.round(alpha * 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(out_assets, "lamp_light.png"), optimize=True)

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

def repo_png(out_assets, fname):
    with Image.open(os.path.join(out_assets, fname)) as im:
        return dict(file=fname, aspect=im.width / im.height, w=im.width, h=im.height)

HINT_SVG_REL = "tools/night_street/sources/hand_hint.svg"
ITEM_HINT_RADIUS = 10.0   # pontos percentuais do mundo; = ExplorationRules.ITEM_HINT_RADIUS

def read_hand_path(repo):
    """Path (d) do ícone de mão em tools/night_street/sources/hand_hint.svg (viewBox 0 0 24 24)."""
    import re
    svg = open(os.path.join(repo, HINT_SVG_REL), encoding="utf-8").read()
    m = re.search(r' d="([^"]+)"', svg)
    if not m:
        sys.exit("hand_hint.svg sem atributo d")
    return m.group(1)

def build(args):
    out_assets = os.path.join(args.repo, "app/src/main/assets/night_street")
    out_kt = os.path.join(args.repo, "app/src/main/java/com/pulchradomina/ui/game/night/NightStreetLayout.kt")
    os.makedirs(out_assets, exist_ok=True)
    os.makedirs(os.path.dirname(out_kt), exist_ok=True)
    road_base = os.path.join(args.repo, ROAD_BASE_REL)

    props = [dict(p, gx=world_x(p["b"], p["x"])) for p in props_spec()]

    master_h = {}
    for p in props:
        master_h[p["s"]] = max(master_h.get(p["s"], 0), p["h"])
    master_h["LAMP"] = LAMP_SPRITE_NATIVE_H

    meta = {}
    tile_w = {}
    if args.from_repo:
        # Modo reprodutível: parte dos PNGs já gerados em app/src/main/assets (não precisa do zip original).
        for key in SPRITE_SOURCES:
            meta[key] = repo_png(out_assets, f"{key.lower()}.png")
        for key in TILE_SOURCES:
            meta[key] = repo_png(out_assets, f"{key.lower()}.png")
            tile_w[key] = meta[key]["w"]
        frame_names = sorted(glob.glob(os.path.join(out_assets, "edgar", "idle_*.png")))
        if len(frame_names) != EDGAR_FRAMES:
            sys.exit(f"Esperava {EDGAR_FRAMES} frames do Edgar no repo, achei {len(frame_names)}")
        with Image.open(frame_names[0]) as im:
            edgar_aspect = im.width / im.height
    else:
        for key, rel in SPRITE_SOURCES.items():
            im = load_cropped(os.path.join(args.source, rel))
            th = max(1, round(master_h[key] * ASSET_SCALE))
            tw = max(1, round(th * im.width / im.height))
            fname = f"{key.lower()}.png"
            resize_rgba(im, tw, th).save(os.path.join(out_assets, fname), optimize=True)
            meta[key] = dict(file=fname, aspect=im.width / im.height)

        measured = head_bottom_ratio(load_cropped(os.path.join(args.source, SPRITE_SOURCES["LAMP"])))
        if abs(measured - LAMP_HEAD_RATIO) > 0.01:
            print(f"aviso: cabeça do poste medida {measured:.4f}, constante {LAMP_HEAD_RATIO}")

        for key, (rel, tw_u, th_u) in TILE_SOURCES.items():
            im = load_cropped(os.path.join(args.source, rel))
            if tw_u is None:
                tw_u = round(th_u * im.width / im.height)   # calçada: mantém a proporção do asset
            tile_w[key] = tw_u
            fname = f"{key.lower()}.png"
            tile = resize_rgba(im, round(tw_u * ASSET_SCALE), round(th_u * ASSET_SCALE))
            tile.save(os.path.join(out_assets, fname), optimize=True)
            meta[key] = dict(file=fname, aspect=im.width / im.height)
            if key == "ROAD":
                os.makedirs(os.path.dirname(road_base), exist_ok=True)
                tile.save(road_base, optimize=True)   # base sem textura (entrada de derive_assets)

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

    write_lamp_light(out_assets)
    derive_assets(out_assets, road_base)
    for key, rel in PREBUILT_SPRITES.items():
        meta[key] = repo_png(out_assets, rel)

    lamps = [lamp_geometry(l) for l in LAMPS_SPEC]
    ground_props = [dict(s=g["s"], gx=g["x"], base=g["base"], h=g["h"]) for g in GROUND_PROPS_SPEC]

    # ---------------- Kotlin gerado
    sprite_names = [f'    {key}("{meta[key]["file"]}")' for key in list(SPRITE_SOURCES) + list(TILE_SOURCES) + list(PREBUILT_SPRITES)]
    prop_lines = []
    for p in props:
        prop_lines.append(
            f'        Prop(NightSprite.{p["s"]}, x = {fk(p["gx"])}, top = {fk(sprite_top(p))}, '
            f'h = {fk(p["h"])}, layer = Layer.{p["layer"]}, parallax = {fk(p["par"])}, '
            f'flip = {"true" if p["flip"] else "false"}, drift = {fk(p["drift"])}),')
    ground_lines = [
        f'        Prop(NightSprite.{g["s"]}, x = {fk(g["gx"])}, top = {fk(g["base"] - g["h"])}, '
        f'h = {fk(g["h"])}, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),'
        for g in ground_props
    ]
    hand_path = read_hand_path(args.repo)
    rim_lines = [
        f'        NightSprite.{k} to (NightSprite.RIM_{k}_R to NightSprite.RIM_{k}_L),' for k in RIM_SPRITES
    ]
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

    /** Itens no chão da calçada (desenhados depois do chão, antes dos postes e do Edgar). Camada não usada. */
    val groundProps: List<Prop> = listOf(
{chr(10).join(ground_lines)}
    )

    val lamps: List<Lamp> = listOf(
{chr(10).join(lamp_lines)}
    )

    /** Máscara de borda (luz da balada) por sprite: (bordas direitas, bordas esquerdas espelhadas). */
    val rimMasks: Map<NightSprite, Pair<NightSprite, NightSprite>> = mapOf(
{chr(10).join(rim_lines)}
    )

    /** Luz da balada no chão: tamanho em unidades e altura do topo dos pontos de neon. */
    const val CLUB_SPILL_W = {fk(CLUB_SPILL_W)}
    const val CLUB_SPILL_H = {fk(CLUB_SPILL_H)}
    const val CLUB_ACCENT_W = {CLUB_ACCENT_W}f
    const val CLUB_ACCENT_H = {CLUB_ACCENT_H}f
    const val CLUB_ACCENT_TOP = {CLUB_ACCENT_TOP}f
    const val CLUB_ACCENT_MARGIN = {CLUB_ACCENT_MARGIN}f

    /** Ícone de mão do aviso de item (SVG, viewBox 0 0 24 24), de tools/night_street/sources/hand_hint.svg. */
    const val ITEM_HINT_HAND_PATH = "{hand_path}"
}}
"""
    with open(out_kt, "w", encoding="utf-8") as f:
        f.write(kt)
    print(f"assets: {len(meta)} sprites/tiles, {EDGAR_FRAMES} frames Edgar, aspecto {edgar_aspect:.4f}")
    print(f"sidewalk tile: {tile_w['SIDEWALK']}x{SIDEWALK_H}  cabeça do poste: {LAMP_HEAD_RATIO}")
    print("Kotlin:", out_kt)
    return dict(props=props, meta=meta, out_assets=out_assets, edgar_aspect=edgar_aspect,
                lamps=lamps, tile_w=tile_w, ground_props=ground_props)

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

def smoothstep01(x):
    t = min(1.0, max(0.0, x))
    return t * t * (3 - 2 * t)

def render_view(built, cam_units, scale, vp_w, vp_h, edgar_x_units, facing_right, frame, cache,
                club=None):
    """
    Compõe a tela como o Canvas faz: céu, camadas, calçada, rua, postes, Edgar, noite, luz.
    club = (cor RGB 0..255, nível 0..0.9) da balada. None = sem luz da balada (antes da v3).
    Prévia em CPU: a soma aditiva do Kotlin é aproximada com ImageChops.add nas mesmas posições.
    """
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

    def add_light(file, left_u, top_u, w_u, h_u, flip, color, k):
        """Soma aditiva de um sprite-máscara colorido (Rim, faixa, neon). Mesmo esquema do Kotlin."""
        if k <= 0:
            return
        left_px = (left_u - cam_units) * scale
        w_px, h_px = w_u * scale, h_u * scale
        if left_px + w_px <= 0 or left_px >= vp_w:
            return
        im = sprite(file, w_px, h_px)
        if flip:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        a = np.array(im.getchannel("A")).astype(np.float32) / 255.0 * k
        if isinstance(color, tuple):
            col = np.array(color, np.float32)
        else:
            col = np.array([(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF], np.float32)
        layer_img = Image.fromarray(np.clip(a[..., None] * col, 0, 255).astype(np.uint8), "RGB")
        x0, y0 = round(left_px), round(top_u * scale)
        box = (max(0, x0), max(0, y0), min(vp_w, x0 + layer_img.width), min(vp_h, y0 + layer_img.height))
        if box[2] <= box[0] or box[3] <= box[1]:
            return
        part = layer_img.crop((box[0] - x0, box[1] - y0, box[2] - x0, box[3] - y0))
        region = canvas.crop(box).convert("RGB")
        canvas.paste(ImageChops.add(region, part).convert("RGBA"), box)

    layer("SKY", None)
    layer("FAR", LAYER_DARKEN["FAR"])
    layer("TREE", LAYER_DARKEN["TREE"])
    layer("BUSH", LAYER_DARKEN["BUSH"])
    layer("FRONT", LAYER_DARKEN["FRONT"])

    # Rim: bordas de árvores e bancos voltadas para a balada (depois da camada, antes do Edgar)
    if club is not None:
        rgb_c, level = club
        for p in built["props"]:
            if p["s"] not in RIM_SPRITES or p["layer"] == "SKY":
                continue
            key = f"RIM_{p['s']}_{'L' if p['flip'] else 'R'}"
            w_u = p["h"] * meta[p["s"]]["aspect"]
            k = smoothstep01((p["gx"] / WORLD_W - 0.35) / 0.6) * level * RIM_GAIN_PREVIEW
            add_light(meta[key]["file"], p["gx"] - w_u / 2, sprite_top(p), w_u, p["h"], p["flip"], rgb_c, k)

    # calçada e rua (rua = faixa + preenchimento abaixo)
    sw_w = built["tile_w"]["SIDEWALK"]
    for k in range(int(np.ceil(WORLD_W / sw_w))):
        paste(meta["SIDEWALK"]["file"], k * sw_w, GROUND_TOP, sw_w, SIDEWALK_H)
    road_bottom = (ROAD_TOP + ROAD_TILE_H) * scale
    if road_bottom < vp_h:
        ImageDraw.Draw(canvas).rectangle([0, road_bottom, vp_w, vp_h], fill=(16, 12, 34, 255))
    for k in range(int(np.ceil(WORLD_W / BLOCK_W))):
        paste(meta["ROAD"]["file"], k * BLOCK_W, ROAD_TOP, BLOCK_W, ROAD_TILE_H)
    for g in built["ground_props"]:
        w_u = g["h"] * meta[g["s"]]["aspect"]
        paste(meta[g["s"]]["file"], g["gx"] - w_u / 2, g["base"] - g["h"], w_u, g["h"])

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
    canvas = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")

    if club is not None:
        rgb_c, level = club
        # faixa da balada no chão (borda direita), depois a luz dos postes
        add_light("club_spill.png", WORLD_W - CLUB_SPILL_W, GROUND_TOP, CLUB_SPILL_W, CLUB_SPILL_H,
                  False, rgb_c, level * SPILL_GAIN_PREVIEW)
        # o array rgb volta a ser a base dos passos seguintes (postes, cones)
        rgb = np.array(canvas.convert("RGB")).astype(np.float32)

    # luz: cone de cada lâmpada, ápice na lâmpada e base no chão (aditivo, em degraus)
    H, W = rgb.shape[:2]
    light_tex = np.array(Image.open(os.path.join(built["out_assets"], "lamp_light.png")).convert("RGBA")).astype(np.float32)
    for g in built["lamps"]:
        apex, ground = g["apex_y"], g["ground_y"]
        left_u = g["x"] - g["base_half"]
        w_px = max(1, round(2 * g["base_half"] * scale))
        h_px = max(1, round((ground - apex) * scale))
        tex = np.array(Image.fromarray(light_tex.astype(np.uint8), "RGBA").resize((w_px, h_px), Image.NEAREST)).astype(np.float32)
        x0 = round((left_u - cam_units) * scale)
        y0 = round(apex * scale)
        ys0, xs0 = max(0, y0), max(0, x0)
        ys1, xs1 = min(H, y0 + h_px), min(W, x0 + w_px)
        if ys1 > ys0 and xs1 > xs0:
            a = tex[ys0 - y0:ys1 - y0, xs0 - x0:xs1 - x0, 3:4] / 255.0
            rgb[ys0:ys1, xs0:xs1] += tex[ys0 - y0:ys1 - y0, xs0 - x0:xs1 - x0, :3] * a

    # lâmpada acesa: retângulo quente logo abaixo da cabeça do poste
    out = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    if club is not None:
        rgb_c, level = club
        # pontos de neon colados na borda direita da tela
        screen_right = vp_w / scale
        add_light("club_accents.png", cam_units + screen_right - CLUB_ACCENT_W - CLUB_ACCENT_MARGIN,
                  CLUB_ACCENT_TOP, CLUB_ACCENT_W, CLUB_ACCENT_H, False, rgb_c, level * ACCENT_GAIN_PREVIEW)
    d = ImageDraw.Draw(out, "RGBA")
    bulb = [(BULB_COLOR_ARGB >> s) & 0xFF for s in (16, 8, 0)]
    for g in built["lamps"]:
        x0 = (g["x"] - cam_units - g["bulb_w"] / 2) * scale
        x1 = (g["x"] - cam_units + g["bulb_w"] / 2) * scale
        y0 = (g["apex_y"] - g["bulb_h"] * 0.4) * scale
        y1 = (g["apex_y"] + g["bulb_h"] * 0.6) * scale
        d.rectangle([x0, y0, x1, y1], fill=(*bulb, 235))
    # aviso da mão sobre o item: só quando o Edgar está a até ITEM_HINT_RADIUS do item
    item = next((g for g in built["ground_props"] if g["s"] == "ITEM"), None)
    if item is not None:
        hint = item_hint_alpha(edgar_x_units / WORLD_W * 100, item["gx"] / WORLD_W * 100)
        if hint > 0:
            draw_item_hint(out, item, cam_units, scale, hint)
    return out.convert("RGB")

RIM_GAIN_PREVIEW = 1.23       # espelha ClubLights.RIM_GAIN (v3.1)
SPILL_GAIN_PREVIEW = 0.6      # espelha ClubLights.SPILL_GAIN
ACCENT_GAIN_PREVIEW = 0.9     # espelha ClubLights.ACCENT_GAIN
CLUB_PREVIEW_COLOR = 0xFF2EC8  # magenta (primeira cor da sequência)
CLUB_PREVIEW_LEVEL = 0.28 + 0.62 * 0.6   # nível com pulso médio

_HAND_POLY = None

def hand_polygon():
    """Contorno da mão (coordenadas 0..24) amostrado do mesmo path do Kotlin. Só a prévia usa."""
    global _HAND_POLY
    if _HAND_POLY is None:
        from svgpathtools import parse_path
        import re
        svg = open(os.path.join(REPO_ROOT, HINT_SVG_REL), encoding="utf-8").read()
        path = parse_path(re.search(r' d="([^"]+)"', svg).group(1))
        pts = []
        for seg in path:
            for i in range(13):
                z = seg.point(i / 12)
                pts.append((z.real, z.imag))
        _HAND_POLY = pts
    return _HAND_POLY

def item_hint_alpha(player_pct, item_pct):
    """Regra do jogo: aviso aparece a até ITEM_HINT_RADIUS pontos percentuais do item."""
    return 1.0 if abs(item_pct - player_pct) <= ITEM_HINT_RADIUS else 0.0

def draw_item_hint(out, g, cam_units, scale, alpha):
    """Círculo branco vazado + mão branca sobre o item (espelha ItemHint.kt)."""
    cx = (g["gx"] - cam_units) * scale
    cy = (g["base"] - g["h"] / 2) * scale
    r = g["h"] * 0.9 * scale * (0.7 + 0.3 * alpha)
    layer = Image.new("RGBA", out.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    a = int(255 * alpha)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(255, 255, 255, a), width=max(2, round(r * 0.08)))
    side = r * 1.1
    pts = [(cx - side / 2 + x * side / 24, cy - side / 2 + y * side / 24) for x, y in hand_polygon()]
    d.polygon(pts, fill=(255, 255, 255, a))
    out.alpha_composite(layer)

def preview(args, built, club=True, club_level=None):
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
        ("08_perto_da_flor_aviso", 86.4, True, 0),
        ("09_longe_da_flor_sem_aviso", 60.0, True, 0),
    ]
    tiles = []
    level = CLUB_PREVIEW_LEVEL if club_level is None else club_level
    club_state = (((CLUB_PREVIEW_COLOR >> 16) & 0xFF, (CLUB_PREVIEW_COLOR >> 8) & 0xFF, CLUB_PREVIEW_COLOR & 0xFF),
                  level) if club else None
    for name, pct, right, frame in shots:
        ex = pct / 100 * WORLD_W
        cam = min(max(0.0, ex - visible / 2), WORLD_W - visible)
        im = render_view(built, cam, scale, vp_w, vp_h, ex, right, frame, cache, club=club_state)
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
    club_state = (((CLUB_PREVIEW_COLOR >> 16) & 0xFF, (CLUB_PREVIEW_COLOR >> 8) & 0xFF, CLUB_PREVIEW_COLOR & 0xFF),
                  CLUB_PREVIEW_LEVEL) if club else None
    world = render_view(built, 0, S, int(WORLD_W * S), int(REF_H * S), START_PERCENT / 100 * WORLD_W,
                        True, 0, cache, club=club_state)
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
    ap.add_argument("--source", help="pasta 'Assets/Pulchara Domina' extraída (modo completo)")
    ap.add_argument("--edgar-dir", help="pasta extraída do zip do Edgar (contém frames/) (modo completo)")
    ap.add_argument("--reference", default=None, help="PNG da referência 941x1672 (para a prévia)")
    ap.add_argument("--preview-dir", default=None, help="pasta para as prévias")
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
    ap.add_argument("--from-repo", action="store_true",
                    help="reconstrói sem o zip: usa os PNGs já em app/src/main/assets/night_street")
    args = ap.parse_args()
    if not args.from_repo and not (args.source and args.edgar_dir):
        sys.exit("Informe --source e --edgar-dir, ou use --from-repo")
    built = build(args)
    if args.preview_dir:
        preview(args, built)

if __name__ == "__main__":
    main()
