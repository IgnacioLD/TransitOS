# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

TransitOS is an AGPL-3.0 Android app (Kotlin, Jetpack Compose, Material 3) for public transport, currently Metrovalencia (FGV) only, designed to become multi-operator. The authoritative design doc is `docs/architecture.md`.

## Commands

JDK 17+, Gradle 9.8 wrapper, AGP 9.4 (built-in Kotlin), compileSdk 37 / targetSdk 36 / minSdk 26. All dependencies and plugins live in `gradle/libs.versions.toml`; shared build config (SDK levels, JVM target, Compose, quality tooling) lives in the `build-logic` convention plugins (`transitos.android.feature|library|compose|application`, `transitos.jvm.library`).

```bash
./gradlew :app:assembleDebug                 # debug APK -> app/build/outputs/apk/debug/
./gradlew spotlessApply                      # auto-format (ktlint + Compose rules)
./gradlew spotlessCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug   # exactly what CI runs
./gradlew :provider-server:test              # one module's unit tests
./gradlew :core-ui:testDebugUnitTest --tests "*.RelativeTimeTest"   # single test class (Android modules)
./gradlew :core:test --tests "*.SomeTest.someCase"                  # :core is pure JVM, so the task is plain `test`
./gradlew :app:bundleRelease                 # signed AAB, needs app/keystore.properties (see app/keystore.properties.example)
```

The **server** (`server/`) is a separate Gradle build, not part of the Android
build. Run its checks from inside `server/`: `./gradlew spotlessCheck detekt test
buildFatJar`.

CI (`.github/workflows/android.yml`) fails on any Spotless, detekt (`config/detekt/detekt.yml`) or Android Lint finding (warnings are errors, missing translations included). Pre-existing findings sit in per-module `detekt-baseline.xml` / `lint-baseline.xml`; fix rather than re-baseline. `dco.yml` requires a `Signed-off-by` line on every commit, so commit with `git commit -s`. Tests use JUnit4, Truth, Turbine, coroutines-test and Ktor `MockEngine`.

## Architecture

Strict one-way layering: presentation (`:app`, `:feature-*`, `:core-ui`, `:core-design`) -> domain (`:core`) <- data (`:provider-*`, `:core-network`, `:core-data`). Key points that span multiple files:

- **`:core` is pure Kotlin/JVM** (no Android) with `-Xexplicit-api=warning`, so declare visibility explicitly there. It holds the universal models (`Stop` is the single boarding-point concept, grouped via `parentStationId`; no `Station` type), repository contracts, and `AppResult`/`AppError` (the only failure surface; providers map exceptions into `AppError`, the UI never sees exceptions).
- **`TransitRepository`** (`core/.../repository/TransitRepository.kt`) is the one contract features use. Streams emit `AppResult` (`Loading`, then `Success`/`Failure`); a `Failure` is only emitted before the first data, afterwards the last good value is kept while the provider retries with backoff (and immediately when `NetworkMonitor` reports connectivity back). Every network call goes through `safeCall`/`requireSuccess` in `:core-network`, the single place exceptions become `AppError`; never `runCatching` around suspend calls (it swallows cancellation).
- **Providers**: operators are aggregated by the **self-hosted server** (`server/`, Kotlin/Ktor). It fetches from FGV/EMT/Renfe, parses their formats, caches politely (TTL cache, one upstream request per interval) and exposes one unified REST API whose JSON is in the `:core` model shapes. The app has a single thin provider, `:provider-server`, that implements `TransitRepository` against that API and registers a `ProviderInfo` into the global `ProviderRegistry` (so Settings discovers it). Adding an operator is a server-side change only. See `docs/multi-operator.md`.
- **FGV quirks + server**: read `server/RESEARCH-metrovalencia.md` before touching the FGV code, which now lives under `server/.../provider/metrovalencia/`. Live arrivals (`horarios-prevision-3`) need session cookies from a prior catalogue call; the server's upstream client keeps a cookie jar (`server/.../net/UpstreamHttpClientFactory.kt`) and installs the session once, server-side. The journey planner endpoint is flaky.
- The app's `:provider-server` base URL comes from `BuildConfig.SERVER_BASE_URL` (see `app/build.gradle.kts`; override with `-Ptransitos.serverBaseUrl=...`).
- **DI is Koin** (no Hilt, no KSP). Each module exposes `di/<Name>Module.kt`; every module is listed in `startKoin` in `app/.../TransitOSApplication.kt`. A new feature or provider must be added to `settings.gradle.kts`, `app/build.gradle.kts` dependencies, and that `modules(...)` list; feature routes go in `app/.../TransitOSApp.kt`.
- **UI state**: each feature has `XUiState` (sealed/immutable), `XViewModel` exposing `StateFlow`, and `XScreen` collecting with `collectAsStateWithLifecycle`. The network map lives in `:app` (`map/`, osmdroid on OSM tiles).
- **Persistence**: `:core-data` implements the preference/favorites contracts from `:core` with a single Preferences DataStore (`TransitosDataStore.kt`). Language is the exception: `AppLocaleLanguagePreference` reads and writes the system per-app locale (AppCompat), the single source of truth for the UI language.

## Conventions and gotchas

- Packages, namespaces and source directories are all `com.glossostudio.transitos.<module-suffix>`.
- **Strings**: never hardcode user-facing text; use `stringResource`. Each module has its own `res/`. The default `values/` is **Spanish**, with `values-en/` (English) and `values-ca/` (Valencian). Add every new string to all three.
- Branch prefixes: `feat/`, `fix/`, `docs/`, `i18n/`, `chore/`, `refactor/`. PRs target `master`.

## Store assets (`art/`)

All Play Store graphics, screenshots, listing text and the Android launcher/splash/brand icons are generated; see `art/README.md`. Edit sources (`art/i18n/<locale>.json`, `art/templates/*.svg`, `art/icon-transitos.svg`), never the generated `.txt`/`.csv`/PNGs or mipmap/drawable icon PNGs. Requires `rsvg-convert` and ImageMagick.

```bash
python3 art/capture-screenshots.py   # needs emulators (transitos_phone/_tab7/_tab10 AVDs) + adb
python3 art/generate.py              # or --graphics / --screenshots / --android-icons
python3 art/generate.py --check      # validate sizes and Play text limits
```
