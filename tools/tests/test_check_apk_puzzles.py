"""Self-test for tools/check_apk_puzzles.py (TASK-020): proves the check can pass AND fail, on fixture zips built in a temp dir."""
import shutil
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import check_apk_puzzles as chk  # noqa: E402


class CheckApkPuzzles(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.repo = self.tmp / "Tangrams"
        self.repo.mkdir()
        (self.repo / "a.json").write_text('{"id":"a"}\n', encoding="utf-8", newline="\n")
        (self.repo / "b.json").write_text('{"id":"b"}\n', encoding="utf-8", newline="\n")
        (self.repo / "puzzle.schema.json").write_text("{}", encoding="utf-8", newline="\n")

    def apk(self, entries):
        p = self.tmp / "fixture.apk"
        with zipfile.ZipFile(p, "w") as z:
            z.writestr("classes.dex", b"x")
            for k, v in entries.items():
                z.writestr(k, v)
        return p

    def good(self):
        return {
            "tangrams/a.json": (self.repo / "a.json").read_bytes(),
            "tangrams/b.json": (self.repo / "b.json").read_bytes(),
            "tangrams/index.txt": b"a.json\nb.json\n",
        }

    def test_match_passes(self):
        self.assertEqual(chk.check(self.apk(self.good()), self.repo), [])
        self.assertEqual(chk.main([str(self.apk(self.good())), str(self.repo)]), 0)

    def test_changed_puzzle_fails(self):
        e = self.good()
        e["tangrams/b.json"] = b'{"id":"B"}\n'
        d = chk.check(self.apk(e), self.repo)
        self.assertEqual(len(d), 1)
        self.assertIn("CHANGED: tangrams/b.json", d[0])
        self.assertEqual(chk.main([str(self.apk(e)), str(self.repo)]), 1)

    def test_missing_puzzle_fails(self):
        e = self.good()
        del e["tangrams/a.json"]
        d = chk.check(self.apk(e), self.repo)
        self.assertEqual(d, ["MISSING in APK: tangrams/a.json"])

    def test_extra_puzzle_fails(self):
        e = self.good()
        e["tangrams/c.json"] = b"{}"
        self.assertEqual(chk.check(self.apk(e), self.repo), ["EXTRA in APK: tangrams/c.json"])

    def test_schema_must_not_ship(self):
        e = self.good()
        e["tangrams/puzzle.schema.json"] = b"{}"
        self.assertEqual(chk.check(self.apk(e), self.repo), ["EXTRA in APK: tangrams/puzzle.schema.json"])

    def test_bad_index_fails(self):
        e = self.good()
        e["tangrams/index.txt"] = b"a.json\n"
        d = chk.check(self.apk(e), self.repo)
        self.assertEqual(len(d), 1)
        self.assertIn("index differs", d[0])

    def test_empty_repo_folder_fails(self):
        empty = self.tmp / "Empty"
        empty.mkdir()
        apk = self.apk({"tangrams/index.txt": b"\n"})
        d = chk.check(apk, empty)
        self.assertEqual(len(d), 1)
        self.assertIn("NO PUZZLES", d[0])
        self.assertEqual(chk.main([str(apk), str(empty)]), 1)

    def test_duplicate_entry_fails(self):
        p = self.tmp / "dup.apk"
        with zipfile.ZipFile(p, "w") as z:
            for k, v in self.good().items():
                z.writestr(k, v)
            with self.assertWarns(UserWarning):   # zipfile warns on a duplicate name; the APK still holds both
                z.writestr("tangrams/a.json", self.good()["tangrams/a.json"])   # even an identical twin fails
        d = chk.check(p, self.repo)
        self.assertEqual(d, ["DUPLICATE entry in APK: tangrams/a.json (2 times)"])

    def test_nested_path_fails(self):
        e = self.good()
        e["tangrams/sub/a.json"] = b'{"id":"a"}\n'
        self.assertEqual(chk.check(self.apk(e), self.repo), ["EXTRA in APK: tangrams/sub/a.json"])

    def test_case_only_difference_fails(self):
        e = self.good()
        e["tangrams/A.json"] = e.pop("tangrams/a.json")
        d = chk.check(self.apk(e), self.repo)
        self.assertIn("MISSING in APK: tangrams/a.json", d)
        self.assertIn("EXTRA in APK: tangrams/A.json", d)

    def test_wrong_case_folder_fails(self):
        e = self.good()
        e["Tangrams/c.json"] = b"{}"
        self.assertEqual(chk.check(self.apk(e), self.repo), ["EXTRA in APK: Tangrams/c.json"])


if __name__ == "__main__":
    unittest.main()
