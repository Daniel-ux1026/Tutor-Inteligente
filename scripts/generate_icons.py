from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "frontend" / "public" / "icons"
OUT.mkdir(parents=True, exist_ok=True)

for size in (192, 512):
    image = Image.new("RGB", (size, size), "#087f7a")
    draw = ImageDraw.Draw(image)
    margin = size // 9
    draw.rounded_rectangle((margin, margin, size - margin, size - margin), radius=size // 5, fill="#ffffff")
    # Libro abierto, construido con formas simples para permanecer legible en tamaños pequeños.
    top = size * 0.34; bottom = size * 0.68; center = size / 2
    draw.rounded_rectangle((size*.23, top, center-3, bottom), radius=size//30, fill="#dff5f2", outline="#087f7a", width=max(2,size//45))
    draw.rounded_rectangle((center+3, top, size*.77, bottom), radius=size//30, fill="#e8f0ff", outline="#2563eb", width=max(2,size//45))
    draw.line((center, top+4, center, bottom-4), fill="#17243a", width=max(2,size//55))
    try:
        font = ImageFont.truetype("arialbd.ttf", size//7)
    except OSError:
        font = ImageFont.load_default()
    label = "TI"
    box = draw.textbbox((0, 0), label, font=font)
    draw.text(((size-(box[2]-box[0]))/2, size*.72), label, font=font, fill="#17243a")
    image.save(OUT / f"icon-{size}.png", optimize=True)

print(f"Iconos generados en {OUT}")
