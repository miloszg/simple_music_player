from PIL import Image, ImageDraw
import math, pathlib

# Flow's plate tones — the covers stay inside the app's own palette so a shelf
# of real artwork and a shelf of fallback plates read as one library.
TONES = {
    "clay":   (0x37, 0x2B, 0x26),
    "cherry": (0xDE, 0x10, 0x43),
    "slate":  (0x20, 0x24, 0x29),
    "ochre":  (0x3D, 0x38, 0x26),
    "ink":    (0x1E, 0x19, 0x19),
    "rose":   (0x60, 0x3D, 0x3A),
}
INK = (0xF2, 0xEE, 0xE9)

S = 1000
out = pathlib.Path("art"); out.mkdir(exist_ok=True)

def base(tone):
    im = Image.new("RGB", (S, S), TONES[tone])
    return im, ImageDraw.Draw(im, "RGBA")

def sun(tone, name):
    """A low disc over a horizon — the genre's whole visual language."""
    im, d = base(tone)
    d.ellipse([S*0.26, S*0.20, S*0.74, S*0.68], fill=INK + (235,))
    for i in range(7):  # slatted horizon
        y = S*0.50 + i*S*0.032
        d.rectangle([0, y, S, y + S*0.011], fill=TONES[tone] + (255,))
    d.rectangle([0, S*0.70, S, S*0.706], fill=INK + (150,))
    return im

def rain(tone, name):
    """Diagonal rain, uneven — nothing metronomic."""
    im, d = base(tone)
    import random; random.seed(hash(name) & 0xFFFF)
    for _ in range(90):
        x = random.uniform(-S*0.2, S)
        y = random.uniform(0, S)
        ln = random.uniform(S*0.06, S*0.20)
        d.line([x, y, x + ln*0.34, y + ln], fill=INK + (random.randint(40, 150),), width=3)
    return im

def arc(tone, name):
    """One brush arc, left open."""
    im, d = base(tone)
    d.arc([S*0.16, S*0.16, S*0.84, S*0.84], start=145, end=60, fill=INK + (240,), width=int(S*0.045))
    return im

def bars(tone, name):
    """Amplitude, off the grid — a nod to the mark without repeating it."""
    im, d = base(tone)
    import random; random.seed(hash(name) & 0xFFFF)
    x = S*0.16
    while x < S*0.86:
        w = random.uniform(S*0.022, S*0.05)
        h = random.uniform(S*0.10, S*0.46)
        d.rounded_rectangle([x, S*0.62 - h, x + w, S*0.62], radius=w/2, fill=INK + (225,))
        x += w + random.uniform(S*0.022, S*0.05)
    d.line([S*0.10, S*0.70, S*0.90, S*0.70], fill=INK + (110,), width=4)
    return im

MAKERS = {"sun": sun, "rain": rain, "arc": arc, "bars": bars}

def make(name, tone, kind):
    im = MAKERS[kind](tone, name)
    p = out / f"{name}.png"
    im.save(p, quality=95)
    return p

if __name__ == "__main__":
    for n, t, k in [("hanami","cherry","sun"), ("shibuya","slate","rain"),
                    ("kissaten","ochre","arc"), ("kaido","ink","bars")]:
        print(make(n, t, k))
