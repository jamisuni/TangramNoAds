#!/usr/bin/env python3
"""
Validate tangram puzzle files (Spec/03-puzzle-format.md).

Usage:
    python tools/validate_puzzles.py Tangrams            # validate every *.json
    python tools/validate_puzzles.py Tangrams/cat.json   # one file

Checks (rule ids match the spec, section "Validation rules"):
  V1  format tag and required fields present
  V2  piece ids are valid and unique
  V3  every polygon is congruent to its piece (exact arithmetic), or placement is valid
  V4  no two pieces overlap (interior)
  V5  shape is connected through shared edges (point-only joins are errors)
  V6  total area equals sum of piece areas (implied by V3+V4, reported for info)
  V7  (removed 2026-09-28; the id stays unused)
  V8  translations: 'en' and 'fi' titles are mandatory (the game ships both languages)
  V9  kind: "full" uses all seven pieces; "mini" (a fast first-success / test puzzle) uses 1-6;
      "warmup" uses all seven
  V10 art (solved picture) is well formed: base colour + known shape types
  V11 build order (information only): the order in which every piece locks on an outline corner or
      on a corner of a piece placed before it. Every valid tangram has one, because the uncovered
      region always has a convex corner (an anchor) and the piece covering it has a vertex there;
      the line is printed for authors and never fails a valid puzzle.
  V12 a "warmup" puzzle exposes at least half of every piece's outline on the silhouette edge
      (REQ-041), so each piece's shape can be seen in the silhouette
Exit code 0 when all files pass, 1 otherwise.
"""
import json
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from tangram_geom import (Q2, PIECE_SET, PIECE_TYPES, piece_polygon, placement_from_polygon,
                          to_float, area, overlap_depth, shared_edge_length, touches_at_point, build_order)

FORMAT_TAG = "tangram-puzzle/1"
KINDS = {"mini", "warmup", "full"}
WARMUP_EXPOSURE = 0.5   # REQ-041: at least half of every piece's outline on the silhouette edge


def perimeter(fp):
    return sum(math.hypot(fp[i][0] - fp[i - 1][0], fp[i][1] - fp[i - 1][1]) for i in range(len(fp)))
CATEGORIES = {"shapes", "animals", "people", "things", "vehicles", "nature", "letters", "numbers"}


def load_solution(puzzle, errors):
    """Returns {piece_id: exact polygon}. Accepts 'polygon' or 'rot/flip/at' entries."""
    polys = {}
    for i, entry in enumerate(puzzle.get("solution", [])):
        pid = entry.get("piece")
        if pid not in PIECE_SET:
            errors.append(f"V2 solution[{i}]: unknown piece id {pid!r}")
            continue
        if pid in polys:
            errors.append(f"V2 piece {pid} used twice")
            continue
        if "polygon" in entry:
            try:
                target = [(Q2.parse(x), Q2.parse(y)) for x, y in entry["polygon"]]
            except Exception as e:  # noqa: BLE001
                errors.append(f"V3 {pid}: bad coordinates ({e})")
                continue
            if placement_from_polygon(pid, target) is None:
                errors.append(f"V3 {pid}: polygon is not congruent to a {PIECE_SET[pid]} piece")
                continue
            polys[pid] = target
        else:
            try:
                at = (Q2.parse(entry["at"][0]), Q2.parse(entry["at"][1]))
                rot = int(entry.get("rot", 0))
                if not 0 <= rot <= 7:
                    raise ValueError("rot must be 0..7")
                polys[pid] = piece_polygon(pid, rot, bool(entry.get("flip", False)), at)
            except Exception as e:  # noqa: BLE001
                errors.append(f"V3 {pid}: bad placement ({e})")
    return polys


def validate(puzzle):
    errors, info = [], []
    # V1
    if puzzle.get("format") != FORMAT_TAG:
        errors.append(f"V1 format must be {FORMAT_TAG!r}")
    for field in ("id", "title", "category", "difficulty", "solution"):
        if field not in puzzle:
            errors.append(f"V1 missing field {field!r}")
    if puzzle.get("category") not in CATEGORIES:
        errors.append(f"V1 unknown category {puzzle.get('category')!r}")
    if not isinstance(puzzle.get("difficulty"), int) or not 1 <= puzzle.get("difficulty", 0) <= 5:
        errors.append("V1 difficulty must be an integer 1..5")
    # V8
    for lang in ("en", "fi"):
        if lang not in puzzle.get("title", {}):
            errors.append(f"V8 title.{lang} is mandatory")
    # V9 (kind)
    kind = puzzle.get("kind", "full")
    if kind not in KINDS:
        errors.append(f"V9 kind must be one of {sorted(KINDS)}")
    if "mini" in puzzle:
        errors.append("V9 the \"mini\" flag was replaced by \"kind\": \"mini\"")
    if errors:
        return errors, info

    polys = load_solution(puzzle, errors)
    if errors:
        return errors, info
    ids = list(polys)
    fp = {k: to_float(v) for k, v in polys.items()}

    # V4 overlaps
    for i in range(len(ids)):
        for j in range(i + 1, len(ids)):
            d = overlap_depth(fp[ids[i]], fp[ids[j]])
            if d > 1e-6:
                errors.append(f"V4 {ids[i]} overlaps {ids[j]} (depth {d:.3f})")

    # V5 connectivity via shared edges
    adj = {k: set() for k in ids}
    point_only = []
    for i in range(len(ids)):
        for j in range(i + 1, len(ids)):
            a, b = ids[i], ids[j]
            if shared_edge_length(fp[a], fp[b]) > 1e-6:
                adj[a].add(b)
                adj[b].add(a)
            elif touches_at_point(fp[a], fp[b]):
                point_only.append((a, b))
    seen, stack = set(), [ids[0]]
    while stack:
        n = stack.pop()
        if n in seen:
            continue
        seen.add(n)
        stack.extend(adj[n] - seen)
    if len(seen) != len(ids):
        errors.append(f"V5 shape not edge-connected; isolated: {sorted(set(ids) - seen)}")
    info.append(f"edge-joins={sum(len(v) for v in adj.values()) // 2} point-joins={len(point_only)}")

    # V6
    total = sum(area(p) for p in fp.values())
    expected = sum(PIECE_TYPES[PIECE_SET[k]][1] for k in ids)
    if abs(total - expected) > 1e-6:
        errors.append(f"V6 area {total} != {expected}")
    info.append(f"pieces={len(ids)} area={total:g}")

    # V9 (pieces per kind)
    if kind == "mini":
        if len(ids) >= len(PIECE_SET):
            errors.append("V9 a mini puzzle uses fewer than 7 pieces; use kind \"full\"")
        info.append("mini")
    elif set(ids) != set(PIECE_SET):
        errors.append(f"V9 a {kind} puzzle must use all 7 pieces; missing {sorted(set(PIECE_SET) - set(ids))}")
    if kind == "warmup":
        info.append("warmup")

    # V12 warm-up measure: exposure = the share of a piece's outline that lies on the silhouette edge
    if kind == "warmup":
        low = []
        for k, p in fp.items():
            shared = sum(shared_edge_length(p, q) for kk, q in fp.items() if kk != k)
            exposure = 1 - shared / perimeter(p)
            if exposure < WARMUP_EXPOSURE - 1e-6:
                low.append(f"{k} {exposure:.2f}")
        if low:
            errors.append(f"V12 a warm-up exposes at least half of every piece's outline; too hidden: {', '.join(low)}")

    # V10
    errors += validate_art(puzzle.get("art"))

    # V11
    if not errors:
        order, stuck = build_order(polys)
        if stuck:   # cannot happen for a valid tangram (see the docstring); kept as a sanity check of the tool
            errors.append(f"V11 build order search failed (tool bug?): stuck {stuck}")
        else:
            info.append("build order " + " ".join(order))
    return errors, info


ART_TYPES = {
    "polygon": {"points"}, "rect": {"x", "y", "w", "h"}, "circle": {"c", "r"},
    "ellipse": {"c", "rx", "ry"}, "line": {"from", "to"}, "path": {"d"},
}
HEX = re.compile(r"^#[0-9A-Fa-f]{6}$")


def validate_art(art):
    if art is None:
        return ["V10 art is missing (every puzzle needs a solved picture)"]
    errs = []
    if not HEX.match(str(art.get("base", ""))):
        errs.append("V10 art.base must be a #RRGGBB colour")
    for i, sh in enumerate(art.get("shapes", [])):
        t = sh.get("type")
        if t not in ART_TYPES:
            errs.append(f"V10 art.shapes[{i}]: unknown type {t!r}")
            continue
        missing = ART_TYPES[t] - set(sh)
        if missing:
            errs.append(f"V10 art.shapes[{i}] ({t}): missing {sorted(missing)}")
        for key in ("fill", "stroke"):
            if key in sh and not HEX.match(str(sh[key])):
                errs.append(f"V10 art.shapes[{i}].{key} must be #RRGGBB")
        if t in ("line", "path") and "stroke" not in sh:
            errs.append(f"V10 art.shapes[{i}] ({t}): needs a stroke colour")
    return errs


def main(argv):
    targets = []
    for arg in argv or ["Tangrams"]:
        p = Path(arg)
        targets += sorted(x for x in p.glob("*.json") if not x.name.endswith(".schema.json")) if p.is_dir() else [p]
    failed = 0
    for path in targets:
        try:
            puzzle = json.loads(path.read_text(encoding="utf-8"))
        except Exception as e:  # noqa: BLE001
            print(f"FAIL {path}: not valid JSON ({e})")
            failed += 1
            continue
        errors, info = validate(puzzle)
        status = "FAIL" if errors else "ok  "
        print(f"{status} {path.name}: {'; '.join(info)}")
        for e in errors:
            print(f"     {e}")
        failed += bool(errors)
    print(f"\n{len(targets) - failed}/{len(targets)} puzzle files valid")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
