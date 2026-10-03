#!/usr/bin/env python3
"""
V-04: the release APK contains no DevTools (G-04, REQ-046 A4; design WO-005 section 6; DA-78, DA-79, DA-85, DA-86).

Usage:
    python v04_release_apk.py [--apk PATH] [--positive-control] [--project-root DIR] [--no-puzzles]

Default mode builds the release APK (:app:assembleRelease) or takes --apk, then scans it:
  * every classes*.dex is parsed (string-id table, MUTF-8): a string under io/github/jamisuni/tangram/devtools
    (slash or dotted form) or the exact string "0417" is a finding; an unparseable dex is a finding plus a raw scan;
  * every other zip entry except dex, images/audio and tangrams/ is searched (ASCII and UTF-16LE) for the package
    forms, the devtools_ resource key and "Wrong passcode." (the digits 0417 are NOT searched here, DA-85);
  * release canary (DA-86): the scanner must itself find the play package, the MainActivity descriptor, the
    app_name key and the "Restart" text, otherwise "scanner blind";
  * folds tools/check_apk_puzzles.check() (--no-puzzles skips it).
--positive-control builds a fresh DEBUG APK (or takes --apk) and requires all four DevTools marker kinds.

Exit: 0 PASS, 1 FAIL, 2 usage / IO / build error. Stdlib only.
"""
import os
import re
import shutil
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

DEVTOOLS_SLASH = "io/github/jamisuni/tangram/devtools"
DEVTOOLS_DOT = "io.github.jamisuni.tangram.devtools"
PASSCODE = "0417"
RES_KEY = "devtools_"
RES_TEXT = "Wrong passcode."
NONDEX_NEEDLES = [DEVTOOLS_SLASH, DEVTOOLS_DOT, RES_KEY, RES_TEXT]

# (label, where, needle, exact): where "dex" = a dex string, "arsc" = bytes of resources.arsc
CANARIES = [
    ("Lio/github/jamisuni/tangram/play/", "dex", "Lio/github/jamisuni/tangram/play/", False),
    ("Lio/github/jamisuni/tangram/MainActivity;", "dex", "Lio/github/jamisuni/tangram/MainActivity;", True),
    ("app_name", "arsc", "app_name", False),
    ("Restart", "arsc", "Restart", False),
]

DEX_NAME = re.compile(r"^classes\d*\.dex$")
DEX_MAGIC = re.compile(rb"^dex\n03[5-9]\x00$")
SKIP_EXT = (".png", ".webp", ".jpg", ".jpeg", ".gif", ".ogg")
ARSC = "resources.arsc"

MARKER_KINDS = [
    "devtools package in dex",
    "dex string 0417",
    "devtools_ key in resources.arsc",
    "Wrong passcode. in resources.arsc",
]


class DexError(Exception):
    pass


def _has(blob, text):
    return text.encode("utf-8") in blob or text.encode("utf-16-le") in blob


def parse_dex_strings(data):
    """All strings of a dex's string table; DexError when it is not a dex we can trust."""
    if len(data) < 0x70:
        raise DexError("shorter than the 0x70 header")
    if not DEX_MAGIC.match(data[:8]):
        raise DexError(f"bad magic {data[:8]!r}")
    header_size = struct.unpack_from("<I", data, 0x24)[0]
    if header_size != 0x70:
        raise DexError(f"header size {header_size:#x}, expected 0x70")
    size, off = struct.unpack_from("<II", data, 0x38)
    if off + size * 4 > len(data):
        raise DexError("string id table beyond end of file")
    out = []
    for i in range(size):
        so = struct.unpack_from("<I", data, off + 4 * i)[0]
        if so >= len(data):
            raise DexError(f"string {i} offset beyond end of file")
        p = so
        while True:  # ULEB128 UTF-16 length; the value is not needed
            if p >= len(data):
                raise DexError(f"string {i} length runs off the file")
            b = data[p]
            p += 1
            if not b & 0x80:
                break
        end = data.find(b"\x00", p)
        if end < 0:
            raise DexError(f"string {i} has no terminator")
        out.append(data[p:end].decode("utf-8", errors="replace"))
    return out


def _collect(apk):
    """Opens the APK once. Returns (non-dex entries {name: bytes}, dex strings [(dex, s)],
    findings from dex parsing, unparseable-dex blobs [(dex, bytes)])."""
    findings = []
    strings = []
    raw_blobs = []
    ndex = 0
    others = {}
    with zipfile.ZipFile(apk) as z:
        for info in z.infolist():
            name = info.filename
            if name.endswith("/"):
                continue
            if DEX_NAME.match(name):
                ndex += 1
                data = z.read(name)
                try:
                    strings.extend((name, s) for s in parse_dex_strings(data))
                except DexError as e:
                    findings.append(f"V-04 FAIL dex-unparseable {name} {e}")
                    raw_blobs.append((name, data))
                continue
            low = name.lower()
            if low.endswith(SKIP_EXT) or low.startswith("tangrams/"):
                continue
            others[name] = z.read(name)
    if ndex == 0:
        findings.append("V-04 FAIL no-dex - no classes*.dex in the APK (a scan of nothing proves nothing)")
    return others, strings, findings, raw_blobs


def _canary_findings(others, strings, raw_blobs):
    out = []
    arsc = others.get(ARSC, b"")
    for label, where, needle, exact in CANARIES:
        if where == "dex":
            if exact:
                found = any(s == needle for _, s in strings)
            else:
                found = any(needle in s for _, s in strings)
            found = found or any(_has(b, needle) for _, b in raw_blobs)
        else:
            found = _has(arsc, needle)
        if not found:
            out.append(f"V-04 FAIL scanner blind {label}; if a rename or R8 was intended, "
                       f"update CANARIES in v04_release_apk.py")
    return out


def scan_apk(apk):
    """Findings ('V-04 FAIL ...' lines) for an APK; empty list = clean and the scanner could see."""
    others, strings, findings, raw_blobs = _collect(apk)
    for dex, s in strings:
        if DEVTOOLS_SLASH in s or DEVTOOLS_DOT in s:
            findings.append(f"V-04 FAIL devtools-class {dex} {s}")
        elif s == PASSCODE:
            findings.append(f"V-04 FAIL passcode {dex} string {PASSCODE}")
    for dex, blob in raw_blobs:
        for needle in NONDEX_NEEDLES:
            if _has(blob, needle):
                findings.append(f"V-04 FAIL devtools-raw {dex} unparseable dex contains {needle}")
    for name in sorted(others):
        for needle in NONDEX_NEEDLES:
            if _has(others[name], needle):
                findings.append(f"V-04 FAIL devtools-resource {name} contains {needle}")
    findings.extend(_canary_findings(others, strings, raw_blobs))
    return findings


def positive_control(apk):
    """Marker kinds NOT found in the APK (empty list = the scanner sees every kind)."""
    others, strings, _findings, _raw = _collect(apk)
    arsc = others.get(ARSC, b"")
    found = {
        MARKER_KINDS[0]: any(DEVTOOLS_SLASH in s for _, s in strings),
        MARKER_KINDS[1]: any(s == PASSCODE for _, s in strings),
        MARKER_KINDS[2]: _has(arsc, RES_KEY),
        MARKER_KINDS[3]: _has(arsc, RES_TEXT),
    }
    return [k for k in MARKER_KINDS if not found[k]]


def _build(root, variant):
    """Deletes the old APK folder, runs Gradle, returns the newest APK. Raises RuntimeError."""
    out_dir = root / "app" / "build" / "outputs" / "apk" / variant
    if out_dir.exists():
        shutil.rmtree(out_dir)
    env = dict(os.environ)
    env.setdefault("JAVA_HOME", r"C:\Program Files\Android\Android Studio\jbr")
    env.setdefault("ANDROID_HOME", r"C:\Users\Jami\AppData\Local\Android\Sdk")
    task = ":app:assemble" + variant.capitalize()
    if os.name == "nt":
        cmd = ["cmd.exe", "/c", ".\\gradlew.bat", task, "--console=plain"]
    else:
        cmd = ["./gradlew", task, "--console=plain"]
    try:
        r = subprocess.run(cmd, cwd=str(root), env=env, capture_output=True, text=True, timeout=1200)
    except subprocess.TimeoutExpired:
        raise RuntimeError(f"{task} timed out after 1200 s")
    if r.returncode != 0:
        raise RuntimeError(f"{task} failed (exit {r.returncode}): {(r.stdout + r.stderr)[-800:]}")
    apks = sorted(out_dir.glob("*.apk"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not apks:
        raise RuntimeError(f"no APK under {out_dir} after {task}")
    return apks[0]


def _puzzle_findings(apk, root):
    tools = root / "tools"
    if not tools.is_dir():
        tools = Path(__file__).resolve().parents[2] / "tools"
    if str(tools) not in sys.path:
        sys.path.insert(0, str(tools))
    import check_apk_puzzles
    return [f"V-04 FAIL puzzles {apk.name} {d}" for d in check_apk_puzzles.check(apk, root / "Tangrams")]


def main(argv):
    apk_arg = None
    root = Path(__file__).resolve().parents[2]
    pc = False
    puzzles = True
    args = list(argv)
    while args:
        a = args.pop(0)
        if a == "--positive-control":
            pc = True
        elif a == "--no-puzzles":
            puzzles = False
        elif a in ("--apk", "--project-root") and args:
            v = args.pop(0)
            if a == "--apk":
                apk_arg = Path(v)
            else:
                root = Path(v)
        else:
            print(__doc__)
            return 2
    try:
        if apk_arg is not None:
            apk = apk_arg
            if not apk.is_file():
                print(f"V-04 ERROR apk not found: {apk}")
                return 2
        else:
            apk = _build(root, "debug" if pc else "release")
        if pc:
            missing = positive_control(apk)
            for k in missing:
                print(f"V-04 FAIL scanner cannot see {k}")
            if missing:
                return 1
            print(f"V-04 POSITIVE-CONTROL PASS: all {len(MARKER_KINDS)} marker kinds found in {apk}")
            return 0
        findings = scan_apk(apk)
        if puzzles:
            findings.extend(_puzzle_findings(apk, root))
    except Exception as e:  # a traceback never decides the exit code
        print(f"V-04 ERROR {type(e).__name__}: {e}")
        return 2
    if findings:
        for f in findings:
            print(f)
        return 1
    print(f"V-04 canary found: {', '.join(c[0] for c in CANARIES)}")
    print(f"V-04 PASS: {apk}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
