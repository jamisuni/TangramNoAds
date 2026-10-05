#!/usr/bin/env python3
"""WO-009 T9c (independent Acceptance Test Author): acceptance tests of V-09, the release-library gate.

Written from REQ-042 (A1 at least 20 puzzles with reviewedByHuman true, A2 at least four categories, A3 shapes-square), REQ-039 A2
(no released puzzle has reviewedByHuman false) and the frozen design (WO-009 section 4 and the seam table), never from the implementation:

  v09_release_library.check(files: dict[str, bytes], ledger: dict, min_count, pre_review: bool) -> list[str]     (findings)
  v09_release_library.main(argv)                                                                                    (exit 0 / 1 / 2)

Findings of section 4: `unreviewed <id>`, `changed after approval <id>`, `unledgered <id>` (flag true, no ledger entry), `count R of N
reviewed (< 20)`, `themes C (< 4)`, `missing shapes-square`, a warning for a ledger entry with no file, a summary
`V-09 library: R of N reviewed, C themes`. "True" is the JSON literal `true` only. `--pre-review` exits 0 when the ONLY findings are
`unreviewed` and the count, and still fails on `changed after approval`, `unledgered`, themes, a missing square or a parse error. Exit 0
pass, 1 findings, 2 cannot read. Inputs `--dir Tangrams` (default) or `--apk <apk>` (its `tangrams/*.json`), `--ledger`, `--min 20`.
The approved hash of a puzzle is `review_hash`: the file with CRLF as LF and the flag normalised to false, sha256 hex (design section 3);
the ledger entry is `{sha256, date, by}` per puzzle id.

Two things the frozen seam leaves open are CALIBRATED, not assumed: how `files` is keyed (file name `<id>.json` or the id) and how the
ledger dict is shaped (`{id: entry}` or `{"puzzles": {id: entry}}` or `{"entries": {id: entry}}`). The first form under which a fully
approved library passes is used for every test; if none passes the whole file fails loudly (the seam is ambiguous or V-09 is wrong).
Tests that need an exit code or a printed line go through `main` on a temporary folder or zip, which is unambiguous.

Run: python .swdev/verifiers/test_v09_acceptance.py
"""
import contextlib
import hashlib
import io
import json
import re
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
sys.path.insert(0, str(HERE))
import v09_release_library as v09  # noqa: E402  (a missing module fails the whole file loudly)

MIN = 20
FAIL_PATTERNS = [
    r"\bunreviewed\b", r"changed after approval", r"\bunledgered\b", r"\bcount \d+ of \d+ reviewed\b",
    r"\bthemes \d+ \(< ", r"missing shapes-square", r"cannot read", r"\bparse\b", r"unreadable",
]


def failures(findings):
    return [f for f in findings if any(re.search(p, f, re.I) for p in FAIL_PATTERNS)]


def review_hash(text):
    t = text.replace("\r\n", "\n")
    t = re.sub(r'("reviewedByHuman"\s*:\s*)(true|false)', r"\1false", t)
    return hashlib.sha256(t.encode("utf-8")).hexdigest()


# ---------------------------------------------------------------------------------------------------------------- fixtures
def real_templates():
    """The shipped puzzle files as text, by id (the real files give realistic content in both provenance layouts)."""
    out = {}
    for p in sorted((REPO / "Tangrams").glob("*.json")):
        if p.name.endswith(".schema.json"):
            continue
        out[p.stem] = p.read_text(encoding="utf-8").replace("\r\n", "\n")
    assert "shapes-square" in out, "fixture: Tangrams/ has no shapes-square.json"
    return out


def set_id(text, pid):
    return re.sub(r'("id"\s*:\s*)"[^"]*"', r'\1"%s"' % pid, text, count=1)


def set_category(text, cat):
    return re.sub(r'("category"\s*:\s*)"[^"]*"', r'\1"%s"' % cat, text, count=1)


def set_provenance(text, flag_literal, layout):
    """Rewrite the whole provenance object: layout 'inline' or 'block'; flag_literal is the raw JSON text, or None to drop the flag."""
    author = '"author": "ai:claude"'
    flag = "" if flag_literal is None else ', "reviewedByHuman": %s' % flag_literal
    if layout == "inline":
        obj = '"provenance": {%s%s}' % (author, flag)
    else:
        lines = ['    %s' % author] + ([] if flag_literal is None else ['    "reviewedByHuman": %s' % flag_literal])
        obj = '"provenance": {\n' + ",\n".join(lines) + "\n  }"
    new, n = re.subn(r'"provenance"\s*:\s*\{[^}]*\}', lambda m: obj, text, count=1)
    assert n == 1, "fixture: no provenance object to rewrite"
    return new


class Lib:
    """A library of puzzle files (text by id) with their flags, and the ledger of what was approved."""

    CATS = ["animals", "people", "things", "vehicles", "nature", "shapes"]

    def __init__(self, n=25, cats=None):
        base = real_templates()
        self.texts = {}  # id -> text, every flag false
        cats = cats or self.CATS
        keep = ["shapes-square"] + [i for i in base if i != "shapes-square"]
        for i in range(n):
            if i < len(keep):
                pid, text = keep[i], base[keep[i]]
            else:  # clones of real files under new ids
                src = keep[i % len(keep)]
                pid = "%s-clone%d" % (cats[i % len(cats)], i)
                text = set_id(base[src], pid)
            self.texts[pid] = text
        for k, pid in enumerate(sorted(self.texts)):
            # categories round-robin (so exactly the given themes occur); the two provenance layouts alternate so both flag forms are exercised
            t = set_category(self.texts[pid], cats[k % len(cats)])
            self.texts[pid] = set_provenance(t, "false", "inline" if k % 2 else "block")
        self.layout = {pid: ("inline" if k % 2 else "block") for k, pid in enumerate(sorted(self.texts))}
        self.ledger = {}  # id -> entry

    def approve(self, ids=None):
        for pid in (ids if ids is not None else list(self.texts)):
            self.texts[pid] = set_provenance(self.texts[pid], "true", self.layout[pid])
            self.ledger[pid] = {"sha256": review_hash(self.texts[pid]), "date": "2026-10-05", "by": "jami"}
        return self

    def ids(self):
        return sorted(self.texts)

    def drop(self, pid):
        self.texts.pop(pid)
        self.ledger.pop(pid, None)
        return self


SEAM = {}  # filled by calibrate()


def encode_files(lib, key_form, texts=None, crlf=()):
    out = {}
    for pid, text in (texts or lib.texts).items():
        if pid in crlf:
            text = text.replace("\n", "\r\n")
        out[(pid + ".json") if key_form == "name" else pid] = text.encode("utf-8")
    return out


def encode_ledger(ledger, form):
    if form == "flat":
        return dict(ledger)
    return {form: dict(ledger)}


def call_check(lib, min_count=MIN, pre_review=False, texts=None, ledger=None, crlf=()):
    files = encode_files(lib, SEAM["key"], texts, crlf)
    led = encode_ledger(lib.ledger if ledger is None else ledger, SEAM["ledger"])
    return v09.check(files, led, min_count, pre_review)


def calibrate():
    good = Lib().approve()
    seen = {}
    for key in ("name", "stem"):
        for form in ("flat", "puzzles", "entries"):
            try:
                found = v09.check(encode_files(good, key), encode_ledger(good.ledger, form), MIN, False)
            except Exception as e:  # noqa: BLE001
                found = ["raised %r" % (e,)]
            seen[(key, form)] = found
            if not failures(found) and not any(f.startswith("raised") for f in found):
                SEAM["key"], SEAM["ledger"] = key, form
                return
    raise AssertionError("seam calibration failed: a fully approved library of 25 puzzles passes under no files-key / ledger-shape form "
                         "(name or stem, flat or puzzles or entries). Findings per form: %r" % (seen,))


def run_main(argv):
    buf = io.StringIO()
    try:
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            rc = v09.main(argv)
    except SystemExit as e:
        rc = e.code
    return (0 if rc is None else rc), buf.getvalue()


class Disk:
    """A temporary Tangrams folder (plus the schema, a README and a previews folder, as the real one has) and a ledger file."""

    def __init__(self, lib, crlf=(), ledger=None, extra_texts=None):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.dir = root / "Tangrams"
        (self.dir / "previews").mkdir(parents=True)
        for pid, text in {**lib.texts, **(extra_texts or {})}.items():
            data = text.replace("\n", "\r\n") if pid in crlf else text
            (self.dir / (pid + ".json")).write_bytes(data.encode("utf-8"))
        schema = REPO / "Tangrams" / "puzzle.schema.json"
        (self.dir / "puzzle.schema.json").write_bytes(schema.read_bytes() if schema.is_file() else b"{}")
        (self.dir / "README.md").write_text("# Tangrams\n", encoding="utf-8")
        (self.dir / "previews" / "sheet.png").write_bytes(b"\x89PNG\r\n")
        self.ledger = root / "ledger.json"
        self.ledger.write_text(json.dumps(encode_ledger(lib.ledger if ledger is None else ledger, SEAM["ledger"])), encoding="utf-8")
        self.root = root

    def argv(self, *more):
        return ["--dir", str(self.dir), "--ledger", str(self.ledger)] + list(more)

    def close(self):
        self.tmp.cleanup()


def setUpModule():
    calibrate()


# ------------------------------------------------------------------------------------------------------------------ check()
class CheckTests(unittest.TestCase):
    # REQ-042 A1 and REQ-039 A2, the pass path: 25 approved puzzles, every one ledgered with its approved hash.
    def test_a_fully_approved_library_passes(self):
        self.assertEqual([], failures(call_check(Lib().approve())))

    # Both flag layouts (the minis' inline provenance and the multi-line one) count: the fixture library holds both.
    def test_the_fixture_holds_both_flag_layouts(self):
        lib = Lib().approve()
        self.assertTrue(any('"provenance": {"author"' in t for t in lib.texts.values()))
        self.assertTrue(any('"provenance": {\n' in t for t in lib.texts.values()))

    # REQ-039 A2: a puzzle whose flag is false is reported, by id.
    def test_an_unreviewed_puzzle_is_reported_by_id(self):
        lib = Lib().approve()
        victim = lib.ids()[3]
        lib.texts[victim] = set_provenance(lib.texts[victim], "false", lib.layout[victim])
        lib.ledger.pop(victim)
        found = failures(call_check(lib))
        self.assertTrue(any(re.search(r"\bunreviewed\b", f) and victim in f for f in found), found)
        others = [f for f in found if victim not in f]
        self.assertEqual([], others, "only the one unreviewed puzzle is a finding (24 of 25 reviewed is still >= 20)")

    # "True is the JSON literal true only": the string "true" and a missing flag are not reviewed.
    def test_the_string_true_and_a_missing_flag_do_not_count_as_reviewed(self):
        for literal, label in (('"true"', "the string"), (None, "a missing flag"), ("1", "the number 1")):
            lib = Lib().approve()
            victim = lib.ids()[5]
            lib.texts[victim] = set_provenance(lib.texts[victim], literal, lib.layout[victim])
            lib.ledger.pop(victim)  # so that no other finding about this id can stand in for `unreviewed`
            found = failures(call_check(lib))
            self.assertTrue(any(re.search(r"unreviewed", f) and victim in f for f in found), "%s must not count as reviewed: %r" % (label, found))

    # F16 mechanism: a flag set without `mark` has no ledger entry.
    def test_a_true_flag_without_a_ledger_entry_is_unledgered(self):
        lib = Lib().approve()
        victim = lib.ids()[7]
        lib.ledger.pop(victim)
        found = failures(call_check(lib))
        self.assertTrue(any(re.search(r"\bunledgered\b", f) and victim in f for f in found), found)

    # REQ-039 rule: approved is of the file's content; an edit after approval voids it.
    def test_an_edit_after_approval_is_reported(self):
        lib = Lib().approve()
        victim = lib.ids()[2]
        # change one byte of the content (a colour digit in the picture) without touching the flag
        edited = re.sub(r'#([0-9A-Fa-f])', lambda m: '#' + ('0' if m.group(1) != '0' else '1'), lib.texts[victim], count=1)
        self.assertNotEqual(edited, lib.texts[victim], "fixture: an edit was made")
        lib.texts[victim] = edited
        found = failures(call_check(lib))
        self.assertTrue(any("changed after approval" in f and victim in f for f in found), found)

    # The approved hash ignores the flag and the line endings: a CRLF checkout of an approved file is not an edit.
    def test_line_endings_are_not_an_edit(self):
        lib = Lib().approve()
        self.assertEqual([], failures(call_check(lib, crlf=set(lib.ids()))))

    # The hash is of the content with the flag normalised: flipping only the flag (false to true, as `mark` does) is no edit either.
    def test_a_ledger_hash_made_from_the_unflagged_text_matches_the_flagged_file(self):
        lib = Lib()
        victim = lib.ids()[4]
        h = review_hash(lib.texts[victim])
        lib.approve([i for i in lib.ids() if i != victim]).approve([victim])
        self.assertEqual(h, lib.ledger[victim]["sha256"])
        self.assertEqual([], failures(call_check(lib)))

    # REQ-042 A1 boundary: exactly 20 reviewed passes, 19 does not (`count R of N reviewed (< 20)`).
    def test_the_count_boundary_is_twenty_inclusive(self):
        ok = Lib(20).approve()
        self.assertEqual([], failures(call_check(ok)), "20 of 20 reviewed passes")
        short = Lib(19).approve()
        found = failures(call_check(short))
        self.assertTrue(any(re.search(r"\bcount 19 of 19 reviewed \(< 20\)", f) for f in found), found)

    def test_the_count_uses_the_given_minimum(self):
        found = failures(call_check(Lib(25).approve(), min_count=30))
        self.assertTrue(any(re.search(r"\bcount 25 of 25 reviewed \(< 30\)", f) for f in found), found)

    # REQ-042 A2 boundary: four themes pass, three do not (`themes C (< 4)`); mini and warm-up files count (the fixture has them).
    def test_the_theme_boundary_is_four_inclusive(self):
        self.assertEqual([], failures(call_check(Lib(25, cats=["shapes", "animals", "things", "nature"]).approve())), "four themes pass")
        found = failures(call_check(Lib(25, cats=["shapes", "animals", "things"]).approve()))
        self.assertTrue(any(re.search(r"\bthemes 3 \(< 4\)", f) for f in found), found)

    # REQ-042 A3: shapes-square must be there.
    def test_a_missing_shapes_square_is_reported(self):
        lib = Lib(25).approve().drop("shapes-square")
        found = failures(call_check(lib))
        self.assertTrue(any("missing shapes-square" in f for f in found), found)

    # A ledger entry with no file is a warning, never a failure.
    def test_a_ledger_entry_without_a_file_is_not_a_failure(self):
        lib = Lib().approve()
        lib.ledger["animals-ghost"] = {"sha256": "0" * 64, "date": "2026-10-05", "by": "jami"}
        self.assertEqual([], failures(call_check(lib)))

    # `check` with pre_review True: the library of 0 reviewed puzzles reports nothing blocking in the way main's exit code tells (see below);
    # here: a changed-after-approval finding is still returned.
    def test_pre_review_still_reports_an_edit_after_approval(self):
        lib = Lib().approve()
        victim = lib.ids()[2]
        lib.texts[victim] = re.sub(r'#([0-9A-Fa-f])', lambda m: '#' + ('0' if m.group(1) != '0' else '1'), lib.texts[victim], count=1)
        found = call_check(lib, pre_review=True)
        self.assertTrue(any("changed after approval" in f and victim in f for f in found), found)


# ------------------------------------------------------------------------------------------------------------------- main()
class MainTests(unittest.TestCase):
    def run_dir(self, lib, *more, **kw):
        d = Disk(lib, **kw)
        try:
            return run_main(d.argv(*more))
        finally:
            d.close()

    # The pass path through the command line: exit 0 and the summary line of section 4.
    def test_a_fully_approved_library_exits_zero_and_prints_the_summary(self):
        rc, out = self.run_dir(Lib(25, cats=["shapes", "animals", "things", "nature", "vehicles"]).approve())
        self.assertEqual(0, rc, out)
        self.assertRegex(out, r"V-09 library: 25 of 25 reviewed, 5 themes")

    # At the close the strict run is RED by design: nothing reviewed, `0 of N reviewed`, exit 1 (the ledger is empty).
    def test_strict_mode_with_nothing_reviewed_exits_one(self):
        rc, out = self.run_dir(Lib(25))
        self.assertEqual(1, rc, out)
        self.assertRegex(out, r"0 of 25 reviewed")

    # --pre-review: the only findings are `unreviewed` and the count, so it exits 0 and says so.
    def test_pre_review_with_nothing_reviewed_exits_zero(self):
        rc, out = self.run_dir(Lib(25), "--pre-review")
        self.assertEqual(0, rc, out)
        self.assertRegex(out, r"0 of 25 reviewed")

    def test_pre_review_with_a_partial_review_exits_zero(self):
        lib = Lib(25)
        lib.approve(lib.ids()[:6])
        rc, out = self.run_dir(lib, "--pre-review")
        self.assertEqual(0, rc, out)
        rc, out = self.run_dir(lib)
        self.assertEqual(1, rc, "strict mode stays red until every puzzle is reviewed: " + out)

    # --pre-review still fails on everything but `unreviewed` and the count.
    def test_pre_review_still_fails_on_an_edit_after_approval(self):
        lib = Lib(25).approve()
        victim = lib.ids()[2]
        lib.texts[victim] = re.sub(r'#([0-9A-Fa-f])', lambda m: '#' + ('0' if m.group(1) != '0' else '1'), lib.texts[victim], count=1)
        rc, out = self.run_dir(lib, "--pre-review")
        self.assertEqual(1, rc, out)
        self.assertIn("changed after approval", out)

    def test_pre_review_still_fails_on_an_unledgered_flag(self):
        lib = Lib(25).approve()
        lib.ledger.pop(lib.ids()[9])
        rc, out = self.run_dir(lib, "--pre-review")
        self.assertEqual(1, rc, out)
        self.assertIn("unledgered", out)

    def test_pre_review_still_fails_on_too_few_themes(self):
        rc, out = self.run_dir(Lib(25, cats=["shapes", "animals", "things"]), "--pre-review")
        self.assertEqual(1, rc, out)
        self.assertRegex(out, r"themes 3 \(< 4\)")

    def test_pre_review_still_fails_on_a_missing_shapes_square(self):
        rc, out = self.run_dir(Lib(25).drop("shapes-square"), "--pre-review")
        self.assertEqual(1, rc, out)
        self.assertIn("missing shapes-square", out)

    # The count is never waived by --pre-review only in the sense of section 4: with nothing wrong but unreviewed puzzles it exits 0 (above);
    # a library of too few files is also only a count finding there. Strict mode names it.
    def test_strict_mode_names_the_count_when_the_library_is_small(self):
        rc, out = self.run_dir(Lib(19).approve())
        self.assertEqual(1, rc, out)
        self.assertRegex(out, r"count 19 of 19 reviewed \(< 20\)")

    # --min changes the bar.
    def test_the_min_option_sets_the_bar(self):
        rc, out = self.run_dir(Lib(25).approve(), "--min", "30")
        self.assertEqual(1, rc, out)
        self.assertRegex(out, r"\(< 30\)")
        rc, out = self.run_dir(Lib(25).approve(), "--min", "25")
        self.assertEqual(0, rc, out)

    # The folder holds more than puzzle files (the schema, a README, a previews folder): none is a puzzle.
    def test_non_puzzle_files_of_the_folder_are_ignored(self):
        rc, out = self.run_dir(Lib(25).approve())
        self.assertEqual(0, rc, out)

    # A ledger entry with no file only warns.
    def test_a_ledger_entry_without_a_file_still_exits_zero(self):
        lib = Lib(25).approve()
        ledger = dict(lib.ledger)
        ledger["animals-ghost"] = {"sha256": "0" * 64, "date": "2026-10-05", "by": "jami"}
        rc, out = self.run_dir(lib, ledger=ledger)
        self.assertEqual(0, rc, out)

    # CRLF files: the approved hash is of the LF content.
    def test_crlf_files_are_not_an_edit(self):
        lib = Lib(25).approve()
        rc, out = self.run_dir(lib, crlf=set(lib.ids()))
        self.assertEqual(0, rc, out)

    # Exit 2: cannot read.
    def test_an_unreadable_puzzle_file_fails_even_in_pre_review(self):
        lib = Lib(25).approve()
        rc, out = self.run_dir(lib, extra_texts={"animals-broken": "{ this is not json"})
        # section 4: a parse error fails even --pre-review; exit 1 (a finding) or 2 (cannot read) both say so, 0 never
        self.assertIn(rc, (1, 2), out)
        rc, out = self.run_dir(lib, "--pre-review", extra_texts={"animals-broken": "{ this is not json"})
        self.assertIn(rc, (1, 2), out)

    def test_a_missing_folder_exits_two(self):
        d = Disk(Lib(25).approve())
        try:
            rc, out = run_main(["--dir", str(d.root / "nowhere"), "--ledger", str(d.ledger)])
        finally:
            d.close()
        self.assertEqual(2, rc, out)

    def test_an_unreadable_ledger_exits_two(self):
        d = Disk(Lib(25).approve())
        try:
            d.ledger.write_text("{ not json", encoding="utf-8")
            rc, out = run_main(d.argv())
        finally:
            d.close()
        self.assertEqual(2, rc, out)

    # --apk: the store-bound artifact's `tangrams/*.json` entries are read like the folder.
    def make_apk(self, lib, crlf=()):
        tmp = tempfile.TemporaryDirectory()
        apk = Path(tmp.name) / "app.apk"
        with zipfile.ZipFile(apk, "w") as z:
            z.writestr("AndroidManifest.xml", b"\x00manifest")
            z.writestr("classes.dex", b"dex\n035\x00")
            z.writestr("tangrams/index.txt", "\n".join(p + ".json" for p in lib.ids()) + "\n")
            for pid, text in lib.texts.items():
                z.writestr("tangrams/%s.json" % pid, (text.replace("\n", "\r\n") if pid in crlf else text).encode("utf-8"))
        led = Path(tmp.name) / "ledger.json"
        led.write_text(json.dumps(encode_ledger(lib.ledger, SEAM["ledger"])), encoding="utf-8")
        return tmp, apk, led

    def test_apk_mode_passes_a_fully_approved_library(self):
        tmp, apk, led = self.make_apk(Lib(25, cats=["shapes", "animals", "things", "nature", "vehicles"]).approve())
        try:
            rc, out = run_main(["--apk", str(apk), "--ledger", str(led)])
        finally:
            tmp.cleanup()
        self.assertEqual(0, rc, out)
        self.assertRegex(out, r"V-09 library: 25 of 25 reviewed")

    def test_apk_mode_fails_strict_and_passes_pre_review_when_nothing_is_reviewed(self):
        tmp, apk, led = self.make_apk(Lib(25))
        try:
            rc, out = run_main(["--apk", str(apk), "--ledger", str(led)])
            self.assertEqual(1, rc, out)
            self.assertRegex(out, r"0 of 25 reviewed")
            rc, out = run_main(["--apk", str(apk), "--ledger", str(led), "--pre-review"])
            self.assertEqual(0, rc, out)
        finally:
            tmp.cleanup()

    def test_apk_mode_sees_an_unreviewed_puzzle(self):
        lib = Lib(25).approve()
        victim = lib.ids()[1]
        lib.texts[victim] = set_provenance(lib.texts[victim], "false", lib.layout[victim])
        lib.ledger.pop(victim)
        tmp, apk, led = self.make_apk(lib)
        try:
            rc, out = run_main(["--apk", str(apk), "--ledger", str(led)])
        finally:
            tmp.cleanup()
        self.assertEqual(1, rc, out)
        self.assertIn(victim, out)

    def test_apk_mode_reads_crlf_entries_like_lf(self):
        lib = Lib(25).approve()
        tmp, apk, led = self.make_apk(lib, crlf=set(lib.ids()))
        try:
            rc, out = run_main(["--apk", str(apk), "--ledger", str(led)])
        finally:
            tmp.cleanup()
        self.assertEqual(0, rc, out)


if __name__ == "__main__":
    unittest.main(verbosity=1)
