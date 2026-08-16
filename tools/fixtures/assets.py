from PIL import Image, ImageDraw, ImageFont
import pathlib, math

FONTS = pathlib.Path("/Users/milosz.gustawski.ext/conductor/workspaces/simple_music_player/port-moresby/app/src/main/res/font")
SERIF, SANS = str(FONTS/"instrument_serif.ttf"), str(FONTS/"instrument_sans.ttf")
BLACK, CHERRY, WHITE = (7,7,7), (0xDE,0x10,0x43), (255,255,255)
out = pathlib.Path("store"); out.mkdir(exist_ok=True)

def mark(d, cx, cy, scale, color):
    """The FLOW mark on the design's 100-unit grid: three bars, then the wave."""
    def X(v): return cx + (v - 54.25) * scale
    def Y(v): return cy + (v - 52.0) * scale
    for x, y0, y1 in [(13,37,67), (29,25,79), (45,17,87)]:
        d.line([X(x), Y(y0), X(x), Y(y1)], fill=color, width=int(10*scale), joint="curve")
        r = 5*scale
        for yy in (y0, y1):
            d.ellipse([X(x)-r, Y(yy)-r, X(x)+r, Y(yy)+r], fill=color)
    # cubic M56,52 C66,34 74,70 96,54
    pts, w = [], int(9*scale)
    for i in range(61):
        t = i/60
        bx = (1-t)**3*56 + 3*(1-t)**2*t*66 + 3*(1-t)*t**2*74 + t**3*96
        by = (1-t)**3*52 + 3*(1-t)**2*t*34 + 3*(1-t)*t**2*70 + t**3*54
        pts.append((X(bx), Y(by)))
    d.line(pts, fill=color, width=w, joint="curve")
    for p in (pts[0], pts[-1]):
        r = w/2
        d.ellipse([p[0]-r, p[1]-r, p[0]+r, p[1]+r], fill=color)

# ── 512x512 store icon ───────────────────────────────────────────────────────
S = 512
icon = Image.new("RGB", (S, S), CHERRY)
mark(ImageDraw.Draw(icon), S/2, S/2, S*0.0048, WHITE)
icon.save(out/"icon.png"); print("  icon.png 512x512")

# ── 1024x500 feature graphic ─────────────────────────────────────────────────
# Play crops this hard on some surfaces, so everything that matters sits in the
# middle 2/3 and nothing is closer than 80px to an edge.
FW, FH = 1024, 500
fg = Image.new("RGB", (FW, FH), BLACK)
d = ImageDraw.Draw(fg)
mark(d, 148, FH/2 - 6, 0.72, CHERRY)
d.text((250, 176), "FLOW", font=ImageFont.truetype(SANS, 76), fill=WHITE)
d.text((252, 268), "The music already on your phone", font=ImageFont.truetype(SERIF, 40), fill=(0xC8,0xC8,0xCE))
d.line([(252, 246), (252+236, 246)], fill=CHERRY, width=4)
fg.save(out/"featureGraphic.png"); print("  featureGraphic.png 1024x500")

# ── feature graphic, take two ────────────────────────────────────────────────
def tracked(d, xy, text, font, fill, tracking):
    """PIL has no letter-spacing; the wordmark is .24em and unreadable without it."""
    x, y = xy
    for ch in text:
        d.text((x, y), ch, font=font, fill=fill)
        x += d.textlength(ch, font=font) + tracking
    return x

fg = Image.new("RGB", (FW, FH), BLACK)
d = ImageDraw.Draw(fg)

wf = ImageFont.truetype(SANS, 68)
sf = ImageFont.truetype(SERIF, 38)
TRACK = 16

word_w = sum(d.textlength(c, font=wf) for c in "FLOW") + TRACK*3
sub = "The music already on your phone"
sub_w = d.textlength(sub, font=sf)

mark_r = 96                 # mark's drawn half-width at the scale below
gap = 46
block_w = mark_r*2 + gap + max(word_w, sub_w)
x0 = (FW - block_w) / 2     # centre the whole lockup, not each part
cy = FH/2

mark(d, x0 + mark_r, cy, 1.05, CHERRY)

tx = x0 + mark_r*2 + gap
tracked(d, (tx, cy - 62), "FLOW", wf, WHITE, TRACK)
d.line([(tx + 2, cy + 12), (tx + 2 + max(word_w, sub_w) - TRACK, cy + 12)], fill=CHERRY, width=3)
d.text((tx + 2, cy + 26), sub, font=sf, fill=(0xB4, 0xB4, 0xBC))

fg.save(out/"featureGraphic.png"); print("  featureGraphic.png rebuilt")
