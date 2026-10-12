# TransitOS: Multi-operator plan

TransitOS ships today with a single operator, Metrovalencia (FGV), talking to
FGV directly from the device. This document is the source of truth for growing
into a multi-operator platform covering the rest of Valencia: **EMT** (urban
buses), **Renfe Cercanías** (suburban rail) and, later, **Metrobus**
(interurban buses).

It is a plan, not the current architecture. `architecture.md` describes what is
built; this file describes what we intend to build and why. When a phase lands,
move the relevant decisions back into `architecture.md`.

## Motivation

Three forces drive this work:

1. **More operators.** Riders do not care who runs the bus; the app should show
   Metrovalencia, EMT and Renfe together. The domain model was designed for this
   from the start (`Operator`, `TransportMode`, `Stop.operatorId`,
   `Line.operatorId`, `ProviderRegistry`).
2. **Politeness.** FGV's live-arrivals endpoint is undocumented and we already
   treat it carefully (low poll rates, aggressive caching, a real `User-Agent`,
   see `provider-metrovalencia/RESEARCH.md`). Reaching three operators from every
   install multiplies the load we place on public infrastructure. A single
   self-hosted server makes one request per interval no matter how many users.
3. **Secrets and reach.** The NAP (National Access Point) GTFS feed requires an
   `ApiKey`. Shipping a key inside an APK is not acceptable. A server holds the
   key and unlocks feeds the app cannot reach on its own.

Two smaller wins follow: GTFS/GTFS-RT parsing is heavy and belongs off the
device, and the existing FGV code (DTOs, mappers, the session-cookie dance) is
already Kotlin and moves to the server almost verbatim.

## Decision: a unified API server (option B), not a transparent proxy

We chose a **unified aggregation API**, not a transparent caching proxy. The
server fetches from every operator, parses their formats, and exposes one
schema of our own. The app keeps exactly one provider, a thin HTTP client
against that API, implementing the same `TransitRepository` contract the UI
already depends on.

```
        device                                  VPS
+---------------------+   HTTPS    +-----------------------------+
| :app / :feature-*   |            | transitos-server (Ktor)     |
|   TransitRepository |<---------->|   /stops /lines /arrivals   |
|        ^            |            |   /alerts /plan /geometries |
|        |            |            |        ^       ^       ^    |
| :provider-server ---+            |        |       |       |    |
+---------------------+            +--------+-------+-------+----+
                                            |       |       |
                                       +----v-+ +---v--+ +--v----+
                                       | FGV  | | EMT  | | Renfe |
                                       +------+ +------+ +-------+
```

Why option B over a proxy:

- The heavy, volatile part (GTFS CSV, GTFS-RT protobuf, FGV JSON quirks) lives in
  one place we control, not in every install.
- The `ApiKey` for NAP never leaves the server.
- Politeness is a server concern with one obvious implementation, not a property
  we hope every client honours.
- The `TransitRepository` contract already abstracts the operator away, so the
  app barely changes.

The cost is an upfront server and a single point of failure; both are addressed
under [Risks](#risks).

## Scope

**In scope**

- A self-hosted server (`server/`, Kotlin/Ktor) that aggregates operators and
  exposes a unified REST API.
- Migrating Metrovalencia (FGV) from the app to the server.
- Adding EMT (bus, GTFS static) and Renfe Cercanías (rail, GTFS static +
  GTFS-RT).
- A single thin app provider (`:provider-server`) replacing
  `:provider-metrovalencia`.

**Out of scope (for now)**

- Cross-operator journey planning (e.g. bus + metro in one trip). The FGV
  planner is proxied as-is; a multi-modal planner is a separate project.
- Valenbisi (bike share). Bicycles are not boarding points and do not fit the
  `Stop` model; it would be a distinct feature.
- Real-time data for operators that do not publish it.

## Target architecture

### Modules

| Module / path        | Build              | Responsibility                                     |
| -------------------- | ------------------ | -------------------------------------------------- |
| `server/`            | own Gradle build   | Ktor app: fetch, parse, cache, unify, serve        |
| `:provider-server`   | Android (app)      | `TransitRepository` over the unified API           |

The server has its own Gradle build so the JVM/Ktor build and the Android/AGP
build stay independent. It reuses the serializable domain models from `:core`,
which is already pure Kotlin/JVM (`:core` has no Android dependency). If `:core`
ever grows Android-specific concerns, extract a `:core-model` shared module then;
do not pre-emptively split it.

### Unified API

Canonical stop and line ids keep the `<operatorId>:<localId>` form the app
already uses (`mv:` for Metrovalencia, plus `emt:` and `renfe:`). The server is
free to choose the local id; the app treats ids as opaque strings.

| Endpoint                 | Returns                                          |
| ------------------------ | ------------------------------------------------ |
| `GET /health`            | liveness/readiness for the reverse proxy         |
| `GET /operators`         | available operators and their backends           |
| `GET /stops`             | all boarding points across operators             |
| `GET /lines`             | all commercial lines across operators            |
| `GET /geometries`        | line shapes for the network map                  |
| `GET /arrivals/{stopId}` | next arrivals for a stop                         |
| `GET /alerts`            | service alerts across operators                  |
| `GET /plan?from=&to=&date=&time=&mode=` | journey alternatives            |

Responses use the `:core` model JSON (`Stop`, `Line`, `Arrival`, `Alert`,
`Journey`, `LineGeometry`, `Operator`), so the app can decode straight into
domain types. Failures use the HTTP status codes `:core-network` already maps to
`AppError` (`safeCall`/`requireSuccess`).

### App side

`:provider-server` implements `TransitRepository` against the API, reusing the
`polling`/`StateFlow`/backoff pattern from the current
`MetrovalenciaRepository`. The UI, ViewModels and `TransitRepository` contract
are untouched.

An aggregating router in `:core` (the earlier "`AggregatingTransitRepository`"
idea) is **not** needed under option B and is dropped. The server is the
aggregator.

## Politeness design

Every rule lives on the server, once:

- **Cache by freshness.** TTLs tuned to how fast each datum actually changes:
  arrivals ~30 s, alerts ~5 min, catalogues ~6 h, static GTFS refreshed daily.
- **One upstream request per interval**, not one per user. The FGV session-cookie
  handshake runs once server-side and is reused across all clients.
- **Conditional requests** (`If-Modified-Since` / `ETag`) wherever an operator
  supports them, so unchanged payloads cost a `304`.
- **Outbound rate limiting and exponential backoff** per operator, so a bad
  operator is not hammered and cannot cascade into other feeds.
- **Honest `User-Agent`** identifying the project, matching what the app does
  today.
- **No personal-data logging.** Logs carry aggregate metrics only (request
  counts, latencies, error rates) - consistent with the project's *no tracking*
  goal. Never log user IPs against query content.

## Privacy

TransitOS' privacy note (`PRIVACY.md`) currently states that requests go
"directly from your device to FGV's servers" and that "no intermediate server is
used". Introducing our own server makes that false, so `PRIVACY.md` and
`README.md` must be updated in the same change that lands the server, not before.
The rewritten text must be honest: traffic passes through the project's server,
which keeps no personal data and does no tracking. The value proposition does
not change - it is a self-hosted, non-commercial intermediary, not an analytics
or ad layer.

## Deployment (VPS)

- **Docker**, multi-stage: Gradle build stage -> slim JVM runtime image.
- **Caddy** as reverse proxy for automatic TLS; the server itself stays plain
  HTTP behind it.
- **Configuration via environment variables** (NAP `ApiKey`, TTLs, upstream base
  URLs). Secrets live only in the environment, never in the repository.
- `/health` wired to the reverse proxy for restarts and monitoring.

## Phases

### Phase 0 - Server skeleton

Ktor app with `/health`, configuration plumbing and the unified-schema module,
plus the Docker build and Caddy config. Deploy a hello-world to the VPS.

*Done when*: the VPS serves a healthy `/health` over TLS from a containerised
build.

### Phase 1 - Migrate Metrovalencia to the server

Move the FGV layer (`MetrovalenciaApi`, its session handling, `dto/`,
`mapper/FgvMappers.kt`) into the server, expose `/stops`, `/lines`,
`/geometries`, `/arrivals`, `/alerts`, `/plan`, and replace the app's
`:provider-metrovalencia` with `:provider-server`.

*Done when*: the app shows Metrovalencia with parity to today (arrivals,
catalogues, planner, alerts, map geometry) sourced entirely from the server, and
direct FGV access is gone from the app.

### Phase 2 - EMT (urban bus)

Parse EMT's static GTFS on the server with a minimal hand-rolled reader
(`stops.txt`, `routes.txt`, `trips.txt`, `stop_times.txt`, `shapes.txt`).
Arrivals are scheduled (derived from `stop_times`), not real-time, unless EMT's
GTFS-RT turns out to be available via NAP - verify during the phase.

*Done when*: EMT stops and lines appear alongside Metrovalencia and scheduled
arrivals render for an EMT stop.

### Phase 3 - Renfe Cercanías (rail)

Static GTFS (same reader as Phase 2) plus GTFS-RT. GTFS-RT is protobuf, so this
phase adds the `protobuf-java` runtime and the official `transit_realtime.proto`
to the server, and filters the national feeds down to Valencia Cercanías.

*Done when*: Renfe stops, lines, real-time arrivals and alerts render.

### Phase 4 - Metrobus (interurban buses, optional)

GTFS static + GTFS-RT via NAP, same machinery as Phase 3.

## Risks

- **Single point of failure.** If the server is down, the app has no data. The
  network map already falls back to bundled offline geometry; treat graceful
  degradation as part of Phase 1. A direct-FGV fallback is possible but adds
  complexity - only add it if server outages prove common.
- **Privacy perception.** A server can read as "phoning home" to a privacy-first
  audience. Mitigate with honest docs, a self-hostable server, aggregate-only
  logging, and an AGPL release of the server itself.
- **Migration parity.** Metrovalencia is tested and working; moving it risks
  regressions. The existing mapper and session tests move to the server and must
  keep passing.
- **Planner scope creep.** The FGV planner is proxied unchanged; multi-modal
  planning is explicitly out of scope and should not leak into Phase 1.

## Follow-ups (documentation)

- `docs/architecture.md`: add `server/` and `:provider-server` to the module
  graph and the stack.
- `PRIVACY.md`, `README.md`: rewrite the "no intermediate server" claims in the
  same change that lands the server.
- Each provider gets its own `RESEARCH.md` (data source, licence, endpoint
  quirks), mirroring `provider-metrovalencia/RESEARCH.md`.
