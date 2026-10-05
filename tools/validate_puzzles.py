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
  V13 every art path `d` (art.shapes[i] with "type": "path") parses under the same grammar as the Kotlin
      play/.../PathData.kt: absolute M L Q C Z only, first command M, a minus may start a number, no exponent,
      no implicit repetition (CR-1 F4)
  V14 the art colour count is 3..8 inclusive (REQ-039, DA-162): distinct normalised (upper-case) #RRGGBB over
      art.base and every shape's fill and stroke; a shape at an opacity below 1 adds one visible colour per distinct
      (hex, opacity) pair, because a tint over the base is a colour the player sees
  V15 the union of the placed pieces has no hole: no uncovered area is enclosed by the pieces (a hole would
      make the one-ring outline of the silhouette wrong). Exact: the boundary edges are chained into closed walks;
      more than the one outer walk means a hole. A hole that touches the outside at a single pinch point is still a hole.
      Exempt by id (V15_EXEMPT, printed as information): shapes-warmup-2, shapes-warmup-3, shapes-warmup-4 (DA-169).
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


# V15 grandfathered pockets, by id (never by kind: a new warm-up gets no exemption). DA-169 update.
_GRANDFATHER = "renders correctly, device pixel tests green on API 37 and API 26 (WO-008/009 device runs); grandfathered"
V15_EXEMPT = {
    "shapes-warmup-2": f"pocket pinched at (4, 6); {_GRANDFATHER}",
    "shapes-warmup-3": f"pocket pinched at (4, 3); {_GRANDFATHER}",
    "shapes-warmup-4": f"pocket pinched at (2, 2); {_GRANDFATHER}",
}


def count_holes(fp):
    """V15: number of enclosed uncovered regions (holes) of the union of the float polygons fp.

    Exact, combinatorial: piece edges are split at every vertex lying on them; an edge used by one piece only is a
    boundary edge. Boundary edges are chained into closed walks with the uncovered area on the left (at each vertex
    take the next boundary edge clockwise from the way back). A walk with positive area bounds a hole; the one
    walk with negative area is the outer boundary. A hole that meets the outside only at a pinch point is
    still a hole (its walk is separate).
    """
    def key(pt):
        return (round(pt[0] * 1e6), round(pt[1] * 1e6))
    verts = {key(pt): pt for poly in fp for pt in poly}
    directed = set()
    for poly in fp:
        if sum(poly[i - 1][0] * poly[i][1] - poly[i][0] * poly[i - 1][1] for i in range(len(poly))) < 0:
            poly = poly[::-1]   # counter-clockwise: the piece is on the left of each edge
        for i in range(len(poly)):
            a, b = poly[i - 1], poly[i]
            ab = (b[0] - a[0], b[1] - a[1])
            ll = ab[0] ** 2 + ab[1] ** 2
            on = [a, b]
            for v in verts.values():
                if key(v) in (key(a), key(b)):
                    continue
                av = (v[0] - a[0], v[1] - a[1])
                if abs(ab[0] * av[1] - ab[1] * av[0]) > 1e-7:
                    continue
                if 1e-9 < (av[0] * ab[0] + av[1] * ab[1]) / ll < 1 - 1e-9:
                    on.append(v)
            on.sort(key=lambda v: (v[0] - a[0]) * ab[0] + (v[1] - a[1]) * ab[1])
            for j in range(len(on) - 1):
                directed.add((key(on[j]), key(on[j + 1])))
    boundary = {(v, u) for (u, v) in directed if (v, u) not in directed}   # reversed: uncovered area on the left
    out = {}
    for u, v in boundary:
        out.setdefault(u, []).append(v)
    seen, holes = set(), 0
    for start in boundary:
        if start in seen:
            continue
        walk, cur = [], start
        while cur not in seen:
            seen.add(cur)
            walk.append(cur[0])
            u, v = cur
            back = math.atan2(verts[u][1] - verts[v][1], verts[u][0] - verts[v][0])
            best = None
            for w in out[v]:
                turn = (back - math.atan2(verts[w][1] - verts[v][1], verts[w][0] - verts[v][0])) % (2 * math.pi)
                if turn < 1e-12:
                    turn = 2 * math.pi
                if best is None or turn < best[0]:
                    best = (turn, w)
            cur = (v, best[1])
        pts = [verts[k] for k in walk]
        if sum(pts[i - 1][0] * pts[i][1] - pts[i][0] * pts[i - 1][1] for i in range(len(pts))) > 1e-9:
            holes += 1
    return holes


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

    # V15 no hole
    holes = count_holes(list(fp.values()))
    if holes and puzzle.get("id") in V15_EXEMPT:
        info.append(f"V15 exempt ({puzzle.get('id')}): {V15_EXEMPT[puzzle.get('id')]}")
    elif holes:
        errors.append(f"V15 the pieces enclose {holes} uncovered hole(s); the silhouette must be simply connected")

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
ART_COLOURS_MIN, ART_COLOURS_MAX = 3, 8   # REQ-039 rule, DA-162


def art_colour_count(art):
    """DA-162: number of distinct visible colours of an art block (a set of colour keys is returned by art_colours)."""
    return len(art_colours(art))


def art_colours(art):
    """Distinct visible colours: normalised #RRGGBB of art.base and of every fill and stroke; a shape with opacity
    below 1 contributes (hex, opacity) pairs instead of the bare hex. Malformed colours are skipped (V10 reports them)."""
    out = set()
    if not isinstance(art, dict):
        return out
    base = str(art.get("base", ""))
    if HEX.match(base):
        out.add((base.upper(), 1.0))
    for sh in art.get("shapes", []):
        if not isinstance(sh, dict):
            continue
        op = sh.get("opacity", 1)
        op = float(op) if isinstance(op, (int, float)) and not isinstance(op, bool) else 1.0
        for key in ("fill", "stroke"):
            c = str(sh.get(key, ""))
            if HEX.match(c):
                out.add((c.upper(), op if op < 1 else 1.0))
    return out


def parse_path_data(d):
    """Mirror of play/.../PathData.kt `parse` (CR-1 F4): returns a list of (command, numbers) or None.

    Absolute M L Q C Z only, first command M. Letters may touch numbers; each command takes exactly its arity of
    numbers (no implicit repetition); a number is -?(digits(.digits)?|.digits) (no exponent, no +); separators are
    runs of spaces and/or one comma (between numbers only); every value finite.
    """
    if not isinstance(d, str):
        return None
    out = []
    n = len(d)

    def skip_spaces(k):
        while k < n and d[k] == " ":
            k += 1
        return k

    i = skip_spaces(0)
    if i >= n or d[i] != "M":
        return None
    while True:
        i = skip_spaces(i)
        if i >= n:
            break
        c = d[i]
        arity = {"M": 2, "L": 2, "Q": 4, "C": 6, "Z": 0}.get(c)
        if arity is None:
            return None
        i += 1
        vals = []
        for k in range(arity):
            before = i
            i = skip_spaces(i)
            sep = i > before
            if k > 0 and i < n and d[i] == ",":
                i = skip_spaces(i + 1)
                sep = True
            if k > 0 and not sep and not (i < n and d[i] == "-"):
                return None   # a minus always starts a number
            start = i
            if i < n and d[i] == "-":
                i += 1
            int_start = i
            while i < n and "0" <= d[i] <= "9":
                i += 1
            int_digits = i - int_start
            frac_digits = 0
            if i < n and d[i] == ".":
                i += 1
                fs = i
                while i < n and "0" <= d[i] <= "9":
                    i += 1
                frac_digits = i - fs
                if frac_digits == 0:
                    return None
            if int_digits == 0 and frac_digits == 0:
                return None
            x = float(d[start:i])
            if not math.isfinite(x):
                return None
            vals.append(x)
        if i < n and d[i] != " " and d[i] not in "MLQCZ":
            return None
        out.append((c, vals))
    return out


def path_data_error(d):
    """None when `d` is accepted by the Kotlin grammar, else a short reason."""
    if not isinstance(d, str):
        return "d must be a string"
    return None if parse_path_data(d) is not None else "not in the M/L/Q/C/Z grammar of PathData.kt"


def validate_art(art):
    if art is None:
        return ["V10 art is missing (every puzzle needs a solved picture)"]
    errs = []
    if not HEX.match(str(art.get("base", ""))):
        errs.append("V10 art.base must be a #RRGGBB colour")
    n = art_colour_count(art)
    if not ART_COLOURS_MIN <= n <= ART_COLOURS_MAX:
        errs.append(f"V14 the art uses {n} visible colours; REQ-039 allows {ART_COLOURS_MIN} to {ART_COLOURS_MAX}")
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
        if t == "path" and "d" in sh:
            why = path_data_error(sh["d"])
            if why:
                errs.append(f"V13 art.shapes[{i}] (path): bad path data {sh['d']!r} ({why})")
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
