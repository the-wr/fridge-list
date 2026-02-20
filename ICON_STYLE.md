# FridgeList — Icon Style Guide

This document defines the visual style for all 500 icons in the FridgeList icon set. Every icon must conform to this guide to ensure a consistent, unified appearance across the full set.

---

## Overview

The style is **bold flat clipart** — thick outlines, vibrant fills, simplified shapes, and small playful details. Icons must be immediately recognisable at small sizes (as small as ~48dp) and must remain legible when fully desaturated.

---

## 1. Outlines

- **Stroke weight**: thick, consistent monoline stroke across all icons — do not vary weight within a single icon or across the set
- **Stroke colour**: solid black (`#000000` or near-black)
- **Corners and terminals**: rounded — line ends and joins use round caps/joins throughout, giving a friendly, soft appearance
- **No hairlines**: avoid thin decorative strokes; every line must be clearly visible at small sizes

---

## 2. Colour Fills

- **Style**: flat fills only — no gradients, no drop shadows, no inner glows
- **Palette per icon**: each icon uses **2–3 colours maximum** (excluding black outlines and white highlights)
- **Saturation**: colours are punchy and saturated — "candy-like" — so icons pop against both light and dark backgrounds and remain visually distinctive after desaturation
- **Consistency**: shared objects across icons (e.g. leaves, liquid drops, packaging) should use consistent colour conventions throughout the set (e.g. leaves are always the same green family)

---

## 3. Shape and Perspective

- **Highly simplified**: shapes are reduced to their most recognisable silhouette — remove all unnecessary detail
- **Exaggerated features**: key identifying features may be exaggerated slightly for recognisability (e.g. a mango is a bold teardrop with a prominent leaf; rice is represented by a few dots suggesting texture, not individual grains)
- **Perspective**: flat, 2D — either straight-on or top-down. No isometric or 3D perspective. No cast shadows
- **Instantly recognisable**: every icon must be identifiable without a label; if a shape is ambiguous, add one distinguishing detail rather than adding complexity

---

## 4. Playful Details

Used sparingly to add character without adding visual noise:

- **Shine / highlight**: a small white oval or curved mark suggesting a light reflection (e.g. on fruit, bottles, jars, cans). Gives a sense of volume without 3D shading
- **Texture marks**: small dashes, dots, or short lines to imply surface texture (e.g. skin of a peanut, veins on a cabbage leaf, seeds on a strawberry). Use minimally — 2–4 marks maximum
- **Whimsy**: occasional small expressive details are permitted where they aid recognition or add charm (e.g. steam lines above a hot drink, a tiny face on a bowl). Not required on every icon — use judgement

---

## 5. Canvas and Sizing

- **Canvas**: square, with consistent padding on all sides (icon subject should not touch the canvas edge)
- **Format**: SVG with a clean, well-structured path structure — suitable for conversion to Android VectorDrawable
- **Background**: transparent — no background fill on the canvas
- **Centring**: the primary subject is visually centred on the canvas

---

## 6. Desaturation Compatibility

All icons will be rendered in two states in the app:
- **Full colour** (needed state)
- **Grayscale + reduced opacity** (not needed state)

Design requirements for desaturation compatibility:
- Icons must remain legible and distinguishable from one another when fully desaturated — rely on **shape and outline**, not colour alone, for identity
- Avoid using colour as the *only* differentiator between similar icons (e.g. red apple vs green apple — add a shape difference too)
- The black outline provides the primary structure in the desaturated state; ensure it is strong enough to carry the icon on its own

---

## 7. Consistency Across the Set

- All 500 icons must look like they belong to the same family — same stroke weight, same palette intensity, same level of detail, same perspective conventions
- When drawing related items (e.g. different types of cheese, different bottles), use consistent visual conventions as a starting point and differentiate with colour and one key shape detail
- Icons that are inherently similar in silhouette (e.g. oil bottle vs vinegar bottle, shampoo vs conditioner vs body wash, spice jars) **must** include a clear differentiator — a distinctive label colour block, a visible ingredient detail, or a unique cap/shape

---

## Reference Examples (Style Description)

| Element | Treatment |
|---|---|
| Mango | Bold teardrop body, prominent leaf, white oval highlight, 2 colours + black |
| Oats / Porridge | Round bowl, dotted texture on oats, optional tiny highlight on bowl |
| Cabbage | Round head, curved vein lines as texture marks |
| Cream / Dairy box | Rectangular carton, colour band, white highlight stripe |
| Peanut | Oval body, dashed texture marks along the shell, warm tan fill |
| Hot drink | Cup silhouette, 2–3 curved steam lines rising from the top |
