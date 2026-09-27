#!/usr/bin/env python3
"""
Exhaustive solver that fills a target outline with the tangram pieces using the
game's own locking rule (each piece placed on an outline corner or a corner of
an earlier piece, fully inside, no overlap). This is how shapes-rectangle was
found (the 5-piece inner part was solved separately, then rotated by -45°).

    python tools/puzzle_search/rect_solver.py

Edit TARGETS below: each target is an exact polygon (Q2 = a + b*sqrt(2)).
Slow for all 7 pieces; set IDS to a subset to solve part of a shape.
"""
import sys, itertools
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from tangram_geom import *
IDS = ["LT1","LT2","MT","SQ","PG","ST1","ST2"]
def clip(subject, clipper):
    out = subject
    for i in range(len(clipper)):
        A, B = clipper[i], clipper[(i+1) % len(clipper)]; inp = out; out = []
        if not inp: break
        side = lambda p: (B[0]-A[0])*(p[1]-A[1]) - (B[1]-A[1])*(p[0]-A[0])
        for j in range(len(inp)):
            P, Q = inp[j], inp[(j+1) % len(inp)]; sp, sq = side(P), side(Q)
            if sp >= -1e-9: out.append(P)
            if (sp >= -1e-9) != (sq >= -1e-9):
                t = sp/(sp-sq); out.append((P[0]+t*(Q[0]-P[0]), P[1]+t*(Q[1]-P[1])))
    return out
def sarea(P): return sum(P[i][0]*P[(i+1)%len(P)][1]-P[(i+1)%len(P)][0]*P[i][1] for i in range(len(P)))/2
def ccw(P): return P if sarea(P) > 0 else P[::-1]
def inter(P, Q):
    r = clip(ccw(P), ccw(Q)); return abs(sarea(r)) if len(r) > 2 else 0
def solve(target_exact):
    tf = to_float(target_exact)
    variants = {}
    for pid in IDS:
        vs = []; seen = set()
        for rot in range(8):
            for flip in ((False, True) if pid == "PG" else (False,)):
                loc = piece_polygon(pid, rot, flip, (Q2(0), Q2(0)))
                key = frozenset(loc)
                if key in seen: continue
                seen.add(key); vs.append(loc)
        variants[pid] = vs
    placed = {}
    def anchors():
        s = set(target_exact)
        for p in placed.values(): s.update(p)
        return s
    def rec(i):
        if i == len(IDS): return True
        pid = IDS[i]
        for loc in variants[pid]:
            for v in loc:
                for a in list(anchors()):
                    poly = [(x + a[0] - v[0], y + a[1] - v[1]) for x, y in loc]
                    fp = to_float(poly); ar = abs(sarea(fp))
                    if inter(fp, tf) < ar - 1e-6: continue
                    if any(inter(fp, to_float(q)) > 1e-6 for q in placed.values()): continue
                    placed[pid] = poly
                    if rec(i + 1): return True
                    del placed[pid]
        return False
    return dict(placed) if rec(0) else None
s = Q2(0, 1)
TARGETS = {
  "rect-classic": [(Q2(0),Q2(0)),(Q2(0,4),Q2(0)),(Q2(0,4),Q2(0,2)),(Q2(0),Q2(0,2))],
}
import json
res = {}
for name, t in TARGETS.items():
    sol = solve(t)
    print(name, "solved" if sol else "no solution")
    if sol:
        o, stuck = build_order(sol)
        print("  build order", o, "stuck", stuck)
        res[name] = {k: [[x.to_json(), y.to_json()] for x, y in v] for k, v in sol.items()}
json.dump(res, open("solutions.json", "w"))
print("wrote solutions.json")
