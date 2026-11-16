#!/usr/bin/env python3
"""Renders Play Store screenshots + feature graphic that mirror the app UI."""
import math
import os
from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 1920
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "store")

BG = (13, 17, 23)
SURFACE = (22, 29, 38)
SURFACE2 = (30, 39, 50)
STROKE = (42, 54, 68)
TEXT = (242, 246, 250)
MUTED = (138, 151, 168)
ACCENT = (45, 212, 167)
ACCENT_D = (6, 42, 34)
WARN = (243, 181, 68)
DANGER = (255, 107, 107)
OP_BG = (27, 39, 51)
C_BG = (58, 33, 38)
DEL_BG = (49, 38, 42)
DEG_BG = (19, 51, 44)

FONT_PATH = "/system/fonts/Roboto-Regular.ttf"


def font(size):
    return ImageFont.truetype(FONT_PATH, size)


def text(d, xy, s, size, color, bold=False, anchor="la"):
    f = font(size)
    stroke = max(1, size // 42) if bold else 0
    d.text(xy, s, font=f, fill=color, anchor=anchor,
           stroke_width=stroke, stroke_fill=color)
    return f


def width_of(d, s, size, bold=False):
    f = font(size)
    return d.textlength(s, font=f)


def rounded(d, box, radius, fill=None, outline=None, w=0):
    d.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=w)


def status_bar(d):
    text(d, (36, 28), "9:41", 36, TEXT, True)
    x = W - 210
    for i in range(4):
        h = 12 + i * 6
        d.rounded_rectangle([x + i * 16, 66 - h, x + i * 16 + 9, 66], 3, fill=TEXT)
    d.arc([W - 150, 30, W - 86, 82], 205, 335, fill=TEXT, width=5)
    d.arc([W - 138, 42, W - 98, 80], 205, 335, fill=TEXT, width=5)
    d.ellipse([W - 122, 62, W - 114, 70], fill=TEXT)
    rounded(d, [W - 70, 32, W - 24, 68], 9, outline=TEXT, w=4)
    rounded(d, [W - 64, 38, W - 40, 62], 4, fill=TEXT)
    d.rectangle([W - 22, 44, W - 18, 56], fill=TEXT)


def header(d, subtitle="10 tools · currency rates cached offline"):
    text(d, (54, 108), "All-in-One Calculator", 64, TEXT, True)
    text(d, (54, 188), subtitle, 36, MUTED)


TABS = ["Basic", "Scientific", "Units", "Currency", "BMI", "Loan", "Age", "Percent", "Tip", "Discount"]


def tabs(d, selected=0, y=252, start=0):
    x = 36
    for i in range(start, len(TABS)):
        name = TABS[i]
        w = width_of(d, name, 40, True) + 62
        if x + w > W - 20:
            break
        if i == selected:
            rounded(d, [x, y, x + w, y + 80], 40, fill=ACCENT)
            text(d, (x + w / 2, y + 40), name, 40, ACCENT_D, True, "mm")
        else:
            rounded(d, [x, y, x + w, y + 80], 40, fill=SURFACE2)
            text(d, (x + w / 2, y + 40), name, 40, MUTED, True, "mm")
        x += w + 18
    d.line([0, y + 100, W, y + 100], fill=STROKE, width=3)
    return y + 106


def card(d, box, title=None):
    rounded(d, box, 54, fill=SURFACE, outline=STROKE, w=3)
    y = box[1] + 42
    if title:
        text(d, (box[0] + 42, y), title, 45, TEXT, True)
        y += 78
    return y


def chip(d, xy, label, bg, fg, size=30):
    w = width_of(d, label, size, True) + 36
    rounded(d, [xy[0], xy[1], xy[0] + w, xy[1] + 46], 15, fill=bg)
    text(d, (xy[0] + w / 2, xy[1] + 23), label, size, fg, True, "mm")


def backspace_icon(d, box, color):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    pts = [(x0 + w * 0.30, y0 + h * 0.24), (x0 + w * 0.78, y0 + h * 0.24),
           (x0 + w * 0.78, y0 + h * 0.76), (x0 + w * 0.30, y0 + h * 0.76),
           (x0 + w * 0.12, y0 + h * 0.50), (x0 + w * 0.30, y0 + h * 0.24)]
    d.line(pts, fill=color, width=6, joint="curve")
    d.line([(x0 + w * 0.46, y0 + h * 0.38), (x0 + w * 0.64, y0 + h * 0.62)], fill=color, width=6)
    d.line([(x0 + w * 0.64, y0 + h * 0.38), (x0 + w * 0.46, y0 + h * 0.62)], fill=color, width=6)


def swap_icon(d, box, color):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    d.line([(x0 + w * 0.18, y0 + h * 0.36), (x0 + w * 0.82, y0 + h * 0.36)], fill=color, width=6)
    d.line([(x0 + w * 0.66, y0 + h * 0.22), (x0 + w * 0.82, y0 + h * 0.36),
            (x0 + w * 0.66, y0 + h * 0.50)], fill=color, width=6, joint="curve")
    d.line([(x0 + w * 0.18, y0 + h * 0.64), (x0 + w * 0.82, y0 + h * 0.64)], fill=color, width=6)
    d.line([(x0 + w * 0.34, y0 + h * 0.50), (x0 + w * 0.18, y0 + h * 0.64),
            (x0 + w * 0.34, y0 + h * 0.78)], fill=color, width=6, joint="curve")


def key(d, x, y, w, h, label, bg, fg, size=62):
    rounded(d, [x, y, x + w, y + h], 46, fill=bg)
    if label == "⌫":
        backspace_icon(d, (x + w * 0.31, y + h * 0.34, x + w * 0.69, y + h * 0.66), fg)
    else:
        text(d, (x + w / 2, y + h / 2 + 2), label, size, fg, True, "mm")


def key_colors(label):
    if label.isdigit():
        return SURFACE2, TEXT
    if label == "=":
        return ACCENT, ACCENT_D
    if label == "C":
        return C_BG, (255, 154, 162)
    if label == "⌫":
        return DEL_BG, (255, 179, 184)
    if label in ("DEG", "RAD"):
        return DEG_BG, ACCENT
    if label in ("(", ")"):
        return SURFACE, MUTED
    if len(label) > 1:
        return SURFACE, ACCENT
    return OP_BG, ACCENT


def keypad(d, rows, top, bottom):
    n = len(rows)
    gap = 16
    h = (bottom - top - gap * (n - 1)) // n
    for ri, row in enumerate(rows):
        y = top + ri * (h + gap)
        weights = [2 if (len(row) == 3 and lbl in ("0", "=")) else 1 for lbl in row]
        total = sum(weights)
        avail = W - 84 - 20 * (len(row) - 1)
        x = 42
        for wgt, label in zip(weights, row):
            kw = int(avail * wgt / total)
            bg, fg = key_colors(label)
            size = 62 if label.isdigit() else (44 if len(label) > 1 else 58)
            key(d, x, y, kw, h, label, bg, fg, size)
            x += kw + 20


BASIC = [
    ["C", "⌫", "%", "÷"],
    ["7", "8", "9", "×"],
    ["4", "5", "6", "−"],
    ["1", "2", "3", "+"],
    ["0", ".", "±", "="],
]

SCI = [
    ["sin", "cos", "tan", "DEG", "C"],
    ["ln", "log", "√", "^", "⌫"],
    ["π", "e", "x!", "(", ")"],
    ["7", "8", "9", "÷", "×"],
    ["4", "5", "6", "−", "+"],
    ["1", "2", "3", "%", "±"],
    ["0", ".", "="],
]


def base_screen(selected=0, subtitle="10 tools · currency rates cached offline"):
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(d)
    header(d, subtitle)
    tabs(d, selected)
    return img, d


def display_card(d, expr, history="", preview=""):
    box = [42, 380, W - 42, 730]
    rounded(d, box, 60, fill=SURFACE, outline=STROKE, w=3)
    chip(d, (72, 412), "HISTORY", (20, 51, 44), ACCENT, 26)
    if history:
        text(d, (W - 72, 418), history, 34, MUTED, False, "ra")
    text(d, (W - 72, 500), expr, 100, TEXT, False, "ra")
    if preview:
        text(d, (W - 72, 646), preview, 44, ACCENT, True, "ra")
    return 760


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    img.save(path, "PNG", optimize=True)
    print("wrote", path)


def shot_basic():
    img, d = base_screen(0)
    top = display_card(d, "14400", "1200 × 12 =")
    keypad(d, BASIC, top + 20, H - 90)
    save(img, "screenshots/01-basic.png")


def shot_scientific():
    img, d = base_screen(1)
    top = display_card(d, "sin(60", "", "= 0.8660254")
    keypad(d, SCI, top + 20, H - 90)
    save(img, "screenshots/02-scientific.png")


def spinner(d, box, label):
    rounded(d, box, 36, fill=SURFACE2)
    text(d, (box[0] + 30, (box[1] + box[3]) / 2), label, 38, TEXT, False, "lm")


def result_panel(d, box, value, unit, big=84):
    rounded(d, box, 48, fill=SURFACE2)
    text(d, (box[2] - 36, box[1] + 44), value, big, ACCENT, True, "ra")
    text(d, (box[2] - 36, box[3] - 56), unit, 36, MUTED, False, "ra")


def shot_units():
    img, d = base_screen(2)
    y = 380
    rounded(d, [42, y, W - 42, y + 250], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y + 38), "Category", 45, TEXT, True)
    spinner(d, [84, y + 112, W - 84, y + 214], "Length")

    y2 = y + 280
    rounded(d, [42, y2, W - 42, y2 + 780], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y2 + 38), "Convert", 45, TEXT, True)
    spinner(d, [84, y2 + 112, 500, y2 + 214], "m · Meter")
    rounded(d, [524, y2 + 112, 664, y2 + 214], 36, fill=OP_BG)
    swap_icon(d, (546, y2 + 136, 642, y2 + 190), ACCENT)
    spinner(d, [688, y2 + 112, W - 84, y2 + 214], "ft · Foot")

    rounded(d, [84, y2 + 244, W - 84, y2 + 364], 36, fill=SURFACE2)
    text(d, (114, y2 + 304), "5", 48, TEXT, False, "lm")

    result_panel(d, [84, y2 + 396, W - 84, y2 + 640], "16.40419948", "ft · Foot")
    text(d, (84, y2 + 668), "1 m = 3.2808399 ft", 36, MUTED)

    y3 = y2 + 812
    rounded(d, [42, y3, W - 42, y3 + 250], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y3 + 42), "8 categories, 60+ units", 42, TEXT, True)
    text(d, (84, y3 + 112), "Length · Weight · Temperature · Area · Volume ·", 34, MUTED)
    text(d, (84, y3 + 158), "Speed · Time · Data — all offline.", 34, MUTED)
    save(img, "screenshots/03-units.png")


def shot_currency():
    img, d = base_screen(3, "live rates · cached for offline use")
    y = 380
    rounded(d, [42, y, W - 42, y + 980], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y + 38), "Currency Converter", 45, TEXT, True)

    spinner(d, [84, y + 112, 500, y + 214], "USD · US Dollar")
    rounded(d, [524, y + 112, 664, y + 214], 36, fill=OP_BG)
    swap_icon(d, (546, y + 136, 642, y + 190), ACCENT)
    spinner(d, [688, y + 112, W - 84, y + 214], "BDT · Taka")

    rounded(d, [84, y + 244, W - 84, y + 364], 36, fill=SURFACE2)
    text(d, (114, y + 304), "100", 48, TEXT, False, "lm")

    result_panel(d, [84, y + 396, W - 84, y + 660], "12,050.00", "BDT · Bangladeshi Taka")

    text(d, (84, y + 690), "1 USD = 120.5 BDT", 36, MUTED)

    text(d, (84, y + 760), "Updated 16 Nov, 19:10", 33, MUTED)
    rounded(d, [W - 84 - 280, y + 730, W - 84, y + 830], 30, fill=ACCENT)
    text(d, (W - 84 - 140, y + 780), "Refresh", 38, ACCENT_D, True, "mm")

    text(d, (84, y + 880),
         "Rates from open.er-api.com, saved on the device so the",
         30, MUTED)
    text(d, (84, y + 922),
         "converter keeps working without internet.",
         30, MUTED)

    y3 = y + 1000
    rounded(d, [42, y3, W - 42, y3 + 280], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y3 + 42), "Offline ready", 42, TEXT, True)
    text(d, (84, y3 + 112), "The last fetched rates stay on your phone, so", 34, MUTED)
    text(d, (84, y3 + 158), "the converter works with no signal too.", 34, MUTED)
    text(d, (84, y3 + 222), "28 currencies · refresh anytime", 32, ACCENT, True)
    save(img, "screenshots/04-currency.png")


def field(d, box, label, value):
    text(d, (box[0], box[1]), label.upper(), 30, MUTED, True)
    rounded(d, [box[0], box[1] + 44, box[2], box[1] + 164], 36, fill=SURFACE2)
    text(d, (box[0] + 30, box[1] + 104), value, 44, TEXT, False, "lm")


def shot_bmi():
    img, d = base_screen(4)
    y = 380
    rounded(d, [42, y, W - 42, y + 1360], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y + 38), "Body Mass Index", 45, TEXT, True)

    rounded(d, [84, y + 112, W - 84, y + 470], 48, fill=SURFACE2)
    text(d, (W / 2, y + 150), "BODY MASS INDEX", 30, MUTED, True, "ma")
    text(d, (W / 2, y + 240), "23.0", 132, ACCENT, True, "ma")
    text(d, (W / 2, y + 390), "Normal weight", 42, ACCENT, True, "ma")

    field(d, [84, y + 506, W - 84], "Weight (kg)", "68")
    field(d, [84, y + 710, W - 84], "Height (cm)", "172")

    text(d, (84, y + 900), "Healthy weight for 172 cm:", 36, MUTED)
    text(d, (84, y + 952), "54.7 – 73.7 kg", 42, TEXT, True)

    ref = [("Underweight", "below 18.5", WARN), ("Normal", "18.5 – 24.9", ACCENT),
           ("Overweight", "25.0 – 29.9", (255, 184, 107)), ("Obese", "30.0 +", DANGER)]
    ry = y + 1030
    for name, rng, col in ref:
        d.ellipse([86, ry + 12, 106, ry + 32], fill=col)
        text(d, (126, ry), name, 36, TEXT)
        text(d, (W - 126, ry), rng, 36, MUTED, False, "ra")
        ry += 62
    save(img, "screenshots/05-bmi.png")


def shot_loan():
    img, d = base_screen(5)
    y = 380
    rounded(d, [42, y, W - 42, y + 1340], 54, fill=SURFACE, outline=STROKE, w=3)
    text(d, (84, y + 38), "Loan / EMI Calculator", 45, TEXT, True)

    rounded(d, [84, y + 112, W - 84, y + 410], 48, fill=SURFACE2)
    text(d, (W / 2, y + 150), "MONTHLY PAYMENT", 30, MUTED, True, "ma")
    text(d, (W / 2, y + 250), "10,502.31", 96, ACCENT, True, "ma")

    field(d, [84, y + 446, W - 84], "Loan amount", "500000")
    field(d, [84, y + 626, W - 84], "Annual interest rate (%)", "9.5")
    field(d, [84, y + 806, W - 84], "Tenure (years)", "5")

    rows = [("Total interest", "130,138.60"), ("Total payment", "630,138.60"),
            ("Number of installments", "60 months")]
    ry = y + 1030
    for label, value in rows:
        text(d, (84, ry), label, 36, MUTED)
        text(d, (W - 84, ry), value, 40, TEXT, True, "ra")
        ry += 78
    save(img, "screenshots/06-loan.png")


def shot_history():
    img, d = base_screen(0)
    top = display_card(d, "14400", "1200 × 12 =")
    keypad(d, BASIC, top + 20, H - 90)

    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 170))
    img.paste(Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB"), (0, 0))
    d = ImageDraw.Draw(img)

    box = [72, 330, W - 72, 330 + 1290]
    rounded(d, box, 44, fill=(24, 30, 38))
    text(d, (box[0] + 50, box[1] + 40), "History", 52, TEXT, True)
    d.line([box[0], box[1] + 120, box[2], box[1] + 120], fill=STROKE, width=3)

    entries = [("1200 × 12", "14400"), ("sin(60)", "0.8660254"),
               ("1250 + 15%", "1437.5"), ("2^10", "1024"),
               ("5!", "120"), ("√(144)", "12"), ("45 × 8", "360")]
    ey = box[1] + 160
    for expr, val in entries:
        text(d, (box[0] + 50, ey), expr, 36, MUTED)
        text(d, (box[0] + 50, ey + 44), "= " + val, 48, TEXT, True)
        ey += 118

    d.line([box[0], box[3] - 130, box[2], box[3] - 130], fill=STROKE, width=3)
    text(d, (box[0] + 50, box[3] - 82), "Close", 32, MUTED)
    text(d, (box[2] - 50, box[3] - 82), "CLEAR ALL", 32, ACCENT, True, "ra")
    save(img, "screenshots/07-history.png")


def feature_graphic():
    fw, fh = 1024, 500
    img = Image.new("RGB", (fw, fh), BG)
    d = ImageDraw.Draw(img)

    # vertical gradient background
    for y in range(fh):
        t = y / fh
        r = int(9 + 12 * t)
        g = int(20 + 26 * t)
        b = int(26 + 30 * t)
        d.line([0, y, fw, y], fill=(r, g, b))

    # glow
    for i in range(220, 0, -1):
        a = int(28 * (1 - i / 220))
        d.ellipse([-160 - i, -180 - i, 460 + i, 440 + i], fill=(13 + a, 40 + a, 35 + a))

    # icon tile
    icon = Image.open(os.path.join(os.path.dirname(OUT), "res", "mipmap-xxxhdpi", "ic_launcher.png"))
    icon = icon.resize((232, 232), Image.LANCZOS)
    img.paste(icon, (86, 134), icon)

    text(d, (370, 158), "All-in-One", 84, TEXT, True)
    text(d, (370, 248), "Calculator", 84, ACCENT, True)
    text(d, (372, 356), "10 tools · one tiny app · fully offline", 36, (168, 182, 199))

    badges = ["Basic", "Scientific", "Units", "Currency", "BMI", "Loan"]
    bx = 372
    for b in badges:
        w = width_of(d, b, 26, True) + 28
        if bx + w > fw - 40:
            break
        rounded(d, [bx, 412, bx + w, 412 + 40], 20, fill=(28, 46, 44))
        text(d, (bx + w / 2, 432), b, 26, ACCENT, True, "mm")
        bx += w + 12

    path = os.path.join(OUT, "feature-graphic-1024x500.png")
    img.save(path, "PNG", optimize=True)
    print("wrote", path)


if __name__ == "__main__":
    os.makedirs(os.path.join(OUT, "screenshots"), exist_ok=True)
    shot_basic()
    shot_scientific()
    shot_units()
    shot_currency()
    shot_bmi()
    shot_loan()
    shot_history()
    feature_graphic()
