#!/usr/bin/env python3
"""
Assets da intro da rua (Novo Jogo): item da calçada e card "Capítulo 1".

A flor (tools/night_intro/sources/item_maveth_flower.png) vem com fundo preto sólido, sem canal alfa.
Este script troca só o fundo por transparência (flood fill a partir da borda); os pixels da arte ficam.

O card "Capítulo 1 / O PRIMEIRO ENCONTRO" já vem sem fundo do usuário e é usado como está
(app/src/main/res/drawable-nodpi/chapter1_title.png), sem recorte.

Saída:
  app/src/main/assets/night_street/item_maveth_flower.png   flor, recortada pela caixa do conteúdo

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

# Pixels com brilho máximo abaixo disto são fundo preto (flood fill a partir da borda).
ITEM_BG_MAX = 8


def crop_to_content(rgba):
    """Caixa do conteúdo (alfa > 0). Só corta o vazio transparente; os pixels não mudam."""
    ys, xs = np.where(rgba[..., 3] > 0)
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def build_item():
    rgb = np.array(Image.open(os.path.join(SRC, "item_maveth_flower.png")).convert("RGB")).astype(np.float32)
    brightness = rgb.max(axis=2)
    background = brightness <= ITEM_BG_MAX
    labels, _ = ndimage.label(background)
    border = set(np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]])))
    border.discard(0)
    outside = np.isin(labels, list(border))
    # Sombra interna da flor (pixels escuros dentro do contorno) continua opaca.
    rgba = np.zeros(rgb.shape[:2] + (4,), np.float32)
    rgba[..., :3] = rgb
    rgba[..., 3] = np.where(outside, 0, 255)
    return _save(rgba, OUT_ITEM)


def _save(rgba, path):
    img = Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")
    img = img.crop(crop_to_content(rgba))
    img.save(path, optimize=True)
    return img.size


def main():
    os.makedirs(os.path.dirname(OUT_ITEM), exist_ok=True)
    print("item:", build_item())


if __name__ == "__main__":
    main()
