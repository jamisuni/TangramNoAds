#!/usr/bin/env python3
"""
Build the clickable HTML prototype from the puzzle files.

    python tools/build_prototype.py   -> Spec/prototype/tangram-prototype.html

The prototype embeds every puzzle in Spec/puzzles (float coordinates + the exact
rot/flip of each slot computed by tangram_geom.placement_from_polygon, and the
silhouette's outline corners, which are the only fixed lock anchors).
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from tangram_geom import PIECE_SET, placement_from_polygon, to_float, outline_corners  # noqa: E402
from validate_puzzles import load_solution, validate  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent


def main():
    files = sorted((f for f in (ROOT / "Spec" / "puzzles").glob("*.json") if not f.name.endswith(".schema.json")),
                   key=lambda p: (json.loads(p.read_text(encoding="utf-8"))["difficulty"], p.stem))
    data = []
    for f in files:
        pz = json.loads(f.read_text(encoding="utf-8"))
        errors, _ = validate(pz)
        if errors:
            raise SystemExit(f"{f.name} is invalid: {errors}")
        slots = []
        for pid, poly in load_solution(pz, []).items():
            rot, flip, _ = placement_from_polygon(pid, poly)
            slots.append({"piece": pid, "type": PIECE_SET[pid], "rot": rot, "flip": flip,
                          "poly": [[round(x, 10), round(y, 10)] for x, y in to_float(poly)]})
        corners = [[round(float(x), 10), round(float(y), 10)] for x, y in outline_corners(load_solution(pz, []))]
        data.append({"id": pz["id"], "title": pz["title"]["en"], "slots": slots, "corners": corners,
                     "difficulty": pz["difficulty"], "art": pz["art"]})
    tpl = (Path(__file__).parent / "prototype_template.html").read_text(encoding="utf-8")
    out = ROOT / "Spec" / "prototype" / "tangram-prototype.html"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(tpl.replace("__PUZZLES__", json.dumps(data, separators=(",", ":"))), encoding="utf-8")
    print(f"wrote {out} with {len(data)} puzzles")


if __name__ == "__main__":
    main()
