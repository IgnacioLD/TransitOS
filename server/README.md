# transitos-server

The multi-operator aggregation server for TransitOS. It fetches from Valencia
transit operators (FGV/Metrovalencia, EMT, Renfe Cercanías), parses their
formats, caches politely, and exposes one unified REST API that the app consumes
through a single thin provider. See [`../docs/multi-operator.md`](../docs/multi-operator.md)
for the full plan.

This is a **standalone Gradle build**, deliberately decoupled from the Android
build at the repository root, so the JVM/Ktor build never drags in AGP. Run it
from inside `server/`.

## Status: Phase 1 (Metrovalencia)

Metrovalencia (FGV) is served end to end. EMT and Renfe land in later phases.

| Endpoint                 | Returns                                      |
| ------------------------ | -------------------------------------------- |
| `GET /health`            | liveness/readiness                           |
| `GET /operators`         | operators and their backends                 |
| `GET /stops`             | boarding points (`mv:<id>` ids)              |
| `GET /lines`             | commercial lines                             |
| `GET /geometries`        | line shapes for the map                      |
| `GET /arrivals/{stopId}` | next arrivals for a stop                     |
| `GET /alerts`            | service alerts                               |
| `GET /plan`              | journey alternatives (`from`, `to`, `date`, optional `time`, `arriveBy`, `minTransferMinutes`) |

Every response is JSON in the app's `:core` model shapes. Upstream failures are
surfaced as HTTP status codes (`404`/`502`) with an `{"error": "..."}` body; the
server never leaks stack traces.

## Local development

```bash
cd server
./gradlew test          # unit tests (currently the /health route)
./gradlew run           # start on http://localhost:8080
curl http://localhost:8080/health
# {"status":"ok","version":"0.1.0"}
```

Configuration is environment-only (no rebuilds to change it):

| Variable           | Default   | Purpose                       |
| ------------------ | --------- | ----------------------------- |
| `TRANSITOS_HOST`   | `0.0.0.0` | Bind address                  |
| `TRANSITOS_PORT`   | `8080`    | Listen port                   |

Politeness is built in: catalogues are cached for hours, arrivals for seconds
and alerts for minutes, so one server instance makes at most one upstream request
per interval regardless of how many app clients connect.

## Quality

```bash
cd server
./gradlew spotlessApply        # format (ktlint)
./gradlew spotlessCheck detekt # what CI will run
```

Formatting and analysis reuse the repository's shared `.editorconfig` and
`config/detekt/detekt.yml`.

## Docker

Build and run the container locally:

```bash
cd server
docker build -t transitos-server .
docker run --rm -p 8080:8080 transitos-server
```

## Deployment (VPS)

`docker-compose.yml` runs the app behind **Caddy**, which terminates TLS with
automatic certificates.

1. Copy `env.example` to `.env` and set `TRANSITOS_DOMAIN` to the public
   hostname (point its DNS A/AAAA record at the VPS first).
2. `docker compose up -d --build`.
3. Verify: `curl https://<TRANSITOS_DOMAIN>/health`.

Secrets (e.g. the NAP `ApiKey`, later phases) go in `.env` only and are never
committed; `.env` is gitignored.

## Privacy

The server keeps **no personal data** and does **no request/access logging**:
logs carry only lifecycle and error messages. This is what lets the app keep its
*no tracking* promise while routing traffic through a project-owned server.
