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
import collections
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


def remove_white_background(tile: Image.Image, white_threshold: int = 230) -> Image.Image:
    """Replace the exterior white background with transparency using flood-fill.

    Flood-fills outward from all four edges, marking every connected near-white
    pixel as background.  Interior whites (e.g. shine highlights) are enclosed
    by the icon outline and therefore never reached by the fill, so they are
    left untouched.
    """
    w, h = tile.size
    r, g, b, a = tile.split()

    min_rgb = ImageChops.darker(ImageChops.darker(r, g), b)
    min_pix = min_rgb.load()

    visited = bytearray(w * h)
    queue = collections.deque()

    def try_enqueue(x, y):
        if 0 <= x < w and 0 <= y < h:
            idx = y * w + x
            if not visited[idx] and min_pix[x, y] >= white_threshold:
                visited[idx] = 1
                queue.append((x, y))

    for x in range(w):
        try_enqueue(x, 0)
        try_enqueue(x, h - 1)
    for y in range(1, h - 1):
        try_enqueue(0, y)
        try_enqueue(w - 1, y)

    while queue:
        x, y = queue.popleft()
        try_enqueue(x + 1, y)
        try_enqueue(x - 1, y)
        try_enqueue(x, y + 1)
        try_enqueue(x, y - 1)

    # BFS outward from the flood-fill boundary to tag a fringe zone.
    # Only pixels within fringe_size steps of a background pixel can be faded.
    # This prevents the fade from touching interior light elements (e.g. shine
    # highlights) that are surrounded by icon content and far from the edge.
    fringe_size = 8
    fringe_dist = bytearray(w * h)  # 0 = not in fringe; 1..fringe_size = distance
    fringe_queue = collections.deque()

    for i in range(w * h):
        if visited[i]:
            x, y = i % w, i // w
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h:
                    nidx = ny * w + nx
                    if not visited[nidx] and not fringe_dist[nidx]:
                        fringe_dist[nidx] = 1
                        fringe_queue.append((nx, ny))

    while fringe_queue:
        x, y = fringe_queue.popleft()
        d = fringe_dist[y * w + x]
        if d < fringe_size:
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h:
                    nidx = ny * w + nx
                    if not visited[nidx] and not fringe_dist[nidx]:
                        fringe_dist[nidx] = d + 1
                        fringe_queue.append((nx, ny))

    # Build final alpha:
    #   background              → 0
    #   fringe zone             → proportional to departure from white_threshold:
    #                             0 at min(RGB)=white_threshold, 255 at white_threshold-fade_range
    #   solid content / interior → original alpha unchanged
    fade_range = 80
    orig_alpha = list(a.getdata())
    min_data = list(min_rgb.getdata())
    final_alpha = []
    for i in range(w * h):
        if visited[i]:
            final_alpha.append(0)
        elif fringe_dist[i]:
            m = min_data[i]
            alpha = (white_threshold - m) * 255 // fade_range
            alpha = max(0, min(255, alpha))
            final_alpha.append(min(orig_alpha[i], alpha))
        else:
            final_alpha.append(orig_alpha[i])

    new_a = Image.new("L", (w, h))
    new_a.putdata(final_alpha)

    result = tile.copy()
    result.putalpha(new_a)
    return result


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
            tile = remove_white_background(tile)
            if args.size != tile.width or args.size != tile.height:
                tile = tile.resize((args.size, args.size), Image.LANCZOS)
            out_path = out_dir / f"ic_grocery_{name}.png"
            tile.save(out_path, "PNG")
            print(f"  -> {out_path.name}")
            total_saved += 1

    print(f"\nDone. {total_saved} icon(s) written to {out_dir}")


if __name__ == "__main__":
    main()
