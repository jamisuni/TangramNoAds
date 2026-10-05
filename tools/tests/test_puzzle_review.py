"""SCAFFOLDING self-test for tools/puzzle_review.py (TASK-092): fixture folders in a temp dir only.
The real Tangrams/ and release/ are never written (the one real-folder test is a refusal that raises before any work)."""
import hashlib
import io
import json
import re
import shutil
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import puzzle_review as pr  # noqa: E402

INLINE = ('{\n  "format": "tangram-puzzle/1",\n  "id": "a-mini",\n  "title": {"en": "Mini", "fi": "Mini fi"},\n'
          '  "difficulty": 1,\n  "kind": "mini",\n'
          '  "provenance": {"author": "ai:x", "reviewedByHuman": false}\n}\n')
MULTI = ('{\n  "format": "tangram-puzzle/1",\n  "id": "b-full",\n  "title": {"en": "Full", "fi": "Full fi"},\n'
         '  "difficulty": 2,\n  "category": "shapes",\n'
         '  "provenance": {\n    "author": "ai:x",\n    "reviewedByHuman": false\n  }\n}\n')


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.dir = self.tmp / "Tangrams"
        self.dir.mkdir()
        self.put("a-mini", INLINE)
        self.put("b-full", MULTI)
        (self.dir / "puzzle.schema.json").write_text("{}", encoding="utf-8")
        self.ledger = self.tmp / "release" / "puzzle-review.json"

    def put(self, pid, text):
        (self.dir / (pid + ".json")).write_bytes(text.encode("utf-8"))

    def read(self, pid):
        return (self.dir / (pid + ".json")).read_bytes().decode("utf-8")

    def sheet_json(self):
        return json.loads(pr.sheet(self.dir, self.ledger)[1])

    def mark(self, ids, sheet=None, **kw):
        return pr.mark_files(self.dir, ids, self.ledger, sheet or self.sheet_json(), "owner", "2026-10-06", **kw)


class HashRules(unittest.TestCase):
    def test_crlf_equals_lf(self):
        self.assertEqual(pr.review_hash("a\r\nb\n"), pr.review_hash("a\nb\n"))

    def test_flag_normalised(self):
        f = '{"reviewedByHuman": false}'
        t = '{"reviewedByHuman":  true}'
        self.assertEqual(pr.review_hash(f), pr.review_hash(t))
        self.assertEqual(pr.review_hash(f), pr.review_hash('{"reviewedByHuman":false}'.replace(":false", ": false")))

    def test_content_changes_hash(self):
        self.assertNotEqual(pr.review_hash('{"a": 1, "reviewedByHuman": false}'), pr.review_hash('{"a": 2, "reviewedByHuman": false}'))

    def test_sha256_hex(self):
        self.assertEqual(pr.review_hash("x"), hashlib.sha256(b"x").hexdigest())


class Sheet(Base):
    def apk(self, entries):
        p = self.tmp / "fixture.apk"
        with zipfile.ZipFile(p, "w") as z:
            for k, v in entries.items():
                z.writestr(k, v)
        return p

    def good(self):
        return {"tangrams/a-mini.json": (self.dir / "a-mini.json").read_bytes(),
                "tangrams/b-full.json": (self.dir / "b-full.json").read_bytes(),
                "tangrams/index.txt": b"a-mini.json\nb-full.json\n"}

    def test_rows_order_and_columns(self):
        md, js = pr.sheet(self.dir, self.ledger)
        rows = json.loads(js)["puzzles"]
        self.assertEqual([r["id"] for r in rows], ["a-mini", "b-full"])
        self.assertEqual(rows[0]["kind"], "mini")
        self.assertEqual(rows[1]["kind"], "full")
        self.assertEqual(rows[1]["rating"], 2)
        self.assertEqual(rows[0]["review_hash"], pr.review_hash(self.read("a-mini")))
        self.assertEqual(rows[0]["status"], "waiting")
        self.assertIn("approve / fix / reject", md)
        self.assertIn("Mini fi", md)

    def test_theme_colours_preview(self):
        self.put("b-full", MULTI.replace('"category": "shapes",', '"category": "shapes", "art": {"base": "#aaaaaa", "shapes": [{"type": "rect", "x": 0, "y": 0, "w": 1, "h": 1, "fill": "#BBBBBB", "stroke": "#cccccc"}]},'))
        (self.dir / "previews").mkdir()
        (self.dir / "previews" / "b-full.png").write_bytes(b"x")
        rows = json.loads(pr.sheet(self.dir, self.ledger)[1])["puzzles"]
        self.assertEqual((rows[1]["theme"], rows[1]["colours"], rows[1]["preview"]), ("shapes", 3, "previews/b-full.png"))
        self.assertEqual((rows[0]["colours"], rows[0]["preview"]), (0, ""))
        # the link is computed from where the sheet is written (S2)
        md = pr.sheet(self.dir, self.ledger, out=self.tmp / "release")[0]
        self.assertIn("[previews/b-full.png](../Tangrams/previews/b-full.png)", md)

    def test_preview_link_resolves_from_sheet_folder(self):
        (self.dir / "previews").mkdir()
        (self.dir / "previews" / "b-full.png").write_bytes(b"x")
        out = self.tmp / "release"
        out.mkdir()
        md = pr.sheet(self.dir, self.ledger, out=out)[0]
        m = re.search(r"\]\(([^)]+)\)", md)
        self.assertIsNotNone(m)
        self.assertTrue((out / m.group(1)).resolve().is_file())

    def test_table_cell_counts_equal(self):
        (self.dir / "previews").mkdir()
        (self.dir / "previews" / "b-full.png").write_bytes(b"x")
        md = pr.sheet(self.dir, self.ledger, out=self.tmp / "release")[0]
        table = [ln for ln in md.splitlines() if ln.startswith("|")]
        self.assertEqual(len(table), 4)  # header, delimiter, 2 rows
        counts = {len(ln.strip().strip("|").split("|")) for ln in table}
        self.assertEqual(counts, {12})

    def test_matching_apk_accepted(self):
        _, js = pr.sheet(self.dir, self.ledger, self.apk(self.good()))
        self.assertEqual(json.loads(js)["apk_sha256"], hashlib.sha256((self.tmp / "fixture.apk").read_bytes()).hexdigest())

    def test_mismatched_apk_refused(self):
        e = self.good()
        e["tangrams/a-mini.json"] = b"{}"
        with self.assertRaises(ValueError):
            pr.sheet(self.dir, self.ledger, self.apk(e))

    def test_sheet_does_not_write(self):
        before = {f.name: f.read_bytes() for f in self.dir.iterdir()}
        pr.sheet(self.dir, self.ledger)
        self.assertEqual(before, {f.name: f.read_bytes() for f in self.dir.iterdir()})
        self.assertFalse(self.ledger.exists())


class MarkFiles(Base):
    def test_real_tangrams_refused(self):
        with self.assertRaises(PermissionError):
            pr.mark_files(pr.ROOT / "Tangrams", [], self.ledger, {"puzzles": []}, "x", "2026-10-06")
        self.assertFalse(self.ledger.exists())

    def test_hash_mismatch_refused_and_nothing_written(self):
        sh = self.sheet_json()
        self.put("b-full", MULTI.replace('"difficulty": 2', '"difficulty": 3'))
        before = self.read("a-mini")
        with self.assertRaises(ValueError):
            self.mark(["a-mini", "b-full"], sh)
        self.assertEqual(self.read("a-mini"), before)
        self.assertFalse(self.ledger.exists())

    def test_unknown_id_raises(self):
        with self.assertRaises(KeyError):
            self.mark(["nope"])

    def test_flip_both_forms_and_ledger(self):
        changed = self.mark(["a-mini", "b-full"])
        self.assertEqual(changed, ["a-mini", "b-full"])
        self.assertIn('"provenance": {"author": "ai:x", "reviewedByHuman": true}', self.read("a-mini"))
        self.assertIn('    "reviewedByHuman": true\n', self.read("b-full"))
        led = json.loads(self.ledger.read_text(encoding="utf-8"))
        self.assertEqual(led["a-mini"], {"sha256": pr.review_hash(INLINE), "by": "owner", "date": "2026-10-06"})
        self.assertEqual(led["b-full"]["sha256"], pr.review_hash(MULTI))
        # the flip leaves the review hash alone, and the sheet now says reviewed
        self.assertEqual([r["status"] for r in self.sheet_json()["puzzles"]], ["reviewed", "reviewed"])

    def test_crlf_file_keeps_endings(self):
        self.put("a-mini", INLINE.replace("\n", "\r\n"))
        self.mark(["a-mini"])
        t = self.read("a-mini")
        self.assertIn("\r\n", t)
        self.assertIn("true", t)

    def test_already_approved_is_not_changed(self):
        self.mark(["a-mini"])
        self.assertEqual(self.mark(["a-mini"]), [])

    def test_stale_needs_explicit_reapproval(self):
        self.mark(["a-mini"])
        self.put("a-mini", self.read("a-mini").replace('"difficulty": 1', '"difficulty": 2'))
        sh = self.sheet_json()
        self.assertEqual(sh["puzzles"][0]["status"], "STALE")
        with self.assertRaises(ValueError):
            self.mark(["a-mini"], sh)
        self.assertEqual(self.mark(["a-mini"], sh, reapprove=["a-mini"]), ["a-mini"])
        led = json.loads(self.ledger.read_text(encoding="utf-8"))
        self.assertEqual(led["a-mini"]["sha256"], sh["puzzles"][0]["review_hash"])
        self.assertEqual(self.sheet_json()["puzzles"][0]["status"], "reviewed")

    def test_sheet_as_text_and_path(self):
        sh_text = pr.sheet(self.dir, self.ledger)[1]
        self.assertEqual(self.mark(["a-mini"], sh_text), ["a-mini"])
        p = self.tmp / "sheet.json"
        p.write_text(sh_text, encoding="utf-8")
        self.assertEqual(self.mark(["b-full"], str(p)), ["b-full"])


class Cli(Base):
    def test_mark_without_tty_refused(self):
        fake = io.StringIO()
        with mock.patch.object(sys, "stdin", fake), mock.patch("sys.stdout", io.StringIO()):
            rc = pr.main(["mark", "a-mini", "--dir", str(self.dir), "--ledger", str(self.ledger), "--sheet", str(self.tmp / "s.json")])
        self.assertEqual(rc, 2)
        self.assertFalse(self.ledger.exists())

    def cli(self, answers, extra=(), tty=True):
        """Run `mark` on the fixture folder with a fake terminal; returns (rc, printed text)."""
        sp = self.tmp / "release" / "sheet.json"
        sp.parent.mkdir(parents=True, exist_ok=True)
        sp.write_text(pr.sheet(self.dir, self.ledger)[1], encoding="utf-8")
        it = iter(answers)
        out = io.StringIO()
        with mock.patch("sys.stdout", out):
            rc = pr.main(["mark", "a-mini", "--dir", str(self.dir), "--ledger", str(self.ledger), "--sheet", str(sp),
                          "--by", "owner", "--date", "2026-10-06", *extra],
                         isatty=lambda: tty, input_fn=lambda prompt: next(it))
        return rc, out.getvalue()

    def test_typed_approved_flips_and_writes_ledger(self):
        rc, _ = self.cli(["APPROVED"])
        self.assertEqual(rc, 0)
        self.assertIn('"reviewedByHuman": true', self.read("a-mini"))
        self.assertEqual(json.loads(self.ledger.read_text(encoding="utf-8"))["a-mini"]["sha256"], pr.review_hash(INLINE))

    def test_wrong_answer_changes_nothing(self):
        rc, text = self.cli(["approved"])
        self.assertEqual(rc, 0)
        self.assertIn("skipped a-mini", text)
        self.assertEqual(self.read("a-mini"), INLINE)
        self.assertFalse(self.ledger.exists())

    def make_stale(self):
        self.mark(["a-mini"])
        self.put("a-mini", self.read("a-mini").replace('"difficulty": 1', '"difficulty": 2'))

    def test_stale_without_reapprove_refused_nonzero(self):
        self.make_stale()
        before, led = self.read("a-mini"), self.ledger.read_text(encoding="utf-8")
        rc, text = self.cli([])  # no prompt may be reached
        self.assertNotEqual(rc, 0)
        self.assertIn("STALE", text)
        self.assertEqual(self.read("a-mini"), before)
        self.assertEqual(self.ledger.read_text(encoding="utf-8"), led)

    def test_reapprove_with_typed_approved(self):
        self.make_stale()
        rc, _ = self.cli(["APPROVED"], extra=["--reapprove"])
        self.assertEqual(rc, 0)
        self.assertEqual(json.loads(self.ledger.read_text(encoding="utf-8"))["a-mini"]["sha256"],
                         pr.review_hash(self.read("a-mini")))

    def test_golden_refresh_failure_is_nonzero(self):
        # pretend the fixture folder is the real Tangrams/ so the refresh runs; subprocess is mocked, nothing real runs
        with mock.patch.object(pr, "ROOT", self.tmp), \
                mock.patch.object(pr.subprocess, "run", return_value=mock.Mock(returncode=3)) as run:
            rc, text = self.cli(["APPROVED"])
        self.assertEqual(run.call_count, 1)
        self.assertEqual(rc, 1)
        self.assertIn("golden refresh failed", text)


if __name__ == "__main__":
    unittest.main()
