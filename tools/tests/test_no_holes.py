"""V15 self-tests (no-holes rule). Scaffolding-level unit tests of validate_puzzles.count_holes."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import json
from validate_puzzles import count_holes, validate, V15_EXEMPT


class NoHolesTest(unittest.TestCase):
    def test_solid_square_passes(self):
        self.assertEqual(count_holes([[(0, 0), (2, 0), (2, 2)], [(0, 0), (2, 2), (0, 2)]]), 0)

    def test_person_hole_geometry_fails(self):
        # LT2, ST1, ST2 and neighbours of the second people-person: hole (2,4)-(3,4)-(2,5)
        polys = [[(1, 5), (1, 1), (3, 3)], [(2, 4), (2, 8), (0, 6)], [(1, 1), (2, 0), (3, 1), (2, 2)],
                 [(4, 4), (2, 4), (3, 3)], [(4, 5), (2, 5), (3, 4)], [(4, 5), (4, 7), (5, 8), (5, 6)],
                 [(5, 4), (5, 6), (3, 4)]]
        self.assertEqual(count_holes(polys), 1)

    def test_outer_pinch_only_passes(self):
        # two squares meeting at one corner plus a bridge: outer pinch, no enclosed area
        polys = [[(0, 0), (1, 0), (1, 1), (0, 1)], [(1, 1), (2, 1), (2, 2), (1, 2)],
                 [(1, 0), (2, 0), (1, 1)], [(0, 1), (1, 1), (1, 2)]]
        self.assertEqual(count_holes(polys), 0)
        # pure point pinch of two squares
        self.assertEqual(count_holes([[(0, 0), (1, 0), (1, 1), (0, 1)], [(1, 1), (2, 1), (2, 2), (1, 2)]]), 0)

    def test_exemption_list_is_exactly_the_three_warmups(self):
        self.assertEqual(set(V15_EXEMPT), {"shapes-warmup-2", "shapes-warmup-3", "shapes-warmup-4"})

    def test_pocketed_fixture_with_other_id_fails(self):
        root = Path(__file__).resolve().parent.parent.parent / "Tangrams"
        p = json.loads((root / "shapes-warmup-4.json").read_text(encoding="utf-8"))
        errs, info = validate(p)
        self.assertFalse([e for e in errs if e.startswith("V15")])
        self.assertTrue([i for i in info if i.startswith("V15 exempt")])
        p["id"] = "shapes-warmup-9"
        errs, _ = validate(p)
        self.assertTrue([e for e in errs if e.startswith("V15")])


if __name__ == "__main__":
    unittest.main()
