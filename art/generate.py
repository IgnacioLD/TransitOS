#!/usr/bin/env python3
"""Reproducible generator for every Google Play Store asset of TransitOS.

It reads the localized strings from ``art/i18n/<locale>.json`` and the SVG
templates from ``art/templates/``, then produces, for every locale (en, es, ca):

  * the 512x512 app icon (locale independent, 24-bit PNG, no alpha)
  * the 1024x500 feature graphic
  * the 180x120 promo graphic
  * framed phone (1080x1920), 7-inch (1200x1920) and 10-inch (1600x2560)
    screenshots, with a localized caption, from the raw captures taken by
    ``art/capture-screenshots.py``
  * the short and full store descriptions
  * ``art/play-store.manifest.json`` mapping every asset to its Play slot

Requires ``rsvg-convert`` and ImageMagick (``magick``) on PATH.

Usage:
    python3 art/generate.py                 # everything that has sources
    python3 art/generate.py --graphics      # icon + feature + promo + copy
    python3 art/generate.py --screenshots   # framed screenshots only
    python3 art/generate.py --check         # validate existing outputs
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

ART = Path(__file__).resolve().parent
REPO = ART.parent
TEMPLATES = ART / "templates"
I18N = ART / "i18n"
RAW = ART / "screenshots" / "raw"
BUILD = ART / ".build"

LOCALES = ("en", "es", "ca")
LOCALE_SUFFIX = {"en": "", "es": "-es", "ca": "-ca"}
LOCALE_DIR_SUFFIX = {"en": "", "es": "_es", "ca": "_ca"}

APP_NAME = "TransitOS"

SCREENS = ("home", "alerts", "search", "planner", "settings", "map", "dark", "amoled")

TRIPTYCH_SCREENS = ("home", "planner", "map")

DEVICES = {
    "phone": {
        "slot": "Phone screenshots",
        "out": ART / "screenshots",
        "raw_size": (1080, 1920),
        "framed_size": (1080, 1920),
        "crop_bottom": 0,
        "dir": "",
    },
    "tablet7": {
        "slot": "7-inch tablet screenshots",
        "out": ART / "screenshots" / "tablet7",
        "raw_size": (1200, 1920),
        "framed_size": (1200, 1920),
        "crop_bottom": 120,
        "dir": "",
    },
    "tablet10": {
        "slot": "10-inch tablet screenshots",
        "out": ART / "screenshots" / "tablet10",
        "raw_size": (1600, 2560),
        "framed_size": (1600, 2560),
        "crop_bottom": 120,
        "dir": "",
    },
}

FONT = "Helvetica Neue, Arial, sans-serif"


# --------------------------------------------------------------------------- #
# Shell helpers
# --------------------------------------------------------------------------- #
def run(cmd: list[str]) -> None:
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(
            f"command failed ({proc.returncode}): {' '.join(cmd)}\n{proc.stderr}"
        )


def require_tools() -> None:
    missing = [t for t in ("rsvg-convert", "magick") if shutil.which(t) is None]
    if missing:
        sys.exit(f"error: required tools not found on PATH: {', '.join(missing)}")


def read_svg(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def icon_markup(x: int, y: int, size: int, radius: int, prefix: str = "logo") -> str:
    """Inline the real TransitOS icon SVG as a rounded, named logo tile.

    Reading ``icon-transitos.svg`` (rather than re-drawing the mark) keeps the
    store graphics byte-for-byte consistent with the shipped app icon. Feature
    ids are namespaced so they cannot clash with the host graphic's own defs.
    """
    svg = read_svg(ART / "icon-transitos.svg")
    inner = svg[svg.index(">", svg.index("<svg")) + 1 : svg.rindex("</svg>")]
    ids = set(re.findall(r'id="([^"]+)"', inner))
    for ident in ids:
        inner = inner.replace(f'id="{ident}"', f'id="{prefix}_{ident}"')
        inner = inner.replace(f"url(#{ident})", f"url(#{prefix}_{ident})")
    scale = size / 512
    clip = f"{prefix}Clip"
    return (
        f'<g transform="translate({x},{y}) scale({scale:.6f})">'
        f'<clipPath id="{clip}"><rect x="0" y="0" width="512" height="512" rx="{radius / scale:.2f}"/></clipPath>'
        f'<g clip-path="url(#{clip})">{inner}</g></g>'
    )


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def fill(template: str, values: dict[str, object]) -> str:
    out = template
    for key, value in values.items():
        out = out.replace("{{" + key + "}}", str(value))
    leftover = [t for t in out.split("{{")[1:] if "}}" in t]
    if leftover:
        names = [t.split("}}")[0] for t in leftover]
        raise RuntimeError(f"unfilled template tokens: {names}")
    return out


def rsvg(svg_text: str, out_png: Path, width: int | None = None, height: int | None = None) -> None:
    BUILD.mkdir(parents=True, exist_ok=True)
    svg_path = BUILD / (out_png.stem + ".svg")
    write(svg_path, svg_text)
    cmd = ["rsvg-convert"]
    if width:
        cmd += ["-w", str(width)]
    if height:
        cmd += ["-h", str(height)]
    cmd += ["-o", str(out_png), str(svg_path)]
    out_png.parent.mkdir(parents=True, exist_ok=True)
    run(cmd)


_MEASURE_CACHE: dict[tuple[str, int, int, float], int] = {}


def measure_text(text: str, size: int, weight: int = 400, letter_spacing: float = 0.0) -> int:
    """Ink width in pixels of ``text`` as rendered by rsvg-convert.

    Used to size pills/chips to their localized label and to fail loudly when a
    caption line would not fit, so no graphic ever ships with clipped text.
    """
    if not text:
        return 0
    cache_key = (text, size, weight, letter_spacing)
    if cache_key in _MEASURE_CACHE:
        return _MEASURE_CACHE[cache_key]
    escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    pad = size * 2
    width = pad * 2 + size * 40
    height = pad * 2 + size * 3
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}">'
        f'<text x="{pad}" y="{pad + size}" font-family="{FONT}" font-size="{size}" '
        f'font-weight="{weight}" letter-spacing="{letter_spacing}" fill="#fff">{escaped}</text></svg>'
    )
    BUILD.mkdir(parents=True, exist_ok=True)
    svg_path = BUILD / "measure.svg"
    png_path = BUILD / "measure.png"
    write(svg_path, svg)
    run(["rsvg-convert", "-o", str(png_path), str(svg_path)])
    out = subprocess.run(
        ["magick", str(png_path), "-trim", "+repage", "-format", "%w", "info:"],
        capture_output=True,
        text=True,
    ).stdout
    result = int(out) if out.strip().isdigit() else 0
    _MEASURE_CACHE[cache_key] = result
    return result


def identify(path: Path) -> tuple[int, int, bool]:
    proc = subprocess.run(
        ["magick", "identify", "-format", "%w %h %[channels]", str(path)],
        capture_output=True,
        text=True,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"identify failed for {path}: {proc.stderr}")
    parts = proc.stdout.split()
    w, h, channels = int(parts[0]), int(parts[1]), parts[2]
    return w, h, "a" not in channels


# --------------------------------------------------------------------------- #
# Copy (descriptions)
# --------------------------------------------------------------------------- #
def build_copy(data: dict) -> None:
    for locale in LOCALES:
        suffix = LOCALE_SUFFIX[locale]
        short = data[locale]["short_description"].strip()
        full = data[locale]["full_description"].strip()
        if len(short) > 80:
            sys.exit(f"error: short description for '{locale}' is {len(short)} chars (max 80)")
        if len(full) > 4000:
            sys.exit(f"error: full description for '{locale}' is {len(full)} chars (max 4000)")
        write(ART / f"play-store-short-description{suffix}.txt", short + "\n")
        write(ART / f"play-store-full-description{suffix}.txt", full + "\n")


def build_listing_csv(data: dict) -> Path:
    """Localized listing text in one CSV, keyed by Play locale code."""
    import csv

    out = ART / "play-store-listing.csv"
    with out.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["locale", "app_title", "short_description", "full_description", "release_notes"])
        for locale in LOCALES:
            d = data[locale]
            title = d.get("store_title", APP_NAME)
            short = d["short_description"].strip()
            full = d["full_description"].strip()
            notes = d.get("release_notes", "").strip()
            if len(title) > 30:
                sys.exit(f"error: title for '{locale}' is {len(title)} chars (max 30)")
            if len(short) > 80:
                sys.exit(f"error: short description for '{locale}' is {len(short)} chars (max 80)")
            if len(full) > 4000:
                sys.exit(f"error: full description for '{locale}' is {len(full)} chars (max 4000)")
            if len(notes) > 500:
                sys.exit(f"error: release notes for '{locale}' are {len(notes)} chars (max 500)")
            writer.writerow([d["play_locale"], title, short, full, notes])
    return out


# --------------------------------------------------------------------------- #
# Icon
# --------------------------------------------------------------------------- #
def build_icon() -> Path:
    out = ART / "icon-transitos.png"
    svg = read_svg(ART / "icon-transitos.svg")
    rsvg(svg, out, 512, 512)
    # Google Play asks for a 32-bit PNG; flattening keeps the file alpha-free
    # as requested (the artwork fills the whole square, so nothing is lost).
    flat = BUILD / "icon-flat.png"
    run(["magick", str(out), "-background", "#006A6A", "-alpha", "remove", "-alpha", "off", f"PNG24:{flat}"])
    shutil.move(str(flat), out)
    return out


# --------------------------------------------------------------------------- #
# Feature graphic + promo graphic
# --------------------------------------------------------------------------- #
def build_feature_graphic(data: dict) -> dict[str, Path]:
    template = read_svg(TEMPLATES / "feature-graphic.svg")
    out: dict[str, Path] = {}
    for locale in LOCALES:
        d = data[locale]
        features = d["feature_graphic"]["features"]
        values = {
            "APP_NAME": APP_NAME,
            "TAGLINE": d["tagline"],
            "TRUST_ROW": "  ·  ".join(d["trust"]),
            "LOGO": icon_markup(72, 62, 108, 28, prefix=f"fg_{locale}"),
        }
        for i, feature in enumerate(features, start=1):
            values[f"F{i}T"] = feature["title"]
            values[f"F{i}S"] = feature["subtitle"]
        card = d["feature_graphic"].get("card", {})
        # Size the live pill to its localized label so the text always fits.
        pill_h = 32
        dot_d = 11
        pad_left, gap, pad_right = 14, 9, 16
        text_w = measure_text(card.get("live", ""), 14, 600)
        pill_w = pad_left + dot_d + gap + text_w + pad_right
        card_width = 418
        card_pad = 34
        pill_x = card_width - card_pad - pill_w
        pill_y = 38
        values.update(
            {
                "CARD_KICKER": card.get("kicker", ""),
                "CARD_LIVE": card.get("live", ""),
                "CARD_STOP": card.get("stop", ""),
                "CARD_DEST": card.get("destination", ""),
                "CARD_MIN": card.get("minutes", ""),
                "CARD_UNIT": card.get("unit", ""),
                "PILL_X": pill_x,
                "PILL_Y": pill_y,
                "PILL_W": pill_w,
                "PILL_H": pill_h,
                "PILL_R": pill_h // 2,
                "PILL_DOT_CX": pill_x + pad_left + dot_d // 2,
                "PILL_DOT_CY": pill_y + pill_h // 2,
                "PILL_TEXT_X": pill_x + pad_left + dot_d + gap,
                "PILL_TEXT_BASELINE": pill_y + pill_h // 2 + 5,
            }
        )
        svg = fill(template, values)
        png = ART / f"feature-graphic{LOCALE_SUFFIX[locale]}.png"

        # Fit checks so nothing collides with the card or overflows it.
        card_x, card_pad, card_w = 534, 34, 418
        if 76 + measure_text(d["tagline"], 20, 500) > card_x - 24:
            raise SystemExit(f"error: {locale} feature tagline too wide")
        if 94 + measure_text("  ·  ".join(d["trust"]), 14, 600, 0.4) > card_x - 24:
            raise SystemExit(f"error: {locale} feature trust row too wide")
        if 34 + measure_text(card.get("kicker", ""), 15, 700, 2.4) > pill_x - 16:
            raise SystemExit(f"error: {locale} feature card kicker collides with the pill")
        if 34 + measure_text(card.get("stop", ""), 34, 700, -0.6) > card_w - card_pad:
            raise SystemExit(f"error: {locale} feature card stop name too wide")
        if 112 + measure_text(card.get("destination", ""), 24, 600) > 282:
            raise SystemExit(f"error: {locale} feature card destination too wide")
        if 292 + 10 + measure_text(card.get("unit", ""), 20, 600) > card_w - card_pad:
            raise SystemExit(f"error: {locale} feature card unit overflows")
        rsvg(svg, png, 1024, 500)
        write(ART / f"feature-graphic{LOCALE_SUFFIX[locale]}.svg", svg)
        out[locale] = png
    return out


def build_promo_graphic(data: dict) -> dict[str, Path]:
    template = read_svg(TEMPLATES / "promo-graphic.svg")
    out: dict[str, Path] = {}
    for locale in LOCALES:
        d = data[locale]
        svg = fill(
            template,
            {
                "APP_NAME": APP_NAME,
                "PROMO_LINE": d["promo"]["line"],
                "LOGO": icon_markup(67, 12, 46, 13, prefix=f"pg_{locale}"),
            },
        )
        png = ART / f"promo-graphic{LOCALE_SUFFIX[locale]}.png"
        if measure_text(d["promo"]["line"], 11, 600) > 156:
            raise SystemExit(f"error: {locale} promo line too wide")
        if measure_text(APP_NAME, 21, 800, -0.4) > 160:
            raise SystemExit(f"error: {locale} promo wordmark too wide")
        rsvg(svg, png, 180, 120)
        out[locale] = png
    return out


# --------------------------------------------------------------------------- #
# Framed screenshots
# --------------------------------------------------------------------------- #
def frame_geometry(w: int, h: int, raw_w: int, raw_h: int, index: int) -> dict[str, object]:
    g: dict[str, object] = {}
    g["W"], g["H"] = w, h
    g["CAP_X"] = round(0.0815 * w)
    g["HEADLINE_SIZE"] = round(0.0320 * h)
    g["SUB_SIZE"] = round(0.0168 * h)
    g["STEP_SIZE"] = round(0.120 * h)
    g["FOOTER_SIZE"] = round(0.0142 * h)
    g["EYEBROW_Y"] = round(0.066 * h)
    g["EYEBROW_W"] = round(0.052 * w)
    g["EYEBROW_H"] = max(6, round(0.0038 * h))
    g["EYEBROW_R"] = g["EYEBROW_H"] // 2  # type: ignore[operator]
    g["ACCENT"] = "#F5C55A"
    g["CAP_Y1"] = round(0.108 * h)
    g["CAP_Y2"] = round(0.1435 * h)
    g["SUB_Y1"] = round(0.172 * h)
    g["SUB_Y2"] = round(0.1935 * h)
    g["STEP_X"] = w - round(0.0815 * w)
    g["STEP_Y"] = round(0.208 * h)
    g["STEP"] = f"{index:02d}"
    g["TOP_LINE_H"] = max(4, round(0.0098 * h))

    aspect = raw_w / raw_h
    dev_h = round(0.700 * h)
    dev_w = round(dev_h * aspect)
    dev_x = round((w - dev_w) / 2)
    dev_y = round(0.216 * h)
    pad = max(12, round(0.0073 * h))
    g["BEZEL_X"] = dev_x - pad
    g["BEZEL_Y"] = dev_y - pad
    g["BEZEL_W"] = dev_w + 2 * pad
    g["BEZEL_H"] = dev_h + 2 * pad
    g["BEZEL_R"] = round(dev_w * 0.05)
    g["BORDER_SW"] = max(1, round(0.0016 * w))
    g["SHADOW_BLUR"] = round(0.013 * w)
    g["SHADOW_DY"] = round(0.008 * w)
    g["DEV_CX"] = w // 2
    g["DEV_CY"] = dev_y + dev_h // 2
    g["DEV_RX"] = round(dev_w * 0.95)
    g["DEV_RY"] = round(dev_h * 0.72)
    g["DEV"] = (dev_x, dev_y, dev_w, dev_h, round(dev_w * 0.045))

    g["MOTIF_SW"] = max(1, round(0.0018 * w))
    g["MOTIF1"] = (
        f"M {-0.03 * w:.0f} {0.050 * h:.0f} H {0.20 * w:.0f} "
        f"L {0.28 * w:.0f} {0.020 * h:.0f} H {1.03 * w:.0f}"
    )
    g["MOTIF2"] = (
        f"M {-0.03 * w:.0f} {0.985 * h:.0f} H {0.35 * w:.0f} "
        f"L {0.43 * w:.0f} {0.945 * h:.0f} H {1.03 * w:.0f}"
    )
    g["MOTIF3"] = (
        f"M {0.955 * w:.0f} {0.240 * h:.0f} V {0.560 * h:.0f} "
        f"L {0.922 * w:.0f} {0.630 * h:.0f} V {0.900 * h:.0f}"
    )
    r = max(4, round(0.0045 * w))
    g["MOTIF_DOTS"] = "".join(
        f'<circle cx="{cx:.0f}" cy="{cy:.0f}" r="{r}"/>'
        for cx, cy in (
            (0.20 * w, 0.050 * h),
            (0.28 * w, 0.020 * h),
            (0.35 * w, 0.985 * h),
            (0.43 * w, 0.945 * h),
            (0.955 * w, 0.240 * h),
            (0.922 * w, 0.630 * h),
            (0.955 * w, 0.900 * h),
        )
    )

    baseline = round(0.952 * h)
    footer_size = g["FOOTER_SIZE"]
    icon = round(0.0192 * h)
    icon_center = baseline - round(0.34 * footer_size)
    g["FOOT_ICON"] = icon
    g["FOOT_ICON_X"] = g["CAP_X"]
    g["FOOT_ICON_Y"] = icon_center - icon // 2
    g["FOOT_ICON_R"] = round(icon * 0.30)
    g["FOOT_HALF"] = icon // 2
    g["FOOT_TRAIN_SCALE"] = round(icon * 0.0086, 4)
    g["FOOT_TEXT_X"] = g["CAP_X"] + icon + round(0.014 * w)
    g["FOOT_BASELINE"] = baseline
    g["FOOT_TAG_X"] = w - g["CAP_X"]
    return g


def prepare_raw(raw: Path, crop_bottom: int, tag: str) -> Path:
    """Optionally chop the system taskbar off the bottom of a raw capture."""
    if crop_bottom <= 0:
        return raw
    out = BUILD / f"rawcrop-{tag}.png"
    run(["magick", str(raw), "-gravity", "south", "-chop", f"0x{crop_bottom}", str(out)])
    return out


def rounded_shot(src: Path, size: tuple[int, int], radius: int, tag: str) -> Path:
    dev_w, dev_h = size
    shot = BUILD / f"shot-{tag}.png"
    run(["magick", str(src), "-resize", f"{dev_w}x{dev_h}!", str(shot)])
    mask_svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{dev_w}" height="{dev_h}">'
        f'<rect width="{dev_w}" height="{dev_h}" fill="#000"/>'
        f'<rect width="{dev_w}" height="{dev_h}" rx="{radius}" fill="#fff"/></svg>'
    )
    mask_png = BUILD / f"mask-{tag}.png"
    rsvg(mask_svg, mask_png, dev_w, dev_h)
    rounded = BUILD / f"rounded-{tag}.png"
    run(["magick", str(shot), str(mask_png), "-alpha", "off", "-compose", "CopyOpacity", "-composite", str(rounded)])
    return rounded


def build_framed(
    raw_dir: Path,
    out_dir: Path,
    framed_size: tuple[int, int],
    locale: str,
    crop_bottom: int = 0,
) -> dict[str, Path]:
    template = read_svg(TEMPLATES / "screenshot-frame.svg")
    data = json.loads((I18N / f"{locale}.json").read_text(encoding="utf-8"))
    out_dir.mkdir(parents=True, exist_ok=True)
    produced: dict[str, Path] = {}

    for index, screen in enumerate(SCREENS, start=1):
        raw = raw_dir / f"{index:02d}-{screen}.png"
        if not raw.exists():
            print(f"  ! skip {raw.relative_to(REPO)} (no raw capture)")
            continue
        prepared = prepare_raw(raw, crop_bottom, f"{locale}-{screen}")
        raw_w, raw_h, _ = identify(prepared)
        width, height = framed_size
        g = frame_geometry(width, height, raw_w, raw_h, index)
        caption = data["captions"][screen]
        values: dict[str, object] = dict(g)

        # Fail loudly rather than ship a graphic with clipped caption text.
        available = width - 2 * g["CAP_X"]
        for label, text, size, weight in (
            ("headline", caption["headline"][0], g["HEADLINE_SIZE"], 700),
            ("headline", caption["headline"][1], g["HEADLINE_SIZE"], 700),
            ("sub", caption["sub"][0], g["SUB_SIZE"], 400),
            ("sub", caption["sub"][1], g["SUB_SIZE"], 400),
        ):
            if text and measure_text(text, size, weight) > available:
                raise SystemExit(
                    f"error: {locale}/{screen} {label} '{text}' does not fit "
                    f"({measure_text(text, size, weight)} > {available}px)"
                )
        tagline_width = measure_text(data["tagline"], g["FOOTER_SIZE"], 400)
        brand_width = g["CAP_X"] + g["FOOT_ICON"] + round(0.014 * width) + measure_text(APP_NAME, g["FOOTER_SIZE"], 700)
        if brand_width + tagline_width + round(0.03 * width) > width:
            raise SystemExit(f"error: {locale}/{screen} footer brand + tagline do not fit")

        values.update(
            {
                "HEADLINE1": caption["headline"][0],
                "HEADLINE2": caption["headline"][1],
                "SUB1": caption["sub"][0],
                "SUB2": caption["sub"][1],
                "TAGLINE": data["tagline"],
                "APP_NAME": APP_NAME,
                "LOGO": icon_markup(
                    g["FOOT_ICON_X"], g["FOOT_ICON_Y"], g["FOOT_ICON"], g["FOOT_ICON_R"],
                    prefix=f"frame_{locale}_{index:02d}",
                ),
            }
        )
        svg = fill(template, values)
        frame_png = BUILD / f"frame-{out_dir.name}-{index:02d}.png"
        rsvg(svg, frame_png, width, height)

        dev_x, dev_y, dev_w, dev_h, dev_r = g["DEV"]
        rounded = rounded_shot(prepared, (dev_w, dev_h), dev_r, f"{out_dir.name}-{index:02d}")
        final = out_dir / f"{index:02d}-{screen}.png"
        run(["magick", str(frame_png), str(rounded), "-geometry", f"+{dev_x}+{dev_y}", "-composite", str(final)])
        produced[screen] = final
    return produced


def build_screenshots() -> dict[str, dict[str, dict[str, Path]]]:
    result: dict[str, dict[str, dict[str, Path]]] = {}
    for device, meta in DEVICES.items():
        result[device] = {}
        for locale in LOCALES:
            raw_dir = RAW / device / locale
            if not raw_dir.exists() or not any(raw_dir.glob("*.png")):
                print(f"  ! no raw captures for {device}/{locale}")
                continue
            print(f"  framing {device}/{locale} ...")
            out_dir = meta["out"] / f"framed{LOCALE_DIR_SUFFIX[locale]}"
            result[device][locale] = build_framed(
                raw_dir, out_dir, meta["framed_size"], locale, meta["crop_bottom"]
            )
    return result


def build_triptychs() -> dict[str, dict[str, Path]]:
    """One image per device/locale showing three screens side by side."""
    template = read_svg(TEMPLATES / "triptych.svg")
    produced: dict[str, dict[str, Path]] = {}
    for device, meta in DEVICES.items():
        produced[device] = {}
        width, height = meta["framed_size"]
        crop = meta["crop_bottom"]
        for locale in LOCALES:
            out_dir = meta["out"] / f"framed{LOCALE_DIR_SUFFIX[locale]}"
            data = json.loads((I18N / f"{locale}.json").read_text(encoding="utf-8"))
            present = []
            for screen in TRIPTYCH_SCREENS:
                raw = RAW / device / locale / f"{SCREENS.index(screen) + 1:02d}-{screen}.png"
                if raw.exists():
                    present.append((screen, prepare_raw(raw, crop, f"trip-{device}-{locale}-{screen}")))
            if len(present) < 3:
                print(f"  ! not enough raw captures for triptych {device}/{locale}")
                continue
            g = triptych_geometry(width, height, present[0][1], data)
            values = dict(g)
            values.update(
                {
                    "HEADLINE1": data["triptych"]["headline"][0],
                    "HEADLINE2": data["triptych"]["headline"][1],
                    "SUB1": data["triptych"]["sub"][0],
                    "SUB2": data["triptych"]["sub"][1],
                    "APP_NAME": APP_NAME,
                    "TAGLINE": data["tagline"],
                    "LOGO": icon_markup(
                        g["FOOT_ICON_X"], g["FOOT_ICON_Y"], g["FOOT_ICON"], g["FOOT_ICON_R"],
                        prefix=f"trip_{locale}",
                    ),
                    "LABEL1": data["triptych"]["labels"][0],
                    "LABEL2": data["triptych"]["labels"][1],
                    "LABEL3": data["triptych"]["labels"][2],
                    "BEZEL1": bezel_markup(g["PANELS"][0]),
                    "BEZEL2": bezel_markup(g["PANELS"][1]),
                    "BEZEL3": bezel_markup(g["PANELS"][2]),
                }
            )
            svg = fill(template, values)
            frame_png = BUILD / f"triptych-{device}-{locale}.png"
            rsvg(svg, frame_png, width, height)
            out = frame_png
            for (screen, raw), panel in zip(present, g["PANELS"]):
                dev_x, dev_y, dev_w, dev_h, dev_r = panel
                rounded = rounded_shot(raw, (dev_w, dev_h), dev_r, f"trip-{device}-{locale}-{screen}-r")
                merged = BUILD / f"triptych-merge-{device}-{locale}-{screen}.png"
                run(["magick", str(out), str(rounded), "-geometry", f"+{dev_x}+{dev_y}", "-composite", str(merged)])
                out = merged
            final = out_dir / "triptych.png"
            final.parent.mkdir(parents=True, exist_ok=True)
            run(["magick", str(out), "-alpha", "remove", "-alpha", "off", f"PNG24:{final}"])
            produced[device][locale] = final
    return produced


def bezel_markup(panel: tuple[int, int, int, int, int]) -> str:
    dev_x, dev_y, dev_w, dev_h, dev_r = panel
    return (
        f'<rect x="{dev_x}" y="{dev_y}" width="{dev_w}" height="{dev_h}" rx="{dev_r}" '
        f'fill="#000000" fill-opacity="0.5" stroke="#FFFFFF" stroke-opacity="0.12"/>'
    )


def triptych_geometry(width: int, height: int, sample_raw: Path, data: dict) -> dict[str, object]:
    g: dict[str, object] = {}
    g["W"], g["H"] = width, height
    g["CAP_X"] = round(0.0815 * width)
    g["HEADLINE_SIZE"] = round(0.0320 * height)
    g["SUB_SIZE"] = round(0.0168 * height)
    g["FOOTER_SIZE"] = round(0.0142 * height)
    g["EYEBROW_Y"] = round(0.058 * height)
    g["EYEBROW_W"] = round(0.052 * width)
    g["EYEBROW_H"] = max(6, round(0.0038 * height))
    g["EYEBROW_R"] = g["EYEBROW_H"] // 2  # type: ignore[operator]
    g["ACCENT"] = "#F5C55A"
    g["CAP_Y1"] = round(0.100 * height)
    g["CAP_Y2"] = round(0.1355 * height)
    g["SUB_Y1"] = round(0.164 * height)
    g["SUB_Y2"] = round(0.1855 * height)
    g["TOP_LINE_H"] = max(4, round(0.0098 * height))
    g["LABEL_SIZE"] = round(0.0180 * height)

    raw_w, raw_h, _ = identify(sample_raw)
    aspect = raw_w / raw_h
    margin = round(0.030 * width)
    gap = round(0.013 * width)
    panel_w = (width - 2 * margin - 2 * gap) // 3
    panel_h = round(panel_w / aspect)
    region_top = round(0.190 * height)
    region_bottom = round(0.900 * height)
    max_panel_h = region_bottom - region_top - round(0.130 * height)
    if panel_h > max_panel_h:
        panel_h = max_panel_h
        panel_w = round(panel_h * aspect)
    panel_y = region_top + (region_bottom - region_top - panel_h - round(0.115 * height)) // 2
    panel_r = round(panel_w * 0.055)
    panels = []
    for i in range(3):
        px = margin + i * (panel_w + gap)
        panels.append((px, panel_y, panel_w, panel_h, panel_r))
    g["PANELS"] = panels
    g["ROUTE_X1"] = panels[0][0] + panel_w // 2
    g["ROUTE_X3"] = panels[2][0] + panel_w // 2
    g["ROUTE_Y"] = panel_y + panel_h + round(0.040 * height)
    g["ROUTE_SW"] = max(2, round(0.0022 * width))
    g["DOT_R"] = max(6, round(0.0058 * width))
    g["DOT_SW"] = max(2, round(0.0022 * width))
    g["LABEL_Y"] = g["ROUTE_Y"] + round(0.056 * height)  # type: ignore[operator]
    g["LABEL_X1"] = panels[0][0] + panel_w // 2
    g["LABEL_X2"] = panels[1][0] + panel_w // 2
    g["LABEL_X3"] = panels[2][0] + panel_w // 2
    g["PANEL_GLOW_CY"] = round((panel_y + panel_h / 2) / height, 4)

    g["MOTIF_SW"] = max(1, round(0.0018 * width))
    g["MOTIF1"] = (
        f"M {-0.03 * width:.0f} {0.030 * height:.0f} H {0.22 * width:.0f} "
        f"L {0.30 * width:.0f} {0.062 * height:.0f} H {1.03 * width:.0f}"
    )
    g["MOTIF2"] = (
        f"M {-0.03 * width:.0f} {0.992 * height:.0f} H {0.34 * width:.0f} "
        f"L {0.42 * width:.0f} {0.955 * height:.0f} H {1.03 * width:.0f}"
    )
    g["MOTIF3"] = (
        f"M {0.960 * width:.0f} {0.250 * height:.0f} V {0.470 * height:.0f} "
        f"L {0.930 * width:.0f} {0.530 * height:.0f} V {0.880 * height:.0f}"
    )
    dr = max(4, round(0.0042 * width))
    g["MOTIF_DOTS"] = "".join(
        f'<circle cx="{cx:.0f}" cy="{cy:.0f}" r="{dr}"/>'
        for cx, cy in (
            (0.22 * width, 0.030 * height),
            (0.30 * width, 0.062 * height),
            (0.34 * width, 0.992 * height),
            (0.42 * width, 0.955 * height),
            (0.960 * width, 0.250 * height),
            (0.930 * width, 0.530 * height),
            (0.960 * width, 0.880 * height),
        )
    )

    baseline = round(0.952 * height)
    footer_size = g["FOOTER_SIZE"]
    icon = round(0.0192 * height)
    icon_center = baseline - round(0.34 * footer_size)
    g["FOOT_ICON"] = icon
    g["FOOT_ICON_X"] = g["CAP_X"]
    g["FOOT_ICON_Y"] = icon_center - icon // 2
    g["FOOT_ICON_R"] = round(icon * 0.30)
    g["FOOT_TEXT_X"] = g["CAP_X"] + icon + round(0.014 * width)
    g["FOOT_BASELINE"] = baseline
    g["FOOT_TAG_X"] = width - g["CAP_X"]

    # Fit checks
    available = width - 2 * g["CAP_X"]
    for text, size, weight in (
        (data["triptych"]["headline"][0], g["HEADLINE_SIZE"], 700),
        (data["triptych"]["headline"][1], g["HEADLINE_SIZE"], 700),
        (data["triptych"]["sub"][0], g["SUB_SIZE"], 400),
        (data["triptych"]["sub"][1], g["SUB_SIZE"], 400),
    ):
        if text and measure_text(text, size, weight) > available:
            raise SystemExit(f"error: triptych text '{text}' does not fit")
    for label in data["triptych"]["labels"]:
        if measure_text(label, g["LABEL_SIZE"], 600) > panel_w - 16:
            raise SystemExit(f"error: triptych label '{label}' does not fit its panel")
    return g


# --------------------------------------------------------------------------- #
# Manifest
# --------------------------------------------------------------------------- #
def build_manifest(data: dict, graphics: dict, screenshots: dict) -> dict:
    assets: list[dict] = []

    def add(**kw: object) -> None:
        assets.append(kw)

    w, h, opaque = identify(ART / "icon-transitos.png")
    add(
        id="app-icon",
        kind="app-icon",
        path="art/icon-transitos.png",
        source="art/icon-transitos.svg",
        play_slot="App icon",
        locale=None,
        width=w,
        height=h,
        format="PNG 24-bit (no alpha)",
        note="Shown at 512x512 in the store listing; Play applies the final mask.",
    )

    graphic_slots = {
        "feature": ("Feature graphic", "feature-graphic", 1024, 500),
        "promo": ("Promo graphic", "promo-graphic", 180, 120),
    }
    for locale in LOCALES:
        suffix = LOCALE_SUFFIX[locale]
        for key, (slot, stem, gw, gh) in graphic_slots.items():
            add(
                id=f"{key}-graphic-{locale}",
                kind=f"{key}-graphic",
                path=f"art/{stem}{suffix}.png",
                source=f"art/templates/{stem}.svg",
                play_slot=slot,
                locale=locale,
                width=gw,
                height=gh,
                format="PNG",
            )

    for device, meta in DEVICES.items():
        slot = meta["slot"]
        suffix = LOCALE_DIR_SUFFIX
        for locale in LOCALES:
            out_dir = f"art/screenshots/{device}/framed{suffix[locale]}" if device != "phone" else f"art/screenshots/framed{suffix[locale]}"
            for index, screen in enumerate(SCREENS, start=1):
                path = Path(out_dir) / f"{index:02d}-{screen}.png"
                if not (REPO / path).exists():
                    continue
                add(
                    id=f"screenshot-{device}-{screen}-{locale}",
                    kind="screenshot",
                    screen=screen,
                    path=str(path).replace("\\", "/"),
                    source=f"art/screenshots/raw/{device}/{locale}/{index:02d}-{screen}.png",
                    play_slot=slot,
                    locale=locale,
                    width=meta["framed_size"][0],
                    height=meta["framed_size"][1],
                    format="PNG 24-bit (no alpha)",
                )
            triptych = Path(out_dir) / "triptych.png"
            if triptych.exists():
                add(
                    id=f"screenshot-{device}-triptych-{locale}",
                    kind="screenshot",
                    screen="triptych",
                    path=str(triptych).replace("\\", "/"),
                    source=f"art/screenshots/raw/{device}/{locale}/ (01-home, 04-planner, 06-map)",
                    play_slot=slot,
                    locale=locale,
                    width=meta["framed_size"][0],
                    height=meta["framed_size"][1],
                    format="PNG 24-bit (no alpha)",
                    note="One image combining three screens side by side.",
                )

    for locale in LOCALES:
        suffix = LOCALE_SUFFIX[locale]
        short = data[locale]["short_description"].strip()
        full = data[locale]["full_description"].strip()
        add(
            id=f"short-description-{locale}",
            kind="description",
            path=f"art/play-store-short-description{suffix}.txt",
            source=f"art/i18n/{locale}.json",
            play_slot="Short description",
            locale=locale,
            max_chars=80,
            chars=len(short),
        )
        add(
            id=f"full-description-{locale}",
            kind="description",
            path=f"art/play-store-full-description{suffix}.txt",
            source=f"art/i18n/{locale}.json",
            play_slot="Full description",
            locale=locale,
            max_chars=4000,
            chars=len(full),
        )
        add(
            id=f"release-notes-{locale}",
            kind="release-notes",
            path="art/play-store-listing.csv",
            source=f"art/i18n/{locale}.json",
            play_slot="Release notes",
            locale=locale,
            max_chars=500,
            chars=len(data[locale].get("release_notes", "").strip()),
        )

    add(
        id="listing-csv",
        kind="listing",
        path="art/play-store-listing.csv",
        source="art/i18n/*.json",
        play_slot="All listing text (title, short, full, release notes)",
        locale=None,
        columns=["locale", "app_title", "short_description", "full_description", "release_notes"],
        format="UTF-8 CSV",
    )

    return {
        "generated_by": "art/generate.py",
        "app": APP_NAME,
        "package": "com.glossostudio.transitos",
        "locales": {loc: data[loc]["play_locale"] for loc in LOCALES},
        "play_slots": [
            "App icon",
            "Feature graphic",
            "Promo graphic",
            "Phone screenshots",
            "7-inch tablet screenshots",
            "10-inch tablet screenshots",
            "Short description",
            "Full description",
        ],
        "assets": assets,
    }


# --------------------------------------------------------------------------- #
# Verification
# --------------------------------------------------------------------------- #
def verify() -> int:
    data = {loc: json.loads((I18N / f"{loc}.json").read_text(encoding="utf-8")) for loc in LOCALES}
    errors: list[str] = []

    expected = [("art/icon-transitos.png", 512, 512, True)]
    for locale in LOCALES:
        suffix = LOCALE_SUFFIX[locale]
        expected.append((f"art/feature-graphic{suffix}.png", 1024, 500, True))
        expected.append((f"art/promo-graphic{suffix}.png", 180, 120, True))
        for device, meta in DEVICES.items():
            base = meta["out"]
            rel = base.relative_to(ART)
            out_dir = ART / rel / f"framed{LOCALE_DIR_SUFFIX[locale]}"
            for index, screen in enumerate(SCREENS, start=1):
                raw = RAW / device / locale / f"{index:02d}-{screen}.png"
                # Alerts depend on live FGV incidents and are skipped when there
                # are none, so they are only required when a raw capture exists.
                if screen == "alerts" and not raw.exists():
                    continue
                expected.append(
                    (str(out_dir.relative_to(REPO) / f"{index:02d}-{screen}.png"), meta["framed_size"][0], meta["framed_size"][1], True)
                )
            expected.append(
                (str(out_dir.relative_to(REPO) / "triptych.png"), meta["framed_size"][0], meta["framed_size"][1], True)
            )

    for rel, ew, eh, want_opaque in expected:
        path = REPO / rel
        if not path.exists():
            errors.append(f"missing {rel}")
            continue
        w, h, opaque = identify(path)
        if (w, h) != (ew, eh):
            errors.append(f"{rel}: expected {ew}x{eh}, got {w}x{h}")
        if want_opaque and not opaque:
            errors.append(f"{rel}: still has an alpha channel")

    for locale in LOCALES:
        short = data[locale]["short_description"]
        full = data[locale]["full_description"]
        notes = data[locale].get("release_notes", "")
        title = data[locale].get("store_title", APP_NAME)
        if len(title) > 30:
            errors.append(f"{locale} title too long ({len(title)})")
        if len(short) > 80:
            errors.append(f"{locale} short description too long ({len(short)})")
        if len(full) > 4000:
            errors.append(f"{locale} full description too long ({len(full)})")
        if notes and len(notes) > 500:
            errors.append(f"{locale} release notes too long ({len(notes)})")

    listing_csv = ART / "play-store-listing.csv"
    if not listing_csv.exists():
        errors.append("missing art/play-store-listing.csv")
    else:
        import csv

        rows = list(csv.DictReader(listing_csv.open(encoding="utf-8")))
        if [r["locale"] for r in rows] != [data[loc]["play_locale"] for loc in LOCALES]:
            errors.append("art/play-store-listing.csv locales do not match")

    manifest_path = ART / "play-store.manifest.json"
    if not manifest_path.exists():
        errors.append("missing art/play-store.manifest.json")

    if errors:
        print("FAILED:")
        for error in errors:
            print("  -", error)
        return 1
    print(f"OK: {len(expected)} image assets verified, descriptions within limits")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--graphics", action="store_true", help="icon, feature/promo graphics and descriptions")
    parser.add_argument("--screenshots", action="store_true", help="framed screenshots only")
    parser.add_argument("--check", action="store_true", help="verify existing outputs")
    args = parser.parse_args()

    if args.check:
        return verify()

    require_tools()
    do_all = not (args.graphics or args.screenshots)
    data = {loc: json.loads((I18N / f"{loc}.json").read_text(encoding="utf-8")) for loc in LOCALES}

    if do_all or args.graphics:
        print("icon ...")
        build_icon()
        print("feature graphics ...")
        build_feature_graphic(data)
        print("promo graphics ...")
        build_promo_graphic(data)
        print("descriptions ...")
        build_copy(data)
        print("listing csv ...")
        build_listing_csv(data)

    if do_all or args.screenshots:
        print("screenshots ...")
        build_screenshots()
        print("triptychs ...")
        build_triptychs()

    manifest = build_manifest(data, {}, {})
    write(ART / "play-store.manifest.json", json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
    print("manifest -> art/play-store.manifest.json")
    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
