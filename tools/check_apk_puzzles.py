#!/usr/bin/env python3
"""
Check that a built APK carries exactly the repo's puzzles (WO-002 code review F4, TASK-020).

Usage:
    python tools/check_apk_puzzles.py <path-to.apk> [Tangrams-dir]

What is compared (content/build.gradle.kts `packagePuzzles`): the Gradle task copies Tangrams/*.json (never
*.schema.json) into `tangrams/` as Java resources, which land at the APK root, and writes `tangrams/index.txt`
(the sorted file names, one per line, LF, trailing newline). So the APK's `tangrams/` entries must equal the
repo's puzzle files byte for byte, plus that generated index.

Reports every difference (missing, extra, changed, bad index). Exit 0 on a match, 1 otherwise, 2 on usage errors.
Stdlib only.
"""
import sys
import zipfile
from pathlib import Path

PREFIX = "tangrams/"


def expected_files(tangrams_dir):
    """{apk entry name: bytes} the build must package from the repo."""
    out = {}
    for f in sorted(Path(tangrams_dir).glob("*.json")):
        if f.name.endswith(".schema.json"):
            continue
        out[PREFIX + f.name] = f.read_bytes()
    names = sorted(n[len(PREFIX):] for n in out)
    out[PREFIX + "index.txt"] = ("\n".join(names) + "\n").encode("utf-8")
    return out


def check(apk_path, tangrams_dir):
    """Returns a list of difference lines; empty means the APK matches the repo."""
    want = expected_files(tangrams_dir)
    diffs = []
    if len(want) <= 1:   # only the generated index: the repo has no puzzles, so a match would prove nothing
        diffs.append(f"NO PUZZLES in {tangrams_dir}: expected at least one *.json puzzle file")
    with zipfile.ZipFile(apk_path) as z:
        # the prefix test ignores case, so a "Tangrams/" or "TANGRAMS/" folder cannot hide as a non-match
        names = [n for n in z.namelist() if n.lower().startswith(PREFIX) and not n.endswith("/")]
        for name in sorted({n for n in names if names.count(n) > 1}):
            diffs.append(f"DUPLICATE entry in APK: {name} ({names.count(name)} times)")
        got = {n: z.read(n) for n in dict.fromkeys(names)}
    for name in sorted(want):
        if name not in got:
            diffs.append(f"MISSING in APK: {name}")
        elif got[name] != want[name]:
            kind = "index differs" if name.endswith("index.txt") else "content differs"
            diffs.append(f"CHANGED: {name} ({kind}; repo {len(want[name])} bytes, APK {len(got[name])} bytes)")
    for name in sorted(got):
        if name not in want:
            diffs.append(f"EXTRA in APK: {name}")
    return diffs


def main(argv):
    if not argv or len(argv) > 2:
        print(__doc__)
        return 2
    apk = Path(argv[0])
    tangrams = Path(argv[1]) if len(argv) == 2 else Path(__file__).resolve().parent.parent / "Tangrams"
    if not apk.is_file():
        print(f"FAIL apk not found: {apk}")
        return 2
    if not tangrams.is_dir():
        print(f"FAIL puzzles folder not found: {tangrams}")
        return 2
    try:
        diffs = check(apk, tangrams)
    except zipfile.BadZipFile:
        print(f"FAIL not a zip/APK: {apk}")
        return 1
    if diffs:
        print(f"FAIL {apk.name}: {len(diffs)} difference(s) from {tangrams}")
        for d in diffs:
            print("  " + d)
        return 1
    count = len(expected_files(tangrams)) - 1
    print(f"PASS {apk.name}: {count} puzzles + index.txt match {tangrams} byte for byte")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
