"""SCAFFOLDING-adjacent check for TASK-009 (WO-002 design 4): the golden carries the validator verdict and difficulty."""
import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import export_geometry_golden as exp  # noqa: E402
from validate_puzzles import validate  # noqa: E402

GOLDEN = json.loads((TOOLS / "golden" / "geometry.json").read_text(encoding="utf-8"))


class GoldenVerdict(unittest.TestCase):
    def test_every_file_has_verdict_and_difficulty(self):
        files = exp.puzzle_files()
        self.assertEqual(sorted(f.stem for f in files), sorted(GOLDEN["puzzles"]))
        for f in files:
            raw = json.loads(f.read_text(encoding="utf-8"))
            entry = GOLDEN["puzzles"][f.stem]
            errors, _ = validate(raw)
            self.assertEqual(errors, [], f.name)
            self.assertEqual(entry["validator"]["pass"], True)
            self.assertEqual(entry["validator"]["errors"], [])
            self.assertEqual(entry["difficulty"], raw["difficulty"])

    def test_exposure_only_for_warmups_and_at_least_half(self):
        for name, entry in GOLDEN["puzzles"].items():
            exposure = entry["validator"]["exposure"]
            if entry["kind"] == "warmup":
                self.assertEqual(len(exposure), 7, name)
                for v in exposure.values():
                    self.assertGreaterEqual(v, 0.5 - 1e-6, name)
            else:
                self.assertEqual(exposure, {}, name)

    def test_failing_puzzle_fails_export(self):
        bad = TOOLS.parent / "Tangrams" / "shapes-warmup-1.json"
        errors = []
        orig = exp.validate
        exp.validate = lambda p: (["V4 forced"], [])
        try:
            self.assertIsNone(exp.build_puzzle(bad, errors))
        finally:
            exp.validate = orig
        self.assertTrue(any("validator fails" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
