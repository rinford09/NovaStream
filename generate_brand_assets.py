"""
NovaStream Brand Asset Generator
Reproducibly generates high-resolution Android launcher icons, TV banners, and logos.
Palette:
  - Midnight Navy: #080B14
  - Deep Navy:     #101827
  - Elevated Navy: #182235
  - Electric Cyan: #35D6F2
  - Nova Violet:   #8768FF
  - Soft White:    #F4F7FC
"""

import os
import math
from PIL import Image, ImageDraw, ImageFont

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(BASE_DIR, "app", "src", "main", "res")

# Color palette
MIDNIGHT_NAVY = (8, 11, 20, 255)       # #080B14
DEEP_NAVY     = (16, 24, 39, 255)      # #101827
ELEVATED_NAVY = (24, 34, 53, 255)      # #182235
BORDER_NAVY   = (38, 53, 77, 255)      # #26354D
ELECTRIC_CYAN = (53, 214, 242, 255)    # #35D6F2
NOVA_VIOLET   = (135, 104, 255, 255)   # #8768FF
SOFT_WHITE    = (244, 247, 252, 255)   # #F4F7FC
COOL_GRAY     = (154, 168, 188, 255)   # #9AA8BC


def draw_nova_star(draw, cx, cy, radius, fill_color):
    """Draws a 4-point radiant Nova star with concave flare curves."""
    points = []
    steps = 36
    r_in = radius * 0.16
    for i in range(steps * 4):
        angle = (i / (steps * 4)) * 2 * math.pi
        arm_phase = (i % steps) / steps
        # Curvature formula for star flare
        curve = math.sin(arm_phase * math.pi)
        r = r_in + (radius - r_in) * ((1.0 - curve) ** 2.2)
        px = cx + r * math.cos(angle)
        py = cy + r * math.sin(angle)
        points.append((px, py))
    draw.polygon(points, fill=fill_color)


def create_launcher_icon(size=512, is_round=False):
    """Creates a high-resolution launcher icon with orbital streaming ribbons and radiant Nova core."""
    scale = size / 108.0
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = size / 2.0, size / 2.0

    # Draw background
    if is_round:
        draw.ellipse([0, 0, size, size], fill=MIDNIGHT_NAVY)
    else:
        # squircle / rounded rect with 22% radius
        r = int(size * 0.22)
        draw.rounded_rectangle([0, 0, size, size], radius=r, fill=MIDNIGHT_NAVY)

    # Cosmic radial glow
    glow_r = int(28 * scale)
    draw.ellipse([cx - glow_r, cy - glow_r, cx + glow_r, cy + glow_r], fill=(53, 214, 242, 35))

    # Outer Streaming Orbital Arcs (Electric Cyan & Nova Violet)
    arc_box = [cx - 27 * scale, cy - 27 * scale, cx + 27 * scale, cy + 27 * scale]
    arc_w = max(2, int(4 * scale))
    draw.arc(arc_box, start=180, end=340, fill=ELECTRIC_CYAN, width=arc_w)
    draw.arc(arc_box, start=0, end=160, fill=NOVA_VIOLET, width=arc_w)

    # Inner Streaming Wave Arcs
    inner_box = [cx - 18 * scale, cy - 18 * scale, cx + 18 * scale, cy + 18 * scale]
    inner_w = max(1, int(2.5 * scale))
    draw.arc(inner_box, start=190, end=330, fill=ELECTRIC_CYAN, width=inner_w)
    draw.arc(inner_box, start=10, end=150, fill=NOVA_VIOLET, width=inner_w)

    # Radiant Cosmic Nova Star (Electric Cyan primary flare, Violet accent, White core)
    draw_nova_star(draw, cx, cy, 21 * scale, fill_color=ELECTRIC_CYAN)
    draw_nova_star(draw, cx, cy, 13 * scale, fill_color=(167, 139, 250, 255))
    draw_nova_star(draw, cx, cy, 9 * scale, fill_color=SOFT_WHITE)

    # Center sparkle dot
    dot_r = max(1, int(2.5 * scale))
    draw.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r], fill=(255, 255, 255, 255))

    return img


def create_tv_banner(width=640, height=360):
    """Creates a high-resolution 16:9 Android TV Leanback Banner."""
    img = Image.new("RGBA", (width, height), MIDNIGHT_NAVY)
    draw = ImageDraw.Draw(img)

    # Subtle horizontal gradient from Midnight Navy to Deep Navy
    for x in range(width):
        t = x / float(width)
        r = int(8 + (16 - 8) * t)
        g = int(11 + (24 - 11) * t)
        b = int(20 + (39 - 20) * t)
        draw.line([(x, 0), (x, height)], fill=(r, g, b, 255))

    # Nova emblem on the left
    emblem_size = int(height * 0.72)
    emblem = create_launcher_icon(size=emblem_size, is_round=True)
    emblem_x = int(width * 0.10)
    emblem_y = int((height - emblem_size) / 2)
    img.paste(emblem, (emblem_x, emblem_y), emblem)

    # NOVASTREAM Wordmark
    text_x = emblem_x + emblem_size + int(width * 0.05)
    text_y = int(height * 0.32)

    try:
        font = ImageFont.truetype("arialbd.ttf", int(height * 0.18))
        sub_font = ImageFont.truetype("arial.ttf", int(height * 0.075))
    except:
        font = ImageFont.load_default()
        sub_font = ImageFont.load_default()

    draw.text((text_x, text_y), "NOVASTREAM", fill=SOFT_WHITE, font=font)

    # Electric Cyan accent line under wordmark
    line_y = text_y + int(height * 0.22)
    line_w = int(width * 0.36)
    draw.line([(text_x, line_y), (text_x + line_w, line_y)], fill=ELECTRIC_CYAN, width=max(2, int(height * 0.012)))

    # Subtitle: CINEMATIC STREAMING
    draw.text((text_x, line_y + int(height * 0.03)), "PREMIUM ENTERTAINMENT", fill=COOL_GRAY, font=sub_font)

    return img


def generate_all():
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    print("Generating Android Launcher Icons...")
    for folder, px in densities.items():
        out_dir = os.path.join(RES_DIR, folder)
        os.makedirs(out_dir, exist_ok=True)

        icon = create_launcher_icon(size=px, is_round=False)
        icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")

        icon_round = create_launcher_icon(size=px, is_round=True)
        icon_round.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
        print(f"  Saved {folder} ({px}x{px})")

    print("\nGenerating Android TV Leanback Banners...")
    banner_xhdpi = create_tv_banner(width=320, height=180)
    banner_xhdpi.save(os.path.join(RES_DIR, "mipmap-xhdpi", "ic_banner.png"), "PNG")
    print("  Saved mipmap-xhdpi/ic_banner.png (320x180)")

    drawable_dir = os.path.join(RES_DIR, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)

    banner_full = create_tv_banner(width=640, height=360)
    banner_full.save(os.path.join(drawable_dir, "novastream_banner.png"), "PNG")
    print("  Saved drawable/novastream_banner.png (640x360)")

    logo_512 = create_launcher_icon(size=512, is_round=False)
    logo_512.save(os.path.join(drawable_dir, "novastream_logo.png"), "PNG")
    print("  Saved drawable/novastream_logo.png (512x512)")

    print("\nBrand asset generation completed successfully!")


if __name__ == "__main__":
    generate_all()
