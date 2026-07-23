# TransitOS — Architecture

This document captures the layering, module graph and the foundational decisions
that govern the codebase. New code must respect what's written here; changes to
the architecture should be proposed, discussed and then reflected back into this
file.

## Guiding principles

1. **Strict layer boundaries.** Dependencies point inward (presentation →
   domain → data). The UI never touches the network. Network DTOs never escape
   the data layer.
2. **One transit contract.** The rest of the app depends on
   `TransitRepository`; every operator implements it against its own data source.
3. **Universal domain models.** `Stop`, `Line`, `Arrival`, `Alert`, `Operator` —
   modelled once, reused by every provider. Provider-specific concepts stay
   inside provider modules.
4. **Immutability and Flow.** UI state is immutable; repositories expose cold
   `Flow`s so the UI observes continuous updates (essential for live arrivals).
5. **Composition over inheritance.** Small composables; one responsibility each.
6. **Accessibility is not optional.** Every screen is navigable with TalkBack,
   respects dynamic font sizes and stays usable at high contrast.

## Layered architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Presentation                                                │
│   :app · :feature-* · :core-ui · :core-design               │
│   Compose UI, ViewModels, navigation, theme                 │
└──────────────────────────┬──────────────────────────────────┘
                           │ depends on
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Domain                                                      │
│   :core                                                     │
│   Pure Kotlin. Models, repository contracts, AppResult      │
└──────────────────────────┬──────────────────────────────────┘
                           │ implemented by
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Data                                                        │
│   :provider-metrovalencia · :core-network                   │
│   Repository impls, Ktor HttpClient, network DTOs + mappers │
└─────────────────────────────────────────────────────────────┘
```

Rules:

- The UI **never** talks directly to networking.
- The UI **never** contains business logic.
- Business logic lives in the domain layer (use cases) or in the repository
  implementations.
- Network implementations stay inside providers.
- Network DTOs are **never** exposed outside the data layer. They are always
  mapped into domain models.

## Module graph

```
:app
 ├── :core
 ├── :core-network
 ├── :core-design
 ├── :core-ui        → :core, :core-design
 ├── :feature-home   → :core, :core-design, :core-ui
 └── :provider-metrovalencia → :core, :core-network

:core               (pure Kotlin/JVM — no Android)
:core-network       (Android library — Ktor)
:core-design        (Android library — Compose theme)
```

Adding a feature: create `:feature-<name>` depending on `:core`, `:core-design`,
`:core-ui`. Wire its Koin module in `:app`.

Adding an operator: create `:provider-<operator>` depending on `:core` and
`:core-network`, implement `TransitRepository`, expose a Koin module, and
register it in `:app`.

## State management

Single source of truth. The ViewModel owns a `StateFlow<UiState>`; the screen
observes it via `collectAsStateWithLifecycle` and renders by switching on the
sealed `UiState`. User actions become events handed back to the ViewModel. The UI
never mutates state directly.

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(...)  : HomeUiState
    data class Error(...)  : HomeUiState
}
```

## Error handling

`AppResult<T>` and `AppError` (both in `:core`) are the single failure surface.
Network layers map their exceptions into `AppError` variants; the UI switches on
them to produce localised, actionable messages. The app never crashes on a
network failure.

```
AppError: Offline | Network | Timeout | Unauthorized |
           Server(code) | NotFound | Parsing | Unknown
```

## Dependency injection

Koin — chosen over Hilt deliberately:

- Pure Kotlin DSL; no annotation processing, no KSP, faster builds.
- No reflection at runtime.
- Trivially removable if a future fork prefers another DI.
- Each module exposes its own Koin `module { ... }`; the application aggregates
  them in `TransitOSApplication`.

## Stack

- **Kotlin 2.0.21**, Coroutines, Flow
- **Jetpack Compose** (BOM-managed), Material 3, dynamic colour
- **Ktor 3** — HTTP client (one shared `HttpClient`, providers plug in their own
  endpoints)
- **Koin 4** — dependency injection
- **Room / DataStore / Coil** — declared in the version catalog, wired when the
  first feature that needs them lands (favourites cache, preferences, map tiles)
- **Gradle Version Catalog** (`gradle/libs.versions.toml`) — single source for
  every dependency and plugin

## Conventions

- Package root: `app.transitos.<module-suffix>` (e.g. `app.transitos.feature.home`).
- Public API in `:core` is explicit (`-Xexplicit-api=warning`) — visibility is
  always stated, never implied.
- Composables: `Screen` → `Content` → `Component` → `Primitive`. Keep them small
  and stateless; hoist state to the route-level composable or ViewModel.
- Module build files: Kotlin DSL, no `buildSrc` conventions yet — we will extract
  them once duplication across modules justifies it.

## Decisions worth remembering

- **Koin, not Hilt.** Simplicity and Kotlin idioms win.
- **Flat module layout** (`:core`, `:core-network`, `:feature-*`,
  `:provider-*`), following the Breezy Weather convention. Easier to reason about
  than nested `:core:domain` / `:core:data`.
- **`Stop` is the universal boarding-point concept.** A metro platform and a bus
  pole are the same thing from the app's perspective. A separate `Station` type
  is deliberately avoided; group facilities with `Stop.parentStationId`.
- **AGPL-3.0.** Strong copyleft — improvements must stay open, including when
  offered as a service.
