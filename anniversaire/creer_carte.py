"""Carte d'anniversaire d'une seule pièce, claire : la photo (netteté seulement)
se fond dans un fond ivoire où est écrit le message.

Usage : python3 creer_carte.py <photo> <dossier_polices> <sortie.png>
"""
import math
import random
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

photo_path, fonts_dir, out_path = sys.argv[1:4]

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
# agrandissement progressif, sans recadrage ni modification des couleurs
mid = photo.resize((photo.width * 2, photo.height * 2), Image.LANCZOS)
mid = mid.filter(ImageFilter.UnsharpMask(radius=1.2, percent=110, threshold=2))
photo = mid.resize((ph_w, ph_h), Image.LANCZOS)
photo = photo.filter(ImageFilter.UnsharpMask(radius=3.0, percent=120, threshold=3))
photo = photo.filter(ImageFilter.UnsharpMask(radius=1.0, percent=60, threshold=2))

# ---------- Toile ----------
TEXT_H = 800 * S
FADE = 150 * S
H = ph_h + TEXT_H - FADE

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


# ---------- Textes ----------
y0 = ph_h - FADE + 125 * S
gold_text(canvas, (W // 2, y0 + 30 * S), "Joyeux Anniversaire", SCRIPT(120))
gold_text(canvas, (W // 2, y0 + 118 * S), "ma fille", SCRIPT(70))

d = ImageDraw.Draw(canvas)
y = y0 + 190 * S
d.line([W // 2 - 170 * S, y, W // 2 - 24 * S, y], fill=OR, width=2 * S)
d.line([W // 2 + 24 * S, y, W // 2 + 170 * S, y], fill=OR, width=2 * S)
star(d, W // 2, y, 14 * S, OR)

y += 58 * S
f1 = SERIF_IT(35, 500)
for txt in ["Garde ton humilité, ton amour du prochain", "et surtout ta crainte de Dieu."]:
    d.text((W // 2, y), txt, font=f1, fill=VERT_FONCE, anchor="mm")
    y += 52 * S

y += 22 * S
f2 = SERIF_IT(37, 650)
for txt in ["Que l’huile de la faveur divine", "ne cesse jamais de couler", "de ta tête à tes pieds."]:
    gold_text(canvas, (W // 2, y), txt, f2, halo=False)
    y += 54 * S

gold_text(canvas, (W // 2, y + 26 * S), "Avec tout mon amour", SCRIPT(48), halo=False)

d = ImageDraw.Draw(canvas)
for (x, yy, r) in [(110, -330, 20), (975, -300, 16), (90, -80, 14), (995, -110, 18)]:
    star(d, x * S, H + yy * S, r * S, OR)

# ---------- Fin liseré sur tout le pourtour ----------
m = 22 * S
d.rectangle([m, m, W - m, H - m], outline=(255, 250, 235), width=3 * S)
d.rectangle([m + 7 * S, m + 7 * S, W - m - 7 * S, H - m - 7 * S], outline=OR_CLAIR, width=2 * S)

canvas.save(out_path, optimize=True)
print("ok", canvas.size)
