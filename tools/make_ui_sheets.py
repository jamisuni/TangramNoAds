#!/usr/bin/env python3
"""
Generate the annotated UI sheets in Spec/ui/ from the REAL prototype.

    python tools/build_prototype.py      # first
    python tools/make_ui_sheets.py       # -> Spec/ui/*.png

Needs Playwright with Chromium (pip install playwright; playwright install chromium).
Every screenshot is taken from Spec/prototype/tangram-prototype.html, so the drawings
can never drift from what the prototype actually does.
"""
import base64
import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

sys.path.insert(0, str(Path(__file__).parent))
from tangram_geom import PIECE_COLORS, to_float, outline_corners  # noqa: E402
from validate_puzzles import load_solution  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
PROTO = (ROOT / "Spec" / "prototype" / "tangram-prototype.html").as_uri()
OUT = ROOT / "Spec" / "ui"
INK, MUTED, ACCENT = "#2B2D42", "#7E8299", "#3D8BFD"
FONT = "font-family: system-ui, 'Segoe UI', Arial, sans-serif;"

HELPERS = """(() => {
window.__reset = (id) => { for (const k in progress) delete progress[k]; stats.today = 0; stats.total = 0; saveAll(); P = null;
  loadPuzzle(PUZZLES.findIndex(p => p.id === id)); };
window.__place = (ids) => { for (const id of ids) { const s = P.slots.find(q => q.piece === id), pc = pieces.find(p => p.id === id), c = avg(s.poly);
  for (let k = 0; k < 8; k++) for (const f of [false, true]) { const V = verts(pc.type, k, f, c[0], c[1]);
    if (V.every(v => s.poly.some(q => Math.abs(q[0]-v[0]) < 1e-6 && Math.abs(q[1]-v[1]) < 1e-6))) { pc.k = k; pc.f = f; pc.ang = k * 45; } }
  pc.st = 'board'; pc.x = c[0]; pc.y = c[1]; render(pc, true); } prog().status = 'progress'; persistPieces(); updateChrome(); };
window.__rect = (sel) => { const r = document.querySelector(sel).getBoundingClientRect(); return [r.x + r.width / 2, r.y + r.height / 2, r.width, r.height]; };
window.__cell = (id) => [L.cells[id].cx, L.cells[id].cy];
window.__slot = (id) => { const s = P.slots.find(q => q.piece === id); return toScreen(...avg(s.poly)); };
return true; })()"""


def shot(browser, vp, steps):
    """steps(page) sets up the state and returns a dict of named points (CSS px). Returns (png_b64, points, vp)."""
    pg = browser.new_page(viewport={"width": vp[0], "height": vp[1]}, device_scale_factor=2)
    pg.goto(PROTO)
    pg.wait_for_timeout(250)
    pg.evaluate(HELPERS)
    pts = steps(pg) or {}
    pg.wait_for_timeout(350)
    png = pg.screenshot()
    pg.mouse.up()
    pg.close()
    return base64.b64encode(png).decode(), pts, vp


def drag_to(pg, piece, target, hold=True, frac=1.0):
    sx, sy = pg.evaluate(f"__cell('{piece}')")
    tx, ty = target
    pg.mouse.move(sx, sy)
    pg.mouse.down()
    for i in range(1, 15):
        pg.mouse.move(sx + (tx - sx) * i / 14 * frac, sy + (ty - sy) * i / 14 * frac)
        pg.wait_for_timeout(15)
    pg.wait_for_timeout(200)
    if not hold:
        pg.mouse.up()


def device_frame(x, y, w, h, radius):
    return (f'<rect x="{x-10}" y="{y-10}" width="{w+20}" height="{h+20}" rx="{radius+10}" fill="#23252F"/>')


def sheet(name, W, H, title, sub, frames, callouts, notes=()):
    """frames: list of dicts {img, vp, x, y, scale, label}; callouts: (frame_i, (px,py), (tx,ty), text, anchor)."""
    svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" style="{FONT}">',
           f'<rect width="{W}" height="{H}" fill="#FFFFFF"/>',
           f'<text x="40" y="46" font-size="24" font-weight="800" fill="{INK}">{title}</text>',
           f'<text x="40" y="72" font-size="15" fill="{MUTED}">{sub}</text>']
    for fr in frames:
        w, h = fr["vp"][0] * fr["scale"], fr["vp"][1] * fr["scale"]
        svg.append(device_frame(fr["x"], fr["y"], w, h, 26 * fr["scale"] + 8))
        svg.append(f'<clipPath id="c{id(fr)}"><rect x="{fr["x"]}" y="{fr["y"]}" width="{w}" height="{h}" rx="{26*fr["scale"]}"/></clipPath>')
        svg.append(f'<image href="data:image/png;base64,{fr["img"]}" x="{fr["x"]}" y="{fr["y"]}" width="{w}" height="{h}" clip-path="url(#c{id(fr)})"/>')
        if fr.get("label"):
            svg.append(f'<text x="{fr["x"]}" y="{fr["y"] + h + 40}" font-size="17" font-weight="700" fill="{INK}">{fr["label"]}</text>')
        if fr.get("sublabel"):
            svg.append(f'<text x="{fr["x"]}" y="{fr["y"] + h + 62}" font-size="14" fill="{MUTED}">{fr["sublabel"]}</text>')
    for fi, (px, py), (tx, ty), text, anchor in callouts:
        fr = frames[fi]
        X, Y = fr["x"] + px * fr["scale"], fr["y"] + py * fr["scale"]
        svg.append(f'<line x1="{X:.1f}" y1="{Y:.1f}" x2="{tx}" y2="{ty}" stroke="{ACCENT}" stroke-width="1.6"/>')
        svg.append(f'<circle cx="{X:.1f}" cy="{Y:.1f}" r="5" fill="{ACCENT}" stroke="#fff" stroke-width="1.5"/>')
        for i, ln in enumerate(text.split("\n")):
            svg.append(f'<text x="{tx + (8 if anchor == "start" else -8)}" y="{ty + 5 + i*19}" text-anchor="{anchor}" font-size="15" fill="{INK}">{ln}</text>')
    for (x, y, text) in notes:
        for i, ln in enumerate(text.split("\n")):
            svg.append(f'<text x="{x}" y="{y + i*20}" font-size="14.5" fill="{MUTED}">{ln}</text>')
    svg.append("</svg>")
    return "\n".join(svg), (W, H), name


def lock_rules_svg():
    """Diagram of the anchor locking rule, drawn from the Mountain geometry."""
    pz = json.loads((ROOT / "Tangrams" / "nature-mountain.json").read_text(encoding="utf-8"))
    exact = load_solution(pz, [])
    polys = {k: to_float(v) for k, v in exact.items()}
    corners = [(float(x), float(y)) for x, y in outline_corners(exact)]
    W, H = 1500, 560
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" style="{FONT}">',
           '<defs><marker id="ar" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#3D8BFD"/></marker>'
           '<marker id="arr" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="8" markerHeight="8" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#E8505B"/></marker></defs>',
           f'<rect width="{W}" height="{H}" fill="#FFFFFF"/>',
           f'<text x="40" y="46" font-size="24" font-weight="800" fill="{INK}">How a dropped piece locks: to the shape’s corners or to its neighbours</text>',
           f'<text x="40" y="72" font-size="15" fill="{MUTED}">Mountain puzzle. Dots are anchor points (drawn here only to explain; the player never sees them).</text>',
           f'<text x="40" y="92" font-size="15" fill="{MUTED}">A piece locks where one of its corners sits on an anchor, fully inside, with no overlap. Otherwise it goes home.</text>']

    def panel(x0, y0, caption, lines, draw):
        out.append(f'<rect x="{x0}" y="{y0}" width="340" height="400" rx="22" fill="#FBF7EE" stroke="#E6E1D6"/>')
        out.append(f'<text x="{x0+20}" y="{y0+34}" font-size="18" font-weight="800" fill="{INK}">{caption}</text>')
        s, ox, oy = 36, x0 + 26, y0 + 70

        def T(p):
            return (ox + p[0] * s, oy + p[1] * s)

        def poly(pts, **kw):
            attrs = " ".join(f'{k.replace("_", "-")}="{v}"' for k, v in kw.items())
            return f'<polygon points="{" ".join(f"{a:.1f},{b:.1f}" for a, b in map(T, pts))}" {attrs}/>'
        for p in polys.values():
            out.append(poly(p, fill="#4A4E69", stroke="#4A4E69", stroke_width=2.5, stroke_linejoin="round"))
        draw(T, poly)
        for i, ln in enumerate(lines):
            out.append(f'<text x="{x0+20}" y="{y0+262 + i*21}" font-size="14.5" fill="{INK}">{ln}</text>')

    def dots(T, pts, color):
        for a in pts:
            X, Y = T(a)
            out.append(f'<circle cx="{X:.1f}" cy="{Y:.1f}" r="5.5" fill="{color}" stroke="#2B2D42" stroke-width="1.3"/>')

    def piece(poly, pid, T, **kw):
        return poly(polys[pid], fill=PIECE_COLORS[pid], stroke="#FFFFFF", stroke_width=2.5, **kw)

    def d1(T, poly):
        dots(T, corners, "#FFFFFF")
    panel(40, 116, "1 · Outline corners", ["White dots: where the outline turns.", "The mountain has only three.", "Points along a straight side", "are not anchors."], d1)

    def d2(T, poly):
        off = [(x + 0.35, y - 0.3) for x, y in polys["LT1"]]
        out.append(poly(off, fill=PIECE_COLORS["LT1"], opacity=0.45, stroke="#FFFFFF", stroke_width=2, stroke_dasharray="6 4"))
        out.append(piece(poly, "LT1", T))
        dots(T, corners, "#FFFFFF")
        dots(T, [(4, 4), (2, 2)], "#FFD23F")
        (x1, y1), (x2, y2) = T((0.45, 3.6)), T((0.08, 3.93))
        out.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#3D8BFD" stroke-width="3" marker-end="url(#ar)"/>')
    panel(400, 116, "2 · Tie to a corner", ["Dropped a little off, the big triangle", "slides so its corner sits on the", "outline corner (0, 4) and clicks.", "Its corners become new anchors", "(yellow)."], d2)

    def d3(T, poly):
        dots(T, corners, "#FFFFFF")
        out.append(poly(polys["SQ"], fill=PIECE_COLORS["SQ"], opacity=0.6, stroke="#FFFFFF", stroke_width=2, stroke_dasharray="6 4"))
        (x1, y1) = T((5, 2))
        out.append(f'<path d="M{x1},{y1+14} C{x1},{y1+45} {x1+14},{y1+62} {x1+26},{y1+74}" fill="none" stroke="#E8505B" stroke-width="3" marker-end="url(#arr)"/>')
        out.append(f'<text x="{x1+14}" y="{y1+96}" font-size="14" font-weight="700" fill="#E8505B">back to tray</text>')
    panel(760, 116, "3 · Nothing to hold on to", ["The square dropped first, in the", "middle: none of its corners is near", "an anchor, so it goes home.", "Not supported on purpose: puzzles", "are built edge-first (rule V11)."], d3)

    def d4(T, poly):
        for pid in ("LT1", "LT2", "ST1"):
            out.append(piece(poly, pid, T))
        out.append(piece(poly, "SQ", T))
        placed = [v for pid in ("LT1", "LT2", "ST1") for v in polys[pid]]
        dots(T, corners, "#FFFFFF")
        dots(T, [v for v in placed if v not in corners], "#FFD23F")
    panel(1120, 116, "4 · Grow from neighbours", ["With the big triangles and a small", "one placed, their corners give the", "square something to hold, and now", "it locks. Any arrangement that fills", "the shape exactly wins."], d4)
    out.append("</svg>")
    return "\n".join(out), (W, H), "05-lock-rules"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    sheets = []
    with sync_playwright() as p:
        b = p.chromium.launch()
        PH, TB, TP = (390, 844), (1280, 800), (800, 1280)

        # 1 phone play, mid-drag with landing preview
        def s1(pg):
            pg.evaluate("settings.diff='easy'; __reset('animals-cat'); __place(['LT1','LT2','SQ'])")
            for _ in range(8):  # turn the medium triangle in the tray until it matches its place
                if pg.evaluate("(()=>{const pc=pieces.find(p=>p.id==='MT'); const s=P.slots.find(q=>q.piece==='MT'); const c=avg(s.poly); return verts(pc.type,pc.k,pc.f,c[0],c[1]).every(v=>s.poly.some(q=>Math.abs(q[0]-v[0])<1e-6&&Math.abs(q[1]-v[1])<1e-6))})()"):
                    break
                cx, cy = pg.evaluate("__cell('MT')")
                pg.mouse.click(cx, cy)
                pg.wait_for_timeout(60)
            t = pg.evaluate("(()=>{const s=P.slots.find(q=>q.piece==='MT'); const c=avg(s.poly); const pc=pieces.find(p=>p.id==='MT'); const [x,y]=toScreen(c[0]+0.55,c[1]+0.6); return [x, y+liftOffset(pc)]})()")
            drag_to(pg, "MT", t)
            return {"prev": pg.evaluate("__rect('#prevBtn')")[:2], "next": pg.evaluate("__rect('#nextBtn')")[:2],
                    "gear": pg.evaluate("__rect('#gearBtn')")[:2], "title": pg.evaluate("__rect('#pTitle')")[:2],
                    "restart": pg.evaluate("__rect('#restartBtn')")[:2], "slot": pg.evaluate("(()=>{const [x,y]=__slot('MT'); return [x-18,y+10]})()"),
                    "piece": pg.evaluate("[drag.x, drag.y]"), "lt": pg.evaluate("__cell('LT1')")}
        img, pts, vp = shot(b, PH, s1)
        fr = [{"img": img, "vp": vp, "x": 90, "y": 110, "scale": 0.95}]
        R = 90 + 390 * 0.95 + 40
        co = [(0, pts["prev"], (R + 20, 130), "‹ previous puzzle", "start"),
              (0, pts["title"], (R + 20, 170), "Name, difficulty dots, number, state\n(in progress / solved ✓)", "start"),
              (0, pts["next"], (R + 20, 230), "› next puzzle: also the way to skip", "start"),
              (0, pts["gear"], (R + 20, 270), "⚙ settings: difficulty, timer, sound, stats", "start"),
              (0, pts["restart"], (R + 20, 330), "Restart (shown while a puzzle is in progress)", "start"),
              (0, pts["slot"], (R + 20, 420), "Landing preview (Easy/Medium): where the piece\nwill lock if released now. It shows a VALID spot,\nnot the correct one; there are no hidden slots.", "start"),
              (0, pts["piece"], (R + 20, 520), "Dragged piece at full size, floating above the finger.\nTap = turn 45°. Hold it and twist a second finger\n= turn in 45° steps.", "start"),
              (0, pts["lt"], (R + 20, 700), "Tray: same order (big → small), resting turn and colours\nin every puzzle; mini puzzles show only their pieces.\nS / M / L marks the triangle size; ↺ ↻ turn a piece\nbefore you drag it. Phone: 2 rows. Placed pieces\nleave a dashed ghost.", "start")]
        sheets.append(sheet("01-phone-play", 1100, 1000, "Phone · Play screen (portrait)",
                            "Cat, Easy. Three pieces placed; the medium triangle is being dragged. Silhouette only, no inner lines.", fr, co))

        # 2 tablet landscape, hard, flip badge + timer
        def s2(pg):
            pg.evaluate("settings.diff='hard'; __reset('things-arrow'); __place(['LT1','LT2','MT']); prog().time = 83; updateChrome()")
            return {"flip": pg.evaluate("(()=>{const c=document.querySelector('#layerTop circle').getBoundingClientRect(); return [c.x+c.width/2, c.y+c.height/2]})()"),
                    "timer": pg.evaluate("__rect('#timer')")[:2], "tray": pg.evaluate("__cell('SQ')")}
        img, pts, vp = shot(b, TB, s2)
        fr = [{"img": img, "vp": vp, "x": 60, "y": 150, "scale": 0.82}]
        co = [(0, pts["timer"], (1150, 118), "Timer shown on Hard (setting: Hard only / Always / Never)", "end"),
              (0, pts["flip"], (1150, 950), "Hard: flip the parallelogram yourself (⇋ badge, 60 dp touch area)", "end"),
              (0, pts["tray"], (60, 985), "Tablet: ONE wide tray row in the same fixed order; the board takes the full width", "start")]
        sheets.append(sheet("02-tablet-play", 1180, 1010, "Tablet · Play screen (landscape)", "Arrow, Hard. Three pieces placed.", fr, co))

        # 3 layouts: same state on three devices
        def s3(pg):
            pg.evaluate("settings.diff='medium'; __reset('things-house'); __place(['LT1','PG','ST2'])")
        frames, x = [], 60
        for vp_, sc, lab in ((PH, 0.72, "Phone portrait · 390×844"), (TP, 0.48, "Tablet portrait · 800×1280"), (TB, 0.48, "Tablet landscape · 1280×800")):
            img, _, vp = shot(b, vp_, s3)
            frames.append({"img": img, "vp": vp, "x": x, "y": 120, "scale": sc, "label": lab})
            x += vp[0] * sc + 60
        notes = [(frames[2]["x"], 610, "Layout rules\n• Top bar on every size: ‹ name ›  ⚙\n• Width < 600 dp: phone, portrait only, tray in 2 rows\n   (big pieces on top)\n• Width ≥ 600 dp: tablet, both orientations, 1 tray row\n• Tray cells sized per piece → all miniatures share one scale\n• The board gets everything else; the shape is fitted with padding")]
        sheets.append(sheet("03-adaptive-layouts", 1560, 900, "Adaptive layout · the same game state on three screens",
                            "House, three pieces placed. The layout is computed, not drawn per device.", frames, [], notes))

        # 4 browse states
        frames = []
        for i, (setup, lab, sub) in enumerate((
                ("__reset('vehicles-sailboat')", "New", "empty silhouette, full tray"),
                ("__reset('vehicles-sailboat'); __place(['LT1','LT2','MT','PG']); prog().time=95", "In progress (came back later)", "pieces kept, Restart offered, time continues"),
                ("__reset('vehicles-sailboat'); prog().status='solved'; prog().best=141; build()", "Solved", "only the picture: no piece lines; Retry / Next"))):
            img, _, vp = shot(b, PH, lambda pg, s=setup: pg.evaluate("settings.diff='medium';" + s))
            frames.append({"img": img, "vp": vp, "x": 60 + i * 420, "y": 110, "scale": 0.8, "label": lab, "sublabel": sub})
        sheets.append(sheet("04-browse-states", 1320, 900, "Browsing with ‹ ›: each puzzle remembers its state",
                            "Sailboat in its three states. Leaving a puzzle never loses work; › can always skip ahead.", frames, []))

        # 6 solve sequence + support card
        frames = []
        seq = (("__reset('things-house'); __place(['LT1','LT2','MT','ST1','ST2','PG','SQ'])", 0, "1 · Last piece locks", "colourful pieces, click, confetti"),
               ("__reset('things-house'); prog().status='solved'; prog().best=74; build()", 0, "2 · The picture appears", "same outline, stylised art (from the puzzle file)"),
               ("__reset('things-house'); prog().status='solved'; prog().best=74; build(); stats.day=today(); stats.today=1260; stats.total=5400; openSettings()", 0, "3 · Settings: play time", "active time only, kept on the device; absolutely free"))
        for i, (setup, _, lab, sub) in enumerate(seq):
            img, _, vp = shot(b, PH, lambda pg, s=setup: pg.evaluate("settings.diff='medium';" + s))
            frames.append({"img": img, "vp": vp, "x": 60 + i * 420, "y": 110, "scale": 0.8, "label": lab, "sublabel": sub})
        sheets.append(sheet("06-solve-and-playtime", 1320, 900, "Solving: pieces → picture, and play time",
                            "House. The picture is the puzzle's 'art' layer clipped to the silhouette. Play time is tracked on the device only.", frames, []))

        sheets.append(lock_rules_svg())

        pg = b.new_page(device_scale_factor=1.5)
        for svg, (W, H), name in sheets:
            pg.set_viewport_size({"width": W, "height": H})
            pg.set_content(f'<html><body style="margin:0">{svg}</body></html>')
            pg.wait_for_timeout(300)
            pg.screenshot(path=str(OUT / f"{name}.png"), clip={"x": 0, "y": 0, "width": W, "height": H})
            if name == "05-lock-rules":
                (OUT / f"{name}.svg").write_text(svg, encoding="utf-8")
            print("wrote", OUT / f"{name}.png")
        b.close()


if __name__ == "__main__":
    main()
