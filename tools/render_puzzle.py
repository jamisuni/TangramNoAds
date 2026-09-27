#!/usr/bin/env python3
"""
Render puzzle files to SVG preview sheets so a human (or an AI with vision) can
review a puzzle before it ships. One sheet per puzzle shows the four target
display styles used by the assist levels (Spec/01-gameplay.md):

    Colors  - each slot tinted with its piece colour + inner lines   (level 1)
    Lines   - neutral slots with inner lines                          (level 2)
    Shadow  - solid silhouette only                                   (levels 3-4)
    Pieces  - solved with the coloured pieces (moment of solving)
    Picture - the stylised picture revealed after solving (art layer)

Usage:
    python tools/render_puzzle.py Tangrams  out_dir
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from tangram_geom import PIECE_COLORS, to_float  # noqa: E402
from validate_puzzles import load_solution  # noqa: E402

SHADOW = "#4A4E69"
SLOT_NEUTRAL = "#D9DCE8"
LINE = "#8A8FA8"


def tint(hex_color, amount=0.6):
    """Mix a colour with white (amount=1 -> white)."""
    h = hex_color.lstrip("#")
    r, g, b = (int(h[i:i + 2], 16) for i in (0, 2, 4))
    r, g, b = (round(c + (255 - c) * amount) for c in (r, g, b))
    return f"#{r:02X}{g:02X}{b:02X}"


def fit(polys, x, y, w, h, pad=0.08):
    """Return function mapping puzzle coords into box (x,y,w,h), aspect preserved, centred."""
    pts = [p for poly in polys.values() for p in poly]
    minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
    miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
    sw, sh = maxx - minx, maxy - miny
    scale = min(w * (1 - 2 * pad) / sw, h * (1 - 2 * pad) / sh)
    ox = x + (w - sw * scale) / 2 - minx * scale
    oy = y + (h - sh * scale) / 2 - miny * scale
    return lambda p: (ox + p[0] * scale, oy + p[1] * scale), scale


def poly_path(poly, f):
    return " ".join(f"{a:.2f},{b:.2f}" for a, b in (f(p) for p in poly))


def puzzle_svg_group(puzzle, mode, x, y, w, h, scale_override=None, hide=()):
    """SVG <g> for the puzzle target in the given mode, fitted to the box.
    hide: pieces not drawn in 'solved' mode (used for mid-game mockups)."""
    polys = {k: to_float(v) for k, v in load_solution(puzzle, []).items()}
    f, scale = fit(polys, x, y, w, h)
    if scale_override:
        # keep centre, force a scale (so tray and board can share units)
        cx, cy = x + w / 2, y + h / 2
        pts = [p for poly in polys.values() for p in poly]
        mx = (min(p[0] for p in pts) + max(p[0] for p in pts)) / 2
        my = (min(p[1] for p in pts) + max(p[1] for p in pts)) / 2
        scale = scale_override
        f = lambda p: (cx + (p[0] - mx) * scale, cy + (p[1] - my) * scale)  # noqa: E731
    out = ["<g>"]
    sw = max(1.0, scale * 0.04)
    for pid, poly in polys.items():
        pts = poly_path(poly, f)
        if mode == "colors":
            out.append(f'<polygon points="{pts}" fill="{tint(PIECE_COLORS[pid], 0.62)}" stroke="{tint(PIECE_COLORS[pid], 0.2)}" stroke-width="{sw:.2f}" stroke-dasharray="{sw*3:.1f} {sw*2:.1f}" stroke-linejoin="round"/>')
        elif mode == "lines":
            out.append(f'<polygon points="{pts}" fill="{SLOT_NEUTRAL}" stroke="{LINE}" stroke-width="{sw:.2f}" stroke-linejoin="round"/>')
        elif mode == "shadow":
            out.append(f'<polygon points="{pts}" fill="{SHADOW}" stroke="{SHADOW}" stroke-width="{sw*2:.2f}" stroke-linejoin="round"/>')
        elif mode == "solved":
            if pid in hide:
                continue
            out.append(f'<polygon points="{pts}" fill="{PIECE_COLORS[pid]}" stroke="#FFFFFF" stroke-width="{sw:.2f}" stroke-linejoin="round"/>')
    out.append("</g>")
    return "\n".join(out), f, scale, polys


def art_svg(puzzle, f, scale, clip_id):
    """The solved picture: art shapes in puzzle units, clipped to the silhouette."""
    polys = {k: to_float(v) for k, v in load_solution(puzzle, []).items()}
    art = puzzle.get("art", {"base": SHADOW, "shapes": []})
    clip = "".join(f'<polygon points="{poly_path(p, f)}" stroke="#000" stroke-width="{scale*0.02:.2f}" stroke-linejoin="round"/>' for p in polys.values())
    out = [f'<clipPath id="{clip_id}">{clip}</clipPath>', f'<g clip-path="url(#{clip_id})">']
    xs = [f(p)[0] for poly in polys.values() for p in poly]
    ys = [f(p)[1] for poly in polys.values() for p in poly]
    out.append(f'<rect x="{min(xs)-2}" y="{min(ys)-2}" width="{max(xs)-min(xs)+4}" height="{max(ys)-min(ys)+4}" fill="{art["base"]}"/>')
    for sh in art.get("shapes", []):
        t = sh["type"]
        paint = f'fill="{sh.get("fill", "none")}"'
        if "stroke" in sh:
            paint += f' stroke="{sh["stroke"]}" stroke-width="{sh.get("width", 0.05) * scale:.2f}" stroke-linecap="round" stroke-linejoin="round"'
            if t in ("path", "line") and "fill" not in sh:
                paint = paint.replace('fill="none"', 'fill="none"')
        if "opacity" in sh:
            paint += f' opacity="{sh["opacity"]}"'
        if t == "polygon":
            out.append(f'<polygon points="{poly_path(sh["points"], f)}" {paint}/>')
        elif t == "rect":
            x, y = f((sh["x"], sh["y"]))
            out.append(f'<rect x="{x:.2f}" y="{y:.2f}" width="{sh["w"]*scale:.2f}" height="{sh["h"]*scale:.2f}" rx="{sh.get("rx", 0)*scale:.2f}" {paint}/>')
        elif t == "circle":
            x, y = f(sh["c"])
            out.append(f'<circle cx="{x:.2f}" cy="{y:.2f}" r="{sh["r"]*scale:.2f}" {paint}/>')
        elif t == "ellipse":
            x, y = f(sh["c"])
            out.append(f'<ellipse cx="{x:.2f}" cy="{y:.2f}" rx="{sh["rx"]*scale:.2f}" ry="{sh["ry"]*scale:.2f}" {paint}/>')
        elif t == "line":
            (x1, y1), (x2, y2) = f(sh["from"]), f(sh["to"])
            out.append(f'<line x1="{x1:.2f}" y1="{y1:.2f}" x2="{x2:.2f}" y2="{y2:.2f}" {paint}/>')
        elif t == "path":
            out.append(f'<path d="{transform_path(sh["d"], f)}" {paint}/>')
    out.append("</g>")
    return "\n".join(out)


def transform_path(d, f):
    """Map an SVG path made of absolute M/L/Q/C/Z commands from puzzle units to screen."""
    import re as _re
    toks = _re.findall(r"[MLQCZ]|-?\d*\.?\d+", d)
    out, nums = [], []
    for tk in toks + ["M"]:
        if tk in "MLQCZ" and not tk.lstrip("-").replace(".", "").isdigit():
            for i in range(0, len(nums) - 1, 2):
                x, y = f((nums[i], nums[i + 1]))
                out.append(f"{x:.2f},{y:.2f}")
            nums = []
            out.append(tk)
        else:
            nums.append(float(tk))
    return " ".join(out[:-1])


def sheet(puzzle):
    W, H, cell = 665, 290, 200
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="Nunito, Arial, sans-serif">',
             f'<rect width="{W}" height="{H}" fill="#FBF8F2"/>',
             f'<text x="20" y="32" font-size="20" font-weight="700" fill="#2B2D42">{puzzle["title"]["en"]}  <tspan fill="#8A8FA8" font-size="14" font-weight="400">{puzzle["id"]} · difficulty {puzzle["difficulty"]} · {len(puzzle["solution"])} pieces</tspan></text>']
    for i, (mode, label) in enumerate((("shadow", "Silhouette (what the player sees)"), ("solved", "Pieces (the moment of solving)"), ("picture", "Picture (after solving)"))):
        x = 20 + i * (cell + 15)
        parts.append(f'<rect x="{x}" y="50" width="{cell}" height="{cell}" rx="14" fill="#FFFFFF" stroke="#E6E1D6"/>')
        if mode == "picture":
            _, f, scale, _ = puzzle_svg_group(puzzle, "shadow", x, 50, cell, cell)
            parts.append(art_svg(puzzle, f, scale, f"clip-{puzzle['id']}"))
        else:
            g, *_ = puzzle_svg_group(puzzle, mode, x, 50, cell, cell)
            parts.append(g)
        parts.append(f'<text x="{x + cell/2}" y="275" text-anchor="middle" font-size="13" fill="#5C5F77">{label}</text>')
    parts.append("</svg>")
    return "\n".join(parts)


def main(argv):
    src = Path(argv[0] if argv else "Tangrams")
    out = Path(argv[1] if len(argv) > 1 else "Tangrams/previews")
    out.mkdir(parents=True, exist_ok=True)
    files = sorted(f for f in src.glob("*.json") if not f.name.endswith(".schema.json")) if src.is_dir() else [src]
    for p in files:
        puzzle = json.loads(p.read_text(encoding="utf-8"))
        (out / f"{p.stem}.svg").write_text(sheet(puzzle), encoding="utf-8")
        print(f"rendered {out / (p.stem + '.svg')}")


if __name__ == "__main__":
    main(sys.argv[1:])
