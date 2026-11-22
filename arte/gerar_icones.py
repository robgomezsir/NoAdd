"""Gera os PNGs do ícone a partir da arte nova (webp) do projeto NoAdd."""
from PIL import Image, ImageChops

SRC = "arte/icone.webp"
WHITE = (255, 255, 255)

src = Image.open(SRC).convert("RGB")
diff = ImageChops.difference(src, Image.new("RGB", src.size, WHITE)).convert("L")
diff = diff.point(lambda v: 255 if v > 12 else 0)
bbox = diff.getbbox()
art = src.crop(bbox)
print("arte:", bbox, art.size)


def canvas(size, fraction, background):
    """Arte centralizada em um quadrado, ocupando `fraction` do lado."""
    out = Image.new("RGBA", (size, size), background)
    w, h = art.size
    scale = (fraction * size) / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    small = art.resize((nw, nh), Image.LANCZOS)
    out.paste(small, ((size - nw) // 2, (size - nh) // 2), small if small.mode == "RGBA" else None)
    return out


def silhueta(size, fraction):
    """Versão monocromática: o balão sólido em branco, com alfa."""
    w, h = art.size
    scale = (fraction * size) / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    m = diff.crop(bbox).resize((nw, nh), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    layer = Image.new("RGBA", (nw, nh), (255, 255, 255, 255))
    layer.putalpha(m)
    out.paste(layer, ((size - nw) // 2, (size - nh) // 2), layer)
    return out


def save_rgb(img, path):
    """PNG paletizado: mesmo desenho com poucos KB."""
    img.convert("RGB").quantize(colors=96, method=Image.MEDIANCUT, dither=Image.FLOYDSTEINBERG).save(
        path, optimize=True
    )


RES = "app/src/main/res"
# Ícones legados (quadrado branco, arte grande)
for folder, px in [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]:
    icon = canvas(px, 0.80, WHITE + (255,))
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        save_rgb(icon, f"{RES}/{folder}/{name}")
    print(folder, px, "ok")

# Camada adaptativa: arte dentro da zona segura sobre o fundo branco do tema
save_rgb(
    canvas(432, 0.60, WHITE + (255,)),
    f"{RES}/drawable-xxxhdpi/ic_launcher_foreground.png",
)
silhueta(432, 0.60).save(f"{RES}/drawable-xxxhdpi/ic_launcher_monochrome.png", optimize=True)
print("foreground e monocromático ok")
