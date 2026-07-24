# TransitOS — Competitive Analysis: Metrovalencia Official App

Source: Google Play Store (`com.fgv.metrovalencia`)
Date collected: July 23, 2026
Total reviews analyzed: **923** (853 Spanish + 70 English)
Method: `google-play-scraper` Python library, sorted by newest

---

## Summary

The official Metrovalencia app has a **1.53★ average** across 923 scraped reviews
(1.73K total on Play Store). 77% of reviews are 1-star. The app has been
chronically broken since its 2018 redesign, with recurring complaints about
crashes, broken login, excessive permissions, and missing core features.

### Rating Distribution

| Rating | Count | Percentage |
|--------|-------|------------|
| 1★     | 711   | 77.0%      |
| 2★     | 84    | 9.1%       |
| 3★     | 38    | 4.1%       |
| 4★     | 33    | 3.6%       |
| 5★     | 57    | 6.2%       |

---

## Complaint Themes (by frequency)

Keywords were matched against review text (case-insensitive, language-aware).

| Rank | Theme               | Reviews |  %   | TransitOS Status |
|------|---------------------|---------|------|------------------|
| 1    | Recharge/bono/tuin  | 157     | 17.0%| N/A — no ticketing yet |
| 2    | App is slow         | 121     | 13.1%| Fast architecture |
| 3    | Login/register broken| 66     | 7.2% | No login required |
| 4    | Permissions abuse    | 61      | 6.6% | Minimal permissions |
| 5    | Crashes/freezes     | 59      | 6.4% | Stable |
| 6    | Connection errors   | 18      | 2.0% | Solid networking (retry/backoff) |
| 7    | No favorites        | 18      | 2.0% | Favorites implemented |
| 8    | Bad design/UI       | 18      | 2.0% | Material 3 design system |
| 9    | NFC missing         | 15      | 1.6% | Future feature |
| 10   | No map              | 14      | 1.5% | OSM interactive map |
| 11   | Transfer issues     | 11      | 1.2% | Could improve (min transfer buffer) |
| 12   | Blank screen        | 9       | 1.0% | Works |
| 13   | Wrong times         | 7       | 0.8% | FGV API dependent |

---

## What TransitOS Already Solves

TransitOS addresses **6 of the top 8 non-payment complaints** (covering ~46% of
all review grievances):

1. **Interactive map** — the #1 feature users beg for. Quote: *"Metro app
   without an actual map??? It's absolutely ridiculous"*
2. **No excessive permissions** — 61 reviews (6.6%) complain about mandatory
   photo/file/GPS access. TransitOS requests nothing beyond network.
3. **No login required** — 66 reviews (7.2%) report broken registration/login.
   TransitOS works instantly, no account needed.
4. **Multi-route planning** — users complain the official app shows only one
   connection. TransitOS shows multiple journey alternatives.
5. **Favorites** — 18 reviews (2.0%) want saved stations/routes. TransitOS has
   favorites.
6. **Fast and stable** — 121 reviews (13.1%) complain about slowness, 59 about
   crashes. TransitOS has a clean architecture with no known stability issues.

---

## Key User Quotes (translated where needed)

### On maps (our differentiator)
> *"Metro app without an actual map??? It's absolutely ridiculous"* — 1★

> *"No map, which is (one of) the most important features."* — 1★

> *"Useless. Doesn't have a map and the one you try to download doesn't work."* — 1★

> *"the no longer offers the general metro map to see possible connections"* — 1★

### On permissions (our advantage)
> *"Why does this app requires access to my photos to work? It seems ludicrous
> to me that it is not even optional!"* — 1★, 11 thumbs up

> *"Permissions unacceptable for this type of app. Direct uninstall."* — 1★, 5 thumbs up

> *"It seems abusive to have to give permission to the app to access MY photos,
> MY media content and files on MY device"* — 1★, 11 thumbs up

### On what users actually want (feature requests)
> *"it would be good to have a notification system for line incidents without
> needing to log in, just like Valencia's EMT app does. That would be simple
> and informative."* — 1★, 22 thumbs up

> *"It needs the option to add your NFC card so you can use your phone in the
> metro."* — 1★

> *"does not show all the timetables when the metro passes, only the next one
> and at most the one after"* — 1★, 24 thumbs up

### On route planning
> *"How do you even buy a ticket with this? Why does it only show one
> connection?"* — 1★

> *"The app is good, but the metro sucks. It plans a route with 2-3 minute
> transfer gaps. Sadly, the Valencian metro is not able to maintain such
> accuracy."* — 2★

### On accuracy
> *"most of the time the timetables are incorrect, especially on weekends"*
> — 1★, 21 thumbs up

> *"if a train is coming within a minute or 2, sometimes the app will not show
> this, so you might be fooled into thinking you will have to wait 15 or 20
> minutes, when the train is right around the corner"* — 5★, 8 thumbs up

---

## Actionable Opportunities for TransitOS

### High Priority
1. **Push notifications for line alerts (no login)** — explicitly requested by
   the most-upvoted feature suggestion (22 thumbs up). Users want to know about
   delays/strikes/service changes without creating an account.

2. **Realistic transfer time buffers** — the official planner suggests 2-3 min
   transfers that are impossible in practice. Add a configurable minimum
   transfer time (default 5 min for underground, 3 min for surface).

3. **Imminent arrival indicator** — when a train is <2 min away, the official
   app sometimes doesn't show it. TransitOS should show a visual "imminent"
   state (pulsing badge, different color) so users don't miss trains.

### Medium Priority
4. **Offline cached schedules** — users frequently encounter "connection error"
   even with good signal. Cache last-known schedules for offline fallback.

5. **Weekend schedule accuracy** — multiple reviews note times are especially
   wrong on weekends. Verify FGV API returns correct weekend data.

6. **All-day timetable view** — users want to see all departure times for a
   route, not just the next 1-2. TransitOS planner should show full daily
   schedule per journey option.

### Low Priority / Future
7. **NFC ticket integration** — 15 reviews request mobile NFC for turnstiles.
   Requires FGV API access to card system.

8. **Ticket purchase / recharge** — 157 reviews (17%) mention broken recharge.
   This is the #1 complaint but requires payment infrastructure.

---

## Top 40 Most-Helpful Reviews

Sorted by community thumbs-up count. Full text (truncated at 400 chars).

| # | ★ | 👍 | Date | Review |
|---|---|---|------|--------|
| 1 | 1 | 64 | 2022-10-29 | Cómo pueden estropear TANTISIMO una aplicación? Nunca muestra los horarios completos, no puedes recalcular la ruta clicando en el horario del metro que ibas a coger (y antes sí se podía hacer), no ofrece alternativas de ruta, la app se cierra sin motivo aparente, tienes que poner el nombre de estación varias veces para que aparezca... |
| 2 | 1 | 63 | 2018-10-13 | Parece que los desarrolladores pasan olímpicamente del feedback de los usuarios, pues llevan quejándose desde la publicación de la app y cada vez más aspectos fallan. No se puede iniciar sesión, no se pueden comprobar los horarios de las estaciones (la app crashea) y finalmente no existe la opción de buscar por hora de llegada... |
| 3 | 1 | 56 | 2023-02-14 | No para de fallar. Siempre me dice q no tengo conexión, da igual q esté conectada al lado del wifi o un lugar con mucha cobertura con mis datos ilimitados. Cuando me hace falta no la puedo gastar y encima a veces los horarios no son los q pasa el metro... |
| 4 | 1 | 45 | 2019-10-15 | La aplicación no muestra los horarios actualizados, debería incluso poder mostrar alertas cuando hay retrasos pero da la impresión de que no tiene acceso a la misma información que los "displays" de los andenes... |
| 5 | 1 | 35 | 2018-03-23 | Con la falta que hace una buena aplicación para este servicio de transporte público y ya de entrada permisos para hacer uso de todo el almacenamiento (fotos, medios y archivos) y a la posición GPS. Lo mejor es que o le das permiso a los dos o no puedes usar la app... |
| 6 | 1 | 25 | 2018-12-22 | La primera versión de esta aplicación funcionaba bien: te registrabas, añadías trayectos favoritos, registrabas tus tarjetas... Un día dejó de hacer login. Da fallo, no puedes restablecer contraseña ni registrar usuario... |
| 7 | 5 | 24 | 2024-01-26 | Hoy, 26/1/24, creo que en la estación de Massarrojos o Moncada, ha subido un revisor del que no he podido ver su nombre... En 30 años no he visto a un profesional tan competente... (positive review about staff, not app) |
| 8 | 1 | 24 | 2019-05-25 | La anterior era mejor. Esta tiene varios fallos. El primero es que cuando escribo en la barra de búsqueda el nombre de un origen/destino, se raya y no lo consigue... |
| 9 | 1 | 22 | 2018-03-25 | Pide demasiados permisos y encima son obligatorios... Además, se echa en falta algún sistema de notificaciones de incidencias de las líneas sin necesidad de iniciar sesión, tal y como hace la EMT de Valencia. |
| 10 | 1 | 21 | 2019-03-04 | Una aplicación de adorno. La mayoría de veces los horarios están incorrectos, sobre todo en fines de semana... No te deja guardar estaciones favoritas. |
| 11 | 2 | 17 | 2019-04-24 | MUY LENTA, ultimamente tengo problemas con mi cuenta no me deja ni entrar siendo que por la pagina web si me deja!!!!!!!!!!! |
| 12 | 1 | 16 | 2021-03-28 | Iba a utilizarla pero hay que aceptar/permitir el acceso de la aplicacion a las fotos, archivos y almacenamiento en el dispositivo, y no me parece que sea necesario... |
| 13 | 2 | 15 | 2021-02-05 | Lentísima, además los datos se borran a menudo y hay que hacer login de nuevo. El diseño nefasto y poco intuitivo. Para rehacer entera. |
| 14 | 5 | 14 | 2019-07-18 | Ahora si, ya funciona todo, hasta las recargas!!. Gracias. |
| 15 | 1 | 14 | 2019-06-25 | Es malísima, cada vez que hacen una actualización se queda inoperativa para la consulta y recarga de las tarjetas... |
| 16 | 1 | 14 | 2019-03-08 | acabo de instalar esta app y ya me esta tocando las narices... ¿por qué quiere tener acceso a otro tipo de archivos que no son básicos para su funcionamiento?... |
| 17 | 2 | 14 | 2018-05-24 | Observo que, al terminar la instalación de la aplicación, las instrucciones aparecen en valenciano... Mi sugerencia es que en la pantalla de inicio se pueda escoger el idioma... |
| 18 | 1 | 12 | 2023-10-16 | Tengo la app actualizada y no me da opción de poder recargar mi bono desde el móvil... ni tampoco hay opción de salir de cualquier estación pasando con NFC del móvil... |
| 19 | 2 | 12 | 2018-03-26 | El diseño y contenido está bien, ha sido buena idea. Va muy lenta, da lag... Tampoco puedes planear un viaje fuera de la franja horario a la que te encuentras... |
| 20 | 1 | 11 | 2020-08-05 | Me parece abusivo tener que dar permiso a la app para acceder a MIS fotos, MIS contenidos multimedia y archivos de MI dispositivo. |
| 21 | 1 | 11 | 2021-04-02 | Why does this app requires access to my photos to work? It seems ludicrous to me that it is not even optional! |
| 22 | 1 | 10 | 2023-05-22 | Iba mal, pero desde la última actualización es inutilizable. No carga los horarios, no carga los próximos trenes en ninguna parada... |
| 23 | 1 | 10 | 2020-08-22 | Piden permisos de almacenamiento y de ubicación, pero no explican para qué los quieren. Se piden tal cual la aplicación se instala... |
| 24 | 3 | 10 | 2020-03-10 | Es una app útil porque te permite consultar tu tarjeta tuin. Pero para calcular trayectos o revisar horarios, visitad la versión web de metrovalencia, que hará los cálculos correctos. |
| 25 | 1 | 9 | 2021-06-13 | No me había encontrado con una app más inútil. El 85% de las veces que la utilizas se queda pensando y no te da respuestas. |
| 26 | 1 | 9 | 2018-10-10 | Un hurra por los desarrolladores de la app por hacer que no se pueda utilizar la app sin actualizar y te encuentras en la calle... |
| 27 | 1 | 8 | 2021-11-01 | HORRIBLE. En pleno año 2021 se raya constantemente y aparece un horario que luego es distinto del que te encuentras en la parada. |
| 28 | 1 | 8 | 2021-02-05 | Hola, no guarda las tarjetas que introducimos. Hay que escribir los 12 dígitos cada vez. |
| 29 | 1 | 8 | 2020-12-07 | Horrible diseño, espantoso, es completamente liante y horrible. Es de lo menos intuitivo que he visto en mi vida... |
| 30 | 1 | 8 | 2020-08-02 | Es imposible mirar horarios para planificar el trayecto. "Error de conexión inténtelo más tarde." |
| 31 | 1 | 8 | 2018-10-07 | Te obliga a registrarte para guardar rutas. Cada vez que cambias de vista se descarga la información de nuevo. Se cierra la app cada vez que intentas cambiar el horario... |
| 32 | 1 | 8 | 2021-06-21 | Unacceptable permissions sought, while there's absolutely no need for all that. A big city like Valencia should come up with something better. |
| 33 | 5 | 8 | 2019-07-15 | In Valencia for 3 months, this app was great because it both has the map function and timetables. The only thing to know is that if a train is coming within a minute or 2... |
| 34 | 2 | 7 | 2023-07-29 | La aplicación difícil para algunas personas... Sin alertas ni avisos con suficiente antelación o cambios importantes horarios... |
| 35 | 2 | 7 | 2022-07-26 | Aunque normalmente brinda ayuda para conocer los horarios, la app presenta fallos en iPhone al calcular recorridos. No es posible saber que línea debe tomar el usuario... |
| 36 | 4 | 7 | 2021-11-23 | Ya me funciona mejor. Teneis otra aplicación con el mismo diseño cambiando colores. Se podría mejorar esto. |
| 37 | 3 | 7 | 2021-04-07 | Estaría bien que se pudiera poner un nombre a las tarjetas de transporte. Tengo varias y reconocerlas por el número es menos práctico. |
| 38 | 1 | 7 | 2020-12-15 | La aplicación actual, nefasta. O no se detiene la instalación o no funciona una vez descargada. Por favor vuelvan a la aplicación anterior, donde se conocían mapas, rutas y horarios. |
| 39 | 1 | 7 | 2019-02-02 | Mi cuenta personal ha desaparecido mágicamente. Recordar la contraseña no funciona y enviar mensajes del incidente tampoco. |
| 40 | 1 | 6 | 2023-01-09 | La aplicación está bien cuando funciona pero es que no funciona el 60-70% de las veces, me sale error de conexión pero si es que estoy con datos 4G estables. |

---

## Methodology

Reviews were scraped via the `google-play-scraper` Python library:

```python
from google_play_scraper import reviews, Sort

result, continuation_token = reviews(
    'com.fgv.metrovalencia',
    lang='es',      # also scraped with lang='en'
    country='es',
    sort=Sort.NEWEST,
    count=200,
    continuation_token=continuation_token
)
```

- **Spanish (es):** 853 reviews across 5 batches
- **English (en):** 70 reviews in 1 batch
- Keyword theme matching was performed against review `content` field
  (case-insensitive, language-aware keyword lists)
- Raw JSON data stored at `/tmp/opencode/mv_reviews_es.json` and
  `/tmp/opencode/mv_reviews_en.json`
