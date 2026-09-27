#!/usr/bin/env python3
"""
Automated checks for the generated prototype (Spec/prototype/tangram-prototype.html).

    python tools/tests/test_prototype.py          # full run, about 3-4 minutes
    python tools/tests/test_prototype.py --fast   # only the first 3 puzzles in the solve test

Needs Playwright with Chromium. Rebuild first: python tools/build_prototype.py
Exit code 0 when every check passes, 1 otherwise.

Checks
  solve    every puzzle is solved by dragging each piece (in edge-first order) onto its place,
           at Medium/phone 390x844, Hard/tablet 1280x800 (flip badge) and Easy/tablet 800x1280
  anchors  a piece that touches no outline corner (mountain square, dropped first) goes home
  tray     size marks L L M S S; the ↻ / ↺ buttons turn a tray piece +45° / -45° and sit inside
           the cell; a mini puzzle shows only its own pieces at the same tray scale
  tap      a tap on a board piece turns it in place
  twist    a second pointer twisting during a drag turns in 45° steps; a drop far away goes home
  dev      the DEV button asks for the passcode; a wrong one is refused; the right one shows the
           solution overlay and "Solve now" solves the puzzle without setting a best time
"""
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[2]
URL = (ROOT / "Spec" / "prototype" / "tangram-prototype.html").as_uri()
FAST = "--fast" in sys.argv
failures = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (f"  ({detail})" if detail else ""))
    if not ok:
        failures.append(name)


MATCH = """(id)=>{const pc=pieces.find(p=>p.id===id); const s=P.slots.find(s=>s.piece===id); const c=avg(s.poly);
  const V=verts(pc.type,pc.k,pc.f,c[0],c[1]); return V.every(v=>s.poly.some(q=>Math.abs(q[0]-v[0])<1e-6&&Math.abs(q[1]-v[1])<1e-6))}"""
NEXT = """()=>{const A=anchors(null); for (const s of P.slots){const pc=pieces.find(p=>p.id===s.piece); if (pc.st==='board') continue;
  if (s.poly.some(v=>A.some(a=>Math.abs(a[0]-v[0])<1e-6&&Math.abs(a[1]-v[1])<1e-6))) return s.piece;} return null}"""


def load(pg, pid_expr):
    pg.evaluate(f"for (const k in progress) delete progress[k]; P=null; loadPuzzle({pid_expr})")
    pg.wait_for_timeout(120)


def by_id(puzzle_id):
    return f"PUZZLES.findIndex(p=>p.id==='{puzzle_id}')"


def drop(pg, pid, dx=0.15, dy=-0.1):
    t = pg.evaluate(f"""(()=>{{const pc=pieces.find(p=>p.id==='{pid}'); const s=P.slots.find(s=>s.piece==='{pid}'); const c=avg(s.poly);
         const [x,y]=toScreen(c[0]+{dx},c[1]+{dy}); return {{x, y: y+liftOffset(pc), sx:L.cells['{pid}'].cx, sy:L.cells['{pid}'].cy}}}})()""")
    pg.mouse.move(t["sx"], t["sy"])
    pg.mouse.down()
    for i in range(1, 13):
        pg.mouse.move(t["sx"] + (t["x"] - t["sx"]) * i / 12, t["sy"] + (t["y"] - t["sy"]) * i / 12)
        pg.wait_for_timeout(15)
    pg.mouse.up()
    pg.wait_for_timeout(220)


def orient(pg, pid, flip_badge):
    for _ in range(2):
        for _ in range(8):
            if pg.evaluate(MATCH, pid):
                return True
            c = pg.evaluate(f"L.cells['{pid}']")
            pg.mouse.click(c["cx"], c["cy"])
            pg.wait_for_timeout(30)
        if flip_badge:
            box = pg.evaluate("(()=>{const c=document.querySelector('#layerTop [aria-label^=Flip] circle').getBoundingClientRect(); return [c.x+c.width/2, c.y+c.height/2]})()")
            pg.mouse.click(*box)
            pg.wait_for_timeout(30)
        else:
            pg.evaluate(f"(()=>{{const pc=pieces.find(p=>p.id==='{pid}'); pc.f=!pc.f; render(pc,true)}})()")
    return pg.evaluate(MATCH, pid)


def new_page(browser, w, h):
    pg = browser.new_page(viewport={"width": w, "height": h})
    errs = []
    pg.on("pageerror", lambda e: errs.append(str(e)))
    pg.goto(URL)
    pg.wait_for_timeout(250)
    return pg, errs


def test_solve(browser):
    for (w, h), diff, badge in [((390, 844), "medium", False), ((1280, 800), "hard", True), ((800, 1280), "easy", False)]:
        pg, errs = new_page(browser, w, h)
        pg.evaluate(f"settings.diff='{diff}'")
        n = pg.evaluate("PUZZLES.length")
        unsolved = []
        for i in range(3 if FAST else n):
            load(pg, str(i))
            for _ in range(7):
                pid = pg.evaluate(NEXT)
                if not pid:
                    break
                orient(pg, pid, badge)
                drop(pg, pid)
            pg.wait_for_timeout(1700)
            if pg.evaluate("prog().status") != "solved":
                unsolved.append(pg.evaluate("P.id"))
        check(f"solve {diff} {w}x{h}", not unsolved and not errs, f"unsolved {unsolved} errors {errs}" if unsolved or errs else f"{3 if FAST else n} puzzles")
        load(pg, by_id("nature-mountain"))
        orient(pg, "SQ", badge)
        drop(pg, "SQ", 0, 0)
        check(f"anchors {w}x{h}: square with no corner goes home", pg.evaluate("pieces.find(p=>p.id==='SQ').st") == "tray")
        pg.close()


def test_tray(browser):
    for w, h in [(390, 844), (360, 740), (1280, 800), (800, 1280)]:
        pg, errs = new_page(browser, w, h)
        load(pg, by_id("shapes-warmup-1"))
        marks = pg.evaluate("[...document.querySelectorAll('#layerTop text')].map(t=>t.textContent).join('')")
        check(f"tray {w}x{h}: size marks", marks == "LLMSS", marks)
        for pid in ["LT1", "PG", "ST2"]:
            k0 = pg.evaluate(f"pieces.find(p=>p.id==='{pid}').k")
            btn = pg.evaluate(f"""[...pieces.find(p=>p.id==='{pid}').trayUi.querySelectorAll('.spin')].map(g=>{{
                 const c=g.querySelector('circle+circle').getBoundingClientRect(); return [c.x+c.width/2,c.y+c.height/2,c.width]}})""")
            pg.mouse.click(btn[1][0], btn[1][1]); pg.wait_for_timeout(40)
            k1 = pg.evaluate(f"pieces.find(p=>p.id==='{pid}').k")
            pg.mouse.click(btn[0][0], btn[0][1]); pg.wait_for_timeout(40)
            k2 = pg.evaluate(f"pieces.find(p=>p.id==='{pid}').k")
            cell = pg.evaluate(f"L.cells['{pid}']")
            inside = all(cell["x"] <= x - d / 2 and x + d / 2 <= cell["x"] + cell["w"] and cell["y"] <= y - d / 2 and y + d / 2 <= cell["y"] + cell["h"] for x, y, d in btn)
            check(f"tray {w}x{h}: {pid} ↻ ↺ buttons", k1 == (k0 + 1) % 8 and k2 == k0 and inside, f"k {k0}->{k1}->{k2}, inside {inside}")
        ts_full = pg.evaluate("L.ts")
        load(pg, "PUZZLES.findIndex(p=>p.slots.length<7)")
        cells, ts_mini, npieces = pg.evaluate("[Object.keys(L.cells).length, L.ts, pieces.length]")
        check(f"tray {w}x{h}: mini puzzle tray", cells == npieces < 7 and abs(ts_mini - ts_full) < 1e-9 and not errs, f"{cells} cells, ts {ts_mini:.2f} vs {ts_full:.2f}")
        pg.close()


def test_tap_and_twist(browser):
    pg, errs = new_page(browser, 1280, 800)
    load(pg, by_id("shapes-warmup-1"))
    pg.evaluate("""(()=>{const pc=pieces.find(p=>p.id==='LT1'); const s=P.slots.find(s=>s.piece==='LT1'); const c=avg(s.poly);
       pc.st='board'; pc.x=c[0]; pc.y=c[1]; setTurn(pc, s.rot); pc.f=s.flip; render(pc,true);})()""")
    k0 = pg.evaluate("pieces.find(p=>p.id==='LT1').k")
    x, y = pg.evaluate("(()=>{const pc=pieces.find(p=>p.id==='LT1'); return toScreen(pc.x,pc.y)})()")
    pg.mouse.click(x, y); pg.wait_for_timeout(600)
    k1, st = pg.evaluate("[pieces.find(p=>p.id==='LT1').k, pieces.find(p=>p.id==='LT1').st]")
    check("tap: board piece turns in place (or turns back with a shake)", st == "board" and k1 in ((k0 + 1) % 8, k0), f"k {k0}->{k1}")
    pg.close()

    pg, errs = new_page(browser, 390, 844)
    load(pg, by_id("shapes-warmup-1"))
    c = pg.evaluate("L.cells['LT1']")
    pg.mouse.move(c["cx"], c["cy"]); pg.mouse.down()
    pg.mouse.move(c["cx"], c["cy"] - 80, steps=5); pg.mouse.move(c["cx"], c["cy"] - 200, steps=5)
    k0 = pg.evaluate("drag.pc.k")
    ks = pg.evaluate("""(()=>{
      const px=drag.px, py=drag.py, r0=stage.getBoundingClientRect();
      const fire=(type,x,y)=>{ const ev=new PointerEvent(type,{pointerId:7,clientX:x+r0.left,clientY:y+r0.top,bubbles:true,cancelable:true,isPrimary:false});
         (type==='pointerdown'? document.elementFromPoint(x+r0.left,y+r0.top) : window).dispatchEvent(ev); };
      fire('pointerdown', px+80, py); const out=[];
      for (let a=0;a<=100;a+=10){ const r=a*Math.PI/180; fire('pointermove', px+80*Math.cos(r), py+80*Math.sin(r)); out.push(drag.pc.k); }
      fire('pointerup', px+80*Math.cos(100*Math.PI/180), py+80*Math.sin(100*Math.PI/180)); return out; })()""")
    check("twist: 100° of twist turns two 45° steps", ks[-1] == (k0 + 2) % 8 and sorted(set(ks)) == sorted({k0, (k0 + 1) % 8, (k0 + 2) % 8}), str(ks))
    pg.mouse.move(5, 120); pg.mouse.up(); pg.wait_for_timeout(250)
    check("twist: a drop with no lock goes home", pg.evaluate("pieces.find(p=>p.id==='LT1').st") == "tray" and not errs, str(errs))
    pg.close()


def test_dev(browser):
    pg, errs = new_page(browser, 390, 844)
    load(pg, by_id("things-house"))
    pg.click("#devBtn"); pg.fill("#devPin", "1234"); pg.click("#devOk")
    refused = pg.evaluate("!byId('devDlg').hidden && !byId('devLock').hidden && byId('devMsg').textContent.length > 0")
    pg.fill("#devPin", "0417"); pg.press("#devPin", "Enter")
    unlocked = pg.evaluate("!byId('devTools').hidden")
    check("dev: wrong passcode refused, right one unlocks", refused and unlocked)
    pg.click("#devShow")
    n_hint, n_slots = pg.evaluate("[layers.hint.querySelectorAll('polygon').length, P.slots.length]")
    check("dev: solution overlay shows every piece", n_hint == n_slots == 7, f"{n_hint} shapes")
    pg.click("#devBtn"); pg.click("#devSolve"); pg.wait_for_timeout(2000)
    st, best = pg.evaluate("[prog().status, prog().best]")
    check("dev: solve now solves without a best time", st == "solved" and best is None and not errs, f"{st}, best {best}, {errs}")
    pg.close()


if __name__ == "__main__":
    with sync_playwright() as p:
        b = p.chromium.launch()
        test_tray(b)
        test_tap_and_twist(b)
        test_dev(b)
        test_solve(b)
        b.close()
    print(f"\n{'ALL PASS' if not failures else str(len(failures)) + ' FAILED: ' + ', '.join(failures)}")
    sys.exit(1 if failures else 0)
