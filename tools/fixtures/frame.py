from PIL import Image, ImageDraw, ImageFilter

def device(screen: Image.Image, width: int) -> Image.Image:
    """
    A believable phone around a screenshot.

    Body is near-black with a thin lighter rim — the titanium edge catching
    light — plus the side buttons. Returned with an alpha channel so the panel
    can drop a real shadow behind it.
    """
    sw, sh = screen.size
    scale = width / sw
    sw, sh = width, int(sh * scale)

    bezel = max(6, int(width * 0.017))
    rim = max(2, int(width * 0.004))
    W, H = sw + bezel*2, sh + bezel*2
    r_out = int(width * 0.115)
    r_in = r_out - bezel

    dev = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(dev)
    d.rounded_rectangle([0, 0, W-1, H-1], radius=r_out, fill=(28, 28, 30, 255))
    d.rounded_rectangle([0, 0, W-1, H-1], radius=r_out, outline=(126, 126, 134, 255), width=rim)
    d.rounded_rectangle([rim, rim, W-1-rim, H-1-rim], radius=r_out-rim,
                        outline=(58, 58, 62, 255), width=max(1, rim//2))

    shot = screen.resize((sw, sh), Image.LANCZOS).convert("RGBA")
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw-1, sh-1], radius=r_in, fill=255)
    dev.paste(shot, (bezel, bezel), mask)

    # Side buttons, on the metal rim.
    bw = max(2, int(width*0.006))
    for y0, y1 in [(0.185, 0.235), (0.262, 0.335), (0.352, 0.425)]:
        d.rounded_rectangle([-bw, H*y0, rim, H*y1], radius=bw, fill=(150,150,158,255))
    d.rounded_rectangle([W-rim-1, H*0.225, W+bw, H*0.325], radius=bw, fill=(150,150,158,255))
    return dev


def with_shadow(dev: Image.Image, blur=46, dy=26, dx=6, opacity=118, spread=10):
    """Soft contact shadow, offset down. Apple's listings live on this."""
    W, H = dev.size
    pad = blur*3
    canvas = Image.new("RGBA", (W + pad*2, H + pad*2), (0,0,0,0))

    sh = Image.new("RGBA", canvas.size, (0,0,0,0))
    ImageDraw.Draw(sh).rounded_rectangle(
        [pad - spread + dx, pad - spread + dy, pad + W + spread + dx, pad + H + spread + dy],
        radius=int(W*0.12), fill=(0, 0, 0, opacity))
    sh = sh.filter(ImageFilter.GaussianBlur(blur))

    canvas.alpha_composite(sh)
    canvas.alpha_composite(dev, (pad, pad))
    return canvas, pad
