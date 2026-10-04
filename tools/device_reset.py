#!/usr/bin/env python3
"""
Device hygiene for the WO-006 device runs (TASK-042, H-1, DA-108, design section 1).

Usage:
    python tools/device_reset.py --serial S                  reset to native, then verify
    python tools/device_reset.py --serial S --check          verify only
    python tools/device_reset.py --serial S --display SPEC   apply one display spec and verify it (fallback only)

Resets and verifies every global WO-006 touches: display size and density, font_scale, accelerometer_rotation,
user_rotation, the navigation-mode overlay (API 29+), airplane mode (per API level) and, below API 30, the radios.

Exit codes: 0 clean; 1 leftovers (printed, one per line, exactly what was found); 2 cannot reach the device,
unknown AVD, or a baseline constant that is not filled yet (the message names it). Stdlib only.

A non-clean run after a device step voids that step's results (DA-108).

Known limit: on the Phone_API_26 image `svc wifi` is killed (rc 137), so Wi-Fi can be neither cut nor restored
there; reset still tries `svc wifi enable` (harmless), and a Wi-Fi leftover is reported as unresettable.
"""
import argparse
import subprocess
import sys
import time

ADB = r"C:\Users\Jami\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# --- Display specs: must equal the design's DisplaySpec table (designs/WO-006-design.md rev 2, section 1;
# --- the test in tools/tests/test_device_reset.py compares them). name -> (widthDp, heightDp, densityDpi, widthPx, heightPx)
DISPLAY_SPECS = {
    "PHONE_390x844": (390, 844, 400, 975, 2110),
    "PHONE_360x780": (360, 780, 400, 900, 1950),
    "TABLET_1280x800": (1280, 800, 240, 1920, 1200),
    "TABLET_800x1280": (800, 1280, 240, 1200, 1920),
    "TABLET_600x960": (600, 960, 240, 900, 1440),
}

# --- Navigation-mode baseline, a per-AVD CONSTANT (design section 1, E4): never read from the device under check.
# --- Values of Settings.Secure "navigation_mode": 0 = three-button, 1 = two-button, 2 = gestural.
# --- NO_OVERLAY = the AVD has no navigation-mode overlay handling (API below 29).
NO_OVERLAY = "NO_OVERLAY"
# (None would mean "not filled": the script then exits 2 on that AVD at API 29+.)
# Read from the clean AVD by the orchestrator (live round): gestural overlay enabled.
NAV_MODE_BASELINE_API37 = 2

# AVD name -> navigation-mode baseline constant. An AVD missing here is unknown (exit 2).
AVD_NAV_BASELINE = {
    "Medium_Phone_API_37.0": NAV_MODE_BASELINE_API37,
    "Phone_API_26": NO_OVERLAY,
}
# Rotation baselines per AVD, read from the clean AVDs: (accelerometer_rotation, user_rotation).
# An unset user_rotation ("null") equals 0 (the platform default); an unset font_scale equals 1.0.
AVD_ROTATION_BASELINE = {
    "Medium_Phone_API_37.0": (1, 0),
    "Phone_API_26": (1, 0),
}
NAV_BASELINE_CONSTANT_NAME = {
    "Medium_Phone_API_37.0": "NAV_MODE_BASELINE_API37",
    "Phone_API_26": "(none; NO_OVERLAY)",
}

NAV_CATEGORY = {
    0: "com.android.internal.systemui.navbar.threebutton",
    1: "com.android.internal.systemui.navbar.twobutton",
    2: "com.android.internal.systemui.navbar.gestural",
}


class DeviceUnreachable(Exception):
    pass


class ConfigError(Exception):
    """Unknown AVD or an unfilled baseline constant (exit 2)."""


def real_runner(serial):
    """Returns runner(args) -> (returncode, stdout); args are the adb arguments after '-s SERIAL'."""
    def run(args):
        try:
            p = subprocess.run([ADB, "-s", serial] + list(args), capture_output=True, text=True,
                               encoding="utf-8", errors="replace", timeout=60)
        except (OSError, subprocess.TimeoutExpired) as e:
            raise DeviceUnreachable(f"adb failed: {e}")
        return p.returncode, p.stdout
    return run


class Device:
    def __init__(self, runner):
        self.runner = runner

    def shell(self, *args):
        """Runs `adb shell <args>`; returns stdout stripped. A non-zero adb exit raises DeviceUnreachable."""
        rc, out = self.runner(["shell"] + list(args))
        if rc != 0:
            raise DeviceUnreachable(f"adb shell {' '.join(args)} failed (exit {rc})")
        return out.strip()

    def shell_soft(self, *args):
        """Like shell, but a failure is ignored (commands that may be refused)."""
        try:
            rc, out = self.runner(["shell"] + list(args))
        except DeviceUnreachable:
            return ""
        return out.strip() if rc == 0 else ""

    def sdk(self):
        out = self.shell("getprop", "ro.build.version.sdk")
        try:
            return int(out)
        except ValueError:
            raise DeviceUnreachable(f"cannot read the SDK level (getprop gave {out!r})")

    def avd_name(self):
        rc, out = self.runner(["emu", "avd", "name"])
        if rc == 0:
            for line in out.splitlines():
                line = line.strip()
                if line and line != "OK":
                    return line
        name = self.shell("getprop", "ro.boot.qemu.avd_name")
        if not name:
            raise DeviceUnreachable("cannot read the AVD name (emu avd name and getprop ro.boot.qemu.avd_name)")
        return name


def parse_override(text, word):
    """From `wm size` / `wm density` output: the 'Override <word>:' value, or None when there is none."""
    for line in text.splitlines():
        line = line.strip()
        if line.lower().startswith(f"override {word}:"):
            return line.split(":", 1)[1].strip()
    return None


def legacy_airplane(sdk):
    return sdk < 30


# ---------------------------------------------------------------------------------------------------- reset

NAV_SETTLE_SECONDS = 15   # the overlay switch is asynchronous (seen live on API 37: the setting lags by seconds)


def wait_nav_mode(dev, want, seconds=None, sleep=time.sleep):
    """Polls navigation_mode until it equals want or the time is up (the verify then reports a mismatch)."""
    deadline = NAV_SETTLE_SECONDS if seconds is None else seconds
    waited = 0.0
    while dev.shell_soft("settings", "get", "secure", "navigation_mode") != str(want) and waited < deadline:
        sleep(0.5)
        waited += 0.5


def reset(dev, sdk, nav_baseline, rot):
    """Resets everything to native. Individual commands may fail soft; the verify afterwards is the judge."""
    dev.shell_soft("wm", "size", "reset")
    dev.shell_soft("wm", "density", "reset")
    dev.shell_soft("settings", "put", "system", "font_scale", "1.0")
    dev.shell_soft("settings", "put", "system", "accelerometer_rotation", str(rot[0]))
    dev.shell_soft("settings", "put", "system", "user_rotation", str(rot[1]))
    if sdk >= 29 and nav_baseline != NO_OVERLAY:
        # skip when already at the baseline: the overlay switch churns System UI for about 6 s (CR-5 N10)
        if dev.shell_soft("settings", "get", "secure", "navigation_mode") != str(nav_baseline):
            dev.shell_soft("cmd", "overlay", "enable-exclusive", "--category", NAV_CATEGORY[nav_baseline])
            wait_nav_mode(dev, nav_baseline)
    if legacy_airplane(sdk):
        dev.shell_soft("settings", "put", "global", "airplane_mode_on", "0")
        dev.shell_soft("am", "broadcast", "-a", "android.intent.action.AIRPLANE_MODE")   # may be refused; ignored
        dev.shell_soft("svc", "wifi", "enable")
        dev.shell_soft("svc", "data", "enable")
    else:
        dev.shell_soft("cmd", "connectivity", "airplane-mode", "disable")


def apply_display(dev, spec):
    _, _, density, wpx, hpx = DISPLAY_SPECS[spec]
    dev.shell_soft("wm", "size", f"{wpx}x{hpx}")
    dev.shell_soft("wm", "density", str(density))


# ---------------------------------------------------------------------------------------------------- verify

def check_display(dev):
    out = []
    size = parse_override(dev.shell("wm", "size"), "size")
    if size is not None:
        out.append(f"display size override: {size} (native expected)")
    dens = parse_override(dev.shell("wm", "density"), "density")
    if dens is not None:
        out.append(f"display density override: {dens} (native expected)")
    return out


def check_display_spec(dev, spec):
    _, _, density, wpx, hpx = DISPLAY_SPECS[spec]
    out = []
    size = parse_override(dev.shell("wm", "size"), "size")
    if size != f"{wpx}x{hpx}":
        out.append(f"display size: wanted override {wpx}x{hpx}, got {size if size else 'none (native)'}")
    dens = parse_override(dev.shell("wm", "density"), "density")
    if dens != str(density):
        out.append(f"display density: wanted override {density}, got {dens if dens else 'none (native)'}")
    return out


def _get(dev, namespace, key):
    return dev.shell("settings", "get", namespace, key)


def _is_float(text, want):
    try:
        return abs(float(text) - want) < 1e-6
    except ValueError:
        return False


def check_settings(dev, sdk, nav_baseline, rot):
    out = []
    fs = _get(dev, "system", "font_scale")
    if fs != "null" and not _is_float(fs, 1.0):
        out.append(f"font_scale: {fs} (baseline 1.0)")
    for key, base in (("accelerometer_rotation", rot[0]), ("user_rotation", rot[1])):
        v = _get(dev, "system", key)
        # null = unset = platform default; clean only where the baseline is that default (user_rotation 0)
        if v != str(base) and not (v == "null" and key == "user_rotation" and base == 0):
            out.append(f"{key}: {v} (baseline {base})")
    if sdk >= 29 and nav_baseline != NO_OVERLAY:
        v = _get(dev, "secure", "navigation_mode")
        if v != str(nav_baseline):
            out.append(f"navigation_mode: {v} (baseline {nav_baseline})")
    air = _get(dev, "global", "airplane_mode_on")
    if air not in ("0", "null"):   # null = unset = platform default 0
        out.append(f"airplane_mode_on: {air} (baseline 0)")
    if legacy_airplane(sdk):
        wifi = _get(dev, "global", "wifi_on")
        if wifi == "0":
            out.append("wifi radio: off (baseline on) (cannot be reset on this image)")
        data = _get(dev, "global", "mobile_data")
        if data == "0":
            out.append("mobile data radio: off (baseline on)")
    return out


def verify(dev, sdk, nav_baseline, rot):
    return check_display(dev) + check_settings(dev, sdk, nav_baseline, rot)


# ---------------------------------------------------------------------------------------------------- main

def resolve_baseline(avd):
    if avd not in AVD_NAV_BASELINE:
        raise ConfigError(f"unknown AVD {avd!r}: no entry in AVD_NAV_BASELINE (add the AVD and its "
                          f"navigation-mode baseline constant)")
    return AVD_NAV_BASELINE[avd]


def run(serial, check_only=False, display=None, runner=None, out=print):
    """Returns the exit code."""
    dev = Device(runner or real_runner(serial))
    try:
        sdk = dev.sdk()
        avd = dev.avd_name()
        nav = resolve_baseline(avd)
        if avd not in AVD_ROTATION_BASELINE:
            raise ConfigError(f"AVD {avd!r} has no entry in AVD_ROTATION_BASELINE")
        rot = AVD_ROTATION_BASELINE[avd]
        if sdk >= 29 and nav is None:
            raise ConfigError(f"baseline constant {NAV_BASELINE_CONSTANT_NAME.get(avd, '?')} for AVD {avd!r} is not "
                              f"filled yet (record the clean device's navigation_mode from the live probe)")
        if display is not None:
            apply_display(dev, display)
            leftovers = check_display_spec(dev, display)
            what = f"display {display}"
        else:
            if not check_only:
                reset(dev, sdk, nav, rot)
            leftovers = verify(dev, sdk, nav, rot)
            what = "native baseline"
    except DeviceUnreachable as e:
        out(f"FAIL cannot reach device {serial}: {e}")
        return 2
    except ConfigError as e:
        out(f"FAIL {e}")
        return 2
    if leftovers:
        out(f"FAIL {serial} ({avd}, API {sdk}): {len(leftovers)} leftover(s) vs {what}")
        for item in leftovers:
            out("  " + item)
        return 1
    out(f"PASS {serial} ({avd}, API {sdk}): clean ({what})")
    return 0


def main(argv):
    ap = argparse.ArgumentParser(description="Reset and verify the device state for WO-006 device runs.")
    ap.add_argument("--serial", required=True)
    ap.add_argument("--check", action="store_true", help="verify only")
    ap.add_argument("--display", choices=sorted(DISPLAY_SPECS), help="apply one display spec and verify it")
    a = ap.parse_args(argv)
    if a.check and a.display:
        print("FAIL --check and --display cannot be combined")
        return 2
    return run(a.serial, check_only=a.check, display=a.display)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
