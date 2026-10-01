#!/usr/bin/env python3
"""
Automated checks for the generated prototype (Spec/prototype/tangram-prototype.html).

    python tools/tests/test_prototype.py          # full run, about 2-3 minutes
    python tools/tests/test_prototype.py --fast   # only the first 3 puzzles in the solve test

Needs Playwright with Chromium. Rebuild first: python tools/build_prototype.py
Exit code 0 when every check passes, 1 otherwise.

Checks (prototype 0.6, round 6)
  solve    every puzzle is solved by dragging each piece (in edge-first order) onto its place, on a
           phone 390x844, a tablet 1280x800 and a tablet 800x1280; the parallelogram is mirrored with
           the ⇋ badge (always shown, REQ-018)
  anchors  a piece that touches no outline corner (mountain square, dropped first) goes home (REQ-019/020)
  tray     size marks L L M S S; no turn buttons (REQ-044 withdrawn); a tap turns a tray piece +45°
           (REQ-016); a mini puzzle shows only its own pieces at the same tray scale (REQ-013)
  tap      a tap on a board piece turns it in place
  twist    a second pointer twisting during a drag turns in 45° steps; a drop far away goes home
  nav      the list wraps (REQ-024); a long press on › jumps to the next unsolved puzzle and the
           counter opens a grid of every puzzle (REQ-050); the order is kind, rating, id (REQ-040)
  lang     with a Finnish locale the title, buttons and settings are Finnish; otherwise English (REQ-047)
  load     a saved piece that no longer fits the silhouette returns to the tray on load (REQ-025)
  mini     in a mini puzzle a missed drop pulses the outline corners once; a warm-up does not (REQ-051)
  settings no difficulty control; timer off by default; privacy text below the free note (REQ-031/032/049)
  dev      the DEV button asks for the passcode; a wrong one is refused; the right one shows the
           solution overlay and "Solve now" solves the puzzle without setting a best time (REQ-046)
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


def flip_badge_pos(pg):
    return pg.evaluate("(()=>{const c=document.querySelector('#layerTop [role=button] circle').getBoundingClientRect(); return [c.x+c.width/2, c.y+c.height/2]})()")


def orient(pg, pid):
    """Turn (taps) and, for the parallelogram, mirror (⇋ badge) a tray piece until it matches its slot."""
    for _ in range(2):
        for _ in range(8):
            if pg.evaluate(MATCH, pid):
                return True
            c = pg.evaluate(f"L.cells['{pid}']")
            pg.mouse.click(c["cx"], c["cy"])
            pg.wait_for_timeout(30)
        if pid == "PG":
            pg.mouse.click(*flip_badge_pos(pg))
            pg.wait_for_timeout(30)
    return pg.evaluate(MATCH, pid)


def new_page(browser, w, h, locale="en-US"):
    ctx = browser.new_context(viewport={"width": w, "height": h}, locale=locale)
    pg = ctx.new_page()
    errs = []
    pg.on("pageerror", lambda e: errs.append(str(e)))
    pg.goto(URL)
    pg.wait_for_timeout(250)
    return pg, errs


def test_solve(browser):
    for w, h in [(390, 844), (1280, 800), (800, 1280)]:
        pg, errs = new_page(browser, w, h)
        n = pg.evaluate("PUZZLES.length")
        unsolved = []
        for i in range(3 if FAST else n):
            load(pg, str(i))
            for _ in range(7):
                pid = pg.evaluate(NEXT)
                if not pid:
                    break
                orient(pg, pid)
                drop(pg, pid)
            pg.wait_for_timeout(1700)
            if pg.evaluate("prog().status") != "solved":
                unsolved.append(pg.evaluate("P.id"))
        check(f"solve {w}x{h}", not unsolved and not errs, f"unsolved {unsolved} errors {errs}" if unsolved or errs else f"{3 if FAST else n} puzzles")
        load(pg, by_id("nature-mountain"))
        orient(pg, "SQ")
        drop(pg, "SQ", 0, 0)
        check(f"anchors {w}x{h}: square with no corner goes home", pg.evaluate("pieces.find(p=>p.id==='SQ').st") == "tray")
        pg.close()


def test_tray(browser):
    for w, h in [(390, 844), (360, 740), (1280, 800), (800, 1280)]:
        pg, errs = new_page(browser, w, h)
        load(pg, by_id("shapes-warmup-1"))
        marks = pg.evaluate("[...document.querySelectorAll('#layerTop text')].map(t=>t.textContent).join('')")
        check(f"tray {w}x{h}: size marks", marks == "LLMSS", marks)
        spins = pg.evaluate("document.querySelectorAll('#layerTop .spin').length")
        check(f"tray {w}x{h}: no turn buttons", spins == 0, f"{spins} buttons")
        for pid in ["LT1", "PG", "ST2"]:
            k0 = pg.evaluate(f"pieces.find(p=>p.id==='{pid}').k")
            c = pg.evaluate(f"L.cells['{pid}']")
            pg.mouse.click(c["cx"], c["cy"]); pg.wait_for_timeout(40)
            k1 = pg.evaluate(f"pieces.find(p=>p.id==='{pid}').k")
            check(f"tray {w}x{h}: tap turns {pid} +45°", k1 == (k0 + 1) % 8, f"k {k0}->{k1}")
        badge = pg.evaluate("!!document.querySelector('#layerTop [role=button]')")
        check(f"tray {w}x{h}: flip badge shown", badge)
        smallest = pg.evaluate("Math.min(...Object.values(L.cells).flatMap(c=>[c.w,c.h]))")
        check(f"tray {w}x{h}: smallest cell ≥ 56 dp", smallest >= 56, f"{smallest:.0f} dp")
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


def test_nav(browser):
    pg, errs = new_page(browser, 390, 844)
    kinds = pg.evaluate("PUZZLES.map(p=>p.kind)")
    order = {"mini": 0, "warmup": 1, "full": 2}
    keys = pg.evaluate("PUZZLES.map(p=>[p.kind,p.difficulty,p.id])")
    ranked = [(order[a], b, c) for a, b, c in keys]
    check("nav: order is kind, rating, id", ranked == sorted(ranked), str(kinds))
    load(pg, "PUZZLES.length-1")
    pg.click("#nextBtn"); pg.wait_for_timeout(150)
    check("nav: › on the last puzzle wraps to the first", pg.evaluate("idx") == 0)
    pg.click("#prevBtn"); pg.wait_for_timeout(150)
    check("nav: ‹ on the first puzzle wraps to the last", pg.evaluate("idx") == pg.evaluate("PUZZLES.length-1"))
    # long press: puzzles 0..4 solved except 3 → from 0 a long press lands on 3
    pg.evaluate("for (const k in progress) delete progress[k]; [0,1,2,4].forEach(i=>progress[PUZZLES[i].id]={status:'solved',pieces:null,time:5,best:5,solves:1}); P=null; loadPuzzle(0)")
    pg.wait_for_timeout(100)
    b = pg.evaluate("(()=>{const r=byId('nextBtn').getBoundingClientRect(); return [r.x+r.width/2, r.y+r.height/2]})()")
    pg.mouse.move(*b); pg.mouse.down(); pg.wait_for_timeout(700); pg.mouse.up(); pg.wait_for_timeout(150)
    check("nav: long press on › jumps to the next unsolved", pg.evaluate("idx") == 3, f"idx {pg.evaluate('idx')}")
    pg.mouse.move(*b); pg.mouse.down(); pg.wait_for_timeout(100); pg.mouse.up(); pg.wait_for_timeout(150)
    check("nav: short press on › moves one step", pg.evaluate("idx") == 4)
    pg.click("#titleBtn"); pg.wait_for_timeout(150)
    n_cells, n_solved, n_prog, visible = pg.evaluate("[grid.children.length, [...grid.children].filter(c=>c.querySelector('clipPath')).length, grid.querySelectorAll('.prog').length, !byId('gridDlg').hidden]")
    check("nav: the counter opens a grid of every puzzle, solved ones with their picture", visible and n_cells == pg.evaluate("PUZZLES.length") and n_solved == 4, f"{n_cells} cells, {n_solved} pictures, {n_prog} in progress")
    pg.click("#grid .gcell:nth-child(6)"); pg.wait_for_timeout(150)
    check("nav: choosing a grid cell opens that puzzle", pg.evaluate("idx") == 5 and pg.evaluate("byId('gridDlg').hidden") and not errs, str(errs))
    pg.close()


def test_lang(browser):
    pg, errs = new_page(browser, 390, 844, locale="fi-FI")
    load(pg, by_id("animals-cat"))
    t, restart, done, lang = pg.evaluate("[byId('pTitle').textContent, byId('restartBtn').textContent.trim(), byId('closeSettings').textContent, document.documentElement.lang]")
    check("lang: Finnish device → Finnish title and buttons", t == "Kissa" and restart == "Aloita alusta" and done == "Valmis" and lang == "fi", f"{t}, {restart}, {done}, {lang}")
    pg.close()
    pg, errs = new_page(browser, 390, 844, locale="sv-SE")
    load(pg, by_id("animals-cat"))
    t, done = pg.evaluate("[byId('pTitle').textContent, byId('closeSettings').textContent]")
    check("lang: other device language → English", t == "Cat" and done == "Done" and not errs, f"{t}, {done}")
    pg.close()


def test_load_check(browser):
    pg, errs = new_page(browser, 390, 844)
    load(pg, by_id("things-house"))
    # place LT1 correctly, then save a bogus ST1 far outside the silhouette; reload the puzzle
    pg.evaluate("""(()=>{const pc=pieces.find(p=>p.id==='LT1'); const s=P.slots.find(s=>s.piece==='LT1'); const c=avg(s.poly);
       pc.st='board'; pc.x=c[0]; pc.y=c[1]; setTurn(pc, s.rot); pc.f=s.flip; render(pc,true);
       const st=pieces.find(p=>p.id==='ST1'); st.st='board'; st.x=c[0]+40; st.y=c[1]+40; persistPieces(); P=null; loadPuzzle(idx);})()""")
    pg.wait_for_timeout(100)
    lt, st = pg.evaluate("[pieces.find(p=>p.id==='LT1').st, pieces.find(p=>p.id==='ST1').st]")
    check("load: a saved piece outside the silhouette returns to the tray, valid ones stay", lt == "board" and st == "tray" and not errs, f"LT1 {lt}, ST1 {st}")
    pg.close()


def test_mini_pulse(browser):
    pg, errs = new_page(browser, 390, 844)
    load(pg, by_id("shapes-mini-1"))
    c = pg.evaluate("L.cells['ST1']")
    ctr = pg.evaluate("(()=>{const b=L.board; return [b.x+b.w/2, b.y+18]})()")   # inside the board, away from any anchor
    pg.mouse.move(c["cx"], c["cy"]); pg.mouse.down(); pg.mouse.move(ctr[0], ctr[1], steps=8); pg.mouse.up(); pg.wait_for_timeout(60)
    n = pg.evaluate("document.querySelectorAll('#layerTop .pulse circle').length")
    home = pg.evaluate("pieces.find(p=>p.id==='ST1').st")
    check("mini: a missed drop goes home and the outline corners pulse once", home == "tray" and n == pg.evaluate("anchorsStatic.length") and n > 0, f"{n} corners, {home}")
    pg.wait_for_timeout(700)
    check("mini: the pulse is gone after 650 ms", pg.evaluate("document.querySelectorAll('#layerTop .pulse').length") == 0)
    load(pg, by_id("shapes-warmup-1"))
    c = pg.evaluate("L.cells['ST1']")
    pg.mouse.move(c["cx"], c["cy"]); pg.mouse.down(); pg.mouse.move(ctr[0], ctr[1], steps=8); pg.mouse.up(); pg.wait_for_timeout(60)
    check("mini: a warm-up shows no pulse", pg.evaluate("document.querySelectorAll('#layerTop .pulse').length") == 0 and not errs, str(errs))
    pg.close()


def test_settings(browser):
    pg, errs = new_page(browser, 390, 844)
    load(pg, by_id("animals-cat"))
    pg.click("#gearBtn"); pg.wait_for_timeout(100)
    no_diff = pg.evaluate("!document.querySelector('#diffSeg') && !byId('settings').textContent.match(/Easy|Medium|Hard/)")
    timer_off = pg.evaluate("settings.timer === 'off' && byId('timer').hidden")
    order_ok = pg.evaluate("(()=>{const f=byId('freeNote'), p=byId('privacyNote'); return f.nextElementSibling === p && p.textContent.startsWith('Privacy:')})()")
    check("settings: no difficulty control", no_diff)
    check("settings: timer off by default", timer_off)
    check("settings: privacy text right below the free note", order_ok)
    pg.click("#timerBtn"); pg.wait_for_timeout(50)
    check("settings: timer switch shows the timer", pg.evaluate("settings.timer === 'on' && !byId('timer').hidden") and not errs, str(errs))
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
        test_nav(b)
        test_lang(b)
        test_load_check(b)
        test_mini_pulse(b)
        test_settings(b)
        test_dev(b)
        test_solve(b)
        b.close()
    print(f"\n{'ALL PASS' if not failures else str(len(failures)) + ' FAILED: ' + ', '.join(failures)}")
    sys.exit(1 if failures else 0)
