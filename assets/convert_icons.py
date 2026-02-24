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
from PIL import Image


SCRIPT_DIR = Path(__file__).parent
MANIFEST = SCRIPT_DIR / "icon-grid-manifest.json"
OUT_DIR = SCRIPT_DIR / "../app/src/main/res/drawable-nodpi"


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
            if args.size != tile.width or args.size != tile.height:
                tile = tile.resize((args.size, args.size), Image.LANCZOS)
            out_path = out_dir / f"ic_grocery_{name}.png"
            tile.save(out_path, "PNG")
            print(f"  -> {out_path.name}")
            total_saved += 1

    print(f"\nDone. {total_saved} icon(s) written to {out_dir}")


if __name__ == "__main__":
    main()
