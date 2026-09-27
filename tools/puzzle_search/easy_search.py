#!/usr/bin/env python3
"""
Random search for easy warm-up puzzles (how shapes-warmup-1..4 were found).

    python tools/puzzle_search/easy_search.py SEED TRIES [OUT.json]
    e.g.  python tools/puzzle_search/easy_search.py 1 20000 cands.json

Builds random edge-joined 7-piece shapes, keeps those that pass V11 (buildable
edge-first) and where at least 40 % of every piece's outline lies on the
silhouette edge ("exposure"), and writes the 40 best as exact polygons.
A candidate is only raw material: turn it into a Tangrams/*.json file, add a
picture, then run tools/validate_puzzles.py and tools/render_puzzle.py.
"""
import sys, random, math, json
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from tangram_geom import *
IDS = ["LT1","LT2","MT","SQ","PG","ST1","ST2"]
def fpoly(p): return to_float(p)
def perim(fp): return sum(math.hypot(fp[i][0]-fp[i-1][0], fp[i][1]-fp[i-1][1]) for i in range(len(fp)))
def try_build(rng):
    placed = {}
    order = IDS[:]; rng.shuffle(order)
    first = order[0]
    placed[first] = piece_polygon(first, rng.choice([0,2,4,6]), False, (Q2(0),Q2(0)))
    for pid in order[1:]:
        ok=False
        for _ in range(200):
            rot = rng.choice([0,2,4,6]); flip = rng.random()<0.5 if pid=="PG" else False
            local = piece_polygon(pid, rot, flip, (Q2(0),Q2(0)))
            v = rng.choice(local)
            tgt = rng.choice(rng.choice(list(placed.values())))
            at = (tgt[0]-v[0], tgt[1]-v[1])
            poly = [(x+at[0], y+at[1]) for x,y in local]
            fp = fpoly(poly)
            if any(overlap_depth(fp, fpoly(q))>1e-6 for q in placed.values()): continue
            if max(shared_edge_length(fp, fpoly(q)) for q in placed.values()) < 1.41: continue
            placed[pid]=poly; ok=True; break
        if not ok: return None
    return placed
def score(placed):
    fl = {k: fpoly(v) for k,v in placed.items()}
    ex=[]
    for k,p in fl.items():
        sh = sum(shared_edge_length(p, q) for kk,q in fl.items() if kk!=k)
        ex.append(1 - sh/perim(p))
    xs=[x for p in fl.values() for x,_ in p]; ys=[y for p in fl.values() for _,y in p]
    w,h = max(xs)-min(xs), max(ys)-min(ys)
    return min(ex), sum(ex)/len(ex), w, h
rng = random.Random(int(sys.argv[1]) if len(sys.argv)>1 else 1)
cands=[]
for i in range(int(sys.argv[2])):
    pl = try_build(rng)
    if not pl: continue
    o, stuck = build_order(pl)
    if stuck: continue
    mn, av, w, h = score(pl)
    if mn < 0.4: continue
    if max(w,h) > 7 or min(w,h) < 3 or w*h > 30: continue
    cands.append((mn+av - 0.02*w*h/16, pl, mn, av, w, h))
cands.sort(key=lambda c: -c[0])
out=[]
for c in cands[:40]:
    out.append({"score":round(c[0],3),"min":round(c[2],2),"avg":round(c[3],2),"w":round(c[4],2),"h":round(c[5],2),
                "poly":{k:[[x.to_json(),y.to_json()] for x,y in v] for k,v in c[1].items()}})
json.dump(out, open(sys.argv[3] if len(sys.argv) > 3 else f"c_{sys.argv[1]}.json", "w"))
print(len(cands), "candidates; top scores", [o["score"] for o in out[:10]])
