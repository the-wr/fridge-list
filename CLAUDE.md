# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (requires signing config in local.properties)
./gradlew bundleRelease          # AAB for Play Store → app/build/outputs/bundle/release/app-release.aab
./gradlew lint                   # Android Lint
./gradlew test                   # Unit tests
```

Release signing reads `keystore.path`, `keystore.password`, `key.alias`, `key.password` from `local.properties`.

## Architecture

**Single-module MVVM + Jetpack Compose** app targeting Android tablets as always-on refrigerator shopping lists.

### Key layers

- **UI** (`ui/`) — Compose screens + ViewModels. `MainScreen` renders the tile grid; `SetupScreen` is a multi-step wizard (choose provider → OAuth → select list → populate grid).
- **Repository** (`data/repository/TileRepository.kt`) — business logic: sync, toggle, add/remove/move tiles. Uses optimistic updates: write to Room immediately, sync to provider in background, revert on failure.
- **Providers** (`provider/`) — `TodoProvider` interface with implementations for Todoist and Microsoft To Do. `ProviderFactory` returns the active implementation.
- **DI** (`di/AppModule.kt`) — Hilt module providing Room, OkHttp, Moshi, Retrofit instances for each provider API, and singleton provider classes.
- **Local storage** — Room DB (`tiles` table, `TileEntity`); DataStore for settings/auth state; `TokenStore` uses `EncryptedSharedPreferences` for OAuth tokens.

### Navigation

`MainActivity` hosts a `NavHost` with two destinations: `setup` (initial) and `main`. Setup completes by writing `setup_complete = true` to DataStore, then navigating to main.

### Provider abstraction

`TodoProvider` interface: `getLists`, `getTasks`, `createTask`, `completeTask`, `reopenTask`, `refreshToken`. Adding a new provider means implementing this interface and registering it in `ProviderFactory` and `AppModule`.

### Sync strategy

- Tile taps trigger optimistic local toggle → background provider call → revert on failure.
- Periodic sync every 10 minutes pulls provider state and reconciles with Room.
- `SyncResult` is a sealed class: `Success | AuthRequired | Failure(msg) | Offline`.
- On `AuthRequired`: attempt `provider.refreshToken()`, retry once, then surface auth error to UI.

### Data model

`Tile` has `gridRow`, `gridCol`, `iconName`, `taskName`, `taskId` (provider-side), `state` (NEEDED/NOT_NEEDED), and `isOffGrid` (staged but not placed).

## Important Notes

- `android.enableR8.fullMode=false` in `gradle.properties` is intentional — R8 full mode strips the `Signature` bytecode attribute from Retrofit interfaces, causing `ClassCastException` at runtime with suspend functions.
- ProGuard rules in `app/proguard-rules.pro` keep provider/data classes and Moshi `JsonAdapter`s; don't remove them.
- `versionCode` in `app/build.gradle.kts` must be incremented before each Play Store upload.
- The screen is kept always-on via `FLAG_KEEP_SCREEN_ON` in `MainActivity` — this is by design.
