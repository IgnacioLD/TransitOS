# Metrovalencia API, Research findings

Investigation of every data source available for Metrovalencia (FGV), carried
out before writing the provider. Findings dated **2026-07-23** and confirmed
both by live HTTP probes and by decompiling the official app
`es.fgv.metrovalencia` v1.18.0 (Dec 2025).

## TL;DR, what we use, and why (Option 3)

| Need                        | Source                                        | Auth       | Status       |
| --------------------------- | --------------------------------------------- | ---------- | ------------ |
| Stop catalogue              | FGV `/estaciones`                             | none       | ✅ working    |
| Line catalogue              | FGV `/lineas`                                 | none       | ✅ working    |
| Master data (offline dump)  | FGV `/sincronizacion`                         | none       | ✅ working    |
| **Live arrivals at a stop** | FGV `/horarios-prevision-3/{estacion_id_FGV}` | none       | ✅ working    |
| Line-status alerts          | FGV `/incidencias`                            | none       | ✅ working*   |
| Accessibility alerts        | FGV `/incidencias_accesibilidad`              | none       | ✅ working    |
| Journey planner             | FGV `/horarios-online2` (POST form)           | none       | 🟡 flaky     |
| Service alerts (rich)       | NAP GTFS-RT                                    | ApiKey     | ❌ not published |

*`/incidencias` returns only `{id, linea_id, fecha, sede, timestamps}`, line-status
flags with no message text. Sufficient for the MVP's "Estado de líneas" view
(green/red per line); insufficient for human-readable alert bodies.

**Decision (Option 3):** ship the provider against the FGV public REST API for
catalog and live arrivals, isolate it behind `TransitRepository` so it is
trivially swappable, document the legal ambiguity (see [Legal](#legal)) in the
README, and cache aggressively. NAP GTFS is reserved for a future enrichment
(shapes, transfers, scheduled times) when an ApiKey is in place. If FGV ever
objects, the provider can be replaced with an NAP-only implementation without
touching the rest of the app.

---

## 1. FGV official REST API

Base URL: `https://www.fgv.es/fgv/app/{lang}/api/v1/{sede}/`

- `{lang}` ∈ `es` | `en` | `ca`
- `{sede}` ∈ `V` (Valencia) | `A` (Alicante trams)
- No authentication. Only required header: `Accept: application/json`.
- Source of truth: Retrofit annotations recovered from the official APK's
  `com.fgv.transporte.app.*` classes (decompiled for interop only; not
  redistributed). Each Retrofit interface declares exactly this base URL via an
  environment enum (`PRO`/`PRE`/`PCI`).

> **URL history.** Until 2022 the base was `https://www.fgv.es/ap18/api/public/...`
> (the one referenced by `albertodiazsaez/fgv-reloaded`). It has since moved to
> `https://www.fgv.es/fgv/app/...`. The old URL still resolves for
> `/estaciones`, `/lineas`, `/sincronizacion` but **rejects** `/horarios-prevision-3`
> and `/incidencias`. Always use the new URL.

### Endpoints (public subset; authed ones not implemented)

| Method | Path                                  | Returns                                            |
| ------ | ------------------------------------- | -------------------------------------------------- |
| GET    | `estaciones`                          | `[FgvStation]` (~52 stations, 43 KB)              |
| GET    | `lineas`                              | `[FgvLine]` (~9 lines, 3 KB)                      |
| GET    | `sincronizacion`                      | Master dump (5+ MB: graphics, fonts, full data)   |
| GET    | `incidencias`                         | `{incidencias: [{id, linea_id, fecha, sede, ...}]}` |
| GET    | `incidencias_accesibilidad`           | Similar shape, accessibility-scoped                |
| GET    | `horarios-prevision-3/{estacion_id_FGV}` | `{previsiones: [{line, line_id, linea_id_interna, trains: [...]}], aforo_bloqueado, configuracion_aforo}` |
| POST   | `horarios-online2` (form-encoded)     | Journey planner. Fields: `estacion_origen_id`, `estacion_destino_id`, `fecha`. Uses **internal ids**. |
| POST   | `planificador-online2` (form-encoded) | Trip planner v2. Same fields + `hora_salida`, `hora_llegada`. |

Endpoints that require OAuth (`oauth/login`, `usuarios/*`, `tarjetas/*`,
`compra-venta/*`, `mensajes/*`, `reclamaciones/*`, `sugerencias/*`,
`users/estaciones/{id}/favorito`, `sincronizacion/{usuarioId}/...`) are
**out of scope**, they handle personal/financial data and are not needed for
the MVP.

### Station shape

```json
{
  "id": 226,                          // internal id, used by planners
  "estacion_id_FGV": 12,              // FGV's own id, used by horarios-prevision-3 and lineas.stops
  "nombre": "Benimaclet",
  "transbordo": 1,                    // interchange flag (0/1)
  "latitud": 39.4848518372,
  "longitud": -0.3623333275,
  "direccion": "C/ Emilio Baró, frente 11; CP 46020. Valencia.",
  "sede": "V",
  "created_at": "2017-11-06 10:55:57",
  "updated_at": "2026-01-25 03:00:05",
  "deleted_at": null
}
```

> **Two ids.** `horarios-prevision-3/{id}` and `lineas.stops` use
> `estacion_id_FGV` (e.g. `12` for Benimaclet). `horarios-online2` and
> `planificador-online2` use the internal `id` (e.g. `226`). The provider
> exposes `estacion_id_FGV` as the canonical `Stop.id` (because live arrivals
> need it) and stores the internal id as a separate field for future planner
> work.

### Line shape

```json
{
  "id": 42,
  "linea_id_FGV": 1,
  "nombre_corto": "L1",
  "nombre_largo": "L1",
  "tipo": "1",
  "color": "#FEC601",
  "forma_id": 2,
  "stops": "107,80,79,78,77,76,75,74,73,72,55,54,53,52,17,51,25,26,27,28,30,31,32,33,50,49,48,47,46,45,44,43,42,41,40,39,38,37,36,35",
  "sede": "V"
}
```

`stops` is a CSV of `estacion_id_FGV` values in line order.

### Live-arrivals shape (observed live)

```json
{
  "previsiones": [
    {
      "line": 3, "line_id": 3, "linea_id_interna": 44,
      "trains": [
        {
          "cabecera": false,
          "destino": "Aeroport",
          "latitude": null, "longitude": null, "meters": null,
          "seconds": 356,
          "vehicle": 3086,
          "capacity": 506,
          "line_id": 3
        }
      ]
    }
  ],
  "aforo_bloqueado": {"desde": null, "hasta": null},
  "configuracion_aforo": { "es": [...], "en": [...], "ca": [...] }
}
```

Maps cleanly onto our `Arrival` domain model:

| Arrival field      | From                           |
| ------------------ | ------------------------------ |
| `stopId`           | the requested `estacion_id_FGV`|
| `lineId`           | `previsiones[].line_id`        |
| `destination`      | `trains[].destino`             |
| `minutesAway`      | `trains[].seconds / 60` (int)  |
| `estimatedEpochMs` | `now + seconds * 1000`         |
| `vehicleId`        | `trains[].vehicle`             |
| `isRealTime`       | `true`                         |

`configuracion_aforo` and `aforo_bloqueado` describe station occupancy
thresholds, useful for a future "how full is this station" indicator, not for
the MVP.

### Latency and rate limits

No published limits. Empirically each call returns in 50-300 ms. We
**voluntarily** rate-limit at the provider level (poll arrivals at 30 s, refresh
catalogs at most hourly, cache in memory) to stay polite and to avoid tripping
any undocumented anti-abuse rules.

## 2. Spanish NAP (`nap.transportes.gob.es`)

The Ministry of Transport's National Access Point (powered by MIMTRANS), mandated
by EU ITS Directive 2010/40/EU. **Reserved for future use** under Option 3.

- Metrovalencia static GTFS: `https://nap.transportes.gob.es/api/Fichero/download/1168`
  (MobilityDatabase `mdb-2830`, active).
- **Authentication: required.** HTTP header `ApiKey: <key>`. Returns 401 with
  body `Api Key was not provided.` without it. Free registration at
  `https://nap.transportes.gob.es/`.
- **Licence: AGPL-compatible**, see [Legal](#legal).
- GTFS **static only**, no GTFS-Realtime for FGV.

## 3. Renfe Cercanías (future `:provider-renfe`)

Public, no auth. Static at `https://ssl.renfe.com/ftransit/Fichero_CER_FOMENTO/fomento_transit.zip`,
realtime at `https://gtfsrt.renfe.com/{alerts,trip_updates,vehicle_positions}.pb`.
National coverage, filter to Valencia Cercanías at the provider level.

## 4. EMT Valencia (future `:provider-emt-valencia`)

Public, no auth. Static GTFS at
`https://opendata.vlci.valencia.es/dataset/ab058cf8-ad3e-4d9c-ac89-0c6367ecf351/resource/c81b69e6-c082-44dc-acc6-66fc417b4e66/download/google_transit.zip`.
No realtime.

## Legal

**This is a map of the terrain, not legal advice.** FGV is a public body owned
by the Generalitat Valenciana; the data is transit information of clear public
interest. The legal frame that bears on reusing their endpoints:

- **EU PSI Directive 2003/98/EC (as amended by 2013/37/EU)**, transposed in
  Spain by **Ley 37/2007**, favours reuse of public-sector information.
- **EU ITS Directive 2010/40/EU**, actually obliges Member States to make
  travel data available through National Access Points, on open terms.
- **CJEU C-30/14, *PR Aviation v Ryanair***, a publicly-accessible website's
  data may be reused even against clickwrap terms, absent a sui generis
  database right or a contract actually agreed to. The user clicked nothing.
- **Código Penal art. 197 bis**, usually requires circumventing an effective
  access control. No auth, no bypass.

What's clearly **out of bounds**: any endpoint under `tarjetas-transporte`,
`usuarios/*`, `mensajes/*`, `compra-venta/*`, those expose personal/financial
data (card balances, accounts) and fall under GDPR. TransitOS does not call
them.

**Residual grey zone** for `horarios-prevision-3` specifically: it is an
undocumented endpoint built for the official app. Low probability of objection,
but if FGV ever sends a takedown we have no contract to lean on. Mitigations
baked into the project:

1. **Isolation.** The FGV client lives entirely behind
   `:provider-metrovalencia`'s `TransitRepository`. Replacing it with an
   NAP-only implementation touches one module.
2. **Politeness.** Aggressive caching, low poll rates, real `User-Agent`
   identifying the project.
3. **Transparency.** AGPL source. The README declares the data source and the
   fallback strategy.
4. **Attribution.** UI credits "Datos: Metrovalencia / FGV · Powered by MITRAMS"
   even when reading via the FGV endpoint, since the NAP licence asks for it
   regardless of channel.
5. **Reverse-engineering discipline.** The decompiled APK stays in
   `.research/` (gitignored). Only the publicly-observable endpoint URLs and
   field names, re-verified by direct HTTP probes, are referenced in source.

## Recommended implementation order (this milestone)

1. ✅ Research and legal review (this document).
2. ← **Now:** implement `:provider-metrovalencia` against FGV for
   `estaciones`, `lineas`, `horarios-prevision-3`, `incidencias`. Map to domain.
3. Wire `MetrovalenciaRepository` into Koin, replacing the stub.
4. Add mapper unit tests.
5. Future: NAP GTFS for shapes + scheduled times (needs ApiKey). Petition FGV /
   MITRAMS to publish GTFS-Realtime.
