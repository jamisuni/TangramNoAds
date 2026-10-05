#!/usr/bin/env python3
"""
V-09: the release-library gate (WO-009 TASK-093, DA-157; REQ-042 A1-A3, REQ-039 A2; design section 4).

Usage:
    python v09_release_library.py [--dir DIR | --apk PATH] [--ledger PATH] [--min N] [--pre-review]

Input: the puzzle files of --dir (default Tangrams/) or of --apk (its tangrams/*.json, as check_apk_puzzles finds
them). The ledger (default release/puzzle-review.json; a missing file is an empty ledger) is a flat JSON object keyed
by puzzle id: {"<id>": {"sha256": <review_hash>, "by": ..., "date": ...}}. review_hash is imported from
tools/puzzle_review.py and never redefined.

Strict mode fails on: unreviewed <id> (flag not true), unledgered <id> (flag true, no ledger entry),
changed after approval <id> (review_hash differs from the ledger's: STALE), count R of N reviewed (< min),
themes C (< 4), missing shapes-square, unreadable <file>.
--pre-review keeps every finding except "unreviewed" and the reviewed count, and adds "count N puzzles (< min)";
the reviewed count is printed as information, so a library that is internally sound passes while strict is RED.

Prints "V-09 FAIL <finding>" lines (names only) and a summary, then "V-09 PASS" or "V-09 FAIL".
Exit: 0 pass, 1 findings, 2 usage error / cannot read. Read-only: it never writes anything. Stdlib only.
"""
import argparse
import json
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from puzzle_review import review_hash  # noqa: E402  (the only definition)

MIN_THEMES = 4
SQUARE = "shapes-square"
APK_PREFIX = "tangrams/"


def _stem(name):
    return name[:-5] if name.endswith(".json") else name


def _is_puzzle(name):
    return name.endswith(".json") and not name.endswith(".schema.json")


def _parse(raw):
    """(data, flag) of a puzzle file; flag is True only for the JSON literal true, in provenance or at top level."""
    data = json.loads(raw.decode("utf-8"))
    prov = data.get("provenance")
    flag = prov.get("reviewedByHuman") if isinstance(prov, dict) else data.get("reviewedByHuman")
    return data, flag is True


def analyse(files, ledger):
    """Returns (rows, unreadable): rows = [(name, id, theme, flag, status)], status in unreviewed/unledgered/stale/reviewed."""
    rows, unreadable = [], []
    for name in sorted(files):
        if not _is_puzzle(name):
            continue
        raw = files[name]
        try:
            data, flag = _parse(raw)
            pid = data.get("id", _stem(name)) if isinstance(data, dict) else _stem(name)
            theme = data.get("category")
            h = review_hash(raw.decode("utf-8"))
        except (ValueError, AttributeError, UnicodeDecodeError):
            unreadable.append(name)
            continue
        entry = ledger.get(pid)
        if not flag:
            status = "unreviewed"
        elif not isinstance(entry, dict):
            status = "unledgered"
        elif entry.get("sha256") != h:
            status = "stale"
        else:
            status = "reviewed"
        rows.append((name, pid, theme, flag, status))
    return rows, unreadable


def summary(files, ledger):
    """(reviewed, total, themes) for the info line."""
    rows, unreadable = analyse(files, ledger)
    return (sum(1 for r in rows if r[4] == "reviewed"), len(rows) + len(unreadable),
            len({r[2] for r in rows if r[2]}))


def check(files, ledger, min_count=20, pre_review=False):
    """Findings (empty = pass) for {file name: bytes} and a ledger dict."""
    rows, unreadable = analyse(files, ledger)
    out = ["unreadable " + n for n in unreadable]
    for name, pid, _t, _f, status in rows:
        if status == "unreviewed" and not pre_review:
            out.append("unreviewed " + pid)
        elif status == "unledgered":
            out.append("unledgered " + pid)
        elif status == "stale":
            out.append("changed after approval " + pid)
    total = len(rows) + len(unreadable)
    reviewed = sum(1 for r in rows if r[4] == "reviewed")
    if pre_review:
        if total < min_count:
            out.append("count %d puzzles (< %d)" % (total, min_count))
    elif reviewed < min_count:
        out.append("count %d of %d reviewed (< %d)" % (reviewed, total, min_count))
    themes = len({r[2] for r in rows if r[2]})
    if themes < MIN_THEMES:
        out.append("themes %d (< %d)" % (themes, MIN_THEMES))
    if not any(r[1] == SQUARE for r in rows):
        out.append("missing " + SQUARE)
    return out


def read_dir(d):
    return {f.name: f.read_bytes() for f in sorted(Path(d).glob("*.json")) if _is_puzzle(f.name)}


def read_apk(apk):
    out = {}
    with zipfile.ZipFile(apk) as z:
        for n in z.namelist():
            if n.lower().startswith(APK_PREFIX) and not n.endswith("/") and _is_puzzle(n):
                out[n[len(APK_PREFIX):]] = z.read(n)
    return out


class _Parser(argparse.ArgumentParser):
    def error(self, message):
        raise ValueError(message)


def main(argv):
    ap = _Parser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dir", default=None)
    ap.add_argument("--apk", default=None)
    ap.add_argument("--ledger", default=str(ROOT / "release" / "puzzle-review.json"))
    ap.add_argument("--min", type=int, default=20, dest="min_count")
    ap.add_argument("--pre-review", action="store_true")
    try:
        a = ap.parse_args(argv)
        if a.dir and a.apk:
            raise ValueError("give --dir or --apk, not both")
    except ValueError as e:
        print("V-09 usage error: " + str(e))
        return 2
    try:
        if a.apk:
            if not Path(a.apk).is_file():
                print("V-09 FAIL cannot read apk " + Path(a.apk).name)
                return 2
            files = read_apk(a.apk)
        else:
            d = Path(a.dir) if a.dir else ROOT / "Tangrams"
            if not d.is_dir():
                print("V-09 FAIL cannot read folder " + d.name)
                return 2
            files = read_dir(d)
        lp = Path(a.ledger)
        ledger = json.loads(lp.read_text(encoding="utf-8")) if lp.is_file() else {}
        if not isinstance(ledger, dict):
            raise ValueError("ledger is not an object")
    except (OSError, ValueError, zipfile.BadZipFile) as e:
        print("V-09 FAIL cannot read input: " + type(e).__name__)
        return 2
    findings = check(files, ledger, a.min_count, a.pre_review)
    reviewed, total, themes = summary(files, ledger)
    ids = {r[1] for r in analyse(files, ledger)[0]}
    for pid in sorted(k for k in ledger if k not in ids):
        print("V-09 warn ledger entry without file " + str(pid))
    for f in findings:
        print("V-09 FAIL " + f)
    print("V-09 library: %d of %d reviewed, %d themes%s" % (reviewed, total, themes, " (pre-review: info only)" if a.pre_review else ""))
    print("V-09 FAIL" if findings else "V-09 PASS")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
