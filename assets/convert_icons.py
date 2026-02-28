#!/usr/bin/env python3
from __future__ import annotations
"""
convert_icons.py — splits icon grid PNGs into individual drawables.

Usage:
    python convert_icons.py [--size SIZE]

    --size SIZE   Output size in pixels for each icon square (default: 512)

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


def find_icon_centers(
    img: Image.Image,
    count: int,
    white_threshold: int = 230,
    alpha_threshold: int = 20,
) -> list[tuple[float, float]]:
    """Return (cx, cy) in source-image coordinates for each icon in the grid.

    Runs a connected-component scan on the *full* source image so that the
    true visual centre of each icon is found before any cropping happens.
    Each component is assigned to the grid cell whose geometric centre is
    closest to the component's own centre.  All components in a cell are
    unioned into a single bbox, and that bbox's centre becomes the crop
    centre for the icon.

    Falls back to the geometric grid-cell centre for any cell in which no
    content is detected (e.g. truly blank cells or cells with only noise).
    """
    cols = int(math.sqrt(count))
    if cols * cols != count:
        raise ValueError(
            f"Icon count {count} is not a perfect square — cannot infer grid dimensions."
        )
    rows = cols
    w, h = img.size
    cell_w = w / cols
    cell_h = h / rows

    r, g, b, a = img.split()
    min_rgb = ImageChops.darker(ImageChops.darker(r, g), b)
    min_data = min_rgb.load()
    a_data = a.load()

    def is_content(x: int, y: int) -> bool:
        return min_data[x, y] < white_threshold and a_data[x, y] >= alpha_threshold

    # Per-cell accumulated bounding box: (x0, y0, x1, y1), x1/y1 exclusive.
    cell_bboxes: list[list[tuple[int, int, int, int] | None]] = [
        [None] * cols for _ in range(rows)
    ]

    # Ignore components smaller than 0.2% of a cell — those are JPEG/PNG
    # compression artefacts or stray anti-aliasing dots, not real icons.
    min_area = cell_w * cell_h * 0.002

    visited = bytearray(w * h)

    for start_y in range(h):
        for start_x in range(w):
            idx = start_y * w + start_x
            if visited[idx] or not is_content(start_x, start_y):
                continue

            # BFS: find the entire connected component (4-connectivity).
            visited[idx] = 1
            queue = collections.deque([(start_x, start_y)])
            bx0 = bx1 = start_x
            by0 = by1 = start_y
            size = 1

            while queue:
                x, y = queue.popleft()
                if x < bx0: bx0 = x
                if x > bx1: bx1 = x
                if y < by0: by0 = y
                if y > by1: by1 = y
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h:
                        nidx = ny * w + nx
                        if not visited[nidx] and is_content(nx, ny):
                            visited[nidx] = 1
                            queue.append((nx, ny))
                            size += 1

            if size < min_area:
                continue  # noise — skip

            # Assign this component to the grid cell that contains its centre.
            comp_cx = (bx0 + bx1 + 1) / 2.0
            comp_cy = (by0 + by1 + 1) / 2.0
            col = min(cols - 1, int(comp_cx / cell_w))
            row = min(rows - 1, int(comp_cy / cell_h))

            # Merge into the running bbox for that cell.
            prev = cell_bboxes[row][col]
            if prev is None:
                cell_bboxes[row][col] = (bx0, by0, bx1 + 1, by1 + 1)
            else:
                cell_bboxes[row][col] = (
                    min(prev[0], bx0),
                    min(prev[1], by0),
                    max(prev[2], bx1 + 1),
                    max(prev[3], by1 + 1),
                )

    # Build the flat (cx, cy) list in row-major order.
    centers: list[tuple[float, float]] = []
    for row in range(rows):
        for col in range(cols):
            bbox = cell_bboxes[row][col]
            if bbox is not None:
                centers.append(((bbox[0] + bbox[2]) / 2.0, (bbox[1] + bbox[3]) / 2.0))
            else:
                # Nothing detected — fall back to the geometric cell centre.
                print(
                    f"  WARNING: no content detected in grid cell ({row},{col}), "
                    "using geometric centre as fallback"
                )
                centers.append(((col + 0.5) * cell_w, (row + 0.5) * cell_h))

    return centers


def remove_white_background(tile: Image.Image, white_threshold: int = 230) -> Image.Image:
    """Replace the exterior background with transparency using flood-fill.

    Seeds the flood-fill from every pixel on the tile border — not just white
    ones.  White edge pixels are exterior background; non-white edge pixels are
    bleed from a neighbouring icon in the source grid.  Both are treated as
    background.

    The BFS then expands same-type-only:
      • white  pixels expand to white  neighbours  → clears the background
      • non-white pixels expand to non-white neighbours → clears any bleed
        cluster anchored to the edge without crossing into the main icon

    Interior whites (e.g. shine highlights) enclosed by the icon outline are
    never reached by the white fill, so they are left untouched.
    """
    w, h = tile.size
    r, g, b, a = tile.split()

    min_rgb = ImageChops.darker(ImageChops.darker(r, g), b)
    min_pix = min_rgb.load()

    visited = bytearray(w * h)
    queue = collections.deque()

    def seed(x, y):
        idx = y * w + x
        if not visited[idx]:
            visited[idx] = 1
            queue.append((x, y))

    for x in range(w):
        seed(x, 0)
        seed(x, h - 1)
    for y in range(1, h - 1):
        seed(0, y)
        seed(w - 1, y)

    while queue:
        x, y = queue.popleft()
        cur_white = min_pix[x, y] >= white_threshold
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h:
                nidx = ny * w + nx
                if not visited[nidx] and (min_pix[nx, ny] >= white_threshold) == cur_white:
                    visited[nidx] = 1
                    queue.append((nx, ny))

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


def crop_centered(
    img: Image.Image, cx: float, cy: float, tile_w: int, tile_h: int
) -> Image.Image:
    """Crop a tile_w × tile_h region from img centred on (cx, cy).

    If the crop window extends beyond the image boundary the out-of-bounds
    area is filled with opaque white (matching the typical grid background)
    so that remove_white_background can still peel it away cleanly.
    """
    x0 = round(cx - tile_w / 2)
    y0 = round(cy - tile_h / 2)

    # Region that actually overlaps the source image
    src_x0 = max(0, x0)
    src_y0 = max(0, y0)
    src_x1 = min(img.width, x0 + tile_w)
    src_y1 = min(img.height, y0 + tile_h)

    tile = Image.new("RGBA", (tile_w, tile_h), (255, 255, 255, 255))
    region = img.crop((src_x0, src_y0, src_x1, src_y1))
    tile.paste(region, (src_x0 - x0, src_y0 - y0))
    return tile


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

        cols = int(math.sqrt(len(icons)))
        cell_w = round(img.width / cols)
        cell_h = round(img.height / cols)

        centers = find_icon_centers(img, len(icons))

        for name, (cx, cy) in zip(icons, centers):
            tile = crop_centered(img, cx, cy, cell_w, cell_h)
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
