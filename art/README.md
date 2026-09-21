# TransitOS store assets

Every image and text asset needed to publish TransitOS on Google Play, for the
three locales the app ships: **en** (default), **es** and **ca**.

The whole set is generated from source, so it can be refreshed whenever the app
is redesigned. Nothing here is hand-edited in an image editor.

## What is here

| Asset | Size | Locales | Play Console slot |
| --- | --- | --- | --- |
| `icon-transitos.png` (+ `.svg`) | 512x512, 24-bit PNG, no alpha | all (language independent) | App icon |
| `feature-graphic[-es/-ca].png` (+ `.svg`) | 1024x500 | en, es, ca | Feature graphic |
| `promo-graphic[-es/-ca].png` | 180x120 | en, es, ca | Promo graphic |
| `screenshots/framed/`, `framed_es/`, `framed_ca/` | 1080x1920 | en, es, ca | Phone screenshots |
| `screenshots/tablet7/framed/`, `framed_es/`, `framed_ca/` | 1200x1920 | en, es, ca | 7-inch tablet screenshots |
| `screenshots/tablet10/framed/`, `framed_es/`, `framed_ca/` | 1600x2560 | en, es, ca | 10-inch tablet screenshots |
| `play-store-short-description[-es/-ca].txt` | <= 80 chars | en, es, ca | Short description |
| `play-store-full-description[-es/-ca].txt` | <= 4000 chars | en, es, ca | Full description |
| `play-store-listing.csv` | UTF-8 CSV | en-US, es-ES, ca-ES | All listing text at once (title, short, full, release notes) |

The `en` files use the unsuffixed names (for example `feature-graphic.png`,
`screenshots/framed/01-home.png`) so existing references keep working.

`play-store.manifest.json` lists every generated asset with its path, Play slot,
locale, dimensions and source, so a release script can upload them without
guessing.

The screens are numbered `01-home`, `02-alerts`, `03-search`, `04-planner`,
`05-settings`, `06-map`, `07-dark` and `08-amoled`, plus a `triptych.png` that
combines three screens side by side in one image. `02-alerts` only exists when
FGV is publishing live service alerts at capture time; if there are none, the
capture and framing steps skip it with a warning. That gives up to 9 images per
device form factor, within the Play limit of 8 screenshots per form factor when
`02-alerts` is present.

## Listing text (CSV)

`play-store-listing.csv` is the single-file version of the store copy, keyed by
the Play locale codes:

| locale | language |
| --- | --- |
| `en-US` | English |
| `es-ES` | Spanish |
| `ca-ES` | Valencian |

Columns are `locale, app_title, short_description, full_description,
release_notes`. The authoritative source is still `i18n/<locale>.json`;
`generate.py` writes both the CSV and the per-locale `.txt` files from it and
fails if any value exceeds a Play limit (title 30, short 80, full 4000,
release notes 500).

## Sources of truth

- **Copy and graphics text**: `i18n/<locale>.json`. Contains the tagline, trust
  points, feature-graphic card labels, screenshot captions and the full store
  descriptions. Edit here, never in the generated `.txt` files.
- **Layout**: `templates/feature-graphic.svg`, `templates/promo-graphic.svg` and
  `templates/screenshot-frame.svg`. They use `{{TOKEN}}` placeholders that
  `generate.py` fills per locale (and, for the frame, per device size).
- **Icon**: `icon-transitos.svg` (flat, full-bleed, derived from the app's
  adaptive launcher icon).
- **Raw captures**: `screenshots/raw/<device>/<locale>/NN-<screen>.png`, written
  by `capture-screenshots.py`.

## Requirements

- `rsvg-convert` and ImageMagick (`magick`) on `PATH`.
- Python 3.9+ (standard library only).
- For captures: running Android emulators and `adb`.

## Regenerate everything

```bash
# 1. Capture fresh raw screenshots from the emulators (optional but required
#    whenever the UI changed).
python3 art/capture-screenshots.py

# 2. Frame them and render every graphic, description and the manifest.
python3 art/generate.py

# 3. Validate sizes, alpha and description limits.
python3 art/generate.py --check
```

To skip the emulator step (for example when only editing copy or the icon):

```bash
python3 art/generate.py --graphics      # icon, feature/promo graphics, copy, manifest
python3 art/generate.py --screenshots   # only re-frame existing raw captures
```

The generator is idempotent: running it again produces the same files. It also
**fails loudly** if a localized caption, tagline or pill would not fit its box,
or if a description exceeds the Play character limit, so clipped text cannot be
committed by accident.

## Captures

`capture-screenshots.py` drives the app over `adb`, looking elements up with UI
Automator so it works on any device and locale. For each device/locale it:

1. forces light mode and sets the per-app locale (`cmd locale set-app-locales`);
2. clears app data and seeds a deterministic set of favourites
   (Benimaclet and Colon) and one saved route (Benimaclet -> Aeroport, labelled
   per locale) by writing the Preferences DataStore file directly;
3. launches the app, waits for the localized "next departure" label, then
   captures each screen and verifies the expected localized text is on screen.

Devices are matched by **AVD name**, not serial:

| Device | Preferred AVD | Fallback | Resolution |
| --- | --- | --- | --- |
| `phone` | `transitos_phone` | `duo` | 1080x1920 |
| `tablet7` | `transitos_tab7` | `tablet7` | 1200x1920 |
| `tablet10` | `transitos_tab10` | `tablet10` | 1600x2560 (portrait) |

The `transitos_*` AVDs are dedicated to asset generation so a capture is never
disturbed by another workload sharing the default emulators. They can be
recreated from the stock AVDs:

```bash
# copy an existing AVD and fix its resolution/density in config.ini
cp -R ~/.android/avd/tablet7.avd ~/.android/avd/transitos_tab7.avd
# then set hw.lcd.width / hw.lcd.height / hw.lcd.density accordingly
```

Useful flags:

```bash
python3 art/capture-screenshots.py --device phone --locale es
python3 art/capture-screenshots.py --screen home --screen map
```

The `dark` and `amoled` screens re-seed `app_theme` (in both the Preferences
DataStore and `shared_prefs/transitos_prefs.xml`) before launching, so the two
extra captures show the real dark and pure-black themes. Tablets expose a system
taskbar below the app; `generate.py` chops those bottom pixels
(`crop_bottom`) before framing so no third-party app icons leak into the store
screenshots.

## Design system

The graphics follow the app's Material 3 Expressive identity:

- Deep teal gradient background (`#0B5F5D` -> `#02191A`) with teal and amber
  radial glows and a subtle film grain.
- Amber (`#F5C55A`) is the single accent, reserved for live/highlight moments.
- The route-line motif uses real Metrovalencia line colours (L1 `#FFD100`,
  L2 `#E60096`, L3 `#DD052C`, L5 `#009B48`, ...) as thin station-dotted curves.
- Typography is Helvetica Neue / Arial. The feature graphic reuses the app's
  signature "next departure" card so the store image matches the product.

## Adding a locale

1. Add `<locale>` to `LOCALES` (and its suffix maps) in `generate.py`.
2. Add `i18n/<locale>.json` with the same keys as the existing files.
3. Add the locale to `LOCALES` and `UI` in `capture-screenshots.py`.
4. Add the locale to `capture`/`generate` and re-run the pipeline.
