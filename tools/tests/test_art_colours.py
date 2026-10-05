"""SCAFFOLDING (disposable self-tests of TASK-091-0, not acceptance tests): validator rule V14, the art colour
count of DA-162 (distinct normalised #RRGGBB over base, fills and strokes; a tint below opacity 1 adds a colour)."""
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
from validate_puzzles import art_colour_count, validate_art  # noqa: E402


def v14(art):
    return [e for e in validate_art(art) if e.startswith("V14")]


def rect(fill, **kw):
    return {"type": "rect", "x": 0, "y": 0, "w": 1, "h": 1, "fill": fill, **kw}


class ArtColours(unittest.TestCase):
    def test_pass(self):
        art = {"base": "#111111", "shapes": [rect("#222222"), rect("#333333")]}
        self.assertEqual(art_colour_count(art), 3)
        self.assertEqual(v14(art), [])

    def test_case_is_normalised(self):
        art = {"base": "#aabbcc", "shapes": [rect("#AABBCC"), rect("#222222"), rect("#333333")]}
        self.assertEqual(art_colour_count(art), 3)

    def test_too_few(self):
        art = {"base": "#111111", "shapes": [rect("#222222")]}
        self.assertEqual(len(v14(art)), 1)

    def test_too_many(self):
        art = {"base": "#000000", "shapes": [rect("#%02X%02X%02X" % (i, i, i)) for i in range(1, 9)]}
        self.assertEqual(art_colour_count(art), 9)
        self.assertEqual(len(v14(art)), 1)

    def test_eight_passes_and_stroke_counts(self):
        art = {"base": "#000000", "shapes": [rect("#%02X%02X%02X" % (i, i, i)) for i in range(1, 7)]
               + [{"type": "line", "from": [0, 0], "to": [1, 1], "stroke": "#0A0A0A"}]}
        self.assertEqual(art_colour_count(art), 8)
        self.assertEqual(v14(art), [])

    def test_tint_adds_a_colour(self):
        plain = {"base": "#FFFFFF", "shapes": [rect("#222222"), rect("#333333")]}
        tinted = {"base": "#FFFFFF", "shapes": [rect("#222222"), rect("#333333"), rect("#FFFFFF", opacity=0.35)]}
        self.assertEqual(art_colour_count(plain), 3)
        self.assertEqual(art_colour_count(tinted), 4)
        two_tints = {"base": "#FFFFFF", "shapes": [rect("#222222", opacity=0.6), rect("#222222", opacity=0.85)]}
        self.assertEqual(art_colour_count(two_tints), 3)


if __name__ == "__main__":
    unittest.main()
