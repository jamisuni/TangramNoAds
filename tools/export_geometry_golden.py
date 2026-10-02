#!/usr/bin/env python3
"""
Export the geometry golden: the reference answers the Kotlin kernel must reproduce (G-03, WO-001 design 8).

    python tools/export_geometry_golden.py      -> tools/golden/geometry.json

Reads every Tangrams/*.json (not the schema) with the validator's loader and writes, deterministically
(sorted keys, LF, indent 1): the five piece shapes with their areas, the 80 transforms (5 shapes x 2
mirrors x 8 turns at (0,0)), and per puzzle file its sha256 (CRLF normalized to LF), kind, solution
polygons with their poses, outline corners, area and build order.

Exact numbers are strings: R = "n" or "n/d"; Q = [R, R] is a + b*sqrt(2); P = [Q, Q] is a point
(the JSON floats of the puzzle files are not exact for thirds).

Exit code 0 when every check passes, 1 otherwise. Checks:
  - every puzzle file loads (load_solution) and has no stuck piece in build_order;
  - the mask decision `is_outline_corner_mask` gives the expected answer on a fixed set of masks;
  - the sampling oracle below recomputes each puzzle's outline corners geometrically and must equal
    tangram_geom.outline_corners on every file (this is what would have caught the old angle-sum rule);
  - shapes-warmup-4 lists (2,2) as an outline corner (DA-7 pin).

Re-run it whenever Tangrams/ or tools/ change (AGENTS.md rule 10).
"""
import hashlib
import json
import math
import sys
from fractions import Fraction as F
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from tangram_geom import (Q2, PIECE_SET, PIECE_TYPES, build_order, is_outline_corner_mask, outline_corners,  # noqa: E402
                          placement_from_polygon, to_float, transform)
from validate_puzzles import load_solution  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "tools" / "golden" / "geometry.json"
FORMAT = "tangram-golden/1"

SAMPLE_RADIUS = 1e-3   # the oracle samples the 8 wedge mid-directions at this distance from a vertex


# ---------------------------------------------------------------------------
# Exact values as strings
# ---------------------------------------------------------------------------
def R(fr):
    fr = F(fr)
    return str(fr.numerator) if fr.denominator == 1 else f"{fr.numerator}/{fr.denominator}"


def Q(q):
    return [R(q.a), R(q.b)]


def P(pt):
    return [Q(pt[0]), Q(pt[1])]


def exact_area(poly):
    """Exact shoelace area of an exact polygon (Q2 points); must be rational."""
    s = Q2(0)
    for i in range(len(poly)):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % len(poly)]
        s = s + x1 * y2 - x2 * y1
    if s.b != 0:
        raise ValueError(f"area has a sqrt(2) part: {s!r}")
    return abs(s.a) / 2


# ---------------------------------------------------------------------------
# Sampling oracle for outline corners (independent of tangram_geom.outline_corners)
# ---------------------------------------------------------------------------
def _inside(pt, poly):
    """Strict interior test of a point against a convex float polygon (either orientation)."""
    sign = 0
    for i in range(len(poly)):
        ax, ay = poly[i]
        bx, by = poly[(i + 1) % len(poly)]
        c = (bx - ax) * (pt[1] - ay) - (by - ay) * (pt[0] - ax)
        if abs(c) < 1e-12:
            return False
        s = 1 if c > 0 else -1
        if sign == 0:
            sign = s
        elif s != sign:
            return False
    return True


def oracle_corners(polys):
    """Outline corners recomputed geometrically: for every solution vertex, which of the 8 wedge
    mid-directions (22.5, 67.5, ... degrees) are inside some piece at a small radius, then the same
    contiguity decision. Returns the set of exact corner points."""
    fl = [to_float(v) for v in polys.values()]
    corners = set()
    for poly in polys.values():
        for p in poly:
            if p in corners:
                continue
            fp = (float(p[0]), float(p[1]))
            mask = 0
            for k in range(8):
                a = math.radians(45 * k + 22.5)
                s = (fp[0] + SAMPLE_RADIUS * math.cos(a), fp[1] + SAMPLE_RADIUS * math.sin(a))
                if any(_inside(s, q) for q in fl):
                    mask |= 1 << k
            if is_outline_corner_mask(mask):
                corners.add(p)
    return corners


# (mask, is corner?) fixed cases for the decision itself (full circle, a wrapping run, 3+1, 2+2, runs of 1..3, 5, 6)
MASK_CASES = [
    (0b11111111, False),
    (0b00001111, False), (0b00011110, False), (0b11000011, False),   # runs of 4, the last wraps 6,7,0,1
    (0b00100111, True), (0b10010011, True),                          # runs 3 + 1 separate (the second wraps 7,0,1)
    (0b00110011, True), (0b01100110, True),                          # 2 + 2 separate
    (0b00000001, True), (0b00000011, True), (0b00000111, True),      # runs of 1..3
    (0b00011111, True), (0b00111111, True),                          # runs of 5 and 6
    (0b01111111, True), (0b00000000, True),
]


def check_mask_decision(errors):
    for mask, corner in MASK_CASES:
        if is_outline_corner_mask(mask) != corner:
            errors.append(f"is_outline_corner_mask({mask:08b}) should be {corner}")


# ---------------------------------------------------------------------------
# The golden
# ---------------------------------------------------------------------------
def build_shapes():
    out = {}
    for t, (local, area, _sym, _chiral) in PIECE_TYPES.items():
        out[t] = {"local": [P((Q2(x), Q2(y))) for x, y in local], "area": R(area)}
    return out


def build_transforms():
    rows = []
    for t, (local, _a, _s, _c) in PIECE_TYPES.items():
        lq = [(Q2(x), Q2(y)) for x, y in local]
        for mirrored in (False, True):
            for turn in range(8):
                rows.append({"shape": t, "turn": turn, "mirrored": mirrored,
                             "corners": [P(c) for c in transform(lq, turn, mirrored, (Q2(0), Q2(0)))]})
    return rows


def puzzle_files():
    return sorted(f for f in (ROOT / "Tangrams").glob("*.json") if not f.name.endswith(".schema.json"))


def build_puzzle(path, errors):
    raw = path.read_bytes()
    puzzle = json.loads(raw.decode("utf-8"))
    load_errors = []
    polys = load_solution(puzzle, load_errors)
    if load_errors:
        errors.append(f"{path.name}: does not load: {'; '.join(load_errors)}")
        return None
    if puzzle.get("id") != path.stem:
        errors.append(f"{path.name}: id {puzzle.get('id')!r} differs from the file name")
        return None

    order, stuck = build_order(polys)
    if stuck:
        errors.append(f"{path.name}: build_order leaves pieces stuck: {stuck}")
        return None

    corners = outline_corners(polys)
    oracle = oracle_corners(polys)
    if set(corners) != oracle:
        only_ref = sorted(set(corners) - oracle, key=repr)
        only_oracle = sorted(oracle - set(corners), key=repr)
        errors.append(f"{path.name}: outline_corners disagrees with the sampling oracle "
                      f"(reference only: {only_ref}, oracle only: {only_oracle})")
        return None

    solution, total = [], F(0)
    for pid, poly in polys.items():
        pose = placement_from_polygon(pid, poly)
        if pose is None:
            errors.append(f"{path.name}: {pid} has no pose")
            return None
        rot, flip, at = pose
        area = exact_area(poly)
        if area != PIECE_TYPES[PIECE_SET[pid]][1]:
            errors.append(f"{path.name}: {pid} exact area {area} differs from its type's")
            return None
        total += area
        solution.append({"piece": pid, "polygon": [P(v) for v in poly],
                         "pose": {"turn": rot, "mirrored": bool(flip), "at": P(at)}})
    return {
        "sha256": hashlib.sha256(raw.replace(b"\r\n", b"\n")).hexdigest(),
        "kind": puzzle.get("kind", "full"),
        "solution": solution,
        "outlineCorners": [P(c) for c in corners],
        "area": R(total),
        "buildOrder": order,
    }, corners


def main():
    errors = []
    check_mask_decision(errors)
    puzzles = {}
    pins = {}
    for path in puzzle_files():
        built = build_puzzle(path, errors)
        if built is None:
            continue
        entry, corners = built
        puzzles[path.stem] = entry
        pins[path.stem] = corners
    if "shapes-warmup-4" not in pins:
        errors.append("shapes-warmup-4 is missing")
    elif (Q2(2), Q2(2)) not in pins["shapes-warmup-4"]:
        errors.append("shapes-warmup-4 must list (2,2) as an outline corner (DA-7)")
    if errors:
        for e in errors:
            print("FAIL " + e)
        return 1

    golden = {"format": FORMAT, "shapes": build_shapes(), "transforms": build_transforms(), "puzzles": puzzles}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(json.dumps(golden, sort_keys=True, indent=1, ensure_ascii=False) + "\n")
    print(f"mask decision: {len(MASK_CASES)} cases ok")
    print(f"sampling oracle agrees with outline_corners on {len(puzzles)} puzzle files; shapes-warmup-4 lists (2,2)")
    print(f"wrote {OUT} ({len(golden['shapes'])} shapes, {len(golden['transforms'])} transforms, {len(puzzles)} puzzles)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
