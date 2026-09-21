#!/usr/bin/env python3
"""Capture the raw TransitOS screenshots from running Android emulators.

The script drives the app through ``adb`` (UI Automator for element lookup, so
it is locale and device independent), seeds a deterministic set of favourites
and saved routes, and writes one PNG per screen and locale to
``art/screenshots/raw/<device>/<locale>/NN-<screen>.png``.

Run ``art/generate.py --screenshots`` afterwards to frame them.

Devices are matched by AVD name, not by serial:

    phone    -> the AVD named ``duo``          (1080x1920)
    tablet7  -> the AVD named ``tablet7``      (1200x1920)
    tablet10 -> the AVD named ``tablet10``     (1600x2560, forced portrait)

Usage:
    python3 art/capture-screenshots.py                  # every running device
    python3 art/capture-screenshots.py --device phone --locale es
    python3 art/capture-screenshots.py --screen home --screen map
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

ART = Path(__file__).resolve().parent
RAW = ART / "screenshots" / "raw"
PKG = "com.glossostudio.transitos"

# Preferred AVD names per device, in priority order. The dedicated
# ``transitos_*`` AVDs exist so captures are not disturbed by other workloads
# sharing the default emulators; the stock names are accepted as a fallback.
AVD_CANDIDATES = {
    "phone": ["transitos_phone", "duo"],
    "tablet7": ["transitos_tab7", "tablet7"],
    "tablet10": ["transitos_tab10", "tablet10"],
}

SCREENS = ("home", "alerts", "search", "planner", "settings", "map", "dark", "amoled")
LOCALES = ("en", "es", "ca")

UI = {
    "en": {
        "home": "Home",
        "search": "Search",
        "planner": "Plan",
        "map": "Map",
        "settings": "Settings",
        "home_ready": "NEXT DEPARTURE",
        "search_placeholder": "Station, line or destination",
        "alerts_header": "Service alerts",
        "planner_ready": "Departs ",
        "route_label": "Home to airport",
    },
    "es": {
        "home": "Inicio",
        "search": "Buscar",
        "planner": "Planificar",
        "map": "Mapa",
        "settings": "Ajustes",
        "home_ready": "PRÓXIMA SALIDA",
        "search_placeholder": "Estación, línea o destino",
        "alerts_header": "Avisos del servicio",
        "planner_ready": "Sale ",
        "route_label": "Casa al aeropuerto",
    },
    "ca": {
        "home": "Inici",
        "search": "Buscar",
        "planner": "Planificar",
        "map": "Mapa",
        "settings": "Ajustos",
        "home_ready": "PRÒXIMA EIXIDA",
        "search_placeholder": "Estació, línia o destí",
        "alerts_header": "Avisos del servei",
        "planner_ready": "Eixida ",
        "route_label": "Casa a l'aeroport",
    },
}

# Benimaclet (mv:12) and Colón (mv:15) are favourites; the saved route goes
# from Benimaclet to the airport (mv:121).
FAVORITES = ["mv:12", "mv:15"]


# --------------------------------------------------------------------------- #
# Protobuf helpers for the Preferences DataStore
# --------------------------------------------------------------------------- #
def varint(n: int) -> bytes:
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        out.append(b | 0x80 if n else b)
        if not n:
            return bytes(out)


def key(field: int, wire: int) -> bytes:
    return varint((field << 3) | wire)


def length_delimited(field: int, data: bytes) -> bytes:
    return key(field, 2) + varint(len(data)) + data


def value_string(text: str) -> bytes:
    return length_delimited(5, text.encode("utf-8"))


def value_string_set(items: list[str]) -> bytes:
    return length_delimited(6, b"".join(length_delimited(1, i.encode("utf-8")) for i in items))


def preferences_pb(entries: list[tuple[str, bytes]]) -> bytes:
    out = bytearray()
    for name, value in entries:
        entry = length_delimited(1, name.encode("utf-8")) + length_delimited(2, value)
        out += length_delimited(1, entry)
    return bytes(out)


# --------------------------------------------------------------------------- #
# adb driver
# --------------------------------------------------------------------------- #
class Device:
    def __init__(self, serial: str, device: str) -> None:
        self.serial = serial
        self.device = device

    def adb(self, *args: str, binary: bool = False) -> bytes | str:
        proc = subprocess.run(
            ["adb", "-s", self.serial, *args],
            capture_output=not binary,
            stdout=subprocess.PIPE if binary else None,
        )
        if binary:
            return proc.stdout
        assert proc.stdout is not None
        return proc.stdout.decode("utf-8", "replace")

    def shell(self, *args: str) -> str:
        return self.adb("shell", *args)  # type: ignore[return-value]

    def nodes(self) -> list[dict]:
        self.shell("uiautomator", "dump", "/sdcard/ui.xml")
        xml = self.shell("cat", "/sdcard/ui.xml")
        result = []
        for match in re.finditer(r"<node[^>]*?/?>", xml):
            tag = match.group(0)
            bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
            if not bounds:
                continue
            text = re.search(r'text="([^"]*)"', tag)
            desc = re.search(r'content-desc="([^"]*)"', tag)
            x1, y1, x2, y2 = (int(v) for v in bounds.groups())
            result.append(
                {
                    "text": text.group(1) if text else "",
                    "desc": desc.group(1) if desc else "",
                    "b": (x1, y1, x2, y2),
                }
            )
        return result

    def find(self, *, text: str | None = None, desc: str | None = None, min_y: int = 0):
        for node in self.nodes():
            if text is not None and node["text"] != text:
                continue
            if desc is not None and node["desc"] != desc:
                continue
            x1, y1, x2, y2 = node["b"]
            if y2 >= min_y:
                return node
        return None

    def tap(self, x: int, y: int) -> None:
        self.shell("input", "tap", str(x), str(y))

    def tap_node(self, node: dict) -> None:
        x1, y1, x2, y2 = node["b"]
        self.tap((x1 + x2) // 2, (y1 + y2) // 2)

    def screen_size(self) -> tuple[int, int]:
        out = self.shell("wm", "size")
        match = re.search(r"(\d+)x(\d+)", out)
        assert match, out
        return int(match.group(1)), int(match.group(2))

    def capture_size(self) -> tuple[int, int]:
        import struct

        data = self.adb("exec-out", "screencap", "-p", binary=True)
        # PNG IHDR: width at byte 16, height at byte 20, both big-endian.
        return struct.unpack(">II", data[16:24])  # type: ignore[arg-type]

    def await_text(self, text: str, timeout: float = 90.0) -> bool:
        deadline = time.time() + timeout
        while time.time() < deadline:
            if self.find(text=text):
                return True
            time.sleep(1.5)
        return False

    def capture(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        data = self.adb("exec-out", "screencap", "-p", binary=True)
        path.write_bytes(data)  # type: ignore[arg-type]


# --------------------------------------------------------------------------- #
# Capture flow
# --------------------------------------------------------------------------- #
def device_serial_for(avd: str) -> str | None:
    out = subprocess.run(["adb", "devices"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if "\t" not in line:
            continue
        serial = line.split("\t")[0]
        if not serial.startswith("emulator-"):
            continue
        name = subprocess.run(
            ["adb", "-s", serial, "emu", "avd", "name"], capture_output=True, text=True
        ).stdout.strip().splitlines()
        if name and name[0].strip() == avd:
            return serial
    return None


def set_orientation(dev: Device) -> None:
    """Force portrait regardless of the AVD's natural orientation."""
    dev.shell("settings", "put", "system", "accelerometer_rotation", "0")
    for rotation in ("0", "1", "3"):
        dev.shell("settings", "put", "system", "user_rotation", rotation)
        time.sleep(1.5)
        width, height = dev.capture_size()
        if height > width:
            return


def push_seed(dev: Device, locale: str, theme: str | None = None) -> None:
    ui = UI[locale]
    route = json.dumps(
        [
            {
                "id": "screenshot-route-1",
                "originStopId": "mv:12",
                "destinationStopId": "mv:121",
                "label": ui["route_label"],
            }
        ],
        separators=(",", ":"),
    )
    entries: list[tuple[str, bytes]] = [
        ("favorite_stop_ids", value_string_set(FAVORITES)),
        ("favorite_routes", value_string(route)),
    ]
    if theme:
        entries.append(("app_theme", value_string(theme)))
    seed = preferences_pb(entries)
    local = "/data/local/tmp/transitos-seed.pb"
    with open("/tmp/transitos-seed.pb", "wb") as handle:
        handle.write(seed)
    dev.adb("push", "/tmp/transitos-seed.pb", local)
    dev.shell("run-as", PKG, "mkdir", "-p", "files/datastore")
    dev.shell(
        "run-as",
        PKG,
        "sh",
        "-c",
        f"'cat {local} > files/datastore/transitos.preferences_pb'",
    )
    if theme:
        # MainActivity reads this synchronously for the very first frame.
        xml = (
            "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
            "<map>\n"
            f"    <string name=\"app_theme\">{theme}</string>\n"
            "</map>\n"
        )
        with open("/tmp/transitos-prefs.xml", "w") as handle:
            handle.write(xml)
        dev.adb("push", "/tmp/transitos-prefs.xml", "/data/local/tmp/transitos-prefs.xml")
        dev.shell(
            "run-as",
            PKG,
            "sh",
            "-c",
            "'cat /data/local/tmp/transitos-prefs.xml > shared_prefs/transitos_prefs.xml'",
        )


def prepare(dev: Device, locale: str) -> None:
    dev.shell("input", "keyevent", "KEYCODE_WAKEUP")
    dev.shell("wm", "dismiss-keyguard")
    set_orientation(dev)
    dev.shell("cmd", "uimode", "night", "no")
    dev.shell("am", "force-stop", PKG)
    dev.shell("pm", "clear", PKG)
    dev.shell("cmd", "locale", "set-app-locales", PKG, "--user", "0", "--locales", locale)
    push_seed(dev, locale)


def launch(dev: Device) -> None:
    dev.shell("am", "force-stop", PKG)
    dev.shell("am", "start", "-n", f"{PKG}/.MainActivity")


def nav(dev: Device, ui_label: str, min_y: int) -> None:
    node = dev.find(text=ui_label, min_y=min_y)
    if not node:
        raise RuntimeError(f"cannot find '{ui_label}'")
    dev.tap_node(node)
    time.sleep(2.5)


def capture_home(dev: Device, locale: str, out: Path) -> None:
    launch(dev)
    if not dev.await_text(UI[locale]["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale}")
    time.sleep(2)
    dev.capture(out / "01-home.png")


def capture_alerts(dev: Device, locale: str, out: Path) -> bool:
    ui = UI[locale]
    launch(dev)
    if not dev.await_text(ui["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale}")
    if not dev.find(text=ui["alerts_header"]):
        # Not visible yet: scroll the home feed to the bottom.
        width, height = dev.screen_size()
        for _ in range(5):
            dev.shell("input", "swipe", str(width // 2), str(int(height * 0.75)), str(width // 2), str(int(height * 0.30)), "350")
            time.sleep(0.8)
            if dev.find(text=ui["alerts_header"]):
                break
    if not dev.find(text=ui["alerts_header"]):
        print(f"  ! no service alerts live for {locale}; skipping alerts capture")
        return False
    time.sleep(1.5)
    dev.capture(out / "02-alerts.png")
    return True


def capture_search(dev: Device, locale: str, out: Path) -> None:
    ui = UI[locale]
    launch(dev)
    if not dev.await_text(ui["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale}")
    _, height = dev.screen_size()
    nav(dev, ui["search"], int(height * 0.88))
    field = dev.find(text=ui["search_placeholder"])
    if not field:
        raise RuntimeError("search field not found")
    dev.tap_node(field)
    time.sleep(1.5)
    dev.shell("input", "text", "ben")
    time.sleep(2.5)
    dev.shell("input", "keyevent", "111")
    time.sleep(1.5)
    dev.capture(out / "03-search.png")


def capture_planner(dev: Device, locale: str, out: Path) -> None:
    ui = UI[locale]
    launch(dev)
    if not dev.await_text(ui["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale}")
    edit = dev.find(desc="Renombrar ruta") or dev.find(desc="Rename route") or dev.find(desc="Canviar nom")
    if not edit:
        raise RuntimeError("saved route card not found")
    width, _ = dev.screen_size()
    _, y1, _, y2 = edit["b"]
    dev.tap(int(width * 0.38), (y1 + y2) // 2)
    ready = ui["planner_ready"]
    deadline = time.time() + 60
    while time.time() < deadline:
        if any(n["text"].startswith(ready) for n in dev.nodes()):
            break
        time.sleep(1.5)
    time.sleep(2)
    dev.capture(out / "04-planner.png")


def capture_settings(dev: Device, locale: str, out: Path) -> None:
    ui = UI[locale]
    launch(dev)
    if not dev.await_text(ui["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale}")
    gear = dev.find(desc=ui["settings"])
    if not gear:
        raise RuntimeError("settings button not found")
    dev.tap_node(gear)
    time.sleep(2.5)
    dev.capture(out / "05-settings.png")


def capture_map(dev: Device, locale: str, out: Path) -> None:
    ui = UI[locale]
    launch(dev)
    if not dev.await_text(ui["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale}")
    _, height = dev.screen_size()
    nav(dev, ui["map"], int(height * 0.88))
    time.sleep(14)
    dev.capture(out / "06-map.png")


def capture_theme(dev: Device, locale: str, out: Path, mode: str, filename: str) -> None:
    ui = UI[locale]
    dev.shell("am", "force-stop", PKG)
    push_seed(dev, locale, theme=mode)
    launch(dev)
    if not dev.await_text(ui["home_ready"], timeout=120):
        raise RuntimeError(f"home never became ready for {locale} ({mode})")
    time.sleep(2.5)
    dev.capture(out / filename)


CAPTURERS = {
    "home": capture_home,
    "alerts": lambda dev, locale, out: capture_alerts(dev, locale, out),
    "search": capture_search,
    "planner": capture_planner,
    "settings": capture_settings,
    "map": capture_map,
    "dark": lambda dev, locale, out: capture_theme(dev, locale, out, "DARK", "07-dark.png"),
    "amoled": lambda dev, locale, out: capture_theme(dev, locale, out, "AMOLED", "08-amoled.png"),
}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--device", choices=["phone", "tablet7", "tablet10"], action="append")
    parser.add_argument("--locale", choices=list(LOCALES), action="append")
    parser.add_argument("--screen", choices=list(SCREENS), action="append")
    args = parser.parse_args()

    devices = args.device or ["phone", "tablet7", "tablet10"]
    locales = args.locale or list(LOCALES)
    screens = args.screen or list(SCREENS)

    found = {}
    for device, candidates in AVD_CANDIDATES.items():
        if device not in devices:
            continue
        serial = next((device_serial_for(a) for a in candidates if device_serial_for(a)), None)
        if serial:
            found[device] = serial
        else:
            print(f"! no running AVD for {device} (tried: {', '.join(candidates)}); skipping")

    if not found:
        sys.exit("error: no matching emulators are running")

    failures = []
    for device, serial in found.items():
        dev = Device(serial, device)
        for locale in locales:
            out = RAW / device / locale
            print(f"[{device}/{locale}] preparing ...")
            try:
                prepare(dev, locale)
            except Exception as exc:  # noqa: BLE001
                failures.append(f"{device}/{locale} prepare: {exc}")
                continue
            for screen in screens:
                print(f"[{device}/{locale}] capture {screen}")
                try:
                    CAPTURERS[screen](dev, locale, out)
                except Exception as exc:  # noqa: BLE001
                    print(f"  x {screen}: {exc}")
                    failures.append(f"{device}/{locale}/{screen}: {exc}")

    if failures:
        print("\nfailures:")
        for failure in failures:
            print("  -", failure)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
