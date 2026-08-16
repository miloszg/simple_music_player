from PIL import Image, ImageDraw, ImageFont
import pathlib, textwrap

FONTS = pathlib.Path("/Users/milosz.gustawski.ext/conductor/workspaces/simple_music_player/port-moresby/app/src/main/res/font")
SERIF = str(FONTS / "instrument_serif.ttf")
SERIF_I = str(FONTS / "instrument_serif_italic.ttf")
SANS = str(FONTS / "instrument_sans.ttf")

W, H = 1080, 1920
BLACK = (0x07, 0x07, 0x07)
CHERRY = (0xDE, 0x10, 0x43)
WHITE = (0xFF, 0xFF, 0xFF)

def fade(c, a):  # flat tint over the ground, no gradients
    return tuple(int(x * a) for x in c)

PANELS = [
    ("home",          BLACK,  "The music\nalready on\nyour phone.",      "No account. No network. No ads."),
    ("library",       CHERRY, "Your shelf,\nnot a feed.",                 "Albums, artists, playlists, liked songs."),
    ("player",        BLACK,  "A player that\ngets out of\nthe way.",     "Lock screen, headphones, Android Auto."),
    ("library-songs", BLACK,  "No artwork?\nStill looks\nlike something.","Untagged albums get a plate, not a grey square."),
    ("search",        CHERRY, "Finds it before\nyou finish\ntyping.",     "The whole library is held in memory."),
    ("settings",      BLACK,  "Nothing leaves\nyour phone.\nEver.",       "There is no network permission to revoke."),
]

def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.size[0]-1, im.size[1]-1], radius=r, fill=255)
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out

out = pathlib.Path("store/phoneScreenshots"); out.mkdir(parents=True, exist_ok=True)

for i, (name, bg, head, sub) in enumerate(PANELS, 1):
    panel = Image.new("RGB", (W, H), bg)
    d = ImageDraw.Draw(panel)
    on_cherry = bg == CHERRY
    ink = WHITE
    ink2 = fade(WHITE, .78) if on_cherry else (0x9A, 0x9A, 0xA2)
    accent = WHITE if on_cherry else (0xFF, 0x38, 0x54)

    # Wordmark rule at the top — the listing reads as one set.
    d.text((72, 84), "FLOW", font=ImageFont.truetype(SANS, 30), fill=accent)
    d.line([(72, 140), (W-72, 140)], fill=fade(WHITE, .16) if not on_cherry else fade(WHITE, .34), width=2)

    fh = ImageFont.truetype(SERIF, 92)
    y = 214
    for line in head.split("\n"):
        d.text((72, y), line, font=fh, fill=ink)
        y += 98

    d.text((72, y + 22), sub, font=ImageFont.truetype(SANS, 32), fill=ink2)

    # Device: bleeds off the bottom edge, the standard listing treatment.
    shot = Image.open(f"shot-{name}.png").convert("RGB")
    dw = 760
    dh = int(shot.size[1] * dw / shot.size[0])
    dev = rounded(shot.resize((dw, dh), Image.LANCZOS), 40)
    dx = (W - dw) // 2
    dy = y + 150
    panel.paste(dev, (dx, dy), dev)
    # hairline edge so the black device separates from a black ground
    ImageDraw.Draw(panel).rounded_rectangle(
        [dx, dy, dx + dw - 1, min(dy + dh - 1, H - 1)], radius=40,
        outline=fade(WHITE, .22), width=2)

    p = out / f"{i}_{name}.png"
    panel.save(p, optimize=True)
    print(f"  {p.name}  {panel.size}")
