"""Carte d'anniversaire d'une seule pièce, claire : la photo (netteté seulement)
se fond dans un fond ivoire où est écrit le message.

Usage : python3 creer_carte.py <photo> <dossier_polices> <sortie.png> [modele_EDSR_x4.pb]

Netteté : si le modèle EDSR est fourni, la photo est agrandie x4 par
super-résolution (OpenCV dnn_superres), puis légèrement renforcée.
Aucune autre retouche (ni recadrage, ni couleurs, ni lumière).
"""
import math
import random
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

photo_path, fonts_dir, out_path = sys.argv[1:4]
model_path = sys.argv[4] if len(sys.argv) > 4 else None

S = 2
W = 1080 * S

IVOIRE = (253, 248, 236)
IVOIRE_2 = (247, 238, 216)
OR_CLAIR = (236, 196, 104)
OR = (196, 146, 46)
OR_FONCE = (138, 96, 22)
VERT_FONCE = (10, 70, 44)


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

# ---------- Photo : uniquement amélioration de la netteté ----------
photo = Image.open(photo_path).convert("RGB")
ph_w = W
ph_h = round(photo.height * ph_w / photo.width)
if model_path:
    import cv2
    import numpy as np

    sr = cv2.dnn_superres.DnnSuperResImpl_create()
    sr.readModel(model_path)
    sr.setModel("edsr", 4)
    up = sr.upsample(cv2.cvtColor(np.array(photo), cv2.COLOR_RGB2BGR))
    photo = Image.fromarray(cv2.cvtColor(up, cv2.COLOR_BGR2RGB))
    photo = photo.resize((ph_w, ph_h), Image.LANCZOS)
    photo = photo.filter(ImageFilter.UnsharpMask(radius=2.0, percent=70, threshold=3))
    photo = photo.filter(ImageFilter.UnsharpMask(radius=0.8, percent=50, threshold=2))
else:
    mid = photo.resize((photo.width * 2, photo.height * 2), Image.LANCZOS)
    mid = mid.filter(ImageFilter.UnsharpMask(radius=1.2, percent=110, threshold=2))
    photo = mid.resize((ph_w, ph_h), Image.LANCZOS)
    photo = photo.filter(ImageFilter.UnsharpMask(radius=3.0, percent=120, threshold=3))
    photo = photo.filter(ImageFilter.UnsharpMask(radius=1.0, percent=60, threshold=2))

# ---------- Toile ----------
FADE = 200 * S
H = ph_h + 330 * S

canvas = Image.new("RGB", (W, H), IVOIRE)
gd = ImageDraw.Draw(canvas)
for y in range(ph_h - FADE, H):
    t = (y - (ph_h - FADE)) / (H - (ph_h - FADE))
    gd.line([(0, y), (W, y)], fill=tuple(int(IVOIRE[i] * (1 - t) + IVOIRE_2[i] * t) for i in range(3)))

# photo en haut, fondue dans l'ivoire sur sa partie basse => une seule pièce
mask = Image.new("L", (ph_w, ph_h), 255)
md = ImageDraw.Draw(mask)
for y in range(ph_h - FADE, ph_h):
    t = (y - (ph_h - FADE)) / FADE
    md.line([(0, y), (ph_w, y)], fill=int(255 * (1 - t) ** 1.4))
canvas.paste(photo, (0, 0), mask)

# ---------- Paillettes dorées dans la zone texte ----------
canvas = canvas.convert("RGBA")
random.seed(7)
sp = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sd = ImageDraw.Draw(sp)
for _ in range(160):
    x = random.randint(0, W)
    y = random.randint(ph_h - FADE // 3, H)
    r = random.choice([1, 1, 2, 2, 3]) * S
    sd.ellipse([x - r, y - r, x + r, y + r], fill=OR_CLAIR + (random.randint(70, 180),))
canvas = Image.alpha_composite(canvas, sp.filter(ImageFilter.GaussianBlur(0.5 * S))).convert("RGB")


def star(draw, x, y, r, col):
    pts = []
    for k in range(8):
        rr = r if k % 2 == 0 else r * 0.22
        a = k * math.pi / 4 - math.pi / 2
        pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
    draw.polygon(pts, fill=col)


def gold_text(img, xy, text, fnt, anchor="mm", halo=True):
    layer = Image.new("L", img.size, 0)
    ImageDraw.Draw(layer).text(xy, text, font=fnt, fill=255, anchor=anchor)
    box = layer.getbbox()
    g = Image.new("RGB", img.size, OR)
    gdraw = ImageDraw.Draw(g)
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
    if halo:  # halo ivoire pour la lisibilité sur le fondu de la photo
        h = layer.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.GaussianBlur(10 * S))
        img.paste(Image.new("RGB", img.size, (255, 253, 245)), (0, 0), h.point(lambda v: min(255, int(v * 1.6))))
    img.paste(g, (0, 0), layer)


# ---------- Textes : couleurs foncées + halo ivoire pour bien ressortir ----------
VERT_TEXTE = (6, 62, 38)
OR_TEXTE = (122, 82, 8)


def strong_text(img, xy, text, fnt, fill, stroke=None, halo=0):
    if halo:
        layer = Image.new("L", img.size, 0)
        ImageDraw.Draw(layer).text(xy, text, font=fnt, fill=255, anchor="mm",
                                   stroke_width=6 * S)
        h = layer.filter(ImageFilter.GaussianBlur(halo * S))
        img.paste(Image.new("RGB", img.size, (255, 252, 242)), (0, 0),
                  h.point(lambda v: min(255, int(v * 2.6))))
    ImageDraw.Draw(img).text(xy, text, font=fnt, fill=fill, anchor="mm",
                             stroke_width=(stroke[0] if stroke else 0),
                             stroke_fill=(stroke[1] if stroke else None))


def fit(maker, size, text, max_w):
    f = maker(size)
    while f.getlength(text) > max_w and size > 10:
        size -= 1
        f = maker(size)
    return f


y0 = ph_h - FADE + 105 * S
strong_text(canvas, (W // 2, y0), "Joyeux Anniversaire", SCRIPT(108), VERT_TEXTE,
            stroke=(2 * S, OR), halo=16)
strong_text(canvas, (W // 2, y0 + 74 * S), "ma fille", SCRIPT(64), VERT_TEXTE,
            stroke=(1 * S, OR), halo=12)

y = y0 + 142 * S
f1 = SERIF_IT(31, 650)
for txt in ["Garde ton humilité, ton amour du prochain", "et surtout ta crainte de Dieu."]:
    strong_text(canvas, (W // 2, y), txt, f1, VERT_TEXTE, halo=6)
    y += 46 * S

y += 12 * S
f2 = SERIF_IT(32, 800)
for txt in ["Que l’huile de la faveur divine ne cesse jamais", "de couler de ta tête à tes pieds."]:
    strong_text(canvas, (W // 2, y), txt, f2, OR_TEXTE, halo=6)
    y += 46 * S

fin = "Dieu n’a pas encore épuisé ses merveilles pour toi"
strong_text(canvas, (W // 2, y + 14 * S), fin, fit(SCRIPT, 54, fin, 900 * S), VERT_TEXTE,
            stroke=(1 * S, OR), halo=6)

d = ImageDraw.Draw(canvas)
for (x, yy, r) in [(90, -230, 16), (990, -215, 14), (95, -70, 12), (985, -80, 16)]:
    star(d, x * S, H + yy * S, r * S, OR)

# ---------- Fin liseré sur tout le pourtour ----------
m = 22 * S
d.rectangle([m, m, W - m, H - m], outline=(255, 250, 235), width=3 * S)
d.rectangle([m + 7 * S, m + 7 * S, W - m - 7 * S, H - m - 7 * S], outline=OR_CLAIR, width=2 * S)

canvas.save(out_path, optimize=True)
print("ok", canvas.size)
