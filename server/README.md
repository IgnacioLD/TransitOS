# transitos-server

The multi-operator aggregation server for TransitOS. It fetches from Valencia
transit operators (FGV/Metrovalencia, EMT, Renfe Cercanías), parses their
formats, caches politely, and exposes one unified REST API that the app consumes
through a single thin provider. See [`../docs/multi-operator.md`](../docs/multi-operator.md)
for the full plan.

This is a **standalone Gradle build**, deliberately decoupled from the Android
build at the repository root, so the JVM/Ktor build never drags in AGP. Run it
from inside `server/`.

## Status: Phase 0 (skeleton)

Only `/health` exists so far. Operator endpoints land in later phases.

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
