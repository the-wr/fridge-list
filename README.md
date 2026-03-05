# FridgeList

An Android tablet app designed to live on your fridge as a permanent, always-on shopping list.

## What it does

FridgeList shows a full-screen grid of grocery item tiles. Tap a tile once to mark an item as needed (full color); tap again to clear it (grayed out). That's the entire interaction — no typing, no navigation, no friction.

All state is stored in a cloud todo provider (Todoist, Microsoft To Do, etc.), so the list is automatically shared and accessible from your phone or any other device.

## Key features

- **One-tap toggle** — items flip between needed and not-needed with a single tap
- **Icon-only grid** — 500 bundled flat clipart icons, readable at a glance from across the room
- **No local database** — all data lives in your connected todo provider
- **Always-on display** — screen stays lit; no screensaver or dim mode
- **Shared list** — changes sync across all your devices via the backend provider
- **Edit mode** — long-press anywhere to rearrange tiles, add new ones, or remove them

## Supported providers

| Provider | Status |
|---|---|
| Todoist | Active |
| Microsoft To Do | Active |
| Google Tasks | Planned |
| TickTick | Planned |

## Setup

On first launch, a wizard walks you through:

1. Choose a todo provider
2. Authenticate via OAuth
3. Select or create a list
4. Set grid dimensions (up to 10 columns × 15 rows)
5. Populate the grid (empty, default set, or import from existing list)

After setup, the main screen is shown and the app runs as a kiosk-style display.

## Technical stack

- Kotlin + Jetpack Compose
- Hilt (dependency injection)
- Room (tile/grid configuration), DataStore (settings), EncryptedSharedPreferences (OAuth tokens)
- Retrofit + Moshi (API networking)
- AppAuth (OAuth 2.0)
- Minimum Android version: API 26 (Android 8.0 Oreo)

## Documentation

- [DESIGN.md](DESIGN.md) — full design specification
- [ICONS.md](ICONS.md) — icon set catalog
- [ICON_STYLE.md](ICON_STYLE.md) — icon visual style guide
- [DEFAULT_GRID.md](DEFAULT_GRID.md) — default grid layout
