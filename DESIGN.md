# FridgeList — Design Document

## Overview

FridgeList is an Android tablet app designed to be mounted on or near a refrigerator as a permanent, always-on shopping list interface. The core interaction model is a single tap: tap an item to mark it as needed, tap again to clear it. The app delegates all data storage and sync to an existing third-party todo list provider.

---

## Goals

- **Minimal friction**: adding an item to the shopping list should require exactly one tap, no typing, no navigation
- **Glanceable**: the current shopping list state should be readable from a distance at a glance
- **Stateless locally**: the app owns no database; all state lives in the connected todo provider
- **Shared list**: because the backend is a cloud todo provider, the list is automatically accessible on phones or other devices

## Non-Goals

- Managing the grocery item catalog from the main screen (configuration is a separate flow)
- Supporting arbitrary text-entry items from the fridge tablet
- Offline-first or local-only operation

---

## Main Screen

The main screen is the only screen the user sees during normal use. It displays a fixed grid of grocery item tiles.

### Tile States

| State | Appearance |
|---|---|
| **Needed** | Full color clipart icon, visually prominent |
| **Not needed** | Desaturated / pale icon, recedes visually |

- Tiles show **icons only** — no text labels
- A single tap toggles between these two states. The toggle is reflected immediately in the UI and synced to the backend provider in the background.
- Empty/unconfigured slots are blank and non-interactive

### Grid Layout

- Grid dimensions are configurable up to **10 columns × 15 rows** (150 tiles maximum)
- Tile size scales to fill the screen for the configured grid dimensions — larger grids mean smaller tiles
- No scrolling; all tiles are always visible on a single screen
- The grid is a hard cap on item count — no overflow, pagination, or secondary screens

### Always-On Behavior

- Screen stays on permanently (wake lock held)
- App runs in a kiosk-style mode: no status bar interaction required, minimal chrome
- No screensaver or dim mode; display stays fully lit at all times

---

## Icon Set

Tiles use a bundled set of premade clipart icons — no user photos, no emoji. The icon set is the sole visual vocabulary of the app.

### Specifications

- **Count**: 200 icons shipped with the app
- **Source**: AI-generated flat clipart
- **Format**: SVG (rendered as VectorDrawable on Android for sharp scaling at any tile size)
- **Style**: flat, simple, bold — readable when small and when desaturated; consistent style across the full set (line weight, perspective, color palette)

### Desaturation Behavior

- **Needed** state: icon rendered at full color
- **Not needed** state: icon converted to grayscale and reduced in opacity (e.g. 35% opacity), so the colored state is strongly distinguishable at a glance

---

## Backend Integration

The app has no local database. All shopping list state is stored as tasks in a single connected todo list provider.

### Mapping Model

- Each configured tile maps to a **task** in a single configured list/project in the provider
- **Needed** state = task is **incomplete / open**
- **Not needed** state = task is **complete**
- Toggling a tile completes or reopens the corresponding task via the provider's API
- Only one list is supported — no multi-list configuration

### Supported Providers (planned)

| Provider | API |
|---|---|
| Todoist | Todoist REST API |
| Microsoft To Do | Microsoft Graph API |
| Google Tasks | Google Tasks API |
| TickTick | TickTick REST API |

An abstraction layer isolates provider-specific logic so new providers can be added without touching the UI.

### Sync Strategy

- **On launch**: fetch current task states from the provider and render the grid
- **On tile tap**: optimistic UI update immediately, then API call in background
- **Periodic sync**: poll the provider every **10 minutes** to catch changes made elsewhere (e.g. items checked off on a phone)
- **Conflict resolution**: last write wins — whichever change reaches the provider last is authoritative
- **On API failure after tap**: revert the optimistic UI update and show an inline error

### Offline Behavior

- If the device has no connectivity, show a clear error state (e.g. a banner or overlay)
- Do not queue or retry changes made while offline — the user must retry manually once connectivity is restored
- The grid remains visible but interactions are disabled while offline

---

## Setup Flow

### First-Run Wizard

Shown once on first launch. Steps run in sequence:

1. **Choose provider** — select Microsoft To Do, Todoist, Google Tasks, etc.
2. **Authenticate** — OAuth flow for the chosen provider
3. **Select or create a list** — shows all existing lists from the provider; user picks one or creates a new one with a custom name. Existing tasks in the chosen list are left untouched; those that map to configured tiles are linked, others are ignored.
4. **Set grid dimensions** — pick number of rows and columns independently (up to 10 columns × 15 rows). Can be changed later.
5. **Initial grid population** — choose one of three options:
   - **Empty grid** — start with all slots blank; fill manually via Edit Mode
   - **Default grid** — populate with a hardcoded curated set of common grocery tiles, laid out in a sensible default order
   - **Import from list** — scan the existing tasks in the chosen list and automatically place tiles for any task whose name matches a default icon name (case-insensitive); unmatched tasks are ignored; remaining slots are left empty

After step 5 the wizard exits and the main screen is shown. The user can enter Edit Mode at any time to adjust.

### Reconfiguration

- **Provider or list change**: re-enter from settings. Tile configuration (icons, task names, positions) is preserved. The app attempts to match each tile's task name to an existing task in the new list; unmatched tiles get new tasks created automatically.
- **Grid dimensions**: can be changed at any time. Shrinking the grid moves tiles that fall outside the new bounds into a hidden **off-grid store** — they are not deleted. If the grid is later enlarged, off-grid tiles are restored to their original positions (where available). A confirmation prompt is shown before shrinking if tiles would be displaced.
- **Tile edits, additions, removals**: done via Edit Mode directly from the main screen at any time.

---

## Edit Mode

Edit mode is the primary interface for configuring the tile grid. It is designed for quick entry and exit — more like a launcher home screen than a separate settings screen.

### Entering and Exiting

- **Enter**: long press anywhere on the main screen (on a tile or on an empty slot)
- **Exit**: tap a "Done" button (top corner), or long press again, or wait for a short idle timeout
- Edit mode is available at any time, not just during first-run setup

### In Edit Mode

The grid enters an editable state (similar to Android home screen icon editing):

- **Drag tiles** to rearrange them within the grid
- **Tap a configured tile** to open the tile editor (change icon, edit task name, or remove tile)
- **Tap an empty slot** to open the icon picker and add a new tile there
- A **settings button** (e.g. top bar) gives access to grid dimensions and provider/list configuration

### Icon Picker

Accessed when adding or changing a tile's icon. The picker supports two navigation modes:

- **Search**: type a query (e.g. "mil") to filter icons by name across the full set
- **Browse**: hierarchical category view — browse by category, then select an icon within it

Each icon has a **predefined name** (e.g. the milk icon defaults to "Milk"). This name becomes the task name in the todo provider. The user can edit it freely before confirming (e.g. rename to "Oat Milk" or "2% Milk").

### Tile Editor

Shown when tapping an existing tile in edit mode:

- **Change icon** — reopens the icon picker
- **Edit task name** — inline text field, pre-filled with current name
- **Remove tile** — clears the slot (with a brief confirmation); the corresponding task in the provider is **not** deleted

---

## Technical Stack (Proposed)

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with a repository layer per provider
- **Auth**: OAuth 2.0 via AppAuth for Android
- **Networking**: Retrofit + OkHttp
- **State management**: StateFlow / ViewModel

---

## Decisions Log

| # | Question | Decision |
|---|---|---|
| 1 | Conflict resolution | Last write wins |
| 2 | Offline behavior | Show error, no queueing |
| 3 | Custom icons | 200 AI-generated flat SVG clipart icons, bundled with the app. No photos, no emoji. Icon categories defined separately. |
| 4 | Multi-list support | Single list only |
| 5 | Tile overflow | Hard cap at grid size, no scroll or pagination |
| 6 | Settings access control | No PIN required |
