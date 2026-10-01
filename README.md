# TangramNoAds

A classic 7-piece **Tangram for players aged 8 and up** on Android phones and tablets, in Finnish and English. The controls are easy and the puzzles are honestly challenging. Store name: **"Tangram, absolutely free"**: no ads, no purchases, no donations, no network.

> Status: requirements capture with a playable prototype (prototype 0.6, 2026-09-28, round 6). Where things stand and what comes next: **`STATUS.md`**. The requirements live in `Requirements/` (SwReqCollector format). Technology choices are parked.

## Try it
Open **`Spec/prototype/tangram-prototype.html`** in a browser, ideally on a phone and a tablet.
- Drag pieces onto the silhouette. A piece locks to the shape's corners or to pieces already placed, or goes back to the tray.
- **Tap** a piece to turn it 45°, in the tray or on the board. S / M / L on the triangles tells their size. **Hold a piece and twist a second finger** to turn in 45° steps. The **⇋** badge mirrors the parallelogram. On a computer, use the mouse wheel or **R** while dragging.
- Stuck, or checking that a puzzle works? The dashed **DEV** button on the board (passcode 0417) shows the solution. Testing aid only.
- **‹ ›** to browse and skip (the list wraps). **Hold ›** to jump to the next unsolved puzzle; press the **counter** in the top bar for a grid of all puzzles. ⚙ for the timer, sound, play-time stats, the free note and the privacy text. The page is in Finnish when the browser is.

## Folder map
| Path | What |
|---|---|
| `Study/01–04` | Round 0 research: market scan, touch UX research, tangram geometry, first design ideas |
| `Study/05-round1-feedback.md` | Jami's round-1 ideas and how the design answers them |
| `Study/06-snapping-by-anchors.md` | Slot-free locking: how a dropped piece finds its place |
| `Study/07-play-time-and-donations.md` | Active play-time tracking (donations dropped, with the reason) |
| `Study/08-round2-decisions.md` | Round 2: outline-corner anchors only, no donations |
| `Study/archive/` | Draft 0.1 spec, drawings and tools; removed puzzles |
| `Spec/00–05` | Vision, gameplay, UI layout, puzzle format, requirements, open questions |
| `Spec/ui/` | Annotated screenshots of the prototype plus the locking diagram |
| `Tangrams/` | The puzzle library: 13 puzzles (2 mini 3-piece, 4 warm-ups, 5 figures, the rectangle and the classic square), JSON Schema, review sheets |
| `Requirements/` | The requirement collection in SwReqCollector format (ai-led capture) |
| `Spec/prototype/` | The playable prototype (generated) |
| `tools/` | Geometry reference, validator, preview renderer, prototype builder, UI sheet generator |
| `tools/tests/` | Automated browser tests for the prototype |
| `tools/puzzle_search/` | The searches that found the warm-ups and the rectangle |
| `STATUS.md` | Where the project stands, round history, next steps, handoff readiness |
| `AGENTS.md`, `CLAUDE.md` | Rules for AI agents working in this repo (CLAUDE.md points to AGENTS.md) |

## Tools
```bash
python tools/validate_puzzles.py Tangrams                      # rules V1–V6, V8–V10, V12 + the V11 build order, exit 1 on failure (no dependencies)
python tools/render_puzzle.py Tangrams Tangrams/previews   # silhouette | pieces | picture sheets (SVG)
python tools/build_prototype.py                                    # rebuild the HTML prototype
python tools/make_ui_sheets.py                                     # Spec/ui/*.png from the prototype (needs Playwright + Chromium)
python tools/svg2png.py Tangrams/previews/*.svg                    # preview sheets as PNG (Playwright)
python tools/tests/test_prototype.py [--fast]                      # browser tests: every puzzle solves, tray, turning, DEV aid
```

## Licence
GPL-3.0 (see `LICENSE`).
