"""Generates Sprout IDE launcher icon: >_ inside a rounded square with two M3 cookie shapes.

Outputs adaptive-icon vector drawables into app/src/main/res and a PNG preview into tools/.
Coordinates are in the 108x108 adaptive icon viewport; everything stays inside the 66dp safe zone.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

BG = "#0F3B20"          # dark green
SQUARE_TOP = "#2E8B45"  # green
SQUARE_BOTTOM = "#1F6E36"
COOKIE_A = "#8EE09C"    # light green, top-left
COOKIE_B = "#57C06C"    # green, bottom-right
GLYPH = "#EAFBEA"

SQ = (32.0, 32.0, 76.0, 76.0)
SQ_R = 13.0
COOKIE_A_C, COOKIE_A_R, COOKIE_A_N = (34.0, 34.0), 7.5, 9
COOKIE_B_C, COOKIE_B_R, COOKIE_B_N = (74.0, 74.0), 7.5, 7
GAP = 2.2               # ring of background colour between cookie and square
STROKE = 4.6
CHEVRON = [(42.5, 47.0), (49.5, 54.0), (42.5, 61.0)]
UNDERSCORE = [(53.5, 60.5), (65.5, 60.5)]


def cookie_points(c, r, n, depth=0.11, steps=360, rot=-math.pi / 2):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        # raised-cosine lobes look like the rounded scallops of MaterialShapes.Cookie*
        lobe = 0.5 + 0.5 * math.cos(n * (t - rot))
        rr = r * (1 - depth * (1 - lobe) ** 1.5)
        pts.append((c[0] + rr * math.cos(t), c[1] + rr * math.sin(t)))
    return pts


def path_from(pts):
    s = "M%.2f,%.2f" % pts[0]
    s += "".join("L%.2f,%.2f" % p for p in pts[1:])
    return s + "Z"


def rrect_path(x0, y0, x1, y1, r):
    return (f"M{x0 + r},{y0}H{x1 - r}A{r},{r} 0 0 1 {x1},{y0 + r}V{y1 - r}"
            f"A{r},{r} 0 0 1 {x1 - r},{y1}H{x0 + r}A{r},{r} 0 0 1 {x0},{y1 - r}"
            f"V{y0 + r}A{r},{r} 0 0 1 {x0 + r},{y0}Z")


def poly_path(pts):
    return "M%.2f,%.2f" % pts[0] + "".join("L%.2f,%.2f" % p for p in pts[1:])


def vector(body, w=108):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            f'    android:width="{w}dp" android:height="{w}dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            f'{body}</vector>\n')


def fill(d, color):
    return f'    <path android:fillColor="{color}" android:pathData="{d}"/>\n'


def stroke(d, color, width=STROKE):
    return (f'    <path android:strokeColor="{color}" android:strokeWidth="{width}"'
            f' android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{d}"/>\n')


def gradient_fill(d, top, bottom):
    return (f'    <path android:pathData="{d}">\n'
            '        <aapt:attr name="android:fillColor">\n'
            f'            <gradient android:type="linear" android:startX="{SQ[0]}" android:startY="{SQ[1]}"'
            f' android:endX="{SQ[2]}" android:endY="{SQ[3]}"'
            f' android:startColor="{top}" android:endColor="{bottom}"/>\n'
            '        </aapt:attr>\n    </path>\n')


def foreground_body(gap_color=BG):
    b = gradient_fill(rrect_path(*SQ, SQ_R), SQUARE_TOP, SQUARE_BOTTOM)
    for c, r, n, col in ((COOKIE_A_C, COOKIE_A_R, COOKIE_A_N, COOKIE_A), (COOKIE_B_C, COOKIE_B_R, COOKIE_B_N, COOKIE_B)):
        b += fill(path_from(cookie_points(c, r + GAP, n)), gap_color)
        b += fill(path_from(cookie_points(c, r, n)), col)
    b += stroke(poly_path(CHEVRON), GLYPH)
    b += stroke(poly_path(UNDERSCORE), GLYPH)
    return b


def write(name, text):
    with open(os.path.join(RES, name), "w", encoding="utf-8") as f:
        f.write(text)


def main():
    write("drawable/ic_launcher_background.xml", vector(fill("M0,0H108V108H0Z", BG)))
    write("drawable/ic_launcher_foreground.xml", vector(foreground_body()))
    mono = stroke(rrect_path(*SQ, SQ_R), "#FFFFFF", 4.0)
    for c, r, n in ((COOKIE_A_C, COOKIE_A_R, COOKIE_A_N), (COOKIE_B_C, COOKIE_B_R, COOKIE_B_N)):
        mono += fill(path_from(cookie_points(c, r, n)), "#FFFFFF")
    mono += stroke(poly_path(CHEVRON), "#FFFFFF") + stroke(poly_path(UNDERSCORE), "#FFFFFF")
    write("drawable/ic_launcher_monochrome.xml", vector(mono))
    # in-app logo: icon cropped to the visible 72dp area, on a rounded background
    logo = fill(rrect_path(18, 18, 90, 90, 22), BG) + foreground_body()
    write("drawable/ic_logo.xml", vector(logo).replace('android:viewportWidth="108" android:viewportHeight="108"',
                                                        'android:viewportWidth="108" android:viewportHeight="108"'))
    render_preview(os.path.join(ROOT, "tools", "icon_preview.png"))


def render_preview(out, size=1024):
    s = size / 72.0  # visible area 18..90

    def tp(p):
        return ((p[0] - 18) * s, (p[1] - 18) * s)

    img = Image.new("RGBA", (size, size), BG)
    d = ImageDraw.Draw(img)
    # vertical-ish gradient for square
    sq = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    g = ImageDraw.Draw(sq)
    x0, y0 = tp(SQ[:2]); x1, y1 = tp(SQ[2:])
    top = tuple(int(SQUARE_TOP[i:i + 2], 16) for i in (1, 3, 5))
    bot = tuple(int(SQUARE_BOTTOM[i:i + 2], 16) for i in (1, 3, 5))
    for i in range(int(y0), int(y1) + 1):
        k = (i - y0) / (y1 - y0)
        g.line([(x0, i), (x1, i)], fill=tuple(int(top[j] + (bot[j] - top[j]) * k) for j in range(3)))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([x0, y0, x1, y1], radius=SQ_R * s, fill=255)
    img.paste(sq, (0, 0), mask)
    for c, r, n, col in ((COOKIE_A_C, COOKIE_A_R, COOKIE_A_N, COOKIE_A), (COOKIE_B_C, COOKIE_B_R, COOKIE_B_N, COOKIE_B)):
        d.polygon([tp(p) for p in cookie_points(c, r + GAP, n)], fill=BG)
        d.polygon([tp(p) for p in cookie_points(c, r, n)], fill=col)
    w = int(STROKE * s)
    for line in (CHEVRON, UNDERSCORE):
        pts = [tp(p) for p in line]
        d.line(pts, fill=GLYPH, width=w, joint="curve")
        for p in (pts[0], pts[-1]):
            d.ellipse([p[0] - w / 2, p[1] - w / 2, p[0] + w / 2, p[1] + w / 2], fill=GLYPH)
    circle = Image.new("L", (size, size), 0)
    ImageDraw.Draw(circle).rounded_rectangle([0, 0, size, size], radius=size * 0.3, fill=255)
    out_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out_img.paste(img, (0, 0), circle)
    out_img.save(out)


if __name__ == "__main__":
    sys.exit(main())
