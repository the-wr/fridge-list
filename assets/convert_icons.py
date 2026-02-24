#!/usr/bin/env python3
from __future__ import annotations
"""
convert_icons.py — splits icon grid PNGs into individual drawables.

Usage:
    python convert_icons.py [--size SIZE]

    --size SIZE   Output pixel size for each icon (default: 512)

Reads icon-grid-manifest.json from the same directory as this script.
Source grid PNGs are also expected in that same directory.
Output goes to ../app/src/main/res/drawable-nodpi/ relative to this script.
"""

import argparse
import json
import math
import sys
from pathlib import Path
from PIL import Image, ImageChops


SCRIPT_DIR = Path(__file__).parent
MANIFEST = SCRIPT_DIR / "icon-grid-manifest.json"
OUT_DIR = SCRIPT_DIR / "../app/src/main/res/drawable-nodpi"


def center_tile(tile: Image.Image, white_threshold: int = 230, alpha_threshold: int = 20) -> Image.Image:
    """Re-center the non-transparent, non-white content within the tile canvas."""
    r, g, b, a = tile.split()

    # A pixel is "not white" if its darkest channel is below the threshold.
    # Using min(R,G,B) is more noise-resistant than any(R<t, G<t, B<t): a
    # single slightly-off-white channel no longer falsely triggers detection.
    min_rgb = ImageChops.darker(ImageChops.darker(r, g), b)
    not_white = min_rgb.point(lambda v: 255 if v < white_threshold else 0)

    # A pixel is "opaque enough" if its alpha meets the threshold
    a_ok = a.point(lambda v: 255 if v >= alpha_threshold else 0)

    # Content = not near-white AND sufficiently opaque
    content_mask = ImageChops.darker(not_white, a_ok)

    bbox = content_mask.getbbox()
    if bbox is None:
        return tile  # no content found

    cw, ch = tile.size
    content_cx = (bbox[0] + bbox[2]) / 2.0
    content_cy = (bbox[1] + bbox[3]) / 2.0
    offset_x = (cw / 2.0) - content_cx
    offset_y = (ch / 2.0) - content_cy

    # Skip if already centered (within half a pixel)
    if abs(offset_x) < 0.5 and abs(offset_y) < 0.5:
        return tile

    centered = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    centered.paste(tile, (round(offset_x), round(offset_y)))
    return centered


def split_grid(img: Image.Image, count: int) -> list[Image.Image]:
    """Cut a square grid image into `count` equal tiles, left-to-right top-to-bottom."""
    cols = int(math.sqrt(count))
    if cols * cols != count:
        raise ValueError(f"Icon count {count} is not a perfect square — cannot infer grid dimensions.")
    rows = cols
    w, h = img.size
    cell_w = w // cols
    cell_h = h // rows
    tiles = []
    for row in range(rows):
        for col in range(cols):
            x = col * cell_w
            y = row * cell_h
            tiles.append(img.crop((x, y, x + cell_w, y + cell_h)))
    return tiles


def main():
    parser = argparse.ArgumentParser(description="Split icon grid PNGs into individual app drawables.")
    parser.add_argument("--size", type=int, default=512, metavar="SIZE",
                        help="Output size in pixels for each icon square (default: 512)")
    args = parser.parse_args()

    if not MANIFEST.exists():
        print(f"ERROR: manifest not found at {MANIFEST}", file=sys.stderr)
        sys.exit(1)

    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    out_dir = OUT_DIR.resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    total_saved = 0

    for entry in manifest:
        src_path = SCRIPT_DIR / entry["file"]
        icons = entry["icons"]

        if not src_path.exists():
            print(f"WARNING: source file not found, skipping: {src_path}")
            continue

        print(f"Processing {src_path.name}  ({len(icons)} icons)")
        img = Image.open(src_path).convert("RGBA")
        tiles = split_grid(img, len(icons))

        for name, tile in zip(icons, tiles):
            tile = center_tile(tile)
            if args.size != tile.width or args.size != tile.height:
                tile = tile.resize((args.size, args.size), Image.LANCZOS)
            out_path = out_dir / f"ic_grocery_{name}.png"
            tile.save(out_path, "PNG")
            print(f"  -> {out_path.name}")
            total_saved += 1

    print(f"\nDone. {total_saved} icon(s) written to {out_dir}")


if __name__ == "__main__":
    main()
