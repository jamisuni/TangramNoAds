#!/usr/bin/env python3
"""
The owner's puzzle-review record (WO-009 TASK-092, DA-156, design section 3).

  python tools/puzzle_review.py sheet --apk <played debug apk>      (the AI runs this)
  python tools/puzzle_review.py mark <id> [<id> ...] [--reapprove]   (the owner runs this, in a terminal)

`sheet` reads the puzzles from the played APK, refuses when they differ from Tangrams/, and writes
release/puzzle-review-sheet.md and .json: per puzzle the id, titles, kind, rating, review_hash, the ledger
status (waiting / reviewed / STALE) and an empty approve / fix / reject column.

`mark` flips "reviewedByHuman" false -> true and writes the ledger release/puzzle-review.json for the ids the
owner types APPROVED for. It refuses without a terminal, refuses a file whose review_hash differs from the
sheet's (changed since the build that was played), and re-approves a changed approved file (STALE) only when
asked. Agents never run it on the real Tangrams/ (AGENTS rule 7, G-08).

Honest limit: this is a speed bump against accidents, not a forgery control.

Ledger format (JSON object keyed by puzzle id): {"<id>": {"sha256": <review_hash>, "by": <name>, "date": <YYYY-MM-DD>}}
Stdlib only. Files are written with LF line endings.
"""
import argparse
import datetime
import hashlib
import json
import os
import re
import subprocess
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))
import check_apk_puzzles  # noqa: E402
import validate_puzzles  # noqa: E402  (art_colour_count: the V14 counting, imported not mirrored)

FLAG_RE = re.compile(r'"reviewedByHuman"\s*:\s*(true|false)')
FLAG_FALSE = re.compile(r'("reviewedByHuman"\s*:\s*)(false)')
KIND_ORDER = {"mini": 0, "warmup": 1, "full": 2}
ACTIONS = "approve / fix / reject"


def review_hash(text):
    """sha256 of the file text with CRLF as LF and the review flag normalised to false: the hash of what was approved."""
    t = text.replace("\r\n", "\n")
    t = FLAG_RE.sub('"reviewedByHuman": false', t)
    return hashlib.sha256(t.encode("utf-8")).hexdigest()


def _puzzle_files(directory):
    return {f.stem: f for f in sorted(Path(directory).glob("*.json")) if not f.name.endswith(".schema.json")}


def _text(path):
    return path.read_bytes().decode("utf-8")


def _flag(text):
    """The flag value as bool, or None when the file has no flag."""
    m = FLAG_RE.search(text)
    return None if m is None else m.group(1) == "true"


def load_ledger(ledger):
    p = Path(ledger)
    if not p.is_file():
        return {}
    return json.loads(p.read_text(encoding="utf-8"))


def _status(flag, h, entry):
    if flag is True:
        if entry is None:
            return "unledgered"
        return "reviewed" if entry["sha256"] == h else "STALE"
    return "waiting"


def _preview(directory, pid):
    """Relative link to the rendered preview under the puzzle folder, or "" when there is none."""
    for ext in ("png", "svg"):
        if (Path(directory) / "previews" / (pid + "." + ext)).is_file():
            return "previews/%s.%s" % (pid, ext)
    return ""


def _link(directory, preview, out):
    """The preview link as seen from the folder the sheet is written to (forward slashes); relative to dir when out is None."""
    if out is None:
        return preview
    target = (Path(directory) / preview).resolve()
    try:
        return Path(os.path.relpath(target, Path(out).resolve())).as_posix()
    except ValueError:  # another drive: no relative path exists
        return target.as_posix()


def sheet(dir, ledger, apk=None, out=None):
    """Returns (markdown, json_text). Pure apart from reading the APK; refuses (ValueError) when the APK differs from dir.

    out: the folder the sheet file will be written to; the markdown preview links are computed relative to it."""
    apk_sha = None
    if apk is not None:
        diffs = check_apk_puzzles.check(apk, dir)
        if diffs:
            raise ValueError("stale build, rebuild: the APK's puzzles differ from " + str(dir) + "\n  " + "\n  ".join(diffs))
        apk_sha = hashlib.sha256(Path(apk).read_bytes()).hexdigest()
    led = load_ledger(ledger) if ledger is not None else {}
    rows = []
    for pid, f in _puzzle_files(dir).items():
        text = _text(f)
        data = json.loads(text)
        h = review_hash(text)
        rows.append({
            "id": data["id"],
            "title": {"en": data["title"]["en"], "fi": data["title"]["fi"]},
            "theme": data.get("category"),
            "colours": validate_puzzles.art_colour_count(data.get("art", {})),
            "preview": _preview(dir, data["id"]),
            "kind": data.get("kind", "full"),
            "rating": data["difficulty"],
            "review_hash": h,
            "status": _status(_flag(text), h, led.get(data["id"])),
            "decision": "",
        })
    rows.sort(key=lambda r: (KIND_ORDER.get(r["kind"], 9), r["rating"], r["id"]))
    doc = {"apk_sha256": apk_sha, "puzzles": rows}
    js = json.dumps(doc, indent=2, ensure_ascii=False) + "\n"
    lines = [
        "# Puzzle review sheet",
        "",
        "Generated by `tools/puzzle_review.py sheet`; do not edit.",
        "",
        "1. Install the APK below and solve each puzzle by hand. The DEV \"Solve this puzzle now\" aid does not count as solving.",
        "2. Check the silhouette and the picture, and write " + ACTIONS + " in the last column.",
        "3. Tell the AI your fixes and rejects. Run `python tools/puzzle_review.py mark <id> ...` in PowerShell or cmd (Git Bash has no terminal and is refused).",
        "",
        "APK sha256: " + (apk_sha or "(no APK given)"),
        "",
        "| # | id | title en | title fi | theme | kind | rating | colours | preview | review_hash | status | " + ACTIONS + " |",
        "|---|---|---|---|---|---|---|---|---|---|---|---|",
    ]
    for i, r in enumerate(rows, 1):
        lines.append("| %d | %s | %s | %s | %s | %s | %d | %d | %s | %s | %s |  |" % (
            i, r["id"], r["title"]["en"], r["title"]["fi"], r["theme"] or "", r["kind"], r["rating"], r["colours"],
            ("[" + r["preview"] + "](" + _link(dir, r["preview"], out) + ")") if r["preview"] else "", r["review_hash"][:12], r["status"]))
    return "\n".join(lines) + "\n", js


def _write(path, text):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)


def mark_files(dir, ids, ledger, sheet, by, date, _cli=False, reapprove=()):
    """Approve the given puzzle ids: flip the flag to true and write the ledger. Returns the changed ids.

    sheet: the sheet's JSON (text, dict, or a path to the .json file). reapprove: ids (or True for all) whose
    changed-since-approval (STALE) content may be approved again; without it a STALE file is refused.
    Everything is checked before anything is written.
    """
    d = Path(dir).resolve()
    if d == (ROOT / "Tangrams").resolve() and not _cli:
        raise PermissionError("refusing the repository's own Tangrams/: approvals are made only through the CLI `mark`")
    if isinstance(sheet, (str, os.PathLike)) and not str(sheet).lstrip().startswith("{"):
        sheet = Path(sheet).read_text(encoding="utf-8")
    if isinstance(sheet, str):
        sheet = json.loads(sheet)
    sheet_hash = {r["id"]: r["review_hash"] for r in sheet["puzzles"]}
    files = _puzzle_files(d)
    led = load_ledger(ledger)
    plan = []
    for pid in ids:
        if pid not in files:
            raise KeyError("unknown puzzle id: " + pid)
        if pid not in sheet_hash:
            raise ValueError(pid + " is not on the sheet: regenerate the sheet from a fresh build")
        text = _text(files[pid])
        h = review_hash(text)
        if h != sheet_hash[pid]:
            raise ValueError(pid + ": changed since the build you played: rebuild, regenerate the sheet, replay")
        flag = _flag(text)
        if flag is None:
            raise ValueError(pid + ": no reviewedByHuman flag in the file")
        entry = led.get(pid)
        if flag is True and entry is not None and entry["sha256"] == h:
            continue  # already approved at exactly this content
        if flag is True:
            allowed = reapprove is True or pid in set(reapprove or ())
            if not allowed:
                raise ValueError(pid + ": STALE or unledgered (flag true, content differs from the ledger): needs an explicit re-approval")
        plan.append((pid, files[pid], text, h))
    changed = []
    for pid, path, text, h in plan:
        if _flag(text) is False:
            new = FLAG_FALSE.sub(lambda m: m.group(1) + "true", text, count=1)
            with open(path, "wb") as fh:
                fh.write(new.encode("utf-8"))
        led[pid] = {"sha256": h, "by": by, "date": date}
        changed.append(pid)
    if changed:
        _write(ledger, json.dumps(led, indent=2, sort_keys=True, ensure_ascii=False) + "\n")
    return changed


def _cmd_sheet(a):
    try:
        md, js = sheet(a.dir, a.ledger, a.apk, out=a.out)
    except ValueError as e:
        print("FAIL " + str(e))
        return 1
    out = Path(a.out)
    _write(out / "puzzle-review-sheet.md", md)
    _write(out / "puzzle-review-sheet.json", js)
    print("wrote %s and .json" % (out / "puzzle-review-sheet.md"))
    return 0


def _real_tty():
    return sys.stdin.isatty() and sys.stdout.isatty()


def _cmd_mark(a, isatty=_real_tty, input_fn=input):
    """isatty / input_fn are injectable so tests can drive the owner's terminal with a fake."""
    if not isatty():
        print("FAIL mark needs a terminal (stdin and stdout). Run it in PowerShell or cmd; Git Bash reports no terminal.")
        return 2
    sheet_path = Path(a.sheet)
    if not sheet_path.is_file():
        print("FAIL no sheet at %s: run `sheet --apk <played apk>` first" % sheet_path)
        return 2
    doc = json.loads(sheet_path.read_text(encoding="utf-8"))
    rows = {r["id"]: r for r in doc["puzzles"]}
    led = load_ledger(a.ledger)
    approved, stale_ok, refused = [], [], []
    for pid in a.ids:
        r = rows.get(pid)
        if r is None:
            print("FAIL unknown id " + pid)
            return 2
        if r["status"] in ("STALE", "unledgered"):
            if not a.reapprove:
                print("%s is %s: content changed since your approval on %s. Re-run with --reapprove to approve the new content." % (
                    pid, r["status"], led.get(pid, {}).get("date", "?")))
                refused.append(pid)
                continue
            prompt = "%s (%s / %s): content changed since your approval; type APPROVED to approve the new content: " % (
                pid, r["title"]["en"], r["title"]["fi"])
        else:
            prompt = "%s (%s / %s): type APPROVED to approve: " % (pid, r["title"]["en"], r["title"]["fi"])
        if input_fn(prompt).strip() == "APPROVED":
            approved.append(pid)
            if r["status"] in ("STALE", "unledgered"):
                stale_ok.append(pid)
        else:
            print("skipped " + pid)
    if not approved:
        print("nothing approved")
        return 1 if refused else 0
    try:
        changed = mark_files(a.dir, approved, a.ledger, doc, a.by, a.date or datetime.date.today().isoformat(),
                             _cli=True, reapprove=stale_ok)
    except (ValueError, KeyError, PermissionError) as e:
        print("FAIL " + str(e))
        return 1
    print("approved: " + ", ".join(changed))
    rc = 1 if refused else 0
    exporter = TOOLS / "export_geometry_golden.py"
    if changed and exporter.is_file() and Path(a.dir).resolve() == (ROOT / "Tangrams").resolve():
        print("refreshing the golden ...")
        try:
            res = subprocess.run([sys.executable, str(exporter)], cwd=str(ROOT), check=False)
            code = res.returncode
        except OSError as e:
            print("ERROR the golden refresh could not run: %s. Run tools/export_geometry_golden.py before gradlew test." % e)
            return 1
        if code != 0:
            print("ERROR the golden refresh failed (exit status %s). Run tools/export_geometry_golden.py and fix it before gradlew test." % code)
            return 1
    return rc


def main(argv, isatty=_real_tty, input_fn=input):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("sheet")
    s.add_argument("--dir", default=str(ROOT / "Tangrams"))
    s.add_argument("--ledger", default=str(ROOT / "release" / "puzzle-review.json"))
    s.add_argument("--apk", required=True, help="the played debug APK; its puzzles must equal --dir")
    s.add_argument("--out", default=str(ROOT / "release"))
    m = sub.add_parser("mark")
    m.add_argument("ids", nargs="+")
    m.add_argument("--dir", default=str(ROOT / "Tangrams"))
    m.add_argument("--ledger", default=str(ROOT / "release" / "puzzle-review.json"))
    m.add_argument("--sheet", default=str(ROOT / "release" / "puzzle-review-sheet.json"))
    m.add_argument("--by", default=os.environ.get("USERNAME") or os.environ.get("USER") or "owner")
    m.add_argument("--date", default=None)
    m.add_argument("--reapprove", action="store_true", help="allow approving a changed (STALE) file again")
    a = ap.parse_args(argv)
    return _cmd_sheet(a) if a.cmd == "sheet" else _cmd_mark(a, isatty, input_fn)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
