#!/usr/bin/env python3
"""
Assets da intro da rua (Novo Jogo): item da calçada e card "Capítulo 1".

A flor (tools/night_intro/sources/item_maveth_flower.png) vem com fundo preto sólido, sem canal alfa.
Este script troca só o fundo por transparência (flood fill a partir da borda); os pixels da arte ficam.

O card "Capítulo 1 / O PRIMEIRO ENCONTRO" já vem sem fundo do usuário e é usado como está
(app/src/main/res/drawable-nodpi/chapter1_title.png), sem recorte.

O contorno branco de adesivo (anel claro ligado ao fundo) vira um contorno lilás discreto na arte.
Um segundo PNG, o indicador, tem só esse anel em dourado quente; o jogo o desenha por cima e pulsa a
opacidade, para sinalizar que o item é interativo sem parecer um adesivo.

Saída:
  app/src/main/assets/night_street/item_maveth_flower.png     flor, recortada pela caixa do conteúdo
  app/src/main/assets/night_street/item_maveth_indicator.png  anel dourado (mesmo recorte), pulsa no jogo

Uso (a partir da raiz do repo):
  python3 tools/night_intro/build_intro_assets.py

Dependências: numpy, Pillow, scipy.
"""
import os

import numpy as np
from PIL import Image
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(HERE, "sources")
OUT_ITEM = os.path.join(REPO, "app/src/main/assets/night_street/item_maveth_flower.png")
OUT_INDICATOR = os.path.join(REPO, "app/src/main/assets/night_street/item_maveth_indicator.png")

# Pixels com brilho máximo abaixo disto são fundo preto (flood fill a partir da borda).
ITEM_BG_MAX = 8
# Pixels claros acima disto, ligados ao fundo, formam o anel de adesivo.
RING_MIN_BRIGHTNESS = 170
# Cor do contorno na arte (lilás apagado, cabe na paleta noturna) e do indicador (dourado quente).
RING_ART_RGB = (112, 82, 160)
INDICATOR_RGB = (255, 206, 140)
# Halo do indicador, em proporção da altura da flor: faixa dourada que sai do contorno e pulsa.
HALO_RATIO = 0.085


def crop_to_content(rgba):
    """Caixa do conteúdo (alfa > 0). Só corta o vazio transparente; os pixels não mudam."""
    ys, xs = np.where(rgba[..., 3] > 0)
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def ring_mask(rgb, outside):
    """Anel claro do contorno: componente de pixels claros que encosta no fundo (realces internos ficam de fora)."""
    light = (~outside) & (rgb.max(axis=2) > RING_MIN_BRIGHTNESS)
    labels, _ = ndimage.label(light, structure=np.ones((3, 3)))
    touching = ndimage.binary_dilation(outside, iterations=1)
    ids = set(np.unique(labels[touching & light]))
    ids.discard(0)
    return np.isin(labels, list(ids)) & light


def build_item():
    rgb = np.array(Image.open(os.path.join(SRC, "item_maveth_flower.png")).convert("RGB")).astype(np.float32)
    brightness = rgb.max(axis=2)
    background = brightness <= ITEM_BG_MAX
    labels, _ = ndimage.label(background)
    border = set(np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]])))
    border.discard(0)
    outside = np.isin(labels, list(border))
    opaque = ~outside
    ring = ring_mask(rgb, outside)

    # Arte: sombra interna da flor continua igual; só o anel claro vira contorno lilás apagado.
    art = rgb.copy()
    art[ring] = RING_ART_RGB
    rgba = np.zeros(rgb.shape[:2] + (4,), np.float32)
    rgba[..., :3] = art
    rgba[..., 3] = np.where(outside, 0, 255)
    crop = crop_to_content(rgba)
    _save(rgba, OUT_ITEM, crop)

    # Indicador: o anel dourado + um halo fora da flor. Janela = recorte da flor com margem de H px
    # em cada lado, para o halo não ser cortado. O centro da janela é o centro da flor.
    x0, y0, x1, y1 = crop
    H = max(4, round(HALO_RATIO * (y1 - y0)))
    opaque_p = np.pad(opaque, H, constant_values=False)
    ring_p = np.pad(ring, H, constant_values=False)
    op_w = opaque_p[y0:y1 + 2 * H, x0:x1 + 2 * H]
    ring_w = ring_p[y0:y1 + 2 * H, x0:x1 + 2 * H]
    dist = ndimage.distance_transform_edt(~op_w)     # 0 dentro da flor; distância para fora
    halo = (dist > 0) & (dist <= H)
    solid = halo & (dist <= 0.55 * H)                # faixa interna do halo: sólida
    outer = halo & ~solid                            # faixa externa: dithering (pixel art, sem degradê)
    bayer = _bayer(op_w.shape)
    outer = outer & (bayer < 0.5)
    ind = np.zeros(op_w.shape + (4,), np.float32)
    ind[ring_w | solid | outer, :3] = INDICATOR_RGB
    ind[ring_w | solid | outer, 3] = 255
    img = Image.fromarray(np.clip(ind, 0, 255).astype(np.uint8), "RGBA")
    img.save(OUT_INDICATOR, optimize=True)
    print("indicador:", img.size, "halo px:", H)
    return crop


def _bayer(shape):
    b = np.array([[0, 2], [3, 1]], np.float32) / 4.0
    h, w = shape
    return np.tile(b, (h // 2 + 1, w // 2 + 1))[:h, :w]


def _save(rgba, path, crop):
    img = Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")
    img = img.crop(crop)
    img.save(path, optimize=True)
    return img.size


def main():
    os.makedirs(os.path.dirname(OUT_ITEM), exist_ok=True)
    print("item e indicador:", build_item())


if __name__ == "__main__":
    main()
