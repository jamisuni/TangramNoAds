#!/usr/bin/env python3
"""
Generate the UI concept drawings in Spec/ui/ from the real puzzle geometry.

    python tools/make_ui_mockups.py            -> Spec/ui/*.svg

Sizes are in dp (1 SVG px = 1 dp). Phone frame = 360 x 780 dp, tablet = 1280 x 800 dp.
Drawings are wireframe-level concepts, not final art.
"""
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from tangram_geom import (PIECE_COLORS, PIECE_SET, PIECE_TYPES, Q2, to_float, transform,  # noqa: E402
                          placement_from_polygon)
from validate_puzzles import load_solution  # noqa: E402
from render_puzzle import tint, SHADOW, SLOT_NEUTRAL, LINE  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
PUZ = ROOT / "Spec" / "puzzles"
OUT = ROOT / "Spec" / "ui"

BG = "#FBF8F2"
PANEL = "#F1ECE1"
INK = "#2B2D42"
MUTED = "#8A8FA8"
ACCENT = "#3D8BFD"
FONT = 'font-family="Nunito, Segoe UI, Arial, sans-serif"'
TRAY_ORDER = ["LT1", "LT2", "MT", "SQ", "PG", "ST1", "ST2"]  # big -> small, fixed order (REQ-TRAY-3)
RANDOM_ROT = {"LT1": 3, "LT2": 6, "MT": 1, "ST1": 5, "ST2": 2, "SQ": 1, "PG": 7}  # demo "random" turns


def load(pid):
    return json.loads((PUZ / f"{pid}.json").read_text(encoding="utf-8"))


def pts(poly):
    return " ".join(f"{x:.1f},{y:.1f}" for x, y in poly)


class Board:
    """Maps puzzle coordinates into a screen box."""

    def __init__(self, puzzle, x, y, w, h, pad=0.1):
        self.puzzle = puzzle
        self.exact = load_solution(puzzle, [])
        self.polys = {k: to_float(v) for k, v in self.exact.items()}
        allp = [p for poly in self.polys.values() for p in poly]
        self.minx, self.maxx = min(p[0] for p in allp), max(p[0] for p in allp)
        self.miny, self.maxy = min(p[1] for p in allp), max(p[1] for p in allp)
        sw, sh = self.maxx - self.minx, self.maxy - self.miny
        self.s = min(w * (1 - 2 * pad) / sw, h * (1 - 2 * pad) / sh)
        self.ox = x + (w - sw * self.s) / 2 - self.minx * self.s
        self.oy = y + (h - sh * self.s) / 2 - self.miny * self.s

    def f(self, p):
        return (self.ox + p[0] * self.s, self.oy + p[1] * self.s)

    def screen(self, pid, dx=0, dy=0):
        return [(a + dx, b + dy) for a, b in map(self.f, self.polys[pid])]

    def target(self, mode):
        out = []
        sw = max(1.2, self.s * 0.035)
        for pid in self.polys:
            p = pts(self.screen(pid))
            if mode == "colors":
                c = PIECE_COLORS[pid]
                out.append(f'<polygon points="{p}" fill="{tint(c, .62)}" stroke="{tint(c, .15)}" stroke-width="{sw:.1f}" stroke-dasharray="6 4" stroke-linejoin="round"/>')
            elif mode == "lines":
                out.append(f'<polygon points="{p}" fill="{SLOT_NEUTRAL}" stroke="{LINE}" stroke-width="{sw:.1f}" stroke-linejoin="round"/>')
            else:
                out.append(f'<polygon points="{p}" fill="{SHADOW}" stroke="{SHADOW}" stroke-width="{sw*1.8:.1f}" stroke-linejoin="round"/>')
        return "\n".join(out)

    def placed(self, pid, dx=0, dy=0, extra=""):
        return (f'<polygon points="{pts(self.screen(pid, dx, dy))}" fill="{PIECE_COLORS[pid]}" stroke="#FFFFFF" '
                f'stroke-width="2" stroke-linejoin="round" {extra}/>')


def piece_shape(pid, rot, flip, scale, cx, cy):
    """Piece polygon (screen) with given turn, bbox-centred at (cx, cy)."""
    local = [(Q2(a), Q2(b)) for a, b in PIECE_TYPES[PIECE_SET[pid]][0]]
    poly = to_float(transform(local, rot, flip, (Q2(0), Q2(0))))
    mx = (min(p[0] for p in poly) + max(p[0] for p in poly)) / 2
    my = (min(p[1] for p in poly) + max(p[1] for p in poly)) / 2
    return [(cx + (x - mx) * scale, cy + (y - my) * scale) for x, y in poly]


def solution_turns(board):
    turns = {}
    for pid, poly in board.exact.items():
        rot, flip, _ = placement_from_polygon(pid, poly)
        turns[pid] = (rot, flip)
    return turns


def turn_diameter(pid):
    """Diameter of the circle a piece sweeps when turned about its centroid (orientation independent)."""
    poly = PIECE_TYPES[PIECE_SET[pid]][0]
    cx = sum(p[0] for p in poly) / len(poly)
    cy = sum(p[1] for p in poly) / len(poly)
    return 2 * max(math.hypot(x - cx, y - cy) for x, y in poly)


def tray_rows(ids, rows):
    if rows == 1:
        return [ids]
    big = [p for p in ids if PIECE_SET[p] in ("LT", "MT")]
    small = [p for p in ids if p not in big]
    return [big, small] if big and small else [ids[:math.ceil(len(ids) / 2)], ids[math.ceil(len(ids) / 2):]]


def tray_scale(ids, rows, w, gap, pad, max_cell_h):
    """Largest common miniature scale so every row fits the width (REQ-TRAY-4)."""
    best = float("inf")
    for row in tray_rows(ids, rows):
        d = sum(turn_diameter(p) for p in row)
        best = min(best, (w - gap * (len(row) + 1) - pad * len(row)) / d)
        best = min(best, (max_cell_h - pad) / max(turn_diameter(p) for p in row))
    return best


def tray(board, x, y, w, rows, max_cell_h, gap, state, turns, selected=None, pad=12):
    """Draw the tray panel. Cells are sized per piece (turn circle) so all miniatures share
    one, as-large-as-possible scale. state: pid -> 'tray' | 'ghost'.
    Returns svg, tray_scale, cell_centres, height."""
    ids = [p for p in TRAY_ORDER if p in board.polys]
    s = tray_scale(ids, rows, w, gap, pad, max_cell_h)
    out, centres = [], {}
    cy0 = y + gap
    row_list = tray_rows(ids, rows)
    for row in row_list:
        ch = max(turn_diameter(p) for p in row) * s + pad
        widths = [turn_diameter(p) * s + pad for p in row]
        free = w - sum(widths) - gap * (len(row) - 1)
        cx0 = x + free / 2
        for pid, cw in zip(row, widths):
            centres[pid] = (cx0 + cw / 2, cy0 + ch / 2)
            out.append(f'<rect x="{cx0:.1f}" y="{cy0:.1f}" width="{cw:.1f}" height="{ch:.1f}" rx="16" fill="#FFFFFF" stroke="#E2DBCB"/>')
            rot, flip = turns[pid]
            shape = piece_shape(pid, rot, flip, s, cx0 + cw / 2, cy0 + ch / 2)
            if state.get(pid) == "ghost":
                out.append(f'<polygon points="{pts(shape)}" fill="none" stroke="#D5CDBB" stroke-width="2" stroke-dasharray="5 4" stroke-linejoin="round"/>')
            else:
                out.append(f'<polygon points="{pts(shape)}" fill="{PIECE_COLORS[pid]}" stroke="#FFFFFF" stroke-width="1.5" stroke-linejoin="round"/>')
            if selected == pid:
                out.append(f'<rect x="{cx0-3:.1f}" y="{cy0-3:.1f}" width="{cw+6:.1f}" height="{ch+6:.1f}" rx="18" fill="none" stroke="{ACCENT}" stroke-width="3"/>')
            cx0 += cw + gap
        cy0 += ch + gap
    h = cy0 - y
    out.insert(0, f'<rect x="{x}" y="{y}" width="{w}" height="{h:.1f}" rx="22" fill="{PANEL}"/>')
    return "\n".join(out), s, centres, h


# ---------------------------------------------------------------- icons
def icon_button(cx, cy, r, kind, fill="#FFFFFF", stroke="#E2DBCB", color=INK):
    g = [f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="2"/>']
    k = r / 28
    if kind == "home":
        g.append(f'<path d="M{cx-13*k},{cy+1*k} L{cx},{cy-12*k} L{cx+13*k},{cy+1*k} M{cx-9*k},{cy-2*k} L{cx-9*k},{cy+12*k} L{cx+9*k},{cy+12*k} L{cx+9*k},{cy-2*k}" fill="none" stroke="{color}" stroke-width="{3.2*k}" stroke-linecap="round" stroke-linejoin="round"/>')
    elif kind == "hint":
        g.append(f'<circle cx="{cx}" cy="{cy-4*k}" r="{9*k}" fill="#FFD23F" stroke="{color}" stroke-width="{2.6*k}"/>')
        g.append(f'<rect x="{cx-5*k}" y="{cy+6*k}" width="{10*k}" height="{7*k}" rx="{2*k}" fill="{color}"/>')
        for a in (-60, -20, 20, 60, 180 + 20, 180 - 20):
            pass
    elif kind == "next":
        g.append(f'<path d="M{cx-7*k},{cy-12*k} L{cx+11*k},{cy} L{cx-7*k},{cy+12*k} Z" fill="{color}" stroke="{color}" stroke-width="{2*k}" stroke-linejoin="round"/>')
    elif kind == "again":
        g.append(f'<path d="M{cx+11*k},{cy-2*k} A{11*k},{11*k} 0 1 1 {cx+3*k},{cy-11*k}" fill="none" stroke="{color}" stroke-width="{3.4*k}" stroke-linecap="round"/>')
        g.append(f'<path d="M{cx},{cy-17*k} L{cx+8*k},{cy-11*k} L{cx},{cy-5*k} Z" fill="{color}"/>')
    elif kind == "flip":
        g.append(f'<path d="M{cx},{cy-13*k} L{cx},{cy+13*k}" stroke="{color}" stroke-width="{2*k}" stroke-dasharray="{3*k} {3*k}"/>')
        g.append(f'<path d="M{cx-4*k},{cy-9*k} L{cx-14*k},{cy+9*k} L{cx-4*k},{cy+9*k} Z" fill="{color}"/>')
        g.append(f'<path d="M{cx+4*k},{cy-9*k} L{cx+14*k},{cy+9*k} L{cx+4*k},{cy+9*k} Z" fill="none" stroke="{color}" stroke-width="{2*k}"/>')
    elif kind == "rotate":
        g.append(f'<path d="M{cx-10*k},{cy+4*k} A{11*k},{11*k} 0 1 1 {cx+6*k},{cy+9*k}" fill="none" stroke="{color}" stroke-width="{3*k}" stroke-linecap="round"/>')
        g.append(f'<path d="M{cx+1*k},{cy+14*k} L{cx+9*k},{cy+5*k} L{cx+11*k},{cy+15*k} Z" fill="{color}"/>')
    elif kind == "gear":
        for i in range(8):
            a = i * math.pi / 4
            g.append(f'<line x1="{cx+math.cos(a)*8*k:.1f}" y1="{cy+math.sin(a)*8*k:.1f}" x2="{cx+math.cos(a)*13*k:.1f}" y2="{cy+math.sin(a)*13*k:.1f}" stroke="{color}" stroke-width="{4*k}" stroke-linecap="round"/>')
        g.append(f'<circle cx="{cx}" cy="{cy}" r="{8*k}" fill="none" stroke="{color}" stroke-width="{3.5*k}"/>')
    return "\n".join(g)


def level_badge(cx, cy, level, r=18):
    colors = {1: "#FFD23F", 2: "#2DBE7E", 3: "#F9A826", 4: "#9B5DE5"}
    g = [f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{colors[level]}"/>']
    for i in range(level):
        dx = (i - (level - 1) / 2) * r * 0.42
        g.append(f'<circle cx="{cx+dx:.1f}" cy="{cy}" r="{r*0.15:.1f}" fill="#FFFFFF"/>')
    return "\n".join(g)


def finger(x, y, scale=1.0):
    """Stylised fingertip touching at (x, y)."""
    s = scale
    return (f'<g opacity="0.9"><circle cx="{x}" cy="{y}" r="{16*s}" fill="{ACCENT}" opacity="0.18"/>'
            f'<path d="M{x-11*s},{y+60*s} L{x-11*s},{y+2*s} A{11*s},{11*s} 0 0 1 {x+11*s},{y+2*s} L{x+11*s},{y+60*s} Z" '
            f'fill="#F4C9A8" stroke="#C99A78" stroke-width="1.5"/>'
            f'<path d="M{x-6*s},{y+4*s} A{6*s},{6*s} 0 0 1 {x+6*s},{y+4*s} L{x+6*s},{y+10*s} L{x-6*s},{y+10*s} Z" fill="#FBE3D2"/></g>')


def callout(x, y, tx, ty, text, anchor="start", width=None):
    lines = text.split("\n")
    out = [f'<line x1="{x}" y1="{y}" x2="{tx}" y2="{ty}" stroke="{ACCENT}" stroke-width="1.5"/>',
           f'<circle cx="{x}" cy="{y}" r="4" fill="{ACCENT}"/>']
    for i, ln in enumerate(lines):
        out.append(f'<text x="{tx + (6 if anchor == "start" else -6)}" y="{ty + 4 + i*17}" text-anchor="{anchor}" font-size="13.5" fill="{INK}" {FONT}>{ln}</text>')
    return "\n".join(out)


def defs():
    return ('<defs><filter id="lift" x="-30%" y="-30%" width="160%" height="160%">'
            '<feDropShadow dx="0" dy="10" stdDeviation="7" flood-color="#000" flood-opacity="0.28"/></filter>'
            '<filter id="glow" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="4"/></filter></defs>')


def device(x, y, w, h, radius=36, bezel=12):
    return (f'<rect x="{x-bezel}" y="{y-bezel}" width="{w+2*bezel}" height="{h+2*bezel}" rx="{radius+bezel}" fill="#23252F"/>'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" fill="{BG}"/>')


def svg(w, h, body, bg="#FFFFFF"):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" {FONT}>'
            f'{defs()}<rect width="{w}" height="{h}" fill="{bg}"/>{body}</svg>')


def title(x, y, main, sub=""):
    return (f'<text x="{x}" y="{y}" font-size="22" font-weight="800" fill="{INK}">{main}</text>'
            f'<text x="{x}" y="{y+22}" font-size="14" fill="{MUTED}">{sub}</text>')


# ---------------------------------------------------------------- screens
def play_phone(x0, y0, puzzle, mode, level, state, dragged=None, turns_mode="solution", show_flip=False, hint_on=None):
    """Phone play screen at (x0, y0), 360 x 780. Returns svg and geometry dict."""
    W, H = 360, 780
    body = [device(x0, y0, W, H)]
    # status bar + top bar
    body.append(f'<text x="{x0+24}" y="{y0+20}" font-size="12" fill="{MUTED}">9:41</text>')
    top = y0 + 28
    body.append(icon_button(x0 + 44, top + 34, 28, "home"))
    body.append(icon_button(x0 + W - 44, top + 34, 28, "hint", fill="#FFF6D6" if hint_on else "#FFFFFF"))
    body.append(level_badge(x0 + W / 2, top + 34, level))
    # tray (2 rows) at bottom
    cell, gap = 118, 8
    probe = Board(puzzle, 0, 0, 100, 100)
    turns0 = {p: (0, False) for p in probe.polys}
    tray_h = tray(probe, 0, 0, W - 16, 2, cell, gap, {}, turns0)[3]
    tray_y = y0 + H - 22 - tray_h
    # board fills between
    board_top = top + 70
    board = Board(puzzle, x0 + 8, board_top, W - 16, tray_y - board_top - 8)
    body.append(f'<rect x="{x0+8}" y="{board_top}" width="{W-16}" height="{tray_y-board_top-8}" rx="24" fill="#FFFFFF" stroke="#EFE9DC"/>')
    body.append(board.target(mode))
    for pid, st in state.items():
        if st == "board":
            body.append(board.placed(pid))
    turns = solution_turns(board) if turns_mode == "solution" else {p: (RANDOM_ROT[p], p == "PG") for p in board.polys}
    tray_state = {p: ("ghost" if state.get(p) in ("board", "drag") else "tray") for p in board.polys}
    t_svg, tscale, centres, th = tray(board, x0 + 8, tray_y, W - 16, 2, cell, gap, tray_state, turns)
    body.append(t_svg)
    body.append(f'<rect x="{x0+W/2-60}" y="{y0+H-12}" width="120" height="4" rx="2" fill="#C9C2B3"/>')
    return body, board, centres, tscale


def phone_play_svg():
    cat = load("animals-cat")
    W, H = 1060, 900
    b = [title(40, 46, "Phone · Play screen (portrait)", "Cat · level 2 “Lines” · two gift pieces pre-placed · the child is dragging a small triangle")]
    x0, y0 = 90, 90
    state = {"LT1": "board", "LT2": "board", "SQ": "board", "ST1": "drag"}
    body, board, centres, tscale = play_phone(x0, y0, cat, "lines", 2, state)
    b += body
    # target slot glow for ST1
    slot = board.screen("ST1")
    b.append(f'<polygon points="{pts(slot)}" fill="none" stroke="{PIECE_COLORS["ST1"]}" stroke-width="8" filter="url(#glow)" opacity="0.9"/>')
    b.append(f'<polygon points="{pts(slot)}" fill="none" stroke="{PIECE_COLORS["ST1"]}" stroke-width="3" stroke-linejoin="round"/>')
    # dragged piece: full size, lifted above the finger, offset from slot
    dx, dy = -34, 30
    b.append(board.placed("ST1", dx, dy, 'filter="url(#lift)" transform="translate(0,-4)"'))
    fx = sum(p[0] for p in board.screen("ST1", dx, dy)) / 3
    fy = max(p[1] for p in board.screen("ST1", dx, dy)) + 26
    b.append(finger(fx, fy))
    # drag trail from tray
    cx, cy = centres["ST1"]
    b.append(f'<path d="M{cx},{cy} C{cx-40},{cy-120} {fx+60},{fy+90} {fx+8},{fy+22}" fill="none" stroke="{ACCENT}" stroke-width="2.5" stroke-dasharray="3 7" stroke-linecap="round"/>')
    # callouts
    R = x0 + 360 + 30
    b.append(callout(x0 + 44, y0 + 62, R + 150, 130, "Home (always top-left)"))
    b.append(callout(x0 + 180, y0 + 62, R + 150, 170, "Assist level badge (dots = level 2)"))
    b.append(callout(x0 + 316, y0 + 62, R + 150, 210, "Hint 💡 free and unlimited"))
    b.append(callout(board.screen("LT2")[0][0] - 12, board.screen("LT2")[0][1] - 40, R + 150, 290,
                     "Gift pieces: pre-placed at easy levels\n(from puzzle.assist.preplacedOrder)"))
    b.append(callout(slot[2][0], slot[2][1], R + 150, 370, "Matching slot glows while a piece\nthat fits is dragged near it"))
    b.append(callout(fx + 10, fy - 30, R + 150, 450, "Dragged piece: board size, floats\nABOVE the finger, with a lift shadow"))
    gx, gy = centres["LT1"]
    b.append(callout(gx, gy, R + 150, 690, "Ghost outline: this piece is on the board.\nDrop a piece on the tray → it goes home."))
    mx, my = centres["MT"]
    b.append(callout(mx + 20, my + 20, R + 150, 760, "Tray on phones: 2 rows. Big pieces on top,\nsmall below. Cells sized per piece, so all\nminiatures share one scale (big stays big)."))
    b.append(f'<text x="{R+150}" y="560" font-size="13.5" fill="{MUTED}">No text in the child area. No timer,</text>')
    b.append(f'<text x="{R+150}" y="578" font-size="13.5" fill="{MUTED}">no score, no coins, no ads.</text>')
    return svg(W, H, "\n".join(b))


def tablet_play_svg():
    cat = load("animals-cat")
    W, H = 1440, 1060
    x0, y0 = 80, 160
    TW, TH = 1280, 800
    b = [title(40, 46, "Tablet · Play screen (landscape)", "Cat · level 4 “Master” · silhouette only · pieces arrive turned randomly · PG selected, flip badge shown")]
    b.append(device(x0, y0, TW, TH, radius=28, bezel=16))
    b.append(icon_button(x0 + 52, y0 + 52, 32, "home"))
    b.append(icon_button(x0 + TW - 52, y0 + 52, 32, "hint"))
    b.append(level_badge(x0 + TW - 52, y0 + 128, 4, r=22))
    cell, gap = 150, 14
    probe = Board(cat, 0, 0, 100, 100)
    tray_h = tray(probe, 0, 0, TW - 32, 1, cell, gap, {}, {p: (0, False) for p in probe.polys})[3]
    tray_y = y0 + TH - 22 - tray_h
    board_box = (x0 + 110, y0 + 20, TW - 220, tray_y - y0 - 34)
    b.append(f'<rect x="{board_box[0]}" y="{board_box[1]}" width="{board_box[2]}" height="{board_box[3]}" rx="28" fill="#FFFFFF" stroke="#EFE9DC"/>')
    board = Board(cat, *board_box)
    b.append(board.target("shadow"))
    for pid in ("LT1", "LT2", "MT"):
        b.append(board.placed(pid))
    # PG on the board but not yet in its slot (free placement at level 4), selected with flip badge
    turns = {p: (RANDOM_ROT[p], p == "PG") for p in board.polys}
    pg_shape = piece_shape("PG", 2, True, board.s, board_box[0] + board_box[2] - 150, board_box[1] + 230)
    b.append(f'<polygon points="{pts(pg_shape)}" fill="{PIECE_COLORS["PG"]}" stroke="#FFFFFF" stroke-width="2" stroke-linejoin="round" filter="url(#lift)"/>')
    b.append(f'<polygon points="{pts(pg_shape)}" fill="none" stroke="{ACCENT}" stroke-width="3" stroke-dasharray="8 6" stroke-linejoin="round"/>')
    fxp = max(p[0] for p in pg_shape) + 40
    fyp = min(p[1] for p in pg_shape) + 20
    b.append(icon_button(fxp, fyp, 26, "flip", fill="#FFFFFF", stroke=ACCENT, color=ACCENT))
    tray_state = {p: ("ghost" if p in ("LT1", "LT2", "MT", "PG") else "tray") for p in board.polys}
    t_svg, tscale, centres, th = tray(board, x0 + 16, tray_y, TW - 32, 1, cell, gap, tray_state, turns, selected="SQ")
    b.append(t_svg)
    R = x0 + TW + 20
    b.append(callout(x0 + 52, y0 + 52, x0 + 150, y0 - 62, "Home & hint move to the corners;\nthe board gets the full height"))
    b.append(callout(pg_shape[1][0], pg_shape[1][1], x0 + TW - 420, y0 - 62, "Level 4: a piece may rest anywhere on the board.\nTap = turn 45°.  Flip badge only on the PG.", "start"))
    b.append(callout(fxp, fyp + 26, fxp + 10, y0 + 330, "Flip badge\n48 dp icon, 64 dp hit area"))
    sx, sy = centres["SQ"]
    b.append(callout(sx, sy - 40, x0 + 780, y0 + TH + 45, "Level 4: tap a piece in the tray to pre-turn it (selected)"))
    lx, ly = centres["LT1"]
    b.append(callout(lx, ly + 40, x0 + 40, y0 + TH + 45, "Tray on tablets: ONE wide row, same fixed order (big → small), bigger miniatures"))
    return svg(W, H, "\n".join(b))


def layouts_svg():
    W, H = 1500, 900
    b = [title(40, 46, "Adaptive layout · one rule decides the tray", "Phones (narrow): 2 tray rows, big pieces on top. Tablets (wide): 1 tray row. The tray is always at the bottom; the board takes what is left.")]

    def frame(x, y, w, h, rows, label, sub, s=0.5):
        W_, H_ = w * s, h * s
        g = [device(x, y, W_, H_, radius=18, bezel=7)]
        top_h = 64 * s if w < 700 else 0
        g.append(f'<rect x="{x+8}" y="{y+8}" width="{W_-16}" height="{max(top_h-4, 0)}" rx="8" fill="#E6E1D6"/>' if top_h else "")
        tray_h = (rows * (78 if w < 700 else 124) + (rows + 1) * 10) * s
        ty = y + H_ - tray_h - 10 * s
        by = y + 8 + top_h + 4
        g.append(f'<rect x="{x+8}" y="{by}" width="{W_-16}" height="{ty-by-6}" rx="10" fill="#FFFFFF" stroke="#D9D2C3"/>')
        g.append(f'<text x="{x+W_/2}" y="{(by+ty)/2}" text-anchor="middle" font-size="15" fill="{MUTED}">BOARD</text>')
        g.append(f'<rect x="{x+8}" y="{ty}" width="{W_-16}" height="{tray_h}" rx="10" fill="{PANEL}"/>')
        per_row = 7 if rows == 1 else 3
        cw = min((78 if w < 700 else 124) * s, (W_ - 24 - 8 * 6 * s) / per_row)
        for r in range(rows):
            n = per_row if r == 0 else 4
            rw = n * cw + (n - 1) * 6 * s
            for c in range(n):
                g.append(f'<rect x="{x + W_/2 - rw/2 + c*(cw+6*s):.1f}" y="{ty + 10*s + r*(cw+10*s):.1f}" width="{cw:.1f}" height="{cw:.1f}" rx="5" fill="#FFFFFF"/>')
        g.append(f'<text x="{x}" y="{y+H_+36}" font-size="16" font-weight="700" fill="{INK}">{label}</text>')
        g.append(f'<text x="{x}" y="{y+H_+56}" font-size="13" fill="{MUTED}">{sub}</text>')
        return "\n".join(g)

    b.append(frame(60, 110, 360, 780, 2, "Phone portrait", "360×780 dp · tray 2 rows · locked portrait", s=0.78))
    b.append(frame(420, 110, 800, 1280, 1, "Tablet portrait", "800×1280 dp · tray 1 row", s=0.48))
    b.append(frame(860, 110, 1280, 800, 1, "Tablet landscape", "1280×800 dp · tray 1 row · corner buttons", s=0.48))
    rules = ["Layout rules (Android window size classes):",
             "• Compact width (< 600 dp): phone → portrait only, top bar, tray 2 rows",
             "   row 1 = big pieces (LT, LT, MT), row 2 = small (SQ, PG, ST, ST)",
             "• Medium / Expanded (≥ 600 dp): tablet → both orientations, tray 1 row,",
             "   Home / Hint in the corners",
             "• Cells are sized per piece so ALL miniatures share one scale, the largest",
             "   that fits the width; one tray row is at most 16 % of the window height",
             "• Board = the largest box left; puzzle fitted with 10 % padding",
             "• Phone landscape is NOT supported in v1 (board would be too small)"]
    for i, r in enumerate(rules):
        b.append(f'<text x="860" y="{610 + i*23}" font-size="{15 if i == 0 else 14}" font-weight="{700 if i == 0 else 400}" fill="{INK}">{r}</text>')
    return svg(W, H, "\n".join(b))


def assist_levels_svg():
    cat = load("animals-cat")
    W, H = 1500, 720
    b = [title(40, 46, "Assist ladder · the same puzzle at four levels", "What changes: target display · how pieces arrive in the tray · gift pieces · turning help · snap size")]
    specs = [
        (1, "colors", "solution", ["LT1", "LT2", "MT", "SQ"], "1 · Colours", ["Coloured slots + inner lines", "Pieces already turned right", "4 gift pieces → 3 to place", "Huge snap (whole slot)"]),
        (2, "lines", "solution", ["LT1", "LT2"], "2 · Lines", ["Grey slots + inner lines", "Pieces already turned right", "2 gift pieces → 5 to place", "Large snap"]),
        (3, "shadow", "random", [], "3 · Shadow", ["Silhouette only", "Pieces arrive turned randomly", "Snap TURNS the piece for you", "Medium snap"]),
        (4, "shadow", "random", [], "4 · Master", ["Silhouette only", "Random turn + mirrored PG", "Tap to turn, flip badge for PG", "Small snap, turn must match"]),
    ]
    colw = 350
    for i, (lvl, mode, tmode, gifts, name, notes) in enumerate(specs):
        x = 40 + i * (colw + 12)
        y = 100
        b.append(f'<rect x="{x}" y="{y}" width="{colw}" height="600" rx="22" fill="{BG}" stroke="#E6E1D6"/>')
        b.append(level_badge(x + 36, y + 36, lvl))
        b.append(f'<text x="{x+64}" y="{y+43}" font-size="19" font-weight="800" fill="{INK}">{name}</text>')
        board = Board(cat, x + 20, y + 70, colw - 40, 260)
        b.append(f'<rect x="{x+20}" y="{y+70}" width="{colw-40}" height="260" rx="16" fill="#FFFFFF"/>')
        b.append(board.target(mode))
        for g in gifts:
            b.append(board.placed(g))
        turns = solution_turns(board) if tmode == "solution" else {p: (RANDOM_ROT[p], (p == "PG" and lvl == 4)) for p in board.polys}
        state = {p: ("ghost" if p in gifts else "tray") for p in board.polys}
        t_svg, *_ = tray(board, x + 12, y + 344, colw - 24, 2, 72, 7, state, turns)
        b.append(t_svg)
        for j, n in enumerate(notes):
            b.append(f'<text x="{x+24}" y="{y+520 + j*20}" font-size="14" fill="{INK}">• {n}</text>')
    b.append(f'<text x="40" y="712" font-size="13" fill="{MUTED}">Suggested ages: 1 → 3–4 y · 2 → 4–5 y · 3 → 5–7 y · 4 → 7+ y. The game suggests moving up or down; grown-ups can lock a level.</text>')
    return svg(W, H + 10, "\n".join(b))


def drag_story_svg():
    boat = load("starter-little-boat")
    W, H = 1500, 560
    b = [title(40, 46, "Interaction storyboard · drag, lift, snap", "Little boat · level 2. The moment-to-moment feel of one move (timings are targets for the implementation).")]
    panels = [
        ("1 · Touch", "Finger down on a tray cell.\nPiece is picked at once (no long-press).\nSoft ‘pop’ haptic + sound."),
        ("2 · Lift & grow", "Piece grows from tray size to board\nsize in 120 ms and floats ~48 dp\nabove the finger with a shadow."),
        ("3 · Near its slot", "Within snap range the matching slot\nglows. Nothing jumps yet, so the\nchild keeps control."),
        ("4 · Drop → snap", "Release: the piece glides into the slot\n(180 ms ease-out), click + sparkle.\nWrong place → glides home, soft ‘boing’."),
    ]
    pw = 350
    for i, (name, txt) in enumerate(panels):
        x = 40 + i * (pw + 12)
        y = 90
        b.append(f'<rect x="{x}" y="{y}" width="{pw}" height="440" rx="22" fill="{BG}" stroke="#E6E1D6"/>')
        b.append(f'<text x="{x+20}" y="{y+34}" font-size="18" font-weight="800" fill="{INK}">{name}</text>')
        board = Board(boat, x + 20, y + 50, pw - 40, 200)
        b.append(f'<rect x="{x+20}" y="{y+50}" width="{pw-40}" height="200" rx="14" fill="#FFFFFF"/>')
        b.append(board.target("lines"))
        b.append(board.placed("LT1"))
        b.append(board.placed("PG"))
        turns = solution_turns(board)
        drag = "MT"
        state = {"LT1": "ghost", "PG": "ghost", "MT": "tray" if i == 0 else "ghost", "ST1": "tray"}
        if i == 3:
            state["MT"] = "ghost"
        t_svg, tscale, centres, th = tray(board, x + 20, y + 262, pw - 40, 1, 70, 8, state, turns, selected=drag if i == 0 else None)
        b.append(t_svg)
        cx, cy = centres[drag]
        if i == 0:
            b.append(finger(cx, cy + 6, 0.8))
        elif i == 1:
            shp = piece_shape(drag, *turns[drag], board.s, cx + 10, cy - 105)
            b.append(f'<polygon points="{pts(shp)}" fill="{PIECE_COLORS[drag]}" stroke="#FFFFFF" stroke-width="2" filter="url(#lift)"/>')
            mid = piece_shape(drag, *turns[drag], (board.s + tscale) / 2, cx + 4, cy - 50)
            b.append(f'<polygon points="{pts(mid)}" fill="{PIECE_COLORS[drag]}" opacity="0.25"/>')
            b.append(finger(cx + 10, max(q[1] for q in shp) + 22, 0.8))
        elif i == 2:
            slot = board.screen(drag)
            b.append(f'<polygon points="{pts(slot)}" fill="none" stroke="{PIECE_COLORS[drag]}" stroke-width="8" filter="url(#glow)"/>')
            b.append(f'<polygon points="{pts(slot)}" fill="none" stroke="{PIECE_COLORS[drag]}" stroke-width="3"/>')
            b.append(board.placed(drag, 22, 26, 'filter="url(#lift)"'))
            ys = [p[1] for p in board.screen(drag, 22, 26)]
            xs = [p[0] for p in board.screen(drag, 22, 26)]
            b.append(finger(sum(xs) / 3 + 5, max(ys) + 30, 0.8))
        else:
            b.append(board.placed(drag))
            sx = sum(p[0] for p in board.screen(drag)) / 3
            sy = sum(p[1] for p in board.screen(drag)) / 3
            for k in range(8):
                a = k * math.pi / 4
                b.append(f'<line x1="{sx+math.cos(a)*34:.1f}" y1="{sy+math.sin(a)*34:.1f}" x2="{sx+math.cos(a)*46:.1f}" y2="{sy+math.sin(a)*46:.1f}" stroke="#FFD23F" stroke-width="4" stroke-linecap="round"/>')
        for j, ln in enumerate(txt.split("\n")):
            b.append(f'<text x="{x+20}" y="{y+372 + j*20}" font-size="14" fill="{INK}">{ln}</text>')
    return svg(W, H, "\n".join(b))


def menus_svg():
    W, H = 1560, 1000
    b = [title(40, 46, "Phone · Home, puzzle picker and the ending", "Picture-only navigation. Everything is unlocked, and solved puzzles turn colourful.")]
    # --- Home
    x0, y0 = 60, 100
    b.append(device(x0, y0, 360, 780))
    b.append(f'<text x="{x0+180}" y="{y0+88}" text-anchor="middle" font-size="34" font-weight="900" fill="{INK}">Tangram</text>')
    cats = [("starter-mountain", "#FFE8A3"), ("animals-cat", "#FFD0D4"), ("things-house", "#CFE3FF"),
            ("vehicles-sailboat", "#CFF3E2"), ("shapes-square", "#E7D9FF")]
    for i, (pid, col) in enumerate(cats):
        r, c = divmod(i, 2)
        tx, ty = x0 + 22 + c * 164, y0 + 130 + r * 180
        b.append(f'<rect x="{tx}" y="{ty}" width="152" height="164" rx="26" fill="{col}"/>')
        bd = Board(load(pid), tx + 10, ty + 10, 132, 124)
        b.append(bd.target("shadow"))
        done = [3, 1, 1, 0, 0][i]
        b.append(f'<text x="{tx+76}" y="{ty+154}" text-anchor="middle" font-size="13" fill="{INK}" opacity=".7">{"●" * done}{"○" * (5 - done)}</text>')
    b.append(f'<rect x="{x0+186}" y="{y0+490}" width="152" height="164" rx="26" fill="#FFFFFF" stroke="#D9D2C3" stroke-dasharray="6 5"/>')
    b.append(f'<text x="{x0+262}" y="{y0+580}" text-anchor="middle" font-size="13" fill="{MUTED}">free build (later)</text>')
    b.append(icon_button(x0 + 322, y0 + 740, 20, "gear", color=MUTED))
    b.append(f'<circle cx="{x0+322}" cy="{y0+740}" r="25" fill="none" stroke="{ACCENT}" stroke-width="3" stroke-dasharray="100 60" transform="rotate(-90 {x0+322} {y0+740})"/>')
    b.append(callout(x0 + 300, y0 + 735, x0 + 280, y0 + 700, "Grown-ups: HOLD 3 s\n(ring fills) → settings", "end"))
    b.append(f'<text x="{x0}" y="{y0+830}" font-size="16" font-weight="700" fill="{INK}">Home = worlds</text>')
    # --- Picker
    x1 = 480
    b.append(device(x1, y0, 360, 780))
    b.append(icon_button(x1 + 44, y0 + 62, 28, "home"))
    b.append(f'<rect x="{x1+110}" y="{y0+40}" width="140" height="44" rx="22" fill="#FFD0D4"/>')
    bd = Board(load("animals-cat"), x1 + 115, y0 + 42, 40, 40, pad=0.05)
    b.append(bd.target("shadow"))
    picks = ["animals-cat", "starter-little-boat", "things-house", "starter-mountain", "vehicles-sailboat", "shapes-square",
             "animals-cat", "things-house", "vehicles-sailboat", "starter-mountain", "shapes-square", "starter-little-boat"]
    for i, pid in enumerate(picks):
        r, c = divmod(i, 3)
        tx, ty = x1 + 18 + c * 110, y0 + 110 + r * 150
        b.append(f'<rect x="{tx}" y="{ty}" width="100" height="136" rx="20" fill="#FFFFFF" stroke="#E6E1D6"/>')
        bd = Board(load(pid), tx + 6, ty + 6, 88, 110)
        if i in (0, 1, 3):
            for p in bd.polys:
                b.append(bd.placed(p))
            b.append(f'<circle cx="{tx+84}" cy="{ty+120}" r="9" fill="#2DBE7E"/><path d="M{tx+79},{ty+120} l4,4 l7,-8" stroke="#fff" stroke-width="2.5" fill="none"/>')
        else:
            b.append(bd.target("shadow"))
    b.append(f'<text x="{x1}" y="{y0+830}" font-size="16" font-weight="700" fill="{INK}">World = silhouette grid</text>')
    b.append(f'<text x="{x1}" y="{y0+850}" font-size="13" fill="{MUTED}">solved → shown in colour with a tick</text>')
    # --- Win
    x2 = 900
    b.append(device(x2, y0, 360, 780))
    bd = Board(load("animals-cat"), x2 + 20, y0 + 90, 320, 420)
    for p in bd.polys:
        b.append(bd.placed(p))
    import random
    rnd = random.Random(4)
    for _ in range(46):
        cx, cy = x2 + rnd.uniform(15, 345), y0 + rnd.uniform(40, 560)
        col = rnd.choice(list(PIECE_COLORS.values()))
        b.append(f'<rect x="{cx:.0f}" y="{cy:.0f}" width="8" height="12" rx="2" fill="{col}" transform="rotate({rnd.randint(0,90)} {cx:.0f} {cy:.0f})"/>')
    b.append(f'<rect x="{x2+120}" y="{y0+540}" width="120" height="90" rx="20" fill="#FFF6D6" stroke="#F9A826" stroke-width="3" transform="rotate(-6 {x2+180} {y0+585})"/>')
    bd2 = Board(load("animals-cat"), x2 + 140, y0 + 545, 80, 80)
    for p in bd2.polys:
        b.append(bd2.placed(p))
    b.append(icon_button(x2 + 90, y0 + 700, 40, "again", fill="#FFFFFF"))
    b.append(icon_button(x2 + 250, y0 + 700, 52, "next", fill="#2DBE7E", stroke="#2DBE7E", color="#FFFFFF"))
    b.append(callout(x2 + 238, y0 + 585, x2 + 400, y0 + 585, "Sticker flies\nto the sticker book"))
    b.append(callout(x2 + 300, y0 + 700, x2 + 400, y0 + 690, "Next: big, green, bottom-right\n(thumb zone)"))
    b.append(callout(x2 + 300, y0 + 300, x2 + 400, y0 + 300, "The figure fills with colour piece\nby piece, then wiggles/bounces"))
    b.append(f'<text x="{x2}" y="{y0+830}" font-size="16" font-weight="700" fill="{INK}">Solved = it comes alive</text>')
    b.append(f'<text x="{x2}" y="{y0+850}" font-size="13" fill="{MUTED}">colour fill → wiggle → confetti · big Next, small Again</text>')
    return svg(W, H, "\n".join(b))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    files = {
        "01-phone-play.svg": phone_play_svg(),
        "02-tablet-play.svg": tablet_play_svg(),
        "03-adaptive-layouts.svg": layouts_svg(),
        "04-assist-levels.svg": assist_levels_svg(),
        "05-drag-storyboard.svg": drag_story_svg(),
        "06-phone-menus-and-win.svg": menus_svg(),
    }
    for name, content in files.items():
        (OUT / name).write_text(content, encoding="utf-8")
        print("wrote", OUT / name)


if __name__ == "__main__":
    main()
