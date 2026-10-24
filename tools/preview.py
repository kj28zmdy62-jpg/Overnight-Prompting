"""Contact sheet of block textures, each tiled 3x3, for eyeballing art quality."""
import sys, os, glob
from PIL import Image, ImageDraw
d = "src/main/resources/assets/noclip/textures/block"
pat = sys.argv[1] if len(sys.argv) > 1 else "*"
out = sys.argv[2] if len(sys.argv) > 2 else "/tmp/preview.png"
files = sorted(glob.glob(os.path.join(d, pat + ".png")))
cell = 3 * 32 * 2
cols = 5
rows = (len(files) + cols - 1) // cols
sheet = Image.new("RGB", (cols * (cell + 8), rows * (cell + 20)), (20, 20, 20))
dr = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(f).convert("RGBA")
    w, h = im.size
    tile = Image.new("RGBA", (w * 3, h * 3))
    for x in range(3):
        for y in range(3):
            tile.paste(im, (x * w, y * h))
    tile = tile.resize((cell, cell), Image.NEAREST)
    cx, cy = (i % cols) * (cell + 8), (i // cols) * (cell + 20)
    sheet.paste(tile, (cx, cy + 16), tile)
    dr.text((cx, cy + 2), os.path.basename(f)[:-4], fill=(220, 220, 220))
sheet.save(out)
print(out, len(files))
