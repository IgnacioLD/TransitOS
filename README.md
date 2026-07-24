# TransitOS

> The open-source public transport app — fast, calm, private. The Breezy Weather of transit.

TransitOS is an open-source Android client for public transport, starting with **Metrovalencia** and designed to grow into a multi-operator platform (EMT Valencia, Renfe Cercanías, and beyond). No ads. No tracking. Just the next arrivals.

## Status

Very early. The project skeleton is in place — module graph, theme, navigation and a runnable debug APK. The first provider (`:provider-metrovalencia`) is a stub pending API investigation.

## Platform

- Android, Kotlin, Jetpack Compose, Material 3
- minSdk 26 / target/compile 34
- Kotlin DSL + Gradle Version Catalog

## Architecture

Clean layering with strict dependency direction. See [`ARCHITECTURE.md`](./ARCHITECTURE.md).

```
Presentation (app, feature-*)
        ↓
Domain (core)  — models, repository contracts
        ↓
Data (provider-*, core-network)
```

## Module map

| Module                  | Layer        | Responsibility                                              |
| ----------------------- | ------------ | ----------------------------------------------------------- |
| `:app`                  | Presentation | Application, DI wiring, navigation, bottom bar              |
| `:core`                 | Domain       | Universal models, `TransitRepository` contract, `AppResult` |
| `:core-network`         | Data         | Ktor `HttpClient` factory, Koin network module             |
| `:core-design`          | Presentation | Material 3 theme, colour, typography, shapes                |
| `:core-ui`              | Presentation | Shared composables (`LoadingState`, `ErrorState`)           |
| `:feature-home`         | Presentation | Home / favourites screen + ViewModel                        |
| `:provider-metrovalencia` | Data       | `TransitRepository` implementation for Metrovalencia        |

Adding a new operator (e.g. EMT) means adding one `:provider-emt` module and reconciling the `TransitRepository` binding — nothing else in the app changes.

## Build

Requires JDK 17 and Android SDK 34.

```bash
./gradlew :app:assembleDebug
```

## Licence

Copyright © 2025 the TransitOS contributors.

Released under the [GNU AGPL-3.0](./LICENSE). This is a deliberate choice: any improved version of TransitOS, even one offered as a hosted service, must publish its source under the same terms.
