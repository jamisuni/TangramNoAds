"""Shared-table test for the validator's V13 path-data check (CR-1 F4): the cases below are the accepted and
rejected lists of play/src/test/.../SolvedPiecesScaffoldingTest.kt, which pins the Kotlin PathData grammar."""
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
from validate_puzzles import parse_path_data, validate_art  # noqa: E402

ACCEPTED = [
    "M3.3,3.35 Q3.7,3.55 4,3.35",
    "M0 -1.5L.5,2 Z",
    "M0,0 C1,1 2,2 3,3",
    "M0 0L1-2Z",
    "M0-0",
    "M1-2",
]

REJECTED = [
    "", " ", "L1,1", "m1,1", "M1,1 l2,2", "M1,1 2,2", "M1", "M1,2,3", "M1,2 L3", "M1e2,3", "M+1,2",
    "M1,,2", "M,1 2", "M1.,2", "M1,2 Z3", "M1--2", "M0 0L1--2Z", "M1,2-3", "M1,2 Q1,2,3", "M1,2 X", "M1,2,",
    "M NaN,1", "M1,2 L3,4,", "M1,2 Z,",
]


class PathData(unittest.TestCase):
    def test_accepted(self):
        for d in ACCEPTED:
            self.assertIsNotNone(parse_path_data(d), d)

    def test_rejected(self):
        for d in REJECTED:
            self.assertIsNone(parse_path_data(d), repr(d))

    def test_values(self):
        self.assertEqual(parse_path_data("M0 0L1-2Z"), [("M", [0.0, 0.0]), ("L", [1.0, -2.0]), ("Z", [])])
        self.assertEqual(parse_path_data("M0 -1.5L.5,2 Z")[1], ("L", [0.5, 2.0]))

    def art(self, d):
        return {"base": "#FFFFFF", "shapes": [{"type": "path", "d": d, "stroke": "#000000"}]}

    def test_validator_reports_v13(self):
        self.assertEqual(validate_art(self.art("M0 0L1-2Z")), [])
        errs = validate_art(self.art("M1--2"))
        self.assertEqual(len(errs), 1)
        self.assertTrue(errs[0].startswith("V13 art.shapes[0] (path)"), errs)


if __name__ == "__main__":
    unittest.main()
