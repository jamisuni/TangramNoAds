#!/usr/bin/env python3
"""
Held-out kit compare (WO-006 T6b, F3 / E2).

Usage:  python tools/compare_kits.py <heldout-root> <visible-root>

Each root is the directory that contains `app/src/androidTest/...` (held-out: `.swdev/heldout/WO-006`; visible: `.swdev/staged/WO-006`
until MOVE-DEV6, then the repository root). For every file in `.../acceptance/held/` whose name also exists in `.../acceptance/layout/`
(the visible helper), the two are compared after the `package` line is dropped and the package part of every import line
(`acceptance.held.` / `acceptance.layout.`) is neutralised. Output is ONLY `<name>: equal` or `<name>: differs`, never any body text.
Exit 0 when every compared file is equal, 1 when any differs or nothing could be compared, 2 on a usage or path error. Stdlib only.
"""
import re
import sys
from pathlib import Path

SUB = Path("app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance")


def normalise(path: Path) -> str:
    out = []
    for line in path.read_text(encoding="utf-8").replace("\r\n", "\n").split("\n"):
        if re.match(r"^\s*package\s+[\w.]+\s*$", line):
            continue
        if line.lstrip().startswith("import "):
            line = re.sub(r"\bacceptance\.(held|layout)\.", "acceptance.X.", line)
        out.append(line.rstrip())
    return "\n".join(out).strip()


def main(argv) -> int:
    if len(argv) != 3:
        print("usage: compare_kits.py <heldout-root> <visible-root>", file=sys.stderr)
        return 2
    held_dir = Path(argv[1]) / SUB / "held"
    vis_dir = Path(argv[2]) / SUB / "layout"
    for d in (held_dir, vis_dir):
        if not d.is_dir():
            print(f"missing directory: {d}", file=sys.stderr)
            return 2
    compared = 0
    differs = 0
    for held in sorted(held_dir.glob("*.kt")):
        vis = vis_dir / held.name
        if not vis.is_file():
            continue
        compared += 1
        if normalise(held) == normalise(vis):
            print(f"{held.name}: equal")
        else:
            print(f"{held.name}: differs")
            differs += 1
    if compared == 0:
        print("nothing compared: no held-out file has a visible helper of the same name", file=sys.stderr)
        return 1
    return 1 if differs else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
