"""Carte d'anniversaire : photo (netteté seulement) + message.

Usage : python3 creer_carte.py <photo> <dossier_polices> <sortie.png>
"""
import math
import random
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

photo_path, fonts_dir, out_path = sys.argv[1:4]

S = 2  # rendu haute définition (2160 x 2700)
W, H = 1080 * S, 1350 * S

EMERAUDE_FONCE = (6, 48, 32)
EMERAUDE = (12, 92, 58)
OR_CLAIR = (255, 228, 150)
OR = (222, 176, 74)
OR_FONCE = (160, 112, 30)


def font(name, size, wght=None):
    f = ImageFont.truetype(f"{fonts_dir}/{name}", size * S)
    if wght is not None:
        try:
            f.set_variation_by_axes([wght])
        except Exception:
            pass
    return f


SCRIPT = lambda s: font("GreatVibes-Regular.ttf", s)
SERIF_IT = lambda s, w=500: font("PlayfairDisplay-Italic%5Bwght%5D.ttf", s, w)
CORMORANT = lambda s, w=500: font("CormorantGaramond%5Bwght%5D.ttf", s, w)

# ---------- Fond : dégradé radial émeraude ----------
bg = Image.new("RGB", (W, H), EMERAUDE_FONCE)
glow = Image.new("L", (W, H), 0)
gd = ImageDraw.Draw(glow)
cx, cy = W // 2, int(H * 0.42)
for r in range(int(W * 0.95), 0, -8 * S):
    v = int(255 * (1 - r / (W * 0.95)) ** 1.6)
    gd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=v)
bg = Image.composite(Image.new("RGB", (W, H), EMERAUDE), bg, glow)

# Fines lignes dorées en éventail (discrètes)
rays = Image.new("RGBA", (W, H), (0, 0, 0, 0))
rd = ImageDraw.Draw(rays)
for i in range(48):
    a = i * math.pi * 2 / 48
    rd.line([cx, cy, cx + math.cos(a) * W * 1.5, cy + math.sin(a) * W * 1.5],
            fill=OR + (14,), width=2 * S)
bg = Image.alpha_composite(bg.convert("RGBA"), rays)

# Paillettes dorées
random.seed(11)
sp = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sd = ImageDraw.Draw(sp)
for _ in range(220):
    x, y = random.randint(0, W), random.randint(0, H)
    r = random.choice([1, 1, 2, 2, 3]) * S
    sd.ellipse([x - r, y - r, x + r, y + r], fill=OR_CLAIR + (random.randint(60, 170),))
bg = Image.alpha_composite(bg, sp.filter(ImageFilter.GaussianBlur(0.6 * S)))


def star(draw, x, y, r, col):
    pts = []
    for k in range(8):
        rr = r if k % 2 == 0 else r * 0.22
        a = k * math.pi / 4 - math.pi / 2
        pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
    draw.polygon(pts, fill=col)


sd = ImageDraw.Draw(bg)
for (x, y, r) in [(130, 120, 22), (960, 150, 18), (90, 760, 14), (995, 700, 20),
                  (150, 1240, 16), (940, 1250, 22), (540, 52, 12)]:
    star(sd, x * S, y * S, r * S, OR_CLAIR + (230,))

# ---------- Cadre doré de la carte ----------
d = ImageDraw.Draw(bg)
m = 28 * S
d.rectangle([m, m, W - m, H - m], outline=OR, width=3 * S)
m2 = 40 * S
d.rectangle([m2, m2, W - m2, H - m2], outline=OR + (150,), width=1 * S)
for (x, y) in [(m, m), (W - m, m), (m, H - m), (W - m, H - m)]:
    d.regular_polygon((x, y, 12 * S), 4, rotation=45, fill=OR)


# ---------- Texte doré (dégradé) ----------
def gold_text(img, xy, text, fnt, anchor="mm", shadow=True):
    layer = Image.new("L", img.size, 0)
    ImageDraw.Draw(layer).text(xy, text, font=fnt, fill=255, anchor=anchor)
    box = layer.getbbox()
    if not box:
        return
    grad = Image.new("RGB", img.size, OR)
    gdraw = ImageDraw.Draw(grad)
    top, bot = box[1], box[3]
    for yy in range(top, bot + 1):
        t = (yy - top) / max(1, bot - top)
        if t < 0.5:
            u = t / 0.5
            c = tuple(int(OR_CLAIR[i] * (1 - u) + OR[i] * u) for i in range(3))
        else:
            u = (t - 0.5) / 0.5
            c = tuple(int(OR[i] * (1 - u) + OR_FONCE[i] * u) for i in range(3))
        gdraw.line([(box[0], yy), (box[2], yy)], fill=c)
    if shadow:
        sh = layer.filter(ImageFilter.GaussianBlur(4 * S))
        img.paste(Image.new("RGB", img.size, (0, 20, 10)), (2 * S, 4 * S), sh.point(lambda v: v * 0.8))
    img.paste(grad, (0, 0), layer)


def light_text(img, xy, text, fnt, fill=(250, 244, 228), anchor="mm"):
    layer = Image.new("L", img.size, 0)
    ImageDraw.Draw(layer).text(xy, text, font=fnt, fill=255, anchor=anchor)
    sh = layer.filter(ImageFilter.GaussianBlur(3 * S))
    img.paste(Image.new("RGB", img.size, (0, 18, 8)), (0, 2 * S), sh.point(lambda v: v * 0.7))
    img.paste(Image.new("RGB", img.size, fill), (0, 0), layer)


bg = bg.convert("RGB")

# Titre
gold_text(bg, (W // 2, 128 * S), "Joyeux Anniversaire", SCRIPT(118))
gold_text(bg, (W // 2, 212 * S), "ma fille", SCRIPT(68))

# ---------- Photo : uniquement amélioration de la netteté ----------
photo = Image.open(photo_path).convert("RGB")
ph_h = 620 * S
ph_w = round(photo.width * ph_h / photo.height)
photo = photo.resize((ph_w, ph_h), Image.LANCZOS)  # agrandissement sans recadrage
photo = photo.filter(ImageFilter.UnsharpMask(radius=2.2 * S / 2, percent=90, threshold=2))
photo = photo.filter(ImageFilter.UnsharpMask(radius=0.8, percent=40, threshold=1))

px, py = (W - ph_w) // 2, 262 * S
# ombre portée
shadow = Image.new("L", (W, H), 0)
ImageDraw.Draw(shadow).rectangle([px - 10 * S, py - 10 * S, px + ph_w + 10 * S, py + ph_h + 10 * S], fill=200)
shadow = shadow.filter(ImageFilter.GaussianBlur(18 * S))
bg.paste(Image.new("RGB", (W, H), (0, 15, 8)), (0, 10 * S), shadow)
# cadre doré
d = ImageDraw.Draw(bg)
d.rectangle([px - 14 * S, py - 14 * S, px + ph_w + 13 * S, py + ph_h + 13 * S], fill=OR_FONCE)
d.rectangle([px - 11 * S, py - 11 * S, px + ph_w + 10 * S, py + ph_h + 10 * S], fill=OR)
d.rectangle([px - 5 * S, py - 5 * S, px + ph_w + 4 * S, py + ph_h + 4 * S], fill=OR_CLAIR)
bg.paste(photo, (px, py))

# ---------- Message ----------
lignes = [
    ("Garde ton humilité, ton amour du prochain", "l"),
    ("et surtout ta crainte de Dieu.", "l"),
    (None, None),
    ("Que l’huile de la faveur divine", "g"),
    ("ne cesse jamais de couler", "g"),
    ("de ta tête à tes pieds.", "g"),
]
y = 950 * S
f_light = SERIF_IT(34, 500)
f_gold = SERIF_IT(37, 600)
for txt, kind in lignes:
    if txt is None:
        # petit séparateur doré
        d = ImageDraw.Draw(bg)
        d.line([W // 2 - 140 * S, y + 4 * S, W // 2 - 22 * S, y + 4 * S], fill=OR, width=2 * S)
        d.line([W // 2 + 22 * S, y + 4 * S, W // 2 + 140 * S, y + 4 * S], fill=OR, width=2 * S)
        star(d, W // 2, y + 4 * S, 13 * S, OR_CLAIR)
        y += 40 * S
        continue
    if kind == "l":
        light_text(bg, (W // 2, y), txt, f_light)
    else:
        gold_text(bg, (W // 2, y), txt, f_gold)
    y += 52 * S

# Signature
gold_text(bg, (W // 2, 1285 * S), "Avec tout mon amour", SCRIPT(46))

bg.save(out_path, optimize=True)
print("ok", bg.size, "photo", (ph_w, ph_h))
