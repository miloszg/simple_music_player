from PIL import Image, ImageDraw, ImageFont
import pathlib, frame

FONTS = pathlib.Path("/Users/milosz.gustawski.ext/conductor/workspaces/simple_music_player/port-moresby/app/src/main/res/font")
SERIF, SANS = str(FONTS/"instrument_serif.ttf"), str(FONTS/"instrument_sans.ttf")

W, H = 1080, 1920
PAPER = (0xF4, 0xF2, 0xF0)     # warm off-white, the Apple listing ground
CHERRY = (0xCC, 0x00, 0x35)    # the light-theme cherry — reads on paper
MUTED = (0x6B, 0x63, 0x66)

# Almost no text: one line each. The screenshots carry it.
PANELS = [
    ("home",          "Your music. Nothing else."),
    ("player",        "A player, not a platform."),
    ("library",       "Every album you kept."),
    ("detail",        "Straight to the track."),
    ("search",        "Finds it instantly."),
    ("library-songs", "Even the untagged ones."),
]

out = pathlib.Path("store/phoneScreenshots")
for f in out.glob("*.png"): f.unlink()
out.mkdir(parents=True, exist_ok=True)

hf = ImageFont.truetype(SERIF, 68)

for i, (name, line) in enumerate(PANELS, 1):
    panel = Image.new("RGB", (W, H), PAPER)
    d = ImageDraw.Draw(panel)

    tw = d.textlength(line, font=hf)
    d.text(((W - tw) / 2, 96), line, font=hf, fill=CHERRY)

    dev = frame.device(Image.open(f"shot-{name}.png"), 716)
    canvas, pad = frame.with_shadow(dev)
    # Bleeds off the bottom, so the device reads as continuing past the frame.
    panel.paste(canvas, ((W - canvas.size[0]) // 2, 232 - pad), canvas)

    p = out / f"{i}_{name}.png"
    panel.save(p, optimize=True)
    print(f"  {p.name}")
