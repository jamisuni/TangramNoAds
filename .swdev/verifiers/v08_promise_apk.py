#!/usr/bin/env python3
"""
V-08: the APK keeps the promise (G-01, REQ-001 A1, REQ-008 A1, REQ-010 A2; design WO-006 section 6 item 2, DA-104).

Usage:
    python v08_promise_apk.py [--apk PATH] [--expect-debug] [--project-root DIR] [--aapt2 PATH]

Without --apk it builds its own APK (release; debug with --expect-debug) with V-04's build function and scans it,
so it can never pass an old APK. With --apk it prints the file's sha256 and mtime and refuses (exit 2) an APK older
than the newest input file (the files PromiseSourceScanTest declares plus Tangrams/*.json; no git).

Scans:
  * dex: every classes*.dex is read through the header tables string_ids, type_ids and class_defs. A class DEFINED
    (class_defs -> type_ids -> string_ids) whose descriptor starts with a denied SDK prefix is a finding. A type only
    REFERENCED (java.net.Socket, WebView, from androidx.core) is never a finding. No allow-list.
  * LocaleOverrideActivity, TestConfig and FeedbackProbe must not be defined in release; with --expect-debug all
    three must be defined.
  * feedback callers (WO-007 DA-123/DA-125, via dex_callers.py): on EVERY APK, every caller of a platform sound or
    haptic API (see dex_callers.is_target) must be in FEEDBACK_ALLOW, the pinned allow-list; any other caller is
    "V-08 FAIL feedback-caller <class>-><method>" (exit 1). If no caller matching an INVENTORY entry is found at all
    the call-site reader is blind: "V-08 FAIL scanner blind feedback-caller ..." (exit 1, like the other canaries).
    A malformed code_item or table is a DexError, exit 2. An allow-list row pins the caller class, the caller method,
    the callee class and the callee method (CR-6 S2): the same caller class calling a different target is a finding.
  * the V-04 canaries (imported from v04_release_apk) must be found, otherwise "scanner blind".
  * aapt2 dump xmltree of AndroidManifest.xml (REQ-010 A2: no permissions, not even network). In RELEASE mode ANY
    uses-permission, uses-permission-sdk-23 or permission element is a finding, and a uses-feature that implies
    network or billing is a finding. In --expect-debug mode the debug manifest may carry androidx's own elements
    (debug is not shipped), so those are printed as "V-08 note" lines, not findings; a name that implies network or
    billing is still a finding. The tree must show at least uses-sdk and application, else exit 2 (format change).

Exit: 0 PASS, 1 findings, 2 cannot read / inconsistent dex table / aapt2 missing / stale APK / build error.
Fail closed: a dex table that is truncated or inconsistent raises DexError and ends in exit 2, never a pass.
Stdlib only.
"""
import hashlib
import os
import re
import struct
import subprocess
import sys
import zipfile
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import v04_release_apk as v04  # noqa: E402
import dex_callers  # noqa: E402

DexError = v04.DexError

# (descriptor prefix without the leading L, kind). One constant; every entry is a promise breach (REQ-001 / REQ-008).
DENIED_SDK = [
    ("com/google/android/gms/", "play-services (ads, analytics, location, auth)"),
    ("com/google/firebase/", "firebase (analytics, crashlytics, messaging)"),
    ("com/google/android/play/", "play core (in-app review, billing, install referrer)"),
    ("com/google/android/ump/", "consent / ad-privacy SDK"),
    ("com/google/mlkit/", "ML Kit (model download, analytics)"),
    ("com/google/android/datatransport/", "transport (analytics and crash upload)"),
    ("androidx/webkit/", "webview library (network-capable)"),
    ("androidx/browser/", "custom tabs / browser (network)"),
    ("androidx/ads/", "ads SDK"),
    ("com/google/ads/", "ads SDK"),
    ("com/android/billingclient/", "billing"),
    ("okhttp3/", "network client"),
    ("com/squareup/okhttp/", "network client"),
    ("retrofit2/", "network client"),
    ("io/ktor/", "network client"),
    ("com/android/volley/", "network client"),
    ("io/grpc/", "network client"),
    ("com/applovin/", "ads SDK"),
    ("com/unity3d/ads/", "ads SDK"),
    ("com/ironsource/", "ads SDK"),
    ("com/mopub/", "ads SDK"),
    ("com/vungle/", "ads SDK"),
    ("com/chartboost/", "ads SDK"),
    ("com/facebook/", "social / ads / analytics SDK"),
    ("com/adjust/", "attribution analytics"),
    ("com/appsflyer/", "attribution analytics"),
    ("io/branch/", "attribution analytics"),
    ("com/flurry/", "analytics"),
    ("com/amplitude/", "analytics"),
    ("com/mixpanel/", "analytics"),
    ("com/segment/", "analytics"),
    ("com/bugsnag/", "crash reporting"),
    ("io/sentry/", "crash reporting"),
    ("org/acra/", "crash reporting"),
]

DEBUG_ONLY = [
    "Lio/github/jamisuni/tangram/LocaleOverrideActivity;",
    "Lio/github/jamisuni/tangram/TestConfig;",
    "Lio/github/jamisuni/tangram/FeedbackProbe;",
]

# Pinned allow-list of feedback callers (design WO-007 "Suggested cut" V-1; DA-123, DA-125, DA-127; CR-6 S2).
# One row per call site: (caller class, caller method, callee class, callee method, status, reason).
# Exact class descriptors; method names compare on the part before the first "-" (Kotlin inline-class mangling,
# "onStart-3MmeM6k" is "onStart"). A call is allowed only when ALL FOUR match: an allowed caller class calling a
# different target, or an allowed method calling another callee, is a finding.
# status "inventory" = the platform's own callers inside Compose and androidx.core, taken from the dex of the WO-007
#   release and debug APKs (identical sets, 25 sites), reasons per row.
# status "WO-007" = the app's own outputs, built and found at those names.
# Never extend this list without a DA row (re-judge DA-125 first).
_F = "Landroidx/compose/foundation/"
_U = "Landroidx/compose/ui/"
_HF = _U + "hapticfeedback/HapticFeedback;"
_SE = _U + "platform/SoundEffect;"
_VIEW = "Landroid/view/View;"
_VC = "Landroidx/core/view/ViewCompat;"
_CCN = _F + "CombinedClickableNode"
_ACN = _F + "AbstractClickableNode;"
_TFSS = _F + "text/input/internal/selection/TextFieldSelectionState"
_SM = _F + "text/selection/SelectionManager;"
_TFSM = _F + "text/selection/TextFieldSelectionManager"
_HAP = "performHapticFeedback"
_TEXT = "inventory: text-selection haptic (cursor and handle drag, select all), no text field exists in release"
FEEDBACK_ALLOW = [
    (_ACN, "performClick", _ACN, "playClickSound", "inventory", "inventory: a clickable's click asks for the click sound"),
    (_ACN, "playClickSound", _SE, "playClickSound", "inventory", "inventory: asks the (lever-silenced) SoundEffect"),
    (_CCN + ";", "handleUpEvent", _CCN + ";", "playClickSound", "inventory", "inventory: combinedClickable up event, click sound"),
    (_CCN + ";", "onClickKeyUpEvent", _CCN + ";", "playClickSound", "inventory", "inventory: keyboard click, sound"),
    (_CCN + "$handleDownEvent$1;", "invokeSuspend", _HF, _HAP, "inventory", "inventory: long-press haptic on down"),
    (_CCN + "$handleDownEvent$2;", "invokeSuspend", _HF, _HAP, "inventory", "inventory: long-press haptic on down"),
    (_CCN + ";", "handleDeepPress", _HF, _HAP, "inventory", "inventory: deep-press haptic (E3)"),
    (_TFSS + "$TextFieldTextDragObserver;", "onStart", _HF, _HAP, "inventory", _TEXT),
    (_TFSS + ";", "detectCursorHandleDragGestures$lambda$3", _HF, _HAP, "inventory", _TEXT),
    (_TFSS + ";", "updateSelection", _HF, _HAP, "inventory", _TEXT),
    (_SM, "onRelease", _HF, _HAP, "inventory", _TEXT),
    (_SM, "selectAllInSelectable$foundation", _HF, _HAP, "inventory", _TEXT),
    (_SM, "updateSelection", _HF, _HAP, "inventory", _TEXT),
    (_TFSM + "$cursorDragObserver$1;", "onDrag", _HF, _HAP, "inventory", _TEXT),
    (_TFSM + "$touchSelectionObserver$1;", "onStart", _HF, _HAP, "inventory", _TEXT),
    (_TFSM + ";", "updateSelection", _HF, _HAP, "inventory", _TEXT),
    (_U + "hapticfeedback/PlatformHapticFeedback;", _HAP, _VC, _HAP, "inventory", "inventory: Compose haptic to ViewCompat"),
    (_U + "platform/DefaultHapticFeedback;", _HAP, _VC, _HAP, "inventory", "inventory: Compose haptic to ViewCompat"),
    (_U + "platform/AndroidComposeView$AndroidComposeViewNavigationSoundEffect;", "invoke", _VIEW, "playSoundEffect", "inventory",
     "inventory: keyboard-focus navigation sound (silenced by the ui flag, DA-125 item 3)"),
    (_U + "platform/AndroidSoundEffect;", "playClickSound", _VIEW, "playSoundEffect", "inventory",
     "inventory: the click sound the lever replaces"),
    (_U + "platform/DelegatingSoundEffect;", "playClickSound", _SE, "playClickSound", "inventory", "inventory: delegate"),
    (_VC, _HAP, _VIEW, _HAP, "inventory", "inventory: the only caller of View.performHapticFeedback besides ViewHapticOut"),
    (_U + "platform/HapticDefaults;", "isPremiumVibratorEnabled", "Landroid/os/Vibrator;", "areAllPrimitivesSupported", "inventory",
     "DA-127: a capability query, plays nothing"),
    ("Lio/github/jamisuni/tangram/settings/ViewHapticOut;", "tick", _VIEW, _HAP, "WO-007", "the lock tick, no permission"),
    ("Lio/github/jamisuni/tangram/settings/AudioTrackSoundOut;", "play", "Landroid/media/AudioTrack;", "play", "WO-007",
     "the app's own five cues; stop and reloadStaticData are not targets"),
]


def _base(name):
    return name.split("-")[0]


def allow_entry(cc, cm, ec, en):
    """The FEEDBACK_ALLOW row that admits the call cc->cm calling ec->en, or None."""
    for e in FEEDBACK_ALLOW:
        if e[0] == cc and _base(e[1]) == _base(cm) and e[2] == ec and _base(e[3]) == _base(en):
            return e
    return None

# Manifest names that imply network or billing (substring match, case-insensitive).
PERMISSION_DENY = ["internet", "network", "wifi", "billing", "vending", "ad_id", "gms", "c2dm", "nfc"]
FEATURE_DENY = ["wifi", "telephony", "bluetooth", "nfc", "billing"]

# Stale-guard input set (same list as app/build.gradle.kts declares for PromiseSourceScanTest).
INPUT_PATTERNS = [
    re.compile(r"^[^/]+/src/main/.+"),
    re.compile(r"^[^/]+/src/release/.+"),
    re.compile(r"^[^/]+/src/debug/.+"),
    re.compile(r"^[^/]+/src/[^/]+/res/values[^/]*/strings\.xml$"),
    re.compile(r"^gradle/libs\.versions\.toml$"),
    re.compile(r"(^|.*/)build\.gradle\.kts$"),
    re.compile(r"^settings\.gradle\.kts$"),
    re.compile(r"^Tangrams/[^/]+\.json$"),
]
PRUNE_DIRS = {"build", ".gradle", ".git"}

DEFAULT_BUILD_TOOLS = Path(r"C:\Users\Jami\AppData\Local\Android\Sdk\build-tools")


def parse_dex_classes(data):
    """Descriptors of the classes DEFINED in a dex (class_defs -> type_ids -> string_ids).
    DexError on any truncated or inconsistent table: bad magic/header, file_size mismatch, a table offset or size
    beyond the file, a string or type index out of range. Never returns a partial answer."""
    strings = v04.parse_dex_strings(data)  # magic, header size, string table bounds
    if struct.unpack_from("<I", data, 0x20)[0] != len(data):
        raise DexError("header file_size does not match the file (truncated or padded)")
    nstr = len(strings)
    ntype, type_off = struct.unpack_from("<II", data, 0x40)
    ndef, def_off = struct.unpack_from("<II", data, 0x60)
    if (ntype and type_off < 0x70) or type_off + ntype * 4 > len(data):
        raise DexError("type_ids table beyond end of file")
    if (ndef and def_off < 0x70) or def_off + ndef * 32 > len(data):
        raise DexError("class_defs table beyond end of file")
    types = []
    for i in range(ntype):
        idx = struct.unpack_from("<I", data, type_off + 4 * i)[0]
        if idx >= nstr:
            raise DexError(f"type {i} descriptor index {idx} out of range")
        types.append(strings[idx])
    out = []
    for i in range(ndef):
        cidx = struct.unpack_from("<I", data, def_off + 32 * i)[0]
        if cidx >= ntype:
            raise DexError(f"class_def {i} class_idx {cidx} out of range")
        out.append(types[cidx])
    return out


def _read_apk(apk):
    """(dexes [(name, strings, defined)], arsc bytes). DexError / BadZipFile / OSError mean exit 2."""
    dexes = []
    arsc = b""
    with zipfile.ZipFile(apk) as z:
        for info in z.infolist():
            name = info.filename
            if v04.DEX_NAME.match(name):
                data = z.read(name)
                defined = parse_dex_classes(data)
                dexes.append((name, v04.parse_dex_strings(data), defined, dex_callers.find_callers(data)))
            elif name == v04.ARSC:
                arsc = z.read(name)
    if not dexes:
        raise DexError("no classes*.dex in the APK (a scan of nothing proves nothing)")
    return dexes, arsc


def scan_dex(apk_path, expect_debug=False):
    """Findings ('V-08 FAIL ...' lines); empty list = clean and the scanner could see. Raises DexError (exit 2)."""
    dexes, arsc = _read_apk(apk_path)
    findings = []
    defined_all = set()
    hits = set()
    for name, _strings, defined, callers in dexes:
        hits |= callers
        for desc in defined:
            defined_all.add(desc)
            for prefix, kind in DENIED_SDK:
                if desc.startswith("L" + prefix):
                    findings.append(f"V-08 FAIL sdk-class [{kind}] {name} {desc}")
                    break
    for desc in DEBUG_ONLY:
        if expect_debug and desc not in defined_all:
            findings.append(f"V-08 FAIL debug-class-missing {desc} is not defined (expect-debug)")
        elif not expect_debug and desc in defined_all:
            findings.append(f"V-08 FAIL debug-class-in-release {desc}")
    seen_inventory = False
    for cc, cm, ec, en in sorted(hits):
        entry = allow_entry(cc, cm, ec, en)
        if entry is None:
            findings.append(f"V-08 FAIL feedback-caller {cc}->{cm}  calls {ec}->{en}")
        elif entry[4] == "inventory":
            seen_inventory = True
    if not seen_inventory:
        findings.append("V-08 FAIL scanner blind feedback-caller: no caller of the pinned inventory found in any dex; "
                        "if the Compose classes were renamed or R8 was intended, re-judge DA-125 and update FEEDBACK_ALLOW")
    strings = [(n, s) for n, ss, _, _ in dexes for s in ss]
    findings.extend(canary_findings(arsc, [s for _, s in strings]))
    return findings


def _has(blob, text):
    return text.encode("utf-8") in blob or text.encode("utf-16-le") in blob


def canary_findings(arsc, dex_strings):
    """'scanner blind' lines for every V-04 canary (v04.CANARIES, public) not found."""
    out = []
    for label, where, needle, exact in v04.CANARIES:
        if where == "dex":
            found = any(s == needle if exact else needle in s for s in dex_strings)
        else:
            found = _has(arsc, needle)
        if not found:
            out.append(f"V-08 FAIL scanner blind {label}; if a rename or R8 was intended, "
                       f"update CANARIES in v04_release_apk.py")
    return out


def find_aapt2():
    """Newest build-tools aapt2 under ANDROID_HOME (or the default SDK), or None."""
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    base = Path(home) / "build-tools" if home else DEFAULT_BUILD_TOOLS
    if not base.is_dir():
        base = DEFAULT_BUILD_TOOLS
    if not base.is_dir():
        return None
    exe = "aapt2.exe" if os.name == "nt" else "aapt2"

    def key(p):
        return [int(x) if x.isdigit() else 0 for x in re.split(r"[.\-]", p.name)]

    for d in sorted((p for p in base.iterdir() if p.is_dir()), key=key, reverse=True):
        if (d / exe).is_file():
            return d / exe
    return None


def parse_xmltree(text):
    """[(element, android:name value)] for uses-permission, uses-permission-sdk-23, uses-feature, permission."""
    out = []
    cur = None
    for line in text.splitlines():
        s = line.strip()
        m = re.match(r"^E: ([\w\-]+)", s)
        if m:
            cur = m.group(1)
            continue
        m = re.match(r'^A: (?:http://schemas\.android\.com/apk/res/android:|android:)name(?:\(0x[0-9a-fA-F]+\))?="([^"]*)"', s)
        if m and cur in ("uses-permission", "uses-permission-sdk-23", "uses-feature", "permission"):
            out.append((cur, m.group(1)))
            cur = None
    return out


def xmltree_elements(text):
    """Set of every element name in an aapt2 xmltree dump."""
    return {m.group(1) for m in (re.match(r"^E: ([\w\-]+)", l.strip()) for l in text.splitlines()) if m}


def permission_report(apk_path, aapt2_path, expect_debug=False):
    """(findings, notes). Raises RuntimeError (exit 2) when aapt2 is missing, fails or its output is not a manifest tree."""
    if not aapt2_path or not Path(aapt2_path).is_file():
        raise RuntimeError(f"aapt2 not found: {aapt2_path}")
    r = subprocess.run([str(aapt2_path), "dump", "xmltree", "--file", "AndroidManifest.xml", str(apk_path)],
                       capture_output=True, text=True, timeout=120)
    if r.returncode != 0 or "E: manifest" not in r.stdout:
        raise RuntimeError(f"aapt2 gave no manifest (exit {r.returncode}): {(r.stdout + r.stderr)[-300:]}")
    missing = {"uses-sdk", "application"} - xmltree_elements(r.stdout)
    if missing:
        raise RuntimeError(f"aapt2 output lacks {sorted(missing)}: the parser no longer sees the manifest")
    findings, notes = [], []
    for element, name in parse_xmltree(r.stdout):
        low = name.lower()
        if element == "uses-feature":
            if any(d in low for d in FEATURE_DENY):
                findings.append(f"V-08 FAIL permission {element} {name}")
        elif expect_debug and not any(d in low for d in PERMISSION_DENY):
            notes.append(f"V-08 note permission {element} {name} (debug build, not shipped)")
        else:
            findings.append(f"V-08 FAIL permission {element} {name}")
    return findings, notes


def scan_permissions(apk_path, aapt2_path, expect_debug=False):
    """Findings only (see permission_report)."""
    return permission_report(apk_path, aapt2_path, expect_debug)[0]


def newest_input(root):
    """(mtime, path) of the newest stale-guard input under root; None if there is none."""
    root = Path(root)
    best = None
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in PRUNE_DIRS]
        for f in filenames:
            p = Path(dirpath) / f
            rel = p.relative_to(root).as_posix()
            if any(pat.match(rel) for pat in INPUT_PATTERNS):
                m = p.stat().st_mtime
                if best is None or m > best[0]:
                    best = (m, p)
    return best


def main(argv):
    apk_arg = None
    root = Path(__file__).resolve().parents[2]
    expect_debug = False
    aapt2 = None
    args = list(argv)
    while args:
        a = args.pop(0)
        if a == "--expect-debug":
            expect_debug = True
        elif a in ("--apk", "--project-root", "--aapt2") and args:
            v = args.pop(0)
            if a == "--apk":
                apk_arg = Path(v)
            elif a == "--aapt2":
                aapt2 = Path(v)
            else:
                root = Path(v)
        else:
            print(__doc__)
            return 2
    try:
        if apk_arg is not None:
            apk = apk_arg
            if not apk.is_file():
                print(f"V-08 ERROR apk not found: {apk}")
                return 2
            mtime = apk.stat().st_mtime
            sha = hashlib.sha256(apk.read_bytes()).hexdigest()
            print(f"V-08 note: {apk} sha256 {sha} mtime {datetime.fromtimestamp(mtime).isoformat(timespec='seconds')}")
            newest = newest_input(root)
            if newest is None:
                print(f"V-08 ERROR no input files found under {root}; cannot judge freshness")
                return 2
            if mtime < newest[0]:
                print(f"V-08 ERROR stale apk: {apk.name} is older than {newest[1]}")
                return 2
        else:
            apk = v04._build(root, "debug" if expect_debug else "release")
        if aapt2 is None:
            aapt2 = find_aapt2()
        if aapt2 is None or not Path(aapt2).is_file():
            print(f"V-08 ERROR aapt2 not found: {aapt2}")
            return 2
        pfind, notes = permission_report(str(apk), str(aapt2), expect_debug)
        for n in notes:
            print(n)
        findings = scan_dex(str(apk), expect_debug) + pfind
    except Exception as e:  # a traceback never decides the exit code
        print(f"V-08 ERROR {type(e).__name__}: {e}")
        return 2
    if findings:
        for f in findings:
            print(f)
        return 1
    print(f"V-08 canary found: {', '.join(c[0] for c in v04.CANARIES)}")
    print(f"V-08 PASS ({'debug' if expect_debug else 'release'}): {apk}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
